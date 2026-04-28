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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneymoment.lending.common.enums.AiMessageRole;
import com.moneymoment.lending.common.enums.AiSessionStatus;
import com.moneymoment.lending.dtos.AiChatResponseDto;
import com.moneymoment.lending.dtos.CustomerRequestDto;
import com.moneymoment.lending.dtos.CustomerResponseDto;
import com.moneymoment.lending.dtos.LoanRequestDto;
import com.moneymoment.lending.dtos.LoanResponseDto;
import com.moneymoment.lending.entities.AiChatMessageEntity;
import com.moneymoment.lending.entities.AiChatSessionEntity;
import com.moneymoment.lending.master.MasterService;
import com.moneymoment.lending.repos.AiChatMessageRepository;
import com.moneymoment.lending.repos.AiChatSessionRepository;
import com.moneymoment.lending.repos.CustomerRepository;
import com.moneymoment.lending.repos.LoanRepo;
import com.moneymoment.lending.repos.UserRepository;

@Service
public class AiChatService {

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.model.chat:llama-3.3-70b-versatile}")
    private String chatModel;

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    private final AiChatSessionRepository sessionRepo;
    private final AiChatMessageRepository messageRepo;
    private final CustomerService customerService;
    private final LoanService loanService;
    private final UserRepository userRepo;
    private final CustomerRepository customerRepo;
    private final LoanRepo loanRepo;
    private final MasterService masterService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    AiChatService(AiChatSessionRepository sessionRepo, AiChatMessageRepository messageRepo,
            CustomerService customerService, LoanService loanService,
            UserRepository userRepo, CustomerRepository customerRepo,
            LoanRepo loanRepo, MasterService masterService) {
        this.sessionRepo = sessionRepo;
        this.messageRepo = messageRepo;
        this.customerService = customerService;
        this.loanService = loanService;
        this.userRepo = userRepo;
        this.customerRepo = customerRepo;
        this.loanRepo = loanRepo;
        this.masterService = masterService;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    // ─── Public entry point ───────────────────────────────────────────────────

    public AiChatResponseDto chat(String sessionId, String userMessage, String username) {

        ChatContext ctx = persistIncoming(sessionId, userMessage, username);
        String assistantReply = callGroq(ctx.session, ctx.history, username);
        return persistReply(ctx, assistantReply);
    }

    // ─── Phase 1: save incoming ───────────────────────────────────────────────

    @Transactional
    protected ChatContext persistIncoming(String sessionId, String userMessage, String username) {

        AiChatSessionEntity session = loadOrCreateSession(sessionId, username);

        if (userMessage != null && !userMessage.isBlank()) {
            saveMessage(session, AiMessageRole.USER, userMessage);
        } else {
            saveMessage(session, AiMessageRole.USER, "Hi, I want to create a new customer.");
        }

        List<AiChatMessageEntity> history = messageRepo.findBySessionOrderByCreatedAtAsc(session);
        return new ChatContext(session, history);
    }

    // ─── Phase 3: save reply and return ──────────────────────────────────────

    @Transactional
    protected AiChatResponseDto persistReply(ChatContext ctx, String assistantReply) {
        AiChatSessionEntity session = sessionRepo.findById(ctx.session.getId()).orElse(ctx.session);

        // Parse structured JSON response from AI
        String displayReply = assistantReply;
        java.util.List<String> options = new java.util.ArrayList<>();
        boolean hideInput = false;
        try {
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(assistantReply);
            if (node.has("message")) {
                displayReply = node.get("message").asText();
                if (node.has("options") && node.get("options").isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode opt : node.get("options")) {
                        options.add(opt.asText());
                    }
                }
                hideInput = node.has("hideInput") && node.get("hideInput").asBoolean();
            }
        } catch (Exception ignored) {
            // Not JSON — use as plain text
        }

        saveMessage(session, AiMessageRole.ASSISTANT, displayReply);

        AiChatResponseDto response = new AiChatResponseDto();
        response.setSessionId(session.getSessionUuid());
        response.setSessionStatus(session.getStatus().name());
        response.setReply(displayReply);
        response.setOptions(options.isEmpty() ? null : options);
        response.setHideInput(hideInput);

        if (session.getCreatedCustomer() != null) {
            customerRepo.findById(session.getCreatedCustomer().getId()).ifPresent(c -> {
                response.setCreatedCustomerId(c.getId());
                response.setCreatedCustomerNumber(c.getCustomerNumber());
                response.setCreatedCustomerName(c.getName());
                response.setCustomerAction(session.getCustomerAction());
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

    // ─── Groq API ─────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private String callGroq(AiChatSessionEntity session, List<AiChatMessageEntity> history, String username) {
        try {
            List<Map<String, Object>> messages = buildMessages(history);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", chatModel);
            requestBody.put("messages", messages);
            requestBody.put("tools", buildTools());
            requestBody.put("tool_choice", "auto");
            requestBody.put("max_tokens", 2048);
            requestBody.put("temperature", 0.7);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map<String, Object>> response = restTemplate.postForEntity(
                    GROQ_URL, entity, (Class<Map<String, Object>>) (Class<?>) Map.class);

            Map<String, Object> body = response.getBody();
            if (body == null) return "Sorry, I couldn't process that.";

            List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
            if (choices == null || choices.isEmpty()) return "Sorry, no response from AI.";

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String finishReason = (String) choices.get(0).get("finish_reason");

            // Tool call
            if ("tool_calls".equals(finishReason)) {
                List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) message.get("tool_calls");
                if (toolCalls != null && !toolCalls.isEmpty()) {
                    return handleToolCall(toolCalls.get(0), message, messages, session, username);
                }
            }

            Object content = message.get("content");
            return content != null ? content.toString().trim() : "{\"message\":\"Sorry, I couldn't generate a response.\",\"options\":[],\"hideInput\":false}";

        } catch (HttpClientErrorException e) {
            System.err.println("[Groq ERROR] " + e.getStatusCode() + " — " + e.getResponseBodyAsString());
            return "{\"message\":\"I'm having some trouble right now. Please try again in a moment.\",\"options\":[],\"hideInput\":false}";
        } catch (Exception e) {
            System.err.println("[Groq ERROR] " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return "{\"message\":\"I'm having some trouble right now. Please try again in a moment.\",\"options\":[],\"hideInput\":false}";
        }
    }

    @SuppressWarnings("unchecked")
    private String handleToolCall(Map<String, Object> toolCall, Map<String, Object> assistantMessage,
            List<Map<String, Object>> messages, AiChatSessionEntity session, String username) {
        try {
            String toolCallId = (String) toolCall.get("id");
            Map<String, Object> function = (Map<String, Object>) toolCall.get("function");
            String functionName = (String) function.get("name");
            String argumentsJson = (String) function.get("arguments");

            Map<String, Object> args = objectMapper.readValue(argumentsJson, Map.class);
            String result = executeFunction(functionName, args, session, username);

            // Build follow-up: history + assistant tool_call message + tool result
            List<Map<String, Object>> updatedMessages = new ArrayList<>(messages);
            updatedMessages.add(assistantMessage); // assistant message with tool_calls

            Map<String, Object> toolResultMsg = new HashMap<>();
            toolResultMsg.put("role", "tool");
            toolResultMsg.put("tool_call_id", toolCallId);
            toolResultMsg.put("content", result);
            updatedMessages.add(toolResultMsg);

            Map<String, Object> followUp = new HashMap<>();
            followUp.put("model", chatModel);
            followUp.put("messages", updatedMessages);
            followUp.put("max_tokens", 1024);
            followUp.put("temperature", 0.7);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(followUp, headers);
            ResponseEntity<Map<String, Object>> followUpResponse = restTemplate.postForEntity(
                    GROQ_URL, entity, (Class<Map<String, Object>>) (Class<?>) Map.class);

            Map<String, Object> followUpBody = followUpResponse.getBody();
            if (followUpBody == null) return result;

            List<Map<String, Object>> choices = (List<Map<String, Object>>) followUpBody.get("choices");
            if (choices == null || choices.isEmpty()) return result;

            Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
            Object content = msg.get("content");
            return content != null ? content.toString().trim() : result;

        } catch (Exception e) {
            System.err.println("[ToolCall ERROR] " + e.getMessage());
            return "{\"message\":\"There was an error processing your request.\",\"options\":[],\"hideInput\":false}";
        }
    }

    private String executeFunction(String name, Map<String, Object> args,
            AiChatSessionEntity session, String username) {
        if ("create_customer".equals(name)) return executeCreateCustomer(args, session, username);
        if ("create_loan".equals(name)) return executeCreateLoan(args, session, username);
        if ("lookup_customer".equals(name)) return executeLookupCustomer(args, session);
        return "Unknown function: " + name;
    }

    @Transactional
    protected String executeLookupCustomer(Map<String, Object> args, AiChatSessionEntity session) {
        try {
            String phone = args.get("phone") != null ? args.get("phone").toString().trim() : null;
            String customerNumber = args.get("customerNumber") != null ? args.get("customerNumber").toString().trim() : null;

            java.util.Optional<com.moneymoment.lending.entities.CustomerEntity> found =
                    (phone != null && !phone.isEmpty())
                            ? customerRepo.findByPhone(phone)
                            : (customerNumber != null && !customerNumber.isEmpty()
                                    ? customerRepo.findByCustomerNumber(customerNumber)
                                    : java.util.Optional.empty());

            if (found.isEmpty()) {
                String identifier = phone != null ? "mobile number: " + phone : "customer number: " + customerNumber;
                return "No customer found with " + identifier + ". Please verify and try again.";
            }

            com.moneymoment.lending.entities.CustomerEntity c = found.get();
            session.setCreatedCustomer(customerRepo.getReferenceById(c.getId()));
            session.setCustomerAction("FOUND");
            sessionRepo.save(session);
            return String.format("Customer found! ID: %d, Name: %s, Customer Number: %s",
                    c.getId(), c.getName(), c.getCustomerNumber());
        } catch (Exception e) {
            return "Error looking up customer: " + e.getMessage();
        }
    }

    @Transactional
    protected String executeCreateCustomer(Map<String, Object> input,
            AiChatSessionEntity session, String username) {
        try {
            CustomerRequestDto dto = new CustomerRequestDto();

            if (input.get("pan") != null) dto.setPan(input.get("pan").toString());
            if (input.get("aadhar") != null) dto.setAadhar(input.get("aadhar").toString());
            if (input.get("name") != null) dto.setName(input.get("name").toString());
            if (input.get("phone") != null) dto.setPhone(input.get("phone").toString());
            if (input.get("email") != null) dto.setEmail(input.get("email").toString());
            if (input.get("address") != null) dto.setAddress(input.get("address").toString());
            if (input.get("occupation") != null) dto.setOccupation(input.get("occupation").toString());
            if (input.get("monthlySalary") != null)
                dto.setMonthlySalary(Double.parseDouble(input.get("monthlySalary").toString()));
            if (input.get("employmentType") != null) {
                try {
                    dto.setEmploymentType(com.moneymoment.lending.common.enums.EmploymentType
                            .valueOf(input.get("employmentType").toString().toUpperCase()));
                } catch (Exception ignored) {}
            }
            if (input.get("dob") != null) {
                try { dto.setDob(LocalDateTime.parse(input.get("dob").toString())); }
                catch (Exception ignored) {}
            }
            dto.setCreatedBy(username);

            CustomerResponseDto created = customerService.createCustomer(dto);
            session.setCreatedCustomer(customerRepo.getReferenceById(created.getId()));
            session.setCustomerAction("CREATED");
            session.setStatus(AiSessionStatus.ACTIVE);
            sessionRepo.save(session);

            return String.format("Customer created! Customer Number: %s, Name: %s, ID: %d",
                    created.getCustomerNumber(), created.getName(), created.getId());
        } catch (Exception e) {
            return "Error creating customer: " + e.getMessage();
        }
    }

    @Transactional
    protected String executeCreateLoan(Map<String, Object> input,
            AiChatSessionEntity session, String username) {
        try {
            LoanRequestDto dto = new LoanRequestDto();
            if (input.get("customerId") != null)
                dto.setCustomerId(Long.parseLong(input.get("customerId").toString()));
            else if (session.getCreatedCustomer() != null)
                dto.setCustomerId(session.getCreatedCustomer().getId());

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
        } catch (Exception e) {
            return "Error creating loan: " + e.getMessage();
        }
    }

    // ─── Message Builder ──────────────────────────────────────────────────────

    private List<Map<String, Object>> buildMessages(List<AiChatMessageEntity> history) {
        List<Map<String, Object>> messages = new ArrayList<>();

        // System message first
        messages.add(Map.of("role", "system", "content", buildSystemPrompt()));

        // Merge consecutive same-role messages (Groq/OpenAI allows it but cleaner merged)
        String currentRole = null;
        StringBuilder currentText = new StringBuilder();

        for (AiChatMessageEntity msg : history) {
            String role = msg.getRole() == AiMessageRole.USER ? "user" : "assistant";
            if (role.equals(currentRole)) {
                currentText.append("\n").append(msg.getContent());
            } else {
                if (currentRole != null) {
                    messages.add(Map.of("role", currentRole, "content", currentText.toString()));
                }
                currentRole = role;
                currentText = new StringBuilder(msg.getContent());
            }
        }
        if (currentRole != null) {
            messages.add(Map.of("role", currentRole, "content", currentText.toString()));
        }

        return messages;
    }

    // ─── Tool Definitions ─────────────────────────────────────────────────────

    private List<Map<String, Object>> buildTools() {
        List<Map<String, Object>> tools = new ArrayList<>();

        // create_customer
        Map<String, Object> custProps = new HashMap<>();
        custProps.put("name", Map.of("type", "string", "description", "Full name of the customer"));
        custProps.put("pan", Map.of("type", "string", "description", "PAN number (10-character alphanumeric, e.g. ABCDE1234F)"));
        custProps.put("aadhar", Map.of("type", "string", "description", "Aadhaar number (12 digits, no spaces)"));
        custProps.put("phone", Map.of("type", "string", "description", "10-digit mobile number"));
        custProps.put("email", Map.of("type", "string", "description", "Email address"));
        custProps.put("dob", Map.of("type", "string", "description", "Date of birth in ISO format YYYY-MM-DDTHH:mm:ss"));
        custProps.put("address", Map.of("type", "string", "description", "Full residential address"));
        custProps.put("employmentType", Map.of("type", "string", "enum", List.of("SALARIED", "SELF_EMPLOYED")));
        custProps.put("occupation", Map.of("type", "string", "description", "Job title or employer name"));
        custProps.put("monthlySalary", Map.of("type", "number", "description", "Monthly income in INR"));

        tools.add(Map.of("type", "function", "function", Map.of(
                "name", "create_customer",
                "description", "Creates a new customer. Call ONLY after all fields collected and user confirms.",
                "parameters", Map.of("type", "object", "properties", custProps,
                        "required", List.of("name", "pan", "aadhar", "phone", "email", "employmentType")))));

        // create_loan
        Map<String, Object> loanProps = new HashMap<>();
        loanProps.put("customerId", Map.of("type", "number", "description", "Customer ID"));
        loanProps.put("loanTypeCode", Map.of("type", "string", "description", "Loan type: PL, HL, BL, EL, VL"));
        loanProps.put("loanPurposeCode", Map.of("type", "string", "description", "Purpose code: MEDICAL, HOME_PURCHASE, BUSINESS_EXPANSION, EDUCATION, VEHICLE_PURCHASE, PERSONAL"));
        loanProps.put("purpose", Map.of("type", "string", "description", "Brief purpose description"));
        loanProps.put("loanAmount", Map.of("type", "number", "description", "Loan amount in INR"));
        loanProps.put("tenureMonths", Map.of("type", "number", "description", "Tenure in months"));
        loanProps.put("disbursementAccountNumber", Map.of("type", "string", "description", "Bank account number"));
        loanProps.put("disbursementIfsc", Map.of("type", "string", "description", "IFSC code"));

        tools.add(Map.of("type", "function", "function", Map.of(
                "name", "create_loan",
                "description", "Creates a loan application. Call ONLY after all fields collected and user confirms.",
                "parameters", Map.of("type", "object", "properties", loanProps,
                        "required", List.of("customerId", "loanTypeCode", "loanPurposeCode", "loanAmount", "tenureMonths")))));

        // lookup_customer
        Map<String, Object> lookupProps = new HashMap<>();
        lookupProps.put("phone", Map.of("type", "string", "description", "Customer's 10-digit mobile number (preferred)"));
        lookupProps.put("customerNumber", Map.of("type", "string", "description", "Customer number as fallback (e.g. CUST001234)"));

        tools.add(Map.of("type", "function", "function", Map.of(
                "name", "lookup_customer",
                "description", "Look up an existing customer by mobile number or customer number. Returns the customer ID needed for loan creation.",
                "parameters", Map.of("type", "object", "properties", lookupProps,
                        "required", List.of()))));

        return tools;
    }

    // ─── System Prompt ────────────────────────────────────────────────────────

    private String buildSystemPrompt() {
        String loanTypeOptions = masterService.getAllLoanTypes().stream()
                .map(t -> t.getName() + " (" + t.getCode() + ")")
                .collect(Collectors.joining("\", \"", "[\"", "\"]"));

        String loanPurposeOptions = masterService.getAllLoanPurposes().stream()
                .map(p -> p.getName() + " (" + p.getCode() + ")")
                .collect(Collectors.joining("\", \"", "[\"", "\"]"));

        return "You are a guided onboarding assistant for FinPulse, a lending management system.\n"
            + "You help staff onboard customers and create loan applications step by step.\n\n"
            + "=== CRITICAL: RESPONSE FORMAT ===\n"
            + "You MUST ALWAYS respond in valid JSON with exactly these fields:\n"
            + "{\"message\": \"...\", \"options\": [...], \"hideInput\": true/false}\n"
            + "- options: array of clickable choices. Use [] when free text input is needed.\n"
            + "- hideInput: true when options are shown. false when user needs to type.\n"
            + "- NEVER respond with plain text. ALWAYS valid JSON.\n\n"
            + "=== GUIDED FLOW ===\n\n"
            + "STEP 1 - START\n"
            + "Always begin with: {\"message\": \"Welcome to FinPulse! What would you like to do?\", "
            + "\"options\": [\"Create Loan for Existing Customer\", \"Create New Customer + Loan\"], \"hideInput\": true}\n\n"
            + "STEP 2A - EXISTING CUSTOMER\n"
            + "Ask: {\"message\": \"Please enter the customer's registered 10-digit mobile number.\", \"options\": [], \"hideInput\": false}\n"
            + "Then call lookup_customer with the phone number.\n"
            + "After found: show customer name and proceed to loan type step.\n\n"
            + "STEP 2B - NEW CUSTOMER\n"
            + "Collect fields ONE at a time (hideInput: false for all text inputs):\n"
            + "1. Full name\n2. Date of birth (DD/MM/YYYY)\n3. PAN number\n4. Aadhaar (12 digits)\n"
            + "5. Mobile number\n6. Email\n7. Address\n"
            + "8. Employment type → {\"options\": [\"Salaried\", \"Self-Employed\"], \"hideInput\": true}\n"
            + "9. Employer name / Occupation\n10. Monthly income\n"
            + "Show summary → {\"options\": [\"Yes, Create Customer\", \"No, Review Again\"], \"hideInput\": true}\n"
            + "On confirm → call create_customer.\n\n"
            + "STEP 3 - LOAN TYPE\n"
            + "Ask: {\"message\": \"What type of loan?\", \"options\": " + loanTypeOptions + ", \"hideInput\": true}\n\n"
            + "STEP 4 - LOAN AMOUNT\n"
            + "Ask: {\"message\": \"Enter the loan amount (e.g. 5L, 2Cr, 500000).\", \"options\": [], \"hideInput\": false}\n\n"
            + "STEP 5 - TENURE\n"
            + "Ask: {\"message\": \"Select loan tenure.\", "
            + "\"options\": [\"12 months\", \"24 months\", \"36 months\", \"48 months\", \"60 months\", \"84 months\", \"120 months\"], "
            + "\"hideInput\": true}\n\n"
            + "STEP 6 - LOAN PURPOSE\n"
            + "Ask: {\"message\": \"What is the purpose of this loan?\", \"options\": " + loanPurposeOptions + ", \"hideInput\": true}\n\n"
            + "STEP 7 - BANK DETAILS\n"
            + "Ask bank account number, then IFSC code (hideInput: false each).\n\n"
            + "STEP 8 - CONFIRM LOAN\n"
            + "Show full summary → {\"options\": [\"Yes, Create Loan Application\", \"No, Review Again\"], \"hideInput\": true}\n"
            + "On confirm → call create_loan.\n\n"
            + "=== RULES ===\n"
            + "- ONE question at a time.\n"
            + "- Number conversions: 80k=80000, 5L=500000, 1Cr=10000000\n"
            + "- DOB: DD/MM/YYYY to YYYY-MM-DDTHH:mm:ss (e.g. 15/05/1990 to 1990-05-15T00:00:00)\n"
            + "- PAN: uppercase. Aadhaar: digits only (no spaces).\n"
            + "- Extract loanTypeCode from option (e.g. Personal Loan (PL) → PL)\n"
            + "- Extract loanPurposeCode from option (e.g. Medical (MEDICAL) → MEDICAL)\n"
            + "- Extract tenureMonths from option (e.g. 36 months → 36)\n"
            + "- NEVER call a function until user confirms with yes.\n"
            + "- ALWAYS return valid JSON. No plain text ever.\n";
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

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

    private void saveMessage(AiChatSessionEntity session, AiMessageRole role, String content) {
        AiChatMessageEntity msg = new AiChatMessageEntity();
        msg.setSession(session);
        msg.setRole(role);
        msg.setContent(content);
        messageRepo.save(msg);
    }

    static class ChatContext {
        final AiChatSessionEntity session;
        final List<AiChatMessageEntity> history;
        ChatContext(AiChatSessionEntity s, List<AiChatMessageEntity> h) {
            session = s; history = h;
        }
    }
}
