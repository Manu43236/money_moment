package com.moneymoment.lending.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneymoment.lending.common.enums.AiMessageRole;
import com.moneymoment.lending.common.enums.AiSessionIntent;
import com.moneymoment.lending.common.enums.AiSessionStatus;
import com.moneymoment.lending.common.enums.EmploymentType;
import com.moneymoment.lending.common.validation.AadhaarValidator;
import com.moneymoment.lending.common.validation.PanValidator;
import com.moneymoment.lending.common.validation.PhoneValidator;
import com.moneymoment.lending.dtos.AiChatResponseDto;
import com.moneymoment.lending.dtos.CustomerRequestDto;
import com.moneymoment.lending.dtos.CustomerResponseDto;
import com.moneymoment.lending.dtos.LoanRequestDto;
import com.moneymoment.lending.dtos.LoanResponseDto;
import com.moneymoment.lending.entities.AiChatMessageEntity;
import com.moneymoment.lending.entities.AiChatSessionEntity;
import com.moneymoment.lending.entities.CustomerEntity;
import com.moneymoment.lending.master.MasterService;
import com.moneymoment.lending.master.entities.LoanPurposesEntity;
import com.moneymoment.lending.master.entities.LoanTypesEntity;
import com.moneymoment.lending.repos.AiChatMessageRepository;
import com.moneymoment.lending.repos.AiChatSessionRepository;
import com.moneymoment.lending.repos.CustomerRepository;
import com.moneymoment.lending.repos.LoanRepo;
import com.moneymoment.lending.repos.UserRepository;

/**
 * Provider-independent conversational workflow for customer and loan onboarding.
 * The LLM is optional; this service owns state, validation, confirmation and writes.
 */
@Service
public class ConversationalWorkflowService {

    private static final String CHOOSE_INTENT = "CHOOSE_INTENT";
    private static final String CUSTOMER_NAME = "CUSTOMER_NAME";
    private static final String CUSTOMER_PHONE = "CUSTOMER_PHONE";
    private static final String CUSTOMER_DOB = "CUSTOMER_DOB";
    private static final String CUSTOMER_EMAIL = "CUSTOMER_EMAIL";
    private static final String CUSTOMER_PAN = "CUSTOMER_PAN";
    private static final String CUSTOMER_AADHAAR = "CUSTOMER_AADHAAR";
    private static final String CUSTOMER_ADDRESS = "CUSTOMER_ADDRESS";
    private static final String CUSTOMER_OCCUPATION = "CUSTOMER_OCCUPATION";
    private static final String CUSTOMER_EMPLOYMENT = "CUSTOMER_EMPLOYMENT";
    private static final String CUSTOMER_SALARY = "CUSTOMER_SALARY";
    private static final String CONFIRM_CUSTOMER = "CONFIRM_CUSTOMER";
    private static final String AFTER_CUSTOMER = "AFTER_CUSTOMER";
    private static final String LOAN_CUSTOMER = "LOAN_CUSTOMER";
    private static final String LOAN_TYPE = "LOAN_TYPE";
    private static final String LOAN_PURPOSE = "LOAN_PURPOSE";
    private static final String LOAN_AMOUNT = "LOAN_AMOUNT";
    private static final String LOAN_TENURE = "LOAN_TENURE";
    private static final String LOAN_ACCOUNT = "LOAN_ACCOUNT";
    private static final String LOAN_IFSC = "LOAN_IFSC";
    private static final String CONFIRM_LOAN = "CONFIRM_LOAN";

    private static final Pattern EMAIL = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern IFSC = Pattern.compile("^[A-Z]{4}0[A-Z0-9]{6}$");
    private static final Pattern NUMBER = Pattern.compile("([0-9][0-9,]*(?:\\.[0-9]+)?)");

    private final AiChatSessionRepository sessionRepo;
    private final AiChatMessageRepository messageRepo;
    private final UserRepository userRepo;
    private final CustomerRepository customerRepo;
    private final LoanRepo loanRepo;
    private final CustomerService customerService;
    private final LoanService loanService;
    private final MasterService masterService;
    private final ObjectMapper objectMapper;

    public ConversationalWorkflowService(AiChatSessionRepository sessionRepo,
            AiChatMessageRepository messageRepo, UserRepository userRepo,
            CustomerRepository customerRepo, LoanRepo loanRepo, CustomerService customerService,
            LoanService loanService, MasterService masterService, ObjectMapper objectMapper) {
        this.sessionRepo = sessionRepo;
        this.messageRepo = messageRepo;
        this.userRepo = userRepo;
        this.customerRepo = customerRepo;
        this.loanRepo = loanRepo;
        this.customerService = customerService;
        this.loanService = loanService;
        this.masterService = masterService;
        this.objectMapper = objectMapper;
    }

