package com.moneymoment.lending.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.moneymoment.lending.common.enums.AiImageType;
import com.moneymoment.lending.common.enums.AiMessageRole;
import com.moneymoment.lending.common.enums.AiSessionStatus;
import com.moneymoment.lending.dtos.AiChatResponseDto;
import com.moneymoment.lending.dtos.CustomerRequestDto;
import com.moneymoment.lending.dtos.CustomerResponseDto;
import com.moneymoment.lending.dtos.KycSummaryDto;
import com.moneymoment.lending.dtos.LoanRequestDto;
import com.moneymoment.lending.dtos.LoanResponseDto;
import com.moneymoment.lending.entities.AiChatMessageEntity;
import com.moneymoment.lending.entities.AiChatSessionEntity;
import com.moneymoment.lending.entities.AiKycExtractionEntity;
import com.moneymoment.lending.master.MasterService;
import com.moneymoment.lending.repos.AiChatMessageRepository;
import com.moneymoment.lending.repos.AiChatSessionRepository;
import com.moneymoment.lending.repos.AiKycExtractionRepository;
import com.moneymoment.lending.repos.CustomerRepository;
import com.moneymoment.lending.repos.LoanRepo;
import com.moneymoment.lending.repos.UserRepository;

@Service
public class AiChatService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.model:gemini-1.5-flash}")
    private String model;

    private final AiChatSessionRepository sessionRepo;
    private final AiChatMessageRepository messageRepo;
    private final AiKycExtractionRepository kycRepo;
    private final AiKycService kycService;
    private final CustomerService customerService;
    private final LoanService loanService;
    private final UserRepository userRepo;
    private final CustomerRepository customerRepo;
    private final LoanRepo loanRepo;
    private final MasterService masterService;
    private final RestTemplate restTemplate;

    AiChatService(AiChatSessionRepository sessionRepo, AiChatMessageRepository messageRepo,
            AiKycExtractionRepository kycRepo, AiKycService kycService,
            CustomerService customerService, LoanService loanService,
            UserRepository userRepo, CustomerRepository customerRepo,
            LoanRepo loanRepo, MasterService masterService) {
        this.sessionRepo = sessionRepo;
        this.messageRepo = messageRepo;
        this.kycRepo = kycRepo;
        this.kycService = kycService;
        this.customerService = customerService;
        this.loanService = loanService;
        this.userRepo = userRepo;
        this.customerRepo = customerRepo;
        this.loanRepo = loanRepo;
        this.masterService = masterService;
        this.restTemplate = new RestTemplate();
    }

    // ─── Public entry point ───────────────────────────────────────────────────

    public AiChatResponseDto chat(String sessionId, String userMessage,
            MultipartFile panImage, MultipartFile aadhaarImage, String username) {

        // Phase 1: persist incoming messages (short DB transaction)
        ChatContext ctx = persistIncoming(sessionId, userMessage, panImage, aadhaarImage, username);

        // Phase 2: call Gemini API (outside transaction)
        String assistantReply = callGemini(ctx.session, ctx.history, username);

        // Phase 3: persist reply and build response (short DB transaction)
        return persistReply(ctx, assistantReply);
    }

    // ─── Phase 1 ──────────────────────────────────────────────────────────────

    @Transactional
    protected ChatContext persistIncoming(String sessionId, String userMessage,
            MultipartFile panImage, MultipartFile aadhaarImage, String username) {

        AiChatSessionEntity session = loadOrCreateSession(sessionId, username);
        KycSummaryDto kycSummary = null;

        boolean hasPan = panImage != null && !panImage.isEmpty();
        boolean hasAadhaar = aadhaarImage != null && !aadhaarImage.isEmpty();

        if (hasPan || hasAadhaar) {
            AiKycExtractionEntity kyc = kycService.processDocuments(session, panImage, aadhaarImage);
            kycSummary = toKycSummaryDto(kyc);
            String kycMsg = buildKycContextMessage(kyc, hasPan, hasAadhaar);
            saveMessage(session, AiMessageRole.USER, kycMsg, hasPan ? AiImageType.PAN : AiImageType.AADHAAR);
        }

        if (userMessage != null && !userMessage.isBlank()) {
            saveMessage(session, AiMessageRole.USER, userMessage, null);
        }

        // Nothing sent at all → kick off the conversation
        if ((userMessage == null || userMessage.isBlank()) && !hasPan && !hasAadhaar) {
            saveMessage(session, AiMessageRole.USER, "Hi, I want to create a new customer.", null);
        }

        List<AiChatMessageEntity> history = messageRepo.findBySessionOrderByCreatedAtAsc(session);
        return new ChatContext(session, history, kycSummary);
    }

    // ─── Phase 3 ──────────────────────────────────────────────────────────────

    @Transactional
    protected AiChatResponseDto persistReply(ChatContext ctx, String assistantReply) {
        AiChatSessionEntity session = sessionRepo.findById(ctx.session.getId()).orElse(ctx.session);
        saveMessage(session, AiMessageRole.ASSISTANT, assistantReply, null);

        AiChatResponseDto response = new AiChatResponseDto();
        response.setSessionId(session.getSessionUuid());
        response.setSessionStatus(session.getStatus().name());
        response.setReply(assistantReply);
        response.setKycSummary(ctx.kycSummary);

        if (session.getCreatedCustomer() != null) {
            customerRepo.findById(session.getCreatedCustomer().getId()).ifPresent(c -> {
                response.setCreatedCustomerId(c.getId());
                response.setCreatedCustomerNumber(c.getCustomerNumber());
                response.setCreatedCustomerName(c.getName());
            });
        }
        if (session.getCreatedLoan() != null) {
            loanRepo.findById(session.getCreatedLoan().getId()).ifPresent(l -> {
                response.setCreatedLoanId(l.getId());
                response.setCreatedLoanNumber(l.getLoanNumber());
            });
        }
        return response;
    }

    // ─── Gemini API ───────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private String callGemini(AiChatSessionEntity session, List<AiChatMessageEntity> history, String username) {
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + model + ":generateContent?key=" + apiKey;

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("system_instruction", Map.of("parts", List.of(Map.of("text", buildSystemPrompt()))));
            requestBody.put("contents", buildGeminiMessages(history));
            requestBody.put("tools", List.of(Map.of("function_declarations", buildFunctionDeclarations())));
            requestBody.put("generationConfig", Map.of("maxOutputTokens", 2048, "temperature", 0.7));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.postForEntity(
                    url, entity, (Class<Map<String, Object>>) (Class<?>) Map.class);

            Map<String, Object> body = response.getBody();
            if (body == null) return "Sorry, I couldn't process that. Please try again.";

            // Check if Gemini called a function
            String functionName = extractFunctionName(body);
            if (functionName != null) {
                Map<String, Object> functionArgs = extractFunctionArgs(body);
                return handleFunctionCall(functionName, functionArgs, body, session, history, username, url);
            }

            return extractTextFromGeminiResponse(body);

        } catch (Exception e) {
            return "I'm having some trouble right now. Please try again in a moment.";
        }
    }

    @SuppressWarnings("unchecked")
    private String handleFunctionCall(String functionName, Map<String, Object> args,
            Map<String, Object> geminiResponse, AiChatSessionEntity session,
            List<AiChatMessageEntity> history, String username, String url) {
        try {
            // Execute the function
            String result = executeFunction(functionName, args, session, username);

            // Build follow-up request: original history + model's function call + function result
            List<Map<String, Object>> updatedContents = new ArrayList<>(buildGeminiMessages(history));

            // Model's function call turn
            Map<String, Object> functionCallPart = new HashMap<>();
            functionCallPart.put("functionCall", Map.of("name", functionName, "args", args));
            updatedContents.add(Map.of("role", "model", "parts", List.of(functionCallPart)));

            // User's function response turn
            Map<String, Object> functionResponse = new HashMap<>();
            functionResponse.put("name", functionName);
            functionResponse.put("response", Map.of("result", result));
            Map<String, Object> functionResponsePart = new HashMap<>();
            functionResponsePart.put("functionResponse", functionResponse);
            updatedContents.add(Map.of("role", "user", "parts", List.of(functionResponsePart)));

            Map<String, Object> followUp = new HashMap<>();
            followUp.put("system_instruction", Map.of("parts", List.of(Map.of("text", buildSystemPrompt()))));
            followUp.put("contents", updatedContents);
            followUp.put("generationConfig", Map.of("maxOutputTokens", 1024, "temperature", 0.7));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(followUp, headers);

            ResponseEntity<Map<String, Object>> followUpResponse = restTemplate.postForEntity(
                    url, entity, (Class<Map<String, Object>>) (Class<?>) Map.class);

            return extractTextFromGeminiResponse(followUpResponse.getBody());

        } catch (Exception e) {
            return "There was an error processing your request: " + e.getMessage();
        }
    }

    private String executeFunction(String name, Map<String, Object> args,
            AiChatSessionEntity session, String username) {
        try {
            if ("create_customer".equals(name)) return executeCreateCustomer(args, session, username);
            if ("create_loan".equals(name)) return executeCreateLoan(args, session, username);
            return "Unknown function: " + name;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Transactional
    protected String executeCreateCustomer(Map<String, Object> input,
            AiChatSessionEntity session, String username) {
        CustomerRequestDto dto = new CustomerRequestDto();

        // PAN and Aadhaar come from KYC extraction in DB — not from AI
        AiKycExtractionEntity kyc = kycRepo.findBySession(session).orElse(null);
        if (kyc != null) {
            dto.setPan(kyc.getPanNumber());
            dto.setAadhar(kyc.getAadhaarNumber());
            if (kyc.getPanName() != null && !kyc.getPanName().isEmpty()) {
                dto.setName(kyc.getPanName());
            }
        }

        if (input.get("name") != null) dto.setName(input.get("name").toString());
        if (input.get("phone") != null) dto.setPhone(input.get("phone").toString());
        if (input.get("email") != null) dto.setEmail(input.get("email").toString());
        if (input.get("address") != null) dto.setAddress(input.get("address").toString());
        if (input.get("occupation") != null) dto.setOccupation(input.get("occupation").toString());
        if (input.get("monthlySalary") != null) {
            dto.setMonthlySalary(Double.parseDouble(input.get("monthlySalary").toString()));
        }
        if (input.get("employmentType") != null) {
            try {
                dto.setEmploymentType(
                    com.moneymoment.lending.common.enums.EmploymentType
                        .valueOf(input.get("employmentType").toString().toUpperCase()));
            } catch (Exception ignored) {}
        }
        if (input.get("dob") != null) {
            try {
                dto.setDob(LocalDateTime.parse(input.get("dob").toString()));
            } catch (Exception ignored) {}
        }
        dto.setCreatedBy(username);

        CustomerResponseDto created = customerService.createCustomer(dto);
        session.setCreatedCustomer(customerRepo.getReferenceById(created.getId()));
        session.setStatus(AiSessionStatus.ACTIVE);
        sessionRepo.save(session);

        return String.format("Customer created! Customer Number: %s, Name: %s, ID: %d",
                created.getCustomerNumber(), created.getName(), created.getId());
    }

    @Transactional
    protected String executeCreateLoan(Map<String, Object> input,
            AiChatSessionEntity session, String username) {
        LoanRequestDto dto = new LoanRequestDto();

        if (input.get("customerId") != null) {
            dto.setCustomerId(Long.parseLong(input.get("customerId").toString()));
        } else if (session.getCreatedCustomer() != null) {
            dto.setCustomerId(session.getCreatedCustomer().getId());
        }
        if (input.get("loanTypeCode") != null) dto.setLoanTypeCode(input.get("loanTypeCode").toString());
        if (input.get("loanPurposeCode") != null) dto.setLoanPurposeCode(input.get("loanPurposeCode").toString());
        if (input.get("purpose") != null) dto.setPurpose(input.get("purpose").toString());
        if (input.get("loanAmount") != null) dto.setLoanAmount(Double.parseDouble(input.get("loanAmount").toString()));
        if (input.get("tenureMonths") != null) dto.setTenureMonths(Integer.parseInt(input.get("tenureMonths").toString()));
        if (input.get("disbursementAccountNumber") != null) dto.setDisbursementAccountNumber(input.get("disbursementAccountNumber").toString());
        if (input.get("disbursementIfsc") != null) dto.setDisbursementIfsc(input.get("disbursementIfsc").toString());
        dto.setCreatedBy(username);

        LoanResponseDto created = loanService.createLoan(dto);
        session.setCreatedLoan(loanRepo.getReferenceById(created.getId()));
        session.setStatus(AiSessionStatus.COMPLETED);
        sessionRepo.save(session);

        return String.format("Loan created! Loan Number: %s, Amount: ₹%.0f, EMI: ₹%.0f/month, Tenure: %d months.",
                created.getLoanNumber(), created.getLoanAmount(),
                created.getEmiAmount(), created.getTenureMonths());
    }

    // ─── Message Builders ─────────────────────────────────────────────────────

    /**
     * Converts DB history to Gemini message format.
     * Merges consecutive same-role messages (Gemini requires alternating roles).
     */
    private List<Map<String, Object>> buildGeminiMessages(List<AiChatMessageEntity> history) {
        List<Map<String, Object>> messages = new ArrayList<>();

        String currentRole = null;
        StringBuilder currentText = new StringBuilder();

        for (AiChatMessageEntity msg : history) {
            String role = msg.getRole() == AiMessageRole.USER ? "user" : "model";
            if (role.equals(currentRole)) {
                currentText.append("\n").append(msg.getContent());
            } else {
                if (currentRole != null) {
                    messages.add(makeTextMessage(currentRole, currentText.toString()));
                }
                currentRole = role;
                currentText = new StringBuilder(msg.getContent());
            }
        }
        if (currentRole != null) {
            messages.add(makeTextMessage(currentRole, currentText.toString()));
        }

        return messages;
    }

    private Map<String, Object> makeTextMessage(String role, String text) {
        return Map.of("role", role, "parts", List.of(Map.of("text", text)));
    }

    private void saveMessage(AiChatSessionEntity session, AiMessageRole role,
            String content, AiImageType imageType) {
        AiChatMessageEntity msg = new AiChatMessageEntity();
        msg.setSession(session);
        msg.setRole(role);
        msg.setContent(content);
        msg.setImageType(imageType);
        messageRepo.save(msg);
    }

    // ─── Gemini Response Parsers ──────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private String extractTextFromGeminiResponse(Map<String, Object> body) {
        if (body == null) return "Sorry, something went wrong.";
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");
            if (candidates == null || candidates.isEmpty()) return "Sorry, I couldn't generate a response.";
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            StringBuilder sb = new StringBuilder();
            for (Map<String, Object> part : parts) {
                if (part.containsKey("text")) sb.append(part.get("text"));
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return "Sorry, I couldn't parse the response.";
        }
    }

    @SuppressWarnings("unchecked")
    private String extractFunctionName(Map<String, Object> body) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");
            if (candidates == null || candidates.isEmpty()) return null;
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            for (Map<String, Object> part : parts) {
                if (part.containsKey("functionCall")) {
                    Map<String, Object> fc = (Map<String, Object>) part.get("functionCall");
                    return (String) fc.get("name");
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractFunctionArgs(Map<String, Object> body) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            for (Map<String, Object> part : parts) {
                if (part.containsKey("functionCall")) {
                    Map<String, Object> fc = (Map<String, Object>) part.get("functionCall");
                    return (Map<String, Object>) fc.get("args");
                }
            }
        } catch (Exception ignored) {}
        return new HashMap<>();
    }

    // ─── Function Declarations ────────────────────────────────────────────────

    private List<Map<String, Object>> buildFunctionDeclarations() {
        List<Map<String, Object>> functions = new ArrayList<>();

        // create_customer
        Map<String, Object> custProps = new HashMap<>();
        custProps.put("name", Map.of("type", "STRING", "description", "Full name of the customer"));
        custProps.put("phone", Map.of("type", "STRING", "description", "10-digit mobile number"));
        custProps.put("email", Map.of("type", "STRING", "description", "Email address"));
        custProps.put("dob", Map.of("type", "STRING", "description", "Date of birth in ISO format YYYY-MM-DDTHH:mm:ss"));
        custProps.put("address", Map.of("type", "STRING", "description", "Full residential address"));
        custProps.put("employmentType", Map.of("type", "STRING", "description", "SALARIED or SELF_EMPLOYED"));
        custProps.put("occupation", Map.of("type", "STRING", "description", "Job title or employer name"));
        custProps.put("monthlySalary", Map.of("type", "NUMBER", "description", "Monthly income in INR"));

        Map<String, Object> custFunc = new HashMap<>();
        custFunc.put("name", "create_customer");
        custFunc.put("description", "Creates a new customer. Call ONLY after all fields are collected and user confirms.");
        custFunc.put("parameters", Map.of(
                "type", "OBJECT",
                "properties", custProps,
                "required", List.of("name", "phone", "email", "employmentType")));
        functions.add(custFunc);

        // create_loan
        Map<String, Object> loanProps = new HashMap<>();
        loanProps.put("customerId", Map.of("type", "NUMBER", "description", "Internal customer ID"));
        loanProps.put("loanTypeCode", Map.of("type", "STRING", "description", "Loan type code e.g. PL, HL, BL, EL, VL"));
        loanProps.put("loanPurposeCode", Map.of("type", "STRING", "description", "Purpose code e.g. MEDICAL, HOME_PURCHASE, BUSINESS_EXPANSION, EDUCATION, VEHICLE_PURCHASE, PERSONAL"));
        loanProps.put("purpose", Map.of("type", "STRING", "description", "Brief loan purpose description"));
        loanProps.put("loanAmount", Map.of("type", "NUMBER", "description", "Loan amount in INR"));
        loanProps.put("tenureMonths", Map.of("type", "NUMBER", "description", "Loan tenure in months"));
        loanProps.put("disbursementAccountNumber", Map.of("type", "STRING", "description", "Bank account number"));
        loanProps.put("disbursementIfsc", Map.of("type", "STRING", "description", "IFSC code"));

        Map<String, Object> loanFunc = new HashMap<>();
        loanFunc.put("name", "create_loan");
        loanFunc.put("description", "Creates a loan application. Call ONLY after all fields are collected and user confirms.");
        loanFunc.put("parameters", Map.of(
                "type", "OBJECT",
                "properties", loanProps,
                "required", List.of("customerId", "loanTypeCode", "loanPurposeCode", "loanAmount", "tenureMonths")));
        functions.add(loanFunc);

        return functions;
    }

    // ─── System Prompt ────────────────────────────────────────────────────────

    private String buildSystemPrompt() {
        String loanTypes = masterService.getAllLoanTypes().stream()
                .map(t -> t.getCode() + " (" + t.getName() + ")")
                .collect(Collectors.joining(", "));

        String loanPurposes = masterService.getAllLoanPurposes().stream()
                .map(p -> p.getCode() + " (" + p.getName() + ")")
                .collect(Collectors.joining(", "));

        return """
                You are a KYC and onboarding assistant for FinPulse, a lending management system.
                Your job: collect required information through friendly conversation, then create customers and loan applications.

                === CUSTOMER CREATION FLOW ===
                Step 1: Ask the user to upload the PAN card image.
                Step 2: Ask the user to upload the Aadhaar card image.
                        (KYC extraction results are injected automatically — you don't extract from images yourself.)
                Step 3: After KYC results arrive, check if names match. If MISMATCH, inform user and ask to re-upload.
                Step 4: Collect remaining fields conversationally:
                        - Phone number (10 digits)
                        - Email address
                        - Date of birth (ask in DD/MM/YYYY, convert to YYYY-MM-DDTHH:mm:ss for the tool)
                        - Address
                Step 5: Collect employment details:
                        - Employment type: SALARIED or SELF_EMPLOYED
                        - Occupation / employer name
                        - Monthly income in INR
                Step 6: Show a summary of ALL collected details and ask user to confirm (yes/no).
                Step 7: On confirmation, call create_customer.

                === LOAN CREATION FLOW ===
                Step 1: Ask for loan type. Available: """ + loanTypes + """

                Step 2: Ask for loan amount.
                Step 3: Ask for tenure in months.
                Step 4: Ask for loan purpose. Available purposes: """ + loanPurposes + """

                Step 5: Ask for bank account number and IFSC.
                Step 6: Show summary and ask for confirmation.
                Step 7: On confirmation, call create_loan.

                === RULES ===
                - Be friendly and conversational. Ask one thing at a time.
                - Extract multiple fields from a single reply when possible.
                  Example: "salaried at TCS, 80k/month" → employmentType=SALARIED, occupation=TCS, monthlySalary=80000
                - Convert shorthand: 80k=80000, 5L=500000, 1Cr=10000000
                - For DOB, accept DD/MM/YYYY and convert to ISO format.
                - NEVER ask for PAN or Aadhaar numbers manually — they come from document images only.
                - Do NOT call create_customer or create_loan until the user explicitly confirms.
                - After creating a customer, ask if they want to create a loan too.
                - Keep responses concise. Use bullet points for summaries.
                """;
    }

    // ─── Session & KYC helpers ────────────────────────────────────────────────

    private AiChatSessionEntity loadOrCreateSession(String sessionId, String username) {
        if (sessionId != null && !sessionId.isBlank()) {
            var existing = sessionRepo.findBySessionUuid(sessionId);
            if (existing.isPresent()) {
                AiChatSessionEntity s = existing.get();
                if (s.getExpiresAt() != null && s.getExpiresAt().isBefore(LocalDateTime.now())) {
                    s.setStatus(AiSessionStatus.EXPIRED);
                    sessionRepo.save(s);
                }
                if (s.getStatus() == AiSessionStatus.ACTIVE) return s;
            }
        }
        AiChatSessionEntity session = new AiChatSessionEntity();
        session.setSessionUuid(sessionId != null && !sessionId.isBlank()
                ? sessionId : java.util.UUID.randomUUID().toString());
        userRepo.findByUsername(username).ifPresent(session::setUser);
        return sessionRepo.save(session);
    }

    private String buildKycContextMessage(AiKycExtractionEntity kyc, boolean hasPan, boolean hasAadhaar) {
        StringBuilder sb = new StringBuilder("[KYC Document Processing Result]\n");
        if (hasPan) {
            sb.append("PAN Card scanned:\n");
            sb.append("  - PAN Number: ").append(nonEmpty(kyc.getPanNumber(), "Not detected")).append("\n");
            sb.append("  - Name on PAN: ").append(nonEmpty(kyc.getPanName(), "Not detected")).append("\n");
        }
        if (hasAadhaar) {
            sb.append("Aadhaar Card scanned:\n");
            sb.append("  - Aadhaar Number: ").append(nonEmpty(kyc.getAadhaarNumber(), "Not detected")).append("\n");
            sb.append("  - Name on Aadhaar: ").append(nonEmpty(kyc.getAadhaarName(), "Not detected")).append("\n");
        }
        if (kyc.getNameMatched() != null) {
            sb.append("Name match: ").append(kyc.getNameMatched() ? "VERIFIED" : "MISMATCH")
              .append(" (score: ").append(String.format("%.0f%%", kyc.getMatchScore() * 100)).append(")");
        }
        return sb.toString();
    }

    private KycSummaryDto toKycSummaryDto(AiKycExtractionEntity kyc) {
        KycSummaryDto dto = new KycSummaryDto();
        dto.setPanNumber(kyc.getPanNumber());
        dto.setPanName(kyc.getPanName());
        dto.setAadhaarNumber(kyc.getAadhaarNumber());
        dto.setAadhaarName(kyc.getAadhaarName());
        dto.setNameMatched(kyc.getNameMatched());
        dto.setMatchScore(kyc.getMatchScore());
        return dto;
    }

    private String nonEmpty(String value, String fallback) {
        return (value != null && !value.isEmpty()) ? value : fallback;
    }

    // ─── Internal context carrier ─────────────────────────────────────────────

    static class ChatContext {
        final AiChatSessionEntity session;
        final List<AiChatMessageEntity> history;
        final KycSummaryDto kycSummary;
        ChatContext(AiChatSessionEntity s, List<AiChatMessageEntity> h, KycSummaryDto k) {
            session = s; history = h; kycSummary = k;
        }
    }
}
