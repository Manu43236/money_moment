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

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.model:gemini-1.5-flash}")
    private String model;

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

            String prompt = docType.equals("PAN")
                    ? "Extract the PAN number (10-character alphanumeric like ABCDE1234F) and the full name from this PAN card image. Respond ONLY with valid JSON: {\"number\": \"<pan>\", \"name\": \"<FULL NAME IN UPPERCASE>\"}"
                    : "Extract the Aadhaar number (12 digits, remove spaces) and the full name from this Aadhaar card image. Respond ONLY with valid JSON: {\"number\": \"<12digits>\", \"name\": \"<FULL NAME IN UPPERCASE>\"}";

            // Build Gemini request with inline image
            Map<String, Object> inlineData = new HashMap<>();
            inlineData.put("mimeType", mimeType);
            inlineData.put("data", base64);

            Map<String, Object> imagePart = new HashMap<>();
            imagePart.put("inlineData", inlineData);

            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", prompt);

            List<Object> parts = new ArrayList<>();
            parts.add(imagePart);
            parts.add(textPart);

            Map<String, Object> content = new HashMap<>();
            content.put("parts", parts);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", List.of(content));
            requestBody.put("generationConfig", Map.of("maxOutputTokens", 256, "temperature", 0.0));

            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + model + ":generateContent?key=" + apiKey;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.postForEntity(
                    url, entity, (Class<Map<String, Object>>) (Class<?>) Map.class);

            String text = extractTextFromGeminiResponse(response.getBody());
            text = text.replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```\\s*", "").trim();

            JsonNode json = objectMapper.readTree(text);
            if (json.has("number")) result.put("number", json.get("number").asText().replaceAll("\\s+", ""));
            if (json.has("name")) result.put("name", json.get("name").asText().toUpperCase().trim());

        } catch (Exception e) {
            // Return empty on failure — chat will ask user to re-upload
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private String extractTextFromGeminiResponse(Map<String, Object> body) {
        if (body == null) return "{}";
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");
            if (candidates == null || candidates.isEmpty()) return "{}";
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            if (parts == null || parts.isEmpty()) return "{}";
            Object text = parts.get(0).get("text");
            return text != null ? text.toString() : "{}";
        } catch (Exception e) {
            return "{}";
        }
    }

    private boolean isNonEmpty(String s) {
        return s != null && !s.isEmpty();
    }

    /**
     * Fuzzy name match — handles initials (e.g. "R SHARMA" vs "RAHUL SHARMA").
     */
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