    public AiChatResponseDto chat(String sessionId, String userMessage, String username, Long frontendCustomerId) {
        AiChatSessionEntity session = loadOrCreate(sessionId, username);
        Map<String, String> data = readData(session);
        String message = userMessage == null ? "" : userMessage.trim();

        if (!message.isBlank()) saveMessage(session, AiMessageRole.USER, message);
        if (isRestart(message)) {
            reset(session, data);
            return reply(session, data, "What would you like to do?", List.of("Create Customer", "Create Loan"), true);
        }

        if (frontendCustomerId != null && !data.containsKey("customerId")) {
            customerRepo.findById(frontendCustomerId).ifPresent(c -> rememberCustomer(data, c));
        }

        String step = Optional.ofNullable(session.getWorkflowStep()).orElse(CHOOSE_INTENT);
        if (message.isBlank() && CHOOSE_INTENT.equals(step)) {
            return reply(session, data,
                    "I can create a customer or start a loan application. Choose a workflow to begin.",
                    List.of("Create Customer", "Create Loan"), true);
        }

        try {
            return switch (step) {
                case CHOOSE_INTENT -> chooseIntent(session, data, message);
                case CUSTOMER_NAME -> accept(session, data, "name", message, CUSTOMER_PHONE,
                        "What is the customer's 10-digit mobile number?");
                case CUSTOMER_PHONE -> customerPhone(session, data, message);
                case CUSTOMER_DOB -> customerDob(session, data, message);
                case CUSTOMER_EMAIL -> customerEmail(session, data, message);
                case CUSTOMER_PAN -> customerPan(session, data, message);
                case CUSTOMER_AADHAAR -> customerAadhaar(session, data, message);
                case CUSTOMER_ADDRESS -> accept(session, data, "address", message, CUSTOMER_OCCUPATION,
                        "What is the customer's occupation?");
                case CUSTOMER_OCCUPATION -> acceptWithOptions(session, data, "occupation", message,
                        CUSTOMER_EMPLOYMENT, "Select the employment type.",
                        List.of("Salaried", "Self Employed"));
                case CUSTOMER_EMPLOYMENT -> customerEmployment(session, data, message);
                case CUSTOMER_SALARY -> customerSalary(session, data, message);
                case CONFIRM_CUSTOMER -> confirmCustomer(session, data, message, username);
                case AFTER_CUSTOMER -> afterCustomer(session, data, message);
                case LOAN_CUSTOMER -> loanCustomer(session, data, message);
                case LOAN_TYPE -> loanType(session, data, message);
                case LOAN_PURPOSE -> loanPurpose(session, data, message);
                case LOAN_AMOUNT -> loanAmount(session, data, message);
                case LOAN_TENURE -> loanTenure(session, data, message);
                case LOAN_ACCOUNT -> accept(session, data, "accountNumber", digits(message), LOAN_IFSC,
                        "Enter the IFSC code for disbursement.");
                case LOAN_IFSC -> loanIfsc(session, data, message);
                case CONFIRM_LOAN -> confirmLoan(session, data, message, username);
                default -> {
                    reset(session, data);
                    yield reply(session, data, "Let's start again. What would you like to do?",
                            List.of("Create Customer", "Create Loan"), true);
                }
            };
        } catch (RuntimeException ex) {
            return reply(session, data, ex.getMessage(), List.of(), false);
        }
    }

    private AiChatResponseDto chooseIntent(AiChatSessionEntity session, Map<String, String> data, String message) {
        String normalized = normalize(message);
        if (normalized.contains("customer") || normalized.contains("borrower")) {
            session.setIntent(AiSessionIntent.CREATE_CUSTOMER);
            return move(session, data, CUSTOMER_NAME, "What is the customer's full name?", List.of(), false);
        }
        if (normalized.contains("loan") || normalized.contains("application")) {
            session.setIntent(AiSessionIntent.CREATE_LOAN);
            if (data.containsKey("customerId")) return askLoanType(session, data);
            return move(session, data, LOAN_CUSTOMER,
                    "Enter the customer's customer number or 10-digit mobile number.", List.of(), false);
        }
        return reply(session, data, "Please choose customer creation or loan creation.",
                List.of("Create Customer", "Create Loan"), true);
    }

