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
import com.moneymoment.lending.common.enums.DocumentStatusEnums;
import com.moneymoment.lending.common.utils.EmiCalculator;
import com.moneymoment.lending.dtos.AiChatResponseDto;
import com.moneymoment.lending.dtos.CustomerRequestDto;
import com.moneymoment.lending.dtos.CustomerResponseDto;
import com.moneymoment.lending.dtos.LoanRequestDto;
import com.moneymoment.lending.dtos.LoanResponseDto;
import com.moneymoment.lending.entities.AiChatMessageEntity;
import com.moneymoment.lending.entities.AiChatSessionEntity;
import com.moneymoment.lending.entities.CustomerEntity;
import com.moneymoment.lending.master.MasterService;
import com.moneymoment.lending.repos.AiChatMessageRepository;
import com.moneymoment.lending.repos.AiChatSessionRepository;
import com.moneymoment.lending.repos.CustomerRepository;
import com.moneymoment.lending.repos.DocumentRepository;
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
    private final DocumentRepository documentRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    AiChatService(AiChatSessionRepository sessionRepo, AiChatMessageRepository messageRepo,
            CustomerService customerService, LoanService loanService,
            UserRepository userRepo, CustomerRepository customerRepo,
            LoanRepo loanRepo, MasterService masterService,
            DocumentRepository documentRepository) {
        this.sessionRepo = sessionRepo;
        this.messageRepo = messageRepo;
        this.customerService = customerService;
        this.loanService = loanService;
        this.userRepo = userRepo;
        this.customerRepo = customerRepo;
        this.loanRepo = loanRepo;
        this.masterService = masterService;
        this.documentRepository = documentRepository;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    // ─── Public entry point ───────────────────────────────────────────────────

    public AiChatResponseDto chat(String sessionId, String userMessage, String username, Long frontendCustomerId) {
        ChatContext ctx = persistIncoming(sessionId, userMessage, username);
        String assistantReply = callGroq(ctx.session, ctx.history, username, frontendCustomerId);
        return persistReply(ctx, assistantReply);
    }

    // ─── Phase 1: save incoming ───────────────────────────────────────────────

    @Transactional
    protected ChatContext persistIncoming(String sessionId, String userMessage, String username) {
        AiChatSessionEntity session = loadOrCreateSession(sessionId, username);

        if (userMessage != null && !userMessage.isBlank()) {
            saveMessage(session, AiMessageRole.USER, userMessage);
        } else {
            saveMessage(session, AiMessageRole.USER, "Hi, I want to create a loan application.");
        }

        List<AiChatMessageEntity> history = messageRepo.findBySessionOrderByCreatedAtAsc(session);
        return new ChatContext(session, history);
    }

    // ─── Phase 2: save reply and return ──────────────────────────────────────

    @Transactional
    protected AiChatResponseDto persistReply(ChatContext ctx, String assistantReply) {
        AiChatSessionEntity session = sessionRepo.findById(ctx.session.getId()).orElse(ctx.session);

        String displayReply = assistantReply;
        List<String> options = new ArrayList<>();
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
    private String callGroq(AiChatSessionEntity session, List<AiChatMessageEntity> history, String username, Long frontendCustomerId) {
        try {
            List<Map<String, Object>> messages = buildMessages(history);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", chatModel);
            requestBody.put("messages", messages);
            requestBody.put("tools", buildTools());
            requestBody.put("tool_choice", "auto");
            requestBody.put("max_tokens", 2048);
            requestBody.put("temperature", 0.3);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map<String, Object>> response = restTemplate.postForEntity(
                    GROQ_URL, entity, (Class<Map<String, Object>>) (Class<?>) Map.class);

            Map<String, Object> body = response.getBody();
            if (body == null) return fallbackJson("Sorry, I couldn't process that.");

            List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
            if (choices == null || choices.isEmpty()) return fallbackJson("Sorry, no response from AI.");

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String finishReason = (String) choices.get(0).get("finish_reason");

            if ("tool_calls".equals(finishReason)) {
                List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) message.get("tool_calls");
                if (toolCalls != null && !toolCalls.isEmpty()) {
                    return handleToolCall(toolCalls.get(0), message, messages, session, username, frontendCustomerId);
                }
            }

            Object content = message.get("content");
            String raw = content != null ? content.toString().trim() : "";
            return ensureJson(raw);

        } catch (HttpClientErrorException e) {
            System.err.println("[Groq ERROR] " + e.getStatusCode() + " — " + e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 429) {
                // Rate limit — wait 2s and retry once
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                try {
                    HttpHeaders retryHeaders = new HttpHeaders();
                    retryHeaders.setContentType(MediaType.APPLICATION_JSON);
                    retryHeaders.setBearerAuth(apiKey);
                    Map<String, Object> retryBody = new HashMap<>();
                    retryBody.put("model", chatModel);
                    retryBody.put("messages", buildMessages(history));
                    retryBody.put("tools", buildTools());
                    retryBody.put("tool_choice", "auto");
                    retryBody.put("max_tokens", 1024);
                    retryBody.put("temperature", 0.3);
                    HttpEntity<Map<String, Object>> retryEntity = new HttpEntity<>(retryBody, retryHeaders);
                    ResponseEntity<Map<String, Object>> retryResponse = restTemplate.postForEntity(
                            GROQ_URL, retryEntity, (Class<Map<String, Object>>) (Class<?>) Map.class);
                    Map<String, Object> retryRespBody = retryResponse.getBody();
                    if (retryRespBody != null) {
                        List<Map<String, Object>> retryChoices = (List<Map<String, Object>>) retryRespBody.get("choices");
                        if (retryChoices != null && !retryChoices.isEmpty()) {
                            Map<String, Object> retryMsg = (Map<String, Object>) retryChoices.get(0).get("message");
                            Object content = retryMsg.get("content");
                            return ensureJson(content != null ? content.toString().trim() : "");
                        }
                    }
                } catch (Exception retryEx) {
                    System.err.println("[Groq RETRY ERROR] " + retryEx.getMessage());
                }
                return fallbackJson("The AI assistant is temporarily busy. Please wait a few seconds and send your message again.");
            }
            if (e.getStatusCode().value() == 401) {
                return fallbackJson("AI service configuration error. Please contact your system administrator.");
            }
            return fallbackJson("Unable to process your request at the moment. Please try again.");
        } catch (Exception e) {
            System.err.println("[Groq ERROR] " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return fallbackJson("Something went wrong. Please try sending your message again.");
        }
    }

    @SuppressWarnings("unchecked")
    private String handleToolCall(Map<String, Object> toolCall, Map<String, Object> assistantMessage,
            List<Map<String, Object>> messages, AiChatSessionEntity session, String username, Long frontendCustomerId) {
        try {
            String toolCallId = (String) toolCall.get("id");
            Map<String, Object> function = (Map<String, Object>) toolCall.get("function");
            String functionName = (String) function.get("name");
            String argumentsJson = (String) function.get("arguments");

            Map<String, Object> args = objectMapper.readValue(argumentsJson, Map.class);
            String result = executeFunction(functionName, args, session, username, frontendCustomerId);

            // Short-circuit: get_loan_purposes returns ready-made JSON — no follow-up needed
            if ("get_loan_purposes".equals(functionName)) {
                return result;
            }

            // Build follow-up: history + assistant tool_call + tool result + JSON reminder
            List<Map<String, Object>> updatedMessages = new ArrayList<>(messages);
            updatedMessages.add(assistantMessage);

            Map<String, Object> toolResultMsg = new HashMap<>();
            toolResultMsg.put("role", "tool");
            toolResultMsg.put("tool_call_id", toolCallId);
            // Append JSON format reminder so the follow-up always returns structured JSON
            toolResultMsg.put("content", result
                    + "\n\n[SYSTEM: You MUST respond in valid JSON: "
                    + "{\"message\": \"...\", \"options\": [...], \"hideInput\": true/false}]");
            updatedMessages.add(toolResultMsg);

            Map<String, Object> followUp = new HashMap<>();
            followUp.put("model", chatModel);
            followUp.put("messages", updatedMessages);
            followUp.put("max_tokens", 1024);
            followUp.put("temperature", 0.3);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(followUp, headers);
            ResponseEntity<Map<String, Object>> followUpResponse = restTemplate.postForEntity(
                    GROQ_URL, entity, (Class<Map<String, Object>>) (Class<?>) Map.class);

            Map<String, Object> followUpBody = followUpResponse.getBody();
            if (followUpBody == null) return fallbackJson(result);

            List<Map<String, Object>> choices = (List<Map<String, Object>>) followUpBody.get("choices");
            if (choices == null || choices.isEmpty()) return fallbackJson(result);

            Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
            Object content = msg.get("content");
            String raw = content != null ? content.toString().trim() : "";
            return ensureJson(raw);

        } catch (Exception e) {
            System.err.println("[ToolCall ERROR] " + e.getMessage());
            return fallbackJson("Unable to complete that action. Please try again or restart the conversation.");
        }
    }

    private String executeFunction(String name, Map<String, Object> args,
            AiChatSessionEntity session, String username, Long frontendCustomerId) {
        if ("create_customer".equals(name))      return executeCreateCustomer(args, session, username);
        if ("create_loan".equals(name))          return executeCreateLoan(args, session, username, frontendCustomerId);
        if ("lookup_customer".equals(name))      return executeLookupCustomer(args, session);
        if ("check_eligibility".equals(name))    return executeCheckEligibility(args, session, frontendCustomerId);
        if ("check_documents".equals(name))      return executeCheckDocuments(args, session, frontendCustomerId);
        if ("get_loan_purposes".equals(name))    return executeGetLoanPurposes();
        return "Unknown function: " + name;
    }

    // ─── Tool Implementations ─────────────────────────────────────────────────

    @Transactional
    protected String executeLookupCustomer(Map<String, Object> args, AiChatSessionEntity session) {
        try {
            String phone = args.get("phone") != null ? args.get("phone").toString().trim() : null;
            String customerNumber = args.get("customerNumber") != null ? args.get("customerNumber").toString().trim() : null;

            java.util.Optional<CustomerEntity> found =
                    (phone != null && !phone.isEmpty())
                            ? customerRepo.findByPhone(phone)
                            : (customerNumber != null && !customerNumber.isEmpty()
                                    ? customerRepo.findByCustomerNumber(customerNumber)
                                    : java.util.Optional.empty());

            if (found.isEmpty()) {
                String identifier = phone != null ? "mobile number " + phone : "customer number " + customerNumber;
                return "No customer found with " + identifier + ". Please verify and try again.";
            }

            CustomerEntity c = found.get();
            if (!Boolean.TRUE.equals(c.getIsActive())) {
                return "Customer account is inactive. Please contact support.";
            }

            session.setCreatedCustomer(customerRepo.getReferenceById(c.getId()));
            session.setCustomerAction("FOUND");
            sessionRepo.save(session);

            double salary = c.getMonthlySalary() != null ? c.getMonthlySalary() : 0;
            double creditScore = c.getCreditScore() != null ? c.getCreditScore() : 0;
            String risk = creditScore >= 750 ? "LOW" : creditScore >= 650 ? "MEDIUM" : "HIGH";

            return String.format(
                    "Customer found! ID: %d | Name: %s | Customer No: %s | " +
                    "Monthly Income: ₹%.0f | Credit Score: %.1f | Risk: %s",
                    c.getId(), c.getName(), c.getCustomerNumber(), salary, creditScore, risk);

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

            return String.format("Customer created! ID: %d | Customer Number: %s | Name: %s",
                    created.getId(), created.getCustomerNumber(), created.getName());
        } catch (Exception e) {
            return "Error creating customer: " + e.getMessage();
        }
    }

    @Transactional
    protected String executeCreateLoan(Map<String, Object> input,
            AiChatSessionEntity session, String username, Long frontendCustomerId) {
        try {
            LoanRequestDto dto = new LoanRequestDto();
            Long customerId = frontendCustomerId;
            if (customerId == null && input.get("customerId") != null) {
                try { customerId = Long.parseLong(input.get("customerId").toString()); } catch (Exception ignored) {}
            }
            if (customerId == null && session.getCreatedCustomer() != null)
                customerId = session.getCreatedCustomer().getId();
            dto.setCustomerId(customerId);

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

            return String.format(
                    "Loan application created! Loan Number: %s | Amount: ₹%.0f | " +
                    "EMI: ₹%.0f/month | Tenure: %d months | Status: INITIATED. " +
                    "Next steps: Upload and verify required documents, then proceed with credit assessment from the main application.",
                    created.getLoanNumber(), created.getLoanAmount(),
                    created.getEmiAmount(), created.getTenureMonths());
        } catch (Exception e) {
            return "Error creating loan: " + e.getMessage();
        }
    }

    @Transactional
    protected String executeCheckEligibility(Map<String, Object> args, AiChatSessionEntity session, Long frontendCustomerId) {
        try {
            Long customerId = frontendCustomerId;
            if (customerId == null && args.get("customerId") != null) {
                try { customerId = Long.parseLong(args.get("customerId").toString()); } catch (Exception ignored) {}
            }
            if (customerId == null && session.getCreatedCustomer() != null)
                customerId = session.getCreatedCustomer().getId();
            if (customerId == null) return "Customer not found. Please look up the customer first.";

            String loanTypeCode = args.get("loanTypeCode") != null ? args.get("loanTypeCode").toString() : null;
            if (loanTypeCode == null) return "Loan type is required for eligibility check.";

            double loanAmount = Double.parseDouble(args.get("loanAmount").toString());
            int tenureMonths = Integer.parseInt(args.get("tenureMonths").toString());

            CustomerEntity customer = customerRepo.findById(customerId)
                    .orElseThrow(() -> new RuntimeException("Customer not found"));

            double salary = customer.getMonthlySalary() != null ? customer.getMonthlySalary() : 0;
            double creditScore = customer.getCreditScore() != null ? customer.getCreditScore() : 0;

            double rate;
            try {
                rate = masterService.getApplicableInterestRate(loanTypeCode, creditScore, loanAmount, tenureMonths);
            } catch (Exception e) {
                // Fallback: try with a broader amount range by using a default rate lookup
                rate = masterService.getAllInterestRateConfigs().stream()
                        .filter(c -> c.getLoanType().getCode().equals(loanTypeCode) && c.getIsActive())
                        .filter(c -> creditScore >= c.getMinCreditScore() && creditScore <= c.getMaxCreditScore())
                        .mapToDouble(c -> c.getInterestRate())
                        .findFirst()
                        .orElse(12.0); // default fallback rate
            }

            double proposedEmi = EmiCalculator.calculateEmi(loanAmount, rate, tenureMonths);
            double foir = salary > 0 ? (proposedEmi / salary) * 100 : 100;

            // Max eligible amount at 50% FOIR
            double maxEmi = salary * 0.50;
            double monthlyRate = rate / 12 / 100;
            double onePlusRPowerN = Math.pow(1 + monthlyRate, tenureMonths);
            double maxEligible = monthlyRate > 0
                    ? Math.floor((maxEmi * (onePlusRPowerN - 1)) / (monthlyRate * onePlusRPowerN))
                    : 0;

            boolean eligible = creditScore >= 650 && foir <= 50 && salary >= 15000;
            String risk = creditScore >= 750 ? "LOW" : creditScore >= 650 ? "MEDIUM" : "HIGH";

            String foirStatus = foir <= 50 ? "✓ Within limit (50%)" : "✗ Exceeds limit (50%)";

            return String.format(
                    "Eligibility Result for %s | " +
                    "Monthly Income: ₹%.0f | Credit Score: %.1f | Risk Category: %s | " +
                    "Requested: ₹%.0f | Interest Rate: %.2f%% p.a. | " +
                    "Proposed EMI: ₹%.0f/month | FOIR: %.1f%% %s | " +
                    "Max Eligible Amount: ₹%.0f | " +
                    "Eligible: %s%s",
                    customer.getName(), salary, creditScore, risk,
                    loanAmount, rate,
                    proposedEmi, foir, foirStatus,
                    maxEligible,
                    eligible ? "YES" : "NO",
                    !eligible && loanAmount > maxEligible
                            ? String.format(". Consider reducing amount to ₹%.0f.", maxEligible) : "");

        } catch (Exception e) {
            return "Error checking eligibility: " + e.getMessage();
        }
    }

    @Transactional
    protected String executeCheckDocuments(Map<String, Object> args, AiChatSessionEntity session, Long frontendCustomerId) {
        try {
            Long customerId = frontendCustomerId;
            if (customerId == null && args.get("customerId") != null) {
                try { customerId = Long.parseLong(args.get("customerId").toString()); } catch (Exception ignored) {}
            }
            if (customerId == null && session.getCreatedCustomer() != null)
                customerId = session.getCreatedCustomer().getId();
            if (customerId == null) return "Customer not found. Cannot check documents.";

            var docs = documentRepository.findByCustomerId(customerId);

            if (docs.isEmpty()) {
                return "No documents found for this customer. " +
                       "Required documents: Aadhaar, PAN Card, Salary Slips (last 3 months), " +
                       "Bank Statement (6 months). Please upload via the Documents section in the main application.";
            }

            long verified = docs.stream()
                    .filter(d -> DocumentStatusEnums.VERIFIED.equals(d.getUploadStatus())).count();
            long pending = docs.stream()
                    .filter(d -> DocumentStatusEnums.UPLOADED.equals(d.getUploadStatus())
                              || DocumentStatusEnums.PENDING_VERIFICATION.equals(d.getUploadStatus())).count();
            long rejected = docs.stream()
                    .filter(d -> DocumentStatusEnums.REJECTED.equals(d.getUploadStatus())).count();

            String docList = docs.stream()
                    .map(d -> d.getDocumentType().getName() + " [" + d.getUploadStatus() + "]")
                    .collect(Collectors.joining(", "));

            String readiness = (verified == docs.size())
                    ? "All documents verified — ready for credit assessment."
                    : "Documents pending verification: " + pending + ". All must be verified before credit assessment.";

            return String.format(
                    "Documents on file: %d total (%d verified, %d pending, %d rejected). " +
                    "Details: %s. %s",
                    docs.size(), verified, pending, rejected, docList, readiness);

        } catch (Exception e) {
            return "Error checking documents: " + e.getMessage();
        }
    }

    private String executeGetLoanPurposes() {
        try {
            List<String> options = masterService.getAllLoanPurposes().stream()
                    .map(p -> p.getName() + " (" + p.getCode() + ")")
                    .collect(Collectors.toList());

            if (options.isEmpty()) {
                return fallbackJson("No loan purposes are configured yet. Please contact your administrator.");
            }

            return objectMapper.writeValueAsString(Map.of(
                    "message", "What is the purpose of this loan?",
                    "options", options,
                    "hideInput", true));
        } catch (Exception e) {
            return fallbackJson("Error loading loan purposes: " + e.getMessage());
        }
    }

    // ─── Message Builder ──────────────────────────────────────────────────────

    private List<Map<String, Object>> buildMessages(List<AiChatMessageEntity> history) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", buildSystemPrompt()));

        // Limit to last 15 messages to stay within token limits
        List<AiChatMessageEntity> recentHistory = history.size() > 15
                ? history.subList(history.size() - 15, history.size())
                : history;

        String currentRole = null;
        StringBuilder currentText = new StringBuilder();

        for (AiChatMessageEntity msg : recentHistory) {
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

        // lookup_customer
        Map<String, Object> lookupProps = new HashMap<>();
        lookupProps.put("phone", Map.of("type", "string", "description", "Customer's 10-digit mobile number (preferred)"));
        lookupProps.put("customerNumber", Map.of("type", "string", "description", "Customer number as fallback (e.g. CUST001234)"));
        tools.add(Map.of("type", "function", "function", Map.of(
                "name", "lookup_customer",
                "description", "Look up an existing customer by mobile number or customer number. Returns customer ID, name, income, credit score.",
                "parameters", Map.of("type", "object", "properties", lookupProps, "required", List.of()))));

        // check_eligibility
        Map<String, Object> eligProps = new HashMap<>();
        eligProps.put("loanTypeCode", Map.of("type", "string", "description", "Loan type code e.g. PL, HL, BL, EL, VL"));
        eligProps.put("loanAmount", Map.of("type", "number", "description", "Requested loan amount in INR"));
        eligProps.put("tenureMonths", Map.of("type", "number", "description", "Loan tenure in months"));
        tools.add(Map.of("type", "function", "function", Map.of(
                "name", "check_eligibility",
                "description", "Check customer loan eligibility. Returns FOIR, max eligible amount, proposed EMI, risk category.",
                "parameters", Map.of("type", "object", "properties", eligProps,
                        "required", List.of("loanTypeCode", "loanAmount", "tenureMonths")))));

        // create_customer
        Map<String, Object> custProps = new HashMap<>();
        custProps.put("name", Map.of("type", "string", "description", "Full name of the customer"));
        custProps.put("pan", Map.of("type", "string", "description", "PAN number (10-char alphanumeric, e.g. ABCDE1234F)"));
        custProps.put("aadhar", Map.of("type", "string", "description", "Aadhaar number (12 digits, no spaces)"));
        custProps.put("phone", Map.of("type", "string", "description", "10-digit mobile number"));
        custProps.put("email", Map.of("type", "string", "description", "Email address"));
        custProps.put("dob", Map.of("type", "string", "description", "Date of birth ISO format YYYY-MM-DDTHH:mm:ss"));
        custProps.put("address", Map.of("type", "string", "description", "Full residential address"));
        custProps.put("employmentType", Map.of("type", "string", "enum", List.of("SALARIED", "SELF_EMPLOYED")));
        custProps.put("occupation", Map.of("type", "string", "description", "Job title or employer name"));
        custProps.put("monthlySalary", Map.of("type", "number", "description", "Monthly income in INR"));
        tools.add(Map.of("type", "function", "function", Map.of(
                "name", "create_customer",
                "description", "Create a new customer. Call ONLY after all fields collected and user confirms.",
                "parameters", Map.of("type", "object", "properties", custProps,
                        "required", List.of("name", "pan", "aadhar", "phone", "email", "employmentType")))));

        // create_loan
        Map<String, Object> loanProps = new HashMap<>();
        loanProps.put("loanTypeCode", Map.of("type", "string", "description", "Loan type code: PL, HL, BL, EL, VL"));
        loanProps.put("loanPurposeCode", Map.of("type", "string", "description", "Purpose code from user's selection. Extract the code in parentheses from their chosen purpose (e.g. 'Medical (MEDICAL)' → MEDICAL)."));
        loanProps.put("purpose", Map.of("type", "string", "description", "Brief purpose description"));
        loanProps.put("loanAmount", Map.of("type", "number", "description", "Loan amount in INR"));
        loanProps.put("tenureMonths", Map.of("type", "number", "description", "Tenure in months"));
        loanProps.put("disbursementAccountNumber", Map.of("type", "string", "description", "Bank account number for disbursement"));
        loanProps.put("disbursementIfsc", Map.of("type", "string", "description", "IFSC code"));
        tools.add(Map.of("type", "function", "function", Map.of(
                "name", "create_loan",
                "description", "Create a loan application. Call ONLY after eligibility checked, all fields collected, and user confirms.",
                "parameters", Map.of("type", "object", "properties", loanProps,
                        "required", List.of("loanTypeCode", "loanPurposeCode", "loanAmount", "tenureMonths")))));

        // check_documents
        tools.add(Map.of("type", "function", "function", Map.of(
                "name", "check_documents",
                "description", "Check documents on file for the current customer. Returns count and status of uploaded documents.",
                "parameters", Map.of("type", "object", "properties", new HashMap<>(), "required", List.of()))));

        // get_loan_purposes
        tools.add(Map.of("type", "function", "function", Map.of(
                "name", "get_loan_purposes",
                "description", "Fetch the list of available loan purposes from the database. Call this at Step 7 to show the user the purpose options to select from.",
                "parameters", Map.of("type", "object", "properties", new HashMap<>(), "required", List.of()))));

        return tools;
    }

    // ─── System Prompt ────────────────────────────────────────────────────────

    private String buildSystemPrompt() {
        String loanTypeOptions = masterService.getAllLoanTypes().stream()
                .map(t -> t.getName() + " (" + t.getCode() + ")")
                .collect(Collectors.joining("\", \"", "[\"", "\"]"));

        return "You are a loan onboarding assistant for FinPulse, a lending management system. "
            + "You help bank staff create loan applications step by step.\n\n"

            + "=== CRITICAL: RESPONSE FORMAT ===\n"
            + "You MUST ALWAYS respond in valid JSON with exactly these fields:\n"
            + "{\"message\": \"...\", \"options\": [...], \"hideInput\": true/false}\n"
            + "- options: [] when free text is needed, array of choices otherwise\n"
            + "- hideInput: true when showing options, false when user needs to type\n"
            + "- NEVER plain text. ALWAYS valid JSON. No exceptions.\n\n"

            + "=== FLOW ===\n\n"

            + "STEP 1 — WELCOME\n"
            + "Always start with:\n"
            + "{\"message\": \"Welcome to FinPulse! What would you like to do?\", "
            + "\"options\": [\"Create Loan for Existing Customer\", \"Register New Customer + Loan\"], "
            + "\"hideInput\": true}\n\n"

            + "STEP 2A — EXISTING CUSTOMER\n"
            + "Ask: {\"message\": \"Please enter the customer's 10-digit registered mobile number.\", "
            + "\"options\": [], \"hideInput\": false}\n"
            + "Call lookup_customer(phone). On success show customer profile (name, income, credit score) "
            + "and move to Step 3.\n\n"

            + "STEP 2B — NEW CUSTOMER\n"
            + "Collect fields ONE at a time (hideInput: false for text):\n"
            + "Name → DOB (DD/MM/YYYY) → PAN → Aadhaar → Mobile → Email → Address\n"
            + "→ Employment type: options [\"Salaried\", \"Self-Employed\"], hideInput: true\n"
            + "→ Occupation/Employer → Monthly Income\n"
            + "Show summary → options: [\"Yes, Create Customer\", \"No, Review Again\"], hideInput: true\n"
            + "On confirm → call create_customer. Then proceed to Step 3.\n\n"

            + "STEP 3 — LOAN TYPE\n"
            + "{\"message\": \"What type of loan is required?\", \"options\": " + loanTypeOptions + ", "
            + "\"hideInput\": true}\n\n"

            + "STEP 4 — LOAN AMOUNT\n"
            + "{\"message\": \"What loan amount does the customer require? (e.g. 5L, 2Cr, 500000)\", "
            + "\"options\": [], \"hideInput\": false}\n\n"

            + "STEP 5 — TENURE\n"
            + "{\"message\": \"Select the loan tenure.\", "
            + "\"options\": [\"12 months\", \"24 months\", \"36 months\", \"48 months\", "
            + "\"60 months\", \"84 months\", \"120 months\"], \"hideInput\": true}\n\n"

            + "STEP 6 — ELIGIBILITY CHECK\n"
            + "Call check_eligibility(customerId, loanTypeCode, loanAmount, tenureMonths).\n"
            + "Show the result clearly. Then ask:\n"
            + "options: [\"Proceed with this amount\", \"Adjust loan amount\"], hideInput: true\n"
            + "If \"Adjust loan amount\" → go back to Step 4.\n\n"

            + "STEP 7 — LOAN PURPOSE\n"
            + "Call get_loan_purposes tool. It returns ready-made JSON with all purpose options from the database — return its result directly to the user as-is.\n\n"

            + "STEP 8 — BANK DETAILS\n"
            + "Ask bank account number (hideInput: false), then IFSC code (hideInput: false).\n\n"

            + "STEP 9 — CONFIRM\n"
            + "Show full loan summary (customer, type, amount, tenure, EMI, purpose, bank details).\n"
            + "options: [\"Yes, Create Loan Application\", \"No, Review Again\"], hideInput: true\n"
            + "On confirm → call create_loan. Then call check_documents(customerId).\n"
            + "Show loan number + document status. Session ends.\n\n"

            + "=== RULES ===\n"
            + "- ONE question at a time. Never ask two things at once.\n"
            + "- Amount conversions: 80k=80000, 5L=500000, 2.5L=250000, 1Cr=10000000\n"
            + "- DOB: DD/MM/YYYY → YYYY-MM-DDTHH:mm:ss (e.g. 15/05/1990 → 1990-05-15T00:00:00)\n"
            + "- PAN: uppercase. Aadhaar: digits only, no spaces.\n"
            + "- Extract loanTypeCode from option label (e.g. \"Personal Loan (PL)\" → PL)\n"
            + "- Extract loanPurposeCode from option label (e.g. \"Medical (MEDICAL)\" → MEDICAL)\n"
            + "- Extract tenureMonths from option (e.g. \"36 months\" → 36)\n"
            + "- NEVER call a function until the user has explicitly confirmed.\n"
            + "- Chat scope ends at INITIATED status. Credit assessment and approvals happen in the main application.\n"
            + "- ALWAYS return valid JSON. Never plain text.\n";
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private AiChatSessionEntity loadOrCreateSession(String sessionId, String username) {
        if (sessionId != null && !sessionId.isBlank()) {
            var existing = sessionRepo.findBySessionUuid(sessionId);
            if (existing.isPresent()) {
                AiChatSessionEntity s = existing.get();
                // Refresh expiry on active sessions
                if (s.getStatus() == AiSessionStatus.ACTIVE
                        && (s.getExpiresAt() == null || s.getExpiresAt().isAfter(LocalDateTime.now()))) {
                    return s;
                }
                // Expired or completed — fall through to create a fresh session
            }
        }
        // Always generate a new UUID (avoids unique constraint collision on re-use)
        AiChatSessionEntity session = new AiChatSessionEntity();
        session.setSessionUuid(java.util.UUID.randomUUID().toString());
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

    /** Ensure the string is valid JSON. If not, wrap it in a JSON message object. */
    private String ensureJson(String text) {
        if (text == null || text.isBlank()) return fallbackJson("I couldn't generate a response.");
        try {
            objectMapper.readTree(text);
            return text; // already valid JSON
        } catch (Exception e) {
            return fallbackJson(text);
        }
    }

    private String fallbackJson(String message) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "message", message,
                    "options", List.of(),
                    "hideInput", false));
        } catch (Exception e) {
            return "{\"message\":\"" + message.replace("\"", "'") + "\",\"options\":[],\"hideInput\":false}";
        }
    }

    static class ChatContext {
        final AiChatSessionEntity session;
        final List<AiChatMessageEntity> history;
        ChatContext(AiChatSessionEntity s, List<AiChatMessageEntity> h) {
            session = s; history = h;
        }
    }
}
