package com.moneymoment.lending.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneymoment.lending.entities.AiChatSessionEntity;
import com.moneymoment.lending.entities.AiKycExtractionEntity;
import com.moneymoment.lending.repos.AiKycExtractionRepository;

@Service
public class AiKycService {

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.model.vision:llama-3.2-11b-vision-preview}")
    private String visionModel;

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    private final AiKycExtractionRepository kycRepo;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    AiKycService(AiKycExtractionRepository kycRepo) {
        this.kycRepo = kycRepo;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public AiKycExtractionEntity processDocuments(AiChatSessionEntity session,
            MultipartFile panImage, MultipartFile aadhaarImage) {

        AiKycExtractionEntity kyc = kycRepo.findBySession(session)
                .orElse(new AiKycExtractionEntity());
        kyc.setSession(session);

        if (panImage != null && !panImage.isEmpty()) {
            Map<String, String> extracted = extractFromDocument(panImage, "PAN");
            kyc.setPanNumber(extracted.get("number"));
            kyc.setPanName(extracted.get("name"));
        }

        if (aadhaarImage != null && !aadhaarImage.isEmpty()) {
            Map<String, String> extracted = extractFromDocument(aadhaarImage, "AADHAAR");
            kyc.setAadhaarNumber(extracted.get("number"));
            kyc.setAadhaarName(extracted.get("name"));
        }

        if (isNonEmpty(kyc.getPanName()) && isNonEmpty(kyc.getAadhaarName())) {
            double score = calculateNameMatchScore(kyc.getPanName(), kyc.getAadhaarName());
            kyc.setMatchScore(Math.round(score * 100.0) / 100.0);
            kyc.setNameMatched(score >= 0.70);
            kyc.setVerifiedAt(LocalDateTime.now());
        }

        return kycRepo.save(kyc);
    }

    private Map<String, String> extractFromDocument(MultipartFile file, String docType) {
        Map<String, String> result = new HashMap<>();
        result.put("number", "");
        result.put("name", "");

        try {
            String base64 = Base64.getEncoder().encodeToString(file.getBytes());
            String mimeType = file.getContentType() != null ? file.getContentType() : "image/jpeg";
            String dataUrl = "data:" + mimeType + ";base64," + base64;

            String prompt = docType.equals("PAN")
                    ? "Extract the PAN number (10-character alphanumeric like ABCDE1234F) and the full name from this PAN card. Respond ONLY with valid JSON: {\"number\": \"<pan>\", \"name\": \"<FULL NAME IN UPPERCASE>\"}"
                    : "Extract the Aadhaar number (12 digits, remove any spaces) and the full name from this Aadhaar card. Respond ONLY with valid JSON: {\"number\": \"<12digits>\", \"name\": \"<FULL NAME IN UPPERCASE>\"}";

            // OpenAI vision format with image_url
            Map<String, Object> imageContent = new HashMap<>();
            imageContent.put("type", "image_url");
            imageContent.put("image_url", Map.of("url", dataUrl));

            Map<String, Object> textContent = new HashMap<>();
            textContent.put("type", "text");
            textContent.put("text", prompt);

            List<Object> contentParts = new ArrayList<>();
            contentParts.add(imageContent);
            contentParts.add(textContent);

            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", contentParts);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", visionModel);
            requestBody.put("messages", List.of(userMessage));
            requestBody.put("max_tokens", 256);
            requestBody.put("temperature", 0);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map<String, Object>> response = restTemplate.postForEntity(
                    GROQ_URL, entity, (Class<Map<String, Object>>) (Class<?>) Map.class);

            String text = extractTextFromResponse(response.getBody());
            text = text.replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```\\s*", "").trim();

            JsonNode json = objectMapper.readTree(text);
            if (json.has("number")) result.put("number", json.get("number").asText().replaceAll("\\s+", ""));
            if (json.has("name")) result.put("name", json.get("name").asText().toUpperCase().trim());

        } catch (Exception e) {
            System.err.println("[KYC ERROR] " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private String extractTextFromResponse(Map<String, Object> body) {
        if (body == null) return "{}";
        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
            if (choices == null || choices.isEmpty()) return "{}";
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            Object content = message.get("content");
            return content != null ? content.toString() : "{}";
        } catch (Exception e) {
            return "{}";
        }
    }

    private boolean isNonEmpty(String s) {
        return s != null && !s.isEmpty();
    }

    private double calculateNameMatchScore(String name1, String name2) {
        String n1 = name1.toUpperCase().trim();
        String n2 = name2.toUpperCase().trim();
        if (n1.equals(n2)) return 1.0;

        String[] words1 = n1.split("\\s+");
        String[] words2 = n2.split("\\s+");
        int matches = 0;
        for (String w1 : words1) {
            for (String w2 : words2) {
                if (w1.equals(w2)) { matches++; break; }
                if (w1.length() == 1 && w2.startsWith(w1)) { matches++; break; }
                if (w2.length() == 1 && w1.startsWith(w2)) { matches++; break; }
            }
        }
        int minWords = Math.min(words1.length, words2.length);
        return minWords > 0 ? (double) matches / minWords : 0.0;
    }
}