    private AiChatResponseDto customerPhone(AiChatSessionEntity session, Map<String, String> data, String message) {
        String phone = digits(message);
        PhoneValidator.validate(phone);
        Optional<CustomerEntity> existing = customerRepo.findByPhone(phone);
        if (existing.isPresent()) {
            rememberCustomer(data, existing.get());
            session.setCustomerAction("FOUND");
            session.setCreatedCustomer(existing.get());
            return move(session, data, AFTER_CUSTOMER,
                    "I found " + existing.get().getName() + " (" + existing.get().getCustomerNumber() + "). Would you like to create a loan for this customer?",
                    List.of("Create Loan", "Finish"), true);
        }
        data.put("phone", phone);
        return move(session, data, CUSTOMER_DOB, "Enter date of birth in DD/MM/YYYY format.", List.of(), false);
    }

    private AiChatResponseDto customerDob(AiChatSessionEntity session, Map<String, String> data, String message) {
        LocalDate dob = parseDate(message);
        if (!dob.isBefore(LocalDate.now().minusYears(18))) {
            return reply(session, data, "The customer must be at least 18 years old. Enter a valid date of birth.", List.of(), false);
        }
        data.put("dob", dob.toString());
        return move(session, data, CUSTOMER_EMAIL, "What is the customer's email address?", List.of(), false);
    }

    private AiChatResponseDto customerEmail(AiChatSessionEntity session, Map<String, String> data, String message) {
        String email = message.toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(email).matches()) return reply(session, data, "Enter a valid email address.", List.of(), false);
        data.put("email", email);
        return move(session, data, CUSTOMER_PAN, "Enter the customer's PAN.", List.of(), false);
    }

    private AiChatResponseDto customerPan(AiChatSessionEntity session, Map<String, String> data, String message) {
        String pan = message.replaceAll("\\s", "").toUpperCase(Locale.ROOT);
        PanValidator.validate(pan);
        data.put("pan", pan);
        return move(session, data, CUSTOMER_AADHAAR, "Enter the customer's 12-digit Aadhaar number.", List.of(), false);
    }

    private AiChatResponseDto customerAadhaar(AiChatSessionEntity session, Map<String, String> data, String message) {
        String aadhaar = digits(message);
        AadhaarValidator.validate(aadhaar);
        data.put("aadhaar", aadhaar);
        return move(session, data, CUSTOMER_ADDRESS, "What is the customer's residential address?", List.of(), false);
    }

    private AiChatResponseDto customerEmployment(AiChatSessionEntity session, Map<String, String> data, String message) {
        String n = normalize(message);
        if (n.contains("self")) data.put("employmentType", "SELF_EMPLOYED");
        else if (n.contains("salar")) data.put("employmentType", "SALARIED");
        else return reply(session, data, "Select Salaried or Self Employed.", List.of("Salaried", "Self Employed"), true);
        return move(session, data, CUSTOMER_SALARY, "What is the customer's monthly income?", List.of(), false);
    }

    private AiChatResponseDto customerSalary(AiChatSessionEntity session, Map<String, String> data, String message) {
        double salary = parseAmount(message);
        if (salary <= 0) return reply(session, data, "Enter a valid monthly income.", List.of(), false);
        data.put("monthlySalary", String.valueOf(salary));
        String summary = "Please confirm customer creation:\n"
                + data.get("name") + " · " + data.get("phone") + "\n"
                + data.get("employmentType").replace('_', ' ') + " · Monthly income ₹" + Math.round(salary) + "\n"
                + "PAN " + mask(data.get("pan")) + " · Aadhaar " + mask(data.get("aadhaar"));
        return move(session, data, CONFIRM_CUSTOMER, summary,
                List.of("Confirm Customer", "Start Over"), true);
    }

    private AiChatResponseDto confirmCustomer(AiChatSessionEntity session, Map<String, String> data,
            String message, String username) {
        if (isRestart(message)) {
            reset(session, data);
            return reply(session, data, "What would you like to do?", List.of("Create Customer", "Create Loan"), true);
        }
        if (!normalize(message).contains("confirm")) {
            return reply(session, data, "No customer was created. Confirm or start over.",
                    List.of("Confirm Customer", "Start Over"), true);
        }
        CustomerRequestDto request = new CustomerRequestDto();
        request.setName(data.get("name"));
        request.setPhone(data.get("phone"));
        request.setDob(LocalDate.parse(data.get("dob")).atStartOfDay());
        request.setEmail(data.get("email"));
        request.setPan(data.get("pan"));
        request.setAadhar(data.get("aadhaar"));
        request.setAddress(data.get("address"));
        request.setOccupation(data.get("occupation"));
        request.setEmploymentType(EmploymentType.valueOf(data.get("employmentType")));
        request.setMonthlySalary(Double.valueOf(data.get("monthlySalary")));
        request.setCreatedBy(username);
        request.setHomeBranchCode("BR001");
        CustomerResponseDto created = customerService.createCustomer(request);
        data.put("customerId", created.getId().toString());
        data.put("customerName", created.getName());
        data.put("customerNumber", created.getCustomerNumber());
        session.setCreatedCustomer(customerRepo.findById(created.getId()).orElse(null));
        session.setCustomerAction("CREATED");
        return move(session, data, AFTER_CUSTOMER,
                "Customer " + created.getName() + " was created with number " + created.getCustomerNumber() + ". Create a loan application now?",
                List.of("Create Loan", "Finish"), true);
    }

    private AiChatResponseDto afterCustomer(AiChatSessionEntity session, Map<String, String> data, String message) {
        if (normalize(message).contains("loan")) return askLoanType(session, data);
        session.setStatus(AiSessionStatus.COMPLETED);
        return reply(session, data, "Customer onboarding is complete.", List.of(), false);
    }

    private AiChatResponseDto loanCustomer(AiChatSessionEntity session, Map<String, String> data, String message) {
        String value = message.trim();
        Optional<CustomerEntity> customer = value.toUpperCase(Locale.ROOT).startsWith("CUST")
                ? customerRepo.findByCustomerNumber(value.toUpperCase(Locale.ROOT))
                : customerRepo.findByPhone(digits(value));
        if (customer.isEmpty()) {
            return reply(session, data, "I could not find that customer. Enter a valid customer number or mobile number.", List.of(), false);
        }
        rememberCustomer(data, customer.get());
        session.setCreatedCustomer(customer.get());
        session.setCustomerAction("FOUND");
        return askLoanType(session, data);
    }

    private AiChatResponseDto askLoanType(AiChatSessionEntity session, Map<String, String> data) {
        List<String> options = masterService.getAllLoanTypes().stream()
                .filter(t -> Boolean.TRUE.equals(t.getIsActive()))
                .map(t -> t.getName() + " (" + t.getCode() + ")").toList();
        return move(session, data, LOAN_TYPE,
                "Select a loan product for " + data.get("customerName") + ".", options, true);
    }

    private AiChatResponseDto loanType(AiChatSessionEntity session, Map<String, String> data, String message) {
        LoanTypesEntity type = matchLoanType(message).orElse(null);
        if (type == null) return askLoanType(session, data);
        data.put("loanTypeCode", type.getCode());
        List<String> options = masterService.getAllLoanPurposes().stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                .map(p -> p.getName() + " (" + p.getCode() + ")").toList();
        return move(session, data, LOAN_PURPOSE, "Select the loan purpose.", options, true);
    }

    private AiChatResponseDto loanPurpose(AiChatSessionEntity session, Map<String, String> data, String message) {
        LoanPurposesEntity purpose = matchLoanPurpose(message).orElse(null);
        if (purpose == null) return reply(session, data, "Select a valid purpose.", purposeOptions(), true);
        data.put("loanPurposeCode", purpose.getCode());
        data.put("purpose", purpose.getName());
        return move(session, data, LOAN_AMOUNT, "What loan amount is required?", List.of(), false);
    }

    private AiChatResponseDto loanAmount(AiChatSessionEntity session, Map<String, String> data, String message) {
        double amount = parseAmount(message);
        if (amount < 1000) return reply(session, data, "Enter a valid loan amount.", List.of(), false);
        data.put("loanAmount", String.valueOf(amount));
        List<String> tenures = masterService.getAvailableTenures(data.get("loanTypeCode")).stream()
                .map(t -> t.getTenureMonths() + " months").toList();
        if (tenures.isEmpty()) tenures = List.of("12 months", "24 months", "36 months", "48 months", "60 months");
        return move(session, data, LOAN_TENURE, "Select a repayment tenure.", tenures, true);
    }

    private AiChatResponseDto loanTenure(AiChatSessionEntity session, Map<String, String> data, String message) {
        Matcher matcher = NUMBER.matcher(message.replace(",", ""));
        if (!matcher.find()) return reply(session, data, "Select a valid tenure.", List.of(), false);
        data.put("tenureMonths", String.valueOf(Integer.parseInt(matcher.group(1).split("\\.")[0])));
        return move(session, data, LOAN_ACCOUNT, "Enter the bank account number for disbursement.", List.of(), false);
    }

    private AiChatResponseDto loanIfsc(AiChatSessionEntity session, Map<String, String> data, String message) {
        String ifsc = message.replaceAll("\\s", "").toUpperCase(Locale.ROOT);
        if (!IFSC.matcher(ifsc).matches()) return reply(session, data, "Enter a valid 11-character IFSC code.", List.of(), false);
        data.put("ifsc", ifsc);
        String summary = "Please confirm loan application:\n"
                + data.get("customerName") + " (" + data.get("customerNumber") + ")\n"
                + data.get("loanTypeCode") + " · " + data.get("purpose") + "\n"
                + "Amount ₹" + Math.round(Double.parseDouble(data.get("loanAmount")))
                + " · " + data.get("tenureMonths") + " months";
        return move(session, data, CONFIRM_LOAN, summary,
                List.of("Confirm Loan Application", "Start Over"), true);
    }

    private AiChatResponseDto confirmLoan(AiChatSessionEntity session, Map<String, String> data,
            String message, String username) {
        if (isRestart(message)) {
            reset(session, data);
            return reply(session, data, "What would you like to do?", List.of("Create Customer", "Create Loan"), true);
        }
        if (!normalize(message).contains("confirm")) {
            return reply(session, data, "No loan was created. Confirm or start over.",
                    List.of("Confirm Loan Application", "Start Over"), true);
        }
        LoanRequestDto request = new LoanRequestDto();
        request.setCustomerId(Long.valueOf(data.get("customerId")));
        request.setLoanTypeCode(data.get("loanTypeCode"));
        request.setLoanPurposeCode(data.get("loanPurposeCode"));
        request.setPurpose(data.get("purpose"));
        request.setLoanAmount(Double.valueOf(data.get("loanAmount")));
        request.setTenureMonths(Integer.valueOf(data.get("tenureMonths")));
        request.setDisbursementAccountNumber(data.get("accountNumber"));
        request.setDisbursementIfsc(data.get("ifsc"));
        request.setCreatedBy(username);
        LoanResponseDto created = loanService.createLoan(request);
        session.setCreatedLoan(loanRepo.findById(created.getId()).orElse(null));
        session.setStatus(AiSessionStatus.COMPLETED);
        session.setWorkflowStep("COMPLETED");
        AiChatResponseDto response = reply(session, data,
                "Loan application " + created.getLoanNumber() + " was created successfully.", List.of(), false);
        response.setCreatedLoanId(created.getId());
        response.setCreatedLoanNumber(created.getLoanNumber());
        return response;
    }

    private AiChatResponseDto accept(AiChatSessionEntity session, Map<String, String> data, String key,
            String value, String nextStep, String nextQuestion) {
        if (value == null || value.isBlank()) return reply(session, data, "This value is required.", List.of(), false);
        data.put(key, value.trim());
        return move(session, data, nextStep, nextQuestion, List.of(), false);
    }

    private AiChatResponseDto acceptWithOptions(AiChatSessionEntity session, Map<String, String> data,
            String key, String value, String nextStep, String nextQuestion, List<String> options) {
        if (value == null || value.isBlank()) return reply(session, data, "This value is required.", List.of(), false);
        data.put(key, value.trim());
        return move(session, data, nextStep, nextQuestion, options, true);
    }

    private AiChatResponseDto move(AiChatSessionEntity session, Map<String, String> data, String step,
            String message, List<String> options, boolean hideInput) {
        session.setWorkflowStep(step);
        return reply(session, data, message, options, hideInput);
    }

    private AiChatResponseDto reply(AiChatSessionEntity session, Map<String, String> data,
            String message, List<String> options, boolean hideInput) {
        writeData(session, data);
        sessionRepo.save(session);
        saveMessage(session, AiMessageRole.ASSISTANT, message);
        AiChatResponseDto response = new AiChatResponseDto();
        response.setSessionId(session.getSessionUuid());
        response.setSessionStatus(session.getStatus().name());
        response.setReply(message);
        response.setOptions(options.isEmpty() ? null : options);
        response.setHideInput(hideInput);
        response.setAssistantMode("GUIDED");
        if (data.containsKey("customerId")) {
            response.setCreatedCustomerId(Long.valueOf(data.get("customerId")));
            response.setCreatedCustomerNumber(data.get("customerNumber"));
            response.setCreatedCustomerName(data.get("customerName"));
            response.setCustomerAction(session.getCustomerAction());
        }
        return response;
    }

    private AiChatSessionEntity loadOrCreate(String sessionId, String username) {
        if (sessionId != null && !sessionId.isBlank()) {
            Optional<AiChatSessionEntity> existing = sessionRepo.findBySessionUuid(sessionId);
            if (existing.isPresent() && existing.get().getStatus() == AiSessionStatus.ACTIVE
                    && (existing.get().getExpiresAt() == null || existing.get().getExpiresAt().isAfter(LocalDateTime.now()))) {
                return existing.get();
            }
        }
        AiChatSessionEntity session = new AiChatSessionEntity();
        session.setSessionUuid(UUID.randomUUID().toString());
        session.setWorkflowStep(CHOOSE_INTENT);
        userRepo.findByUsername(username).ifPresent(session::setUser);
        return sessionRepo.save(session);
    }

    private void reset(AiChatSessionEntity session, Map<String, String> data) {
        data.clear();
        session.setIntent(AiSessionIntent.UNKNOWN);
        session.setStatus(AiSessionStatus.ACTIVE);
        session.setWorkflowStep(CHOOSE_INTENT);
        session.setCreatedCustomer(null);
        session.setCreatedLoan(null);
        session.setCustomerAction(null);
    }

    private Map<String, String> readData(AiChatSessionEntity session) {
        if (session.getWorkflowData() == null || session.getWorkflowData().isBlank()) return new LinkedHashMap<>();
        try {
            return objectMapper.readValue(session.getWorkflowData(), new TypeReference<LinkedHashMap<String, String>>() {});
        } catch (Exception ignored) {
            return new LinkedHashMap<>();
        }
    }

    private void writeData(AiChatSessionEntity session, Map<String, String> data) {
        try {
            session.setWorkflowData(objectMapper.writeValueAsString(data));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to save conversation progress.");
        }
    }

    private void saveMessage(AiChatSessionEntity session, AiMessageRole role, String content) {
        AiChatMessageEntity message = new AiChatMessageEntity();
        message.setSession(session);
        message.setRole(role);
        message.setContent(content);
        messageRepo.save(message);
    }

    private void rememberCustomer(Map<String, String> data, CustomerEntity customer) {
        data.put("customerId", customer.getId().toString());
        data.put("customerName", customer.getName());
        data.put("customerNumber", customer.getCustomerNumber());
    }

    private Optional<LoanTypesEntity> matchLoanType(String input) {
        String normalized = normalize(input);
        return masterService.getAllLoanTypes().stream().filter(t ->
                normalized.contains(normalize(t.getCode())) || normalized.contains(normalize(t.getName()))).findFirst();
    }

    private Optional<LoanPurposesEntity> matchLoanPurpose(String input) {
        String normalized = normalize(input);
        return masterService.getAllLoanPurposes().stream().filter(p ->
                normalized.contains(normalize(p.getCode())) || normalized.contains(normalize(p.getName()))).findFirst();
    }

    private List<String> purposeOptions() {
        return masterService.getAllLoanPurposes().stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                .map(p -> p.getName() + " (" + p.getCode() + ")").toList();
    }

    static double parseAmount(String input) {
        if (input == null) return 0;
        String normalized = input.toLowerCase(Locale.ROOT).replace("₹", "").replace(",", "");
        Matcher matcher = NUMBER.matcher(normalized);
        if (!matcher.find()) return 0;
        double value = Double.parseDouble(matcher.group(1));
        if (normalized.contains("crore") || normalized.matches(".*\\bcr\\b.*")) value *= 10_000_000;
        else if (normalized.contains("lakh") || normalized.contains("lac")) value *= 100_000;
        else if (normalized.matches(".*\\d\\s*k\\b.*")) value *= 1_000;
        return value;
    }

    private LocalDate parseDate(String input) {
        try {
            if (input.contains("/")) return LocalDate.parse(input, DateTimeFormatter.ofPattern("d/M/uuuu"));
            return LocalDate.parse(input);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Enter date of birth in DD/MM/YYYY format.");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    private String digits(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private boolean isRestart(String message) {
        String n = normalize(message);
        return n.equals("restart") || n.equals("start over") || n.equals("cancel");
    }

    private String mask(String value) {
        if (value == null || value.length() <= 4) return value;
        return "*".repeat(value.length() - 4) + value.substring(value.length() - 4);
    }
}
