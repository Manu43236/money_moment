package com.moneymoment.lending.seed;

import com.moneymoment.lending.common.enums.EmploymentType;
import com.moneymoment.lending.common.utils.EmiCalculator;
import com.moneymoment.lending.entities.*;
import com.moneymoment.lending.master.entities.*;
import com.moneymoment.lending.master.repos.*;
import com.moneymoment.lending.repos.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
public class DataSeederService {

    private final CustomerRepository customerRepository;
    private final LoanRepo loanRepo;
    private final EmiScheduleRepository emiScheduleRepository;
    private final UserRepository userRepository;
    private final LoneTypeRepo loanTypeRepo;
    private final LoanPurposesRepo loanPurposesRepo;
    private final LoanStatusesRepo loanStatusesRepo;
    private final DisbursementModesRepo disbursementModesRepo;

    public DataSeederService(CustomerRepository customerRepository, LoanRepo loanRepo,
            EmiScheduleRepository emiScheduleRepository, UserRepository userRepository,
            LoneTypeRepo loanTypeRepo, LoanPurposesRepo loanPurposesRepo,
            LoanStatusesRepo loanStatusesRepo, DisbursementModesRepo disbursementModesRepo) {
        this.customerRepository = customerRepository;
        this.loanRepo = loanRepo;
        this.emiScheduleRepository = emiScheduleRepository;
        this.userRepository = userRepository;
        this.loanTypeRepo = loanTypeRepo;
        this.loanPurposesRepo = loanPurposesRepo;
        this.loanStatusesRepo = loanStatusesRepo;
        this.disbursementModesRepo = disbursementModesRepo;
    }

    public boolean isAlreadySeeded() {
        return customerRepository.count() >= 20;
    }

    @Transactional
    public Map<String, Object> seedDemoData() {
        if (isAlreadySeeded()) {
            return Map.of("status", "SKIPPED", "message", "Demo data already exists (20+ customers found)");
        }

        List<LoanTypesEntity> loanTypes = loanTypeRepo.findAll();
        List<LoanPurposesEntity> loanPurposes = loanPurposesRepo.findAll();

        if (loanTypes.isEmpty() || loanPurposes.isEmpty()) {
            return Map.of("status", "ERROR",
                    "message", "Master data missing. Please set up loan types and purposes first via Admin > Masters.");
        }

        LoanStatusesEntity activeStatus = loanStatusesRepo.findByCode("ACTIVE")
                .orElseThrow(() -> new RuntimeException("ACTIVE loan status missing from master data"));
        LoanStatusesEntity overdueStatus = loanStatusesRepo.findByCode("OVERDUE")
                .orElseThrow(() -> new RuntimeException("OVERDUE loan status missing from master data"));
        LoanStatusesEntity npaStatus = loanStatusesRepo.findByCode("NPA")
                .orElseThrow(() -> new RuntimeException("NPA loan status missing from master data"));
        LoanStatusesEntity closedStatus = loanStatusesRepo.findByCode("CLOSED")
                .orElseThrow(() -> new RuntimeException("CLOSED loan status missing from master data"));

        DisbursementModesEntity disbMode = disbursementModesRepo.findByCode("NEFT")
                .orElseGet(() -> disbursementModesRepo.findAll().stream().findFirst()
                        .orElseThrow(() -> new RuntimeException("No disbursement mode found in master data")));

        UserEntity systemUser = userRepository.findAll().stream().findFirst().orElse(null);

        // --- Create 30 customers ---
        List<CustomerEntity> customers = buildCustomers();
        customerRepository.saveAll(customers);
        log.info("[SEED] Created {} customers", customers.size());

        LocalDate today = LocalDate.now();
        int loanCount = 0;
        int emiCount = 0;

        // ================================================================
        // ACTIVE loans: 180 total, EMI days 1–28 spread
        // Days 1–12 get 7 loans each (84), days 13–28 get 6 each (96) = 180
        // ================================================================
        for (int emiDay = 1; emiDay <= 28; emiDay++) {
            int loansForDay = (emiDay <= 12) ? 7 : 6;
            for (int j = 0; j < loansForDay; j++) {
                CustomerEntity customer = customers.get(loanCount % customers.size());
                LoanTypesEntity loanType = loanTypes.get(loanCount % loanTypes.size());
                LoanPurposesEntity purpose = loanPurposes.get(loanCount % loanPurposes.size());

                // Disbursed 2–6 months ago so EMI for this month is in March
                int monthsBack = 2 + (loanCount % 5);
                LocalDate disbDate = today.minusMonths(monthsBack).withDayOfMonth(Math.min(emiDay, 28));
                LocalDate firstEmiDate = disbDate.plusMonths(1).withDayOfMonth(emiDay);

                double amount = pickAmount(loanCount);
                double rate = 10.5 + (loanCount % 8); // 10.5%–17.5%
                int tenure = pickTenure(loanCount);

                LoanEntity loan = buildLoan(
                        loanNumber(loanCount), customer, loanType, purpose,
                        activeStatus, disbMode, amount, rate, tenure,
                        disbDate, firstEmiDate, systemUser);
                loan = loanRepo.save(loan);

                List<EmiScheduleEntity> emis = buildEmiSchedule(
                        loan, customer, firstEmiDate, amount, rate, tenure, today, "ACTIVE");
                emiScheduleRepository.saveAll(emis);
                updateLoanFromEmis(loan, emis, today);
                loanRepo.save(loan);

                loanCount++;
                emiCount += emis.size();
            }
        }
        log.info("[SEED] Created 180 ACTIVE loans");

        // ================================================================
        // OVERDUE loans: 60 total, EMI days 1–28 spread (2–3 per day)
        // Disbursed 5–8 months ago, last 1–3 EMIs missed
        // ================================================================
        for (int i = 0; i < 60; i++) {
            CustomerEntity customer = customers.get(loanCount % customers.size());
            LoanTypesEntity loanType = loanTypes.get(loanCount % loanTypes.size());
            LoanPurposesEntity purpose = loanPurposes.get(loanCount % loanPurposes.size());

            int emiDay = (i % 28) + 1;
            int monthsBack = 5 + (i % 4);
            LocalDate disbDate = today.minusMonths(monthsBack).withDayOfMonth(Math.min(emiDay, 28));
            LocalDate firstEmiDate = disbDate.plusMonths(1).withDayOfMonth(emiDay);

            double amount = pickAmount(loanCount);
            double rate = 12.0 + (loanCount % 6);
            int tenure = pickTenure(loanCount);

            LoanEntity loan = buildLoan(
                    loanNumber(loanCount), customer, loanType, purpose,
                    overdueStatus, disbMode, amount, rate, tenure,
                    disbDate, firstEmiDate, systemUser);
            loan = loanRepo.save(loan);

            List<EmiScheduleEntity> emis = buildEmiSchedule(
                    loan, customer, firstEmiDate, amount, rate, tenure, today, "OVERDUE");
            emiScheduleRepository.saveAll(emis);
            updateLoanFromEmis(loan, emis, today);
            loanRepo.save(loan);

            loanCount++;
            emiCount += emis.size();
        }
        log.info("[SEED] Created 60 OVERDUE loans");

        // ================================================================
        // NPA loans: 30 total, DPD > 90 days
        // Disbursed 9–13 months ago, paid 2–4 EMIs then stopped
        // ================================================================
        for (int i = 0; i < 30; i++) {
            CustomerEntity customer = customers.get(loanCount % customers.size());
            LoanTypesEntity loanType = loanTypes.get(loanCount % loanTypes.size());
            LoanPurposesEntity purpose = loanPurposes.get(loanCount % loanPurposes.size());

            int emiDay = (i % 28) + 1;
            int monthsBack = 9 + (i % 5);
            LocalDate disbDate = today.minusMonths(monthsBack).withDayOfMonth(Math.min(emiDay, 28));
            LocalDate firstEmiDate = disbDate.plusMonths(1).withDayOfMonth(emiDay);

            double amount = pickAmount(loanCount);
            double rate = 13.0 + (loanCount % 5);
            int tenure = pickTenure(loanCount);

            LoanEntity loan = buildLoan(
                    loanNumber(loanCount), customer, loanType, purpose,
                    npaStatus, disbMode, amount, rate, tenure,
                    disbDate, firstEmiDate, systemUser);
            loan = loanRepo.save(loan);

            List<EmiScheduleEntity> emis = buildEmiSchedule(
                    loan, customer, firstEmiDate, amount, rate, tenure, today, "NPA");
            emiScheduleRepository.saveAll(emis);
            updateLoanFromEmis(loan, emis, today);
            loanRepo.save(loan);

            loanCount++;
            emiCount += emis.size();
        }
        log.info("[SEED] Created 30 NPA loans");

        // ================================================================
        // CLOSED loans: 30 total, all EMIs paid
        // 12-month tenure, disbursed 15–20 months ago
        // ================================================================
        for (int i = 0; i < 30; i++) {
            CustomerEntity customer = customers.get(loanCount % customers.size());
            LoanTypesEntity loanType = loanTypes.get(loanCount % loanTypes.size());
            LoanPurposesEntity purpose = loanPurposes.get(loanCount % loanPurposes.size());

            int emiDay = (i % 28) + 1;
            int monthsBack = 15 + (i % 6);
            LocalDate disbDate = today.minusMonths(monthsBack).withDayOfMonth(Math.min(emiDay, 28));
            LocalDate firstEmiDate = disbDate.plusMonths(1).withDayOfMonth(emiDay);

            double amount = pickAmount(loanCount);
            double rate = 11.0 + (loanCount % 5);
            int closedTenure = 12;

            LoanEntity loan = buildLoan(
                    loanNumber(loanCount), customer, loanType, purpose,
                    closedStatus, disbMode, amount, rate, closedTenure,
                    disbDate, firstEmiDate, systemUser);
            loan.setClosedDate(firstEmiDate.plusMonths(closedTenure - 1).atStartOfDay());
            loan = loanRepo.save(loan);

            List<EmiScheduleEntity> emis = buildEmiSchedule(
                    loan, customer, firstEmiDate, amount, rate, closedTenure, today, "CLOSED");
            emiScheduleRepository.saveAll(emis);
            updateLoanFromEmis(loan, emis, today);
            loanRepo.save(loan);

            loanCount++;
            emiCount += emis.size();
        }
        log.info("[SEED] Created 30 CLOSED loans");

        log.info("[SEED] Complete: {} customers, {} loans, {} EMI records", customers.size(), loanCount, emiCount);

        return Map.of(
                "status", "SUCCESS",
                "customers", customers.size(),
                "loans", loanCount,
                "emiRecords", emiCount,
                "breakdown", Map.of(
                        "ACTIVE", 180,
                        "OVERDUE", 60,
                        "NPA", 30,
                        "CLOSED", 30));
    }

    // ----------------------------------------------------------------
    // EMI schedule builder — reducing balance method
    // ----------------------------------------------------------------
    private List<EmiScheduleEntity> buildEmiSchedule(
            LoanEntity loan, CustomerEntity customer, LocalDate firstEmiDate,
            double principal, double annualRate, int tenure,
            LocalDate today, String bucket) {

        double monthlyRate = annualRate / 12.0 / 100.0;
        double emiAmount = EmiCalculator.calculateEmi(principal, annualRate, tenure);
        double outstanding = principal;

        // For OVERDUE: last N EMIs are missed (1–3 based on loan id)
        int missedFromEmi = (int) (tenure - (1 + (loan.getId() % 3))); // index after which emis are overdue
        // For NPA: paid only first 2–4 EMIs
        int npaLastPaidEmi = (int) (2 + (loan.getId() % 3));

        List<EmiScheduleEntity> result = new ArrayList<>();

        for (int i = 1; i <= tenure; i++) {
            LocalDate dueDate = firstEmiDate.plusMonths(i - 1);

            double interest = Math.round(outstanding * monthlyRate * 100.0) / 100.0;
            double principalPart = Math.round((emiAmount - interest) * 100.0) / 100.0;
            outstanding = Math.round((outstanding - principalPart) * 100.0) / 100.0;
            if (outstanding < 0) outstanding = 0.0;

            EmiScheduleEntity emi = new EmiScheduleEntity();
            emi.setLoan(loan);
            emi.setCustomer(customer);
            emi.setEmiNumber(i);
            emi.setDueDate(dueDate);
            emi.setPrincipalAmount(principalPart);
            emi.setInterestAmount(interest);
            emi.setEmiAmount(emiAmount);
            emi.setOutstandingPrincipal(outstanding);
            emi.setCreatedBy("SEED");

            switch (bucket) {
                case "CLOSED" -> {
                    emi.setStatus("PAID");
                    emi.setPaidDate(dueDate.plusDays(1));
                    emi.setAmountPaid(emiAmount);
                    emi.setDaysPastDue(0);
                }
                case "ACTIVE" -> {
                    if (dueDate.isBefore(today)) {
                        emi.setStatus("PAID");
                        emi.setPaidDate(dueDate.plusDays(2));
                        emi.setAmountPaid(emiAmount);
                        emi.setDaysPastDue(0);
                    } else {
                        emi.setStatus("PENDING");
                        emi.setDaysPastDue(0);
                    }
                }
                case "OVERDUE" -> {
                    if (i <= missedFromEmi && dueDate.isBefore(today)) {
                        emi.setStatus("PAID");
                        emi.setPaidDate(dueDate.plusDays(2));
                        emi.setAmountPaid(emiAmount);
                        emi.setDaysPastDue(0);
                    } else if (dueDate.isBefore(today)) {
                        int dpd = (int) (today.toEpochDay() - dueDate.toEpochDay());
                        emi.setStatus("OVERDUE");
                        emi.setDaysPastDue(dpd);
                    } else {
                        emi.setStatus("PENDING");
                        emi.setDaysPastDue(0);
                    }
                }
                case "NPA" -> {
                    if (i <= npaLastPaidEmi && dueDate.isBefore(today)) {
                        emi.setStatus("PAID");
                        emi.setPaidDate(dueDate.plusDays(2));
                        emi.setAmountPaid(emiAmount);
                        emi.setDaysPastDue(0);
                    } else if (dueDate.isBefore(today)) {
                        int dpd = (int) (today.toEpochDay() - dueDate.toEpochDay());
                        emi.setStatus("OVERDUE");
                        emi.setDaysPastDue(dpd);
                    } else {
                        emi.setStatus("PENDING");
                        emi.setDaysPastDue(0);
                    }
                }
            }

            result.add(emi);
        }

        return result;
    }

    // ----------------------------------------------------------------
    // Update loan tracking fields from its EMI schedule
    // ----------------------------------------------------------------
    private void updateLoanFromEmis(LoanEntity loan, List<EmiScheduleEntity> emis, LocalDate today) {
        long paidCount = emis.stream().filter(e -> "PAID".equals(e.getStatus())).count();
        long overdueCount = emis.stream().filter(e -> "OVERDUE".equals(e.getStatus())).count();

        double principalPaid = emis.stream()
                .filter(e -> "PAID".equals(e.getStatus()))
                .mapToDouble(EmiScheduleEntity::getPrincipalAmount)
                .sum();

        double totalOverdue = emis.stream()
                .filter(e -> "OVERDUE".equals(e.getStatus()))
                .mapToDouble(EmiScheduleEntity::getEmiAmount)
                .sum();

        int maxDpd = emis.stream()
                .mapToInt(e -> e.getDaysPastDue() != null ? e.getDaysPastDue() : 0)
                .max().orElse(0);

        LocalDate lastPaidDate = emis.stream()
                .filter(e -> "PAID".equals(e.getStatus()) && e.getPaidDate() != null)
                .map(EmiScheduleEntity::getPaidDate)
                .max(Comparator.naturalOrder()).orElse(null);

        LocalDate nextDue = emis.stream()
                .filter(e -> "PENDING".equals(e.getStatus()) || "OVERDUE".equals(e.getStatus()))
                .map(EmiScheduleEntity::getDueDate)
                .min(Comparator.naturalOrder()).orElse(null);

        loan.setNumberOfPaidEmis((int) paidCount);
        loan.setNumberOfOverdueEmis((int) overdueCount);
        loan.setCurrentDpd(maxDpd);
        loan.setHighestDpd(maxDpd);
        loan.setTotalOverdueAmount(Math.round(totalOverdue * 100.0) / 100.0);
        loan.setTotalPenaltyAmount(0.0);
        loan.setOutstandingAmount(Math.max(0.0, Math.round((loan.getLoanAmount() - principalPaid) * 100.0) / 100.0));
        loan.setLastPaymentDate(lastPaidDate);
        loan.setNextDueDate(nextDue);
    }

    // ----------------------------------------------------------------
    // Build a loan entity (before save — no ID yet)
    // ----------------------------------------------------------------
    private LoanEntity buildLoan(String loanNumber, CustomerEntity customer,
            LoanTypesEntity loanType, LoanPurposesEntity purpose,
            LoanStatusesEntity status, DisbursementModesEntity disbMode,
            double amount, double rate, int tenure,
            LocalDate disbDate, LocalDate firstEmiDate, UserEntity systemUser) {

        double emiAmount = EmiCalculator.calculateEmi(amount, rate, tenure);
        double totalInterest = EmiCalculator.calculateTotalInterest(amount, emiAmount, tenure);
        double totalAmount = EmiCalculator.calculateTotalAmount(amount, emiAmount, tenure);

        LoanEntity loan = new LoanEntity();
        loan.setLoanNumber(loanNumber);
        loan.setCustomer(customer);
        loan.setLoanType(loanType);
        loan.setLoanPurpose(purpose);
        loan.setLoanStatus(status);
        loan.setDisbursementMode(disbMode);
        loan.setLoanAmount(amount);
        loan.setInterestRate(rate);
        loan.setTenureMonths(tenure);
        loan.setPurpose(purpose.getName());
        loan.setProcessingFee(Math.round(amount * 0.01 * 100.0) / 100.0);
        loan.setEmiAmount(emiAmount);
        loan.setTotalInterest(totalInterest);
        loan.setTotalAmount(totalAmount);
        loan.setOutstandingAmount(amount); // updated after EMI schedule built
        loan.setAppliedDate(disbDate.minusDays(15).atStartOfDay());
        loan.setApprovedDate(disbDate.minusDays(7).atStartOfDay());
        loan.setDisbursedDate(disbDate.atStartOfDay());
        loan.setFirstEmiDueDate(firstEmiDate);
        loan.setNextDueDate(firstEmiDate);
        loan.setNumberOfPaidEmis(0);
        loan.setNumberOfOverdueEmis(0);
        loan.setCurrentDpd(0);
        loan.setHighestDpd(0);
        loan.setTotalOverdueAmount(0.0);
        loan.setTotalPenaltyAmount(0.0);
        loan.setRepaymentFrequency("MONTHLY");
        loan.setOriginatingBranchCode("BR001");
        loan.setDisbursementAccountNumber("0000" + (100000000 + loanNumber.hashCode() % 900000000));
        loan.setDisbursementIfsc("HDFC0001234");
        loan.setCreatedBy("SEED");
        loan.setProcessedByUser(systemUser);
        return loan;
    }

    // ----------------------------------------------------------------
    // Loan amount buckets: ₹1L → ₹20L
    // ----------------------------------------------------------------
    private double pickAmount(int index) {
        double[] amounts = {100000, 150000, 200000, 300000, 500000,
                750000, 1000000, 1500000, 2000000, 2500000};
        return amounts[index % amounts.length];
    }

    // Tenure buckets: 12 → 60 months
    private int pickTenure(int index) {
        int[] tenures = {12, 18, 24, 36, 48, 60};
        return tenures[index % tenures.length];
    }

    private String loanNumber(int index) {
        return "LN-SEED-" + String.format("%04d", index + 1);
    }

    // ----------------------------------------------------------------
    // 30 seed customers — realistic Indian profiles
    // ----------------------------------------------------------------
    private List<CustomerEntity> buildCustomers() {
        // {name, phone, pan, aadhar, email, salary, employment, occupation, dob}
        Object[][] data = {
            {"Rajesh Kumar Singh",    "9876500001", "ABRKS1234A", "123456789001", "rajesh.singh@gmail.com",        75000,  "SALARIED",      "Software Engineer",     "1985-06-15"},
            {"Priya Sharma",          "9876500002", "BCRPS2345B", "123456789002", "priya.sharma@gmail.com",        65000,  "SALARIED",      "Bank Manager",          "1988-03-22"},
            {"Amit Verma",            "9876500003", "CDRAV3456C", "123456789003", "amit.verma@gmail.com",          90000,  "SALARIED",      "CA",                    "1982-11-10"},
            {"Sunita Patel",          "9876500004", "DESPS4567D", "123456789004", "sunita.patel@gmail.com",        45000,  "SALARIED",      "Teacher",               "1990-07-05"},
            {"Mohammed Hussain",      "9876500005", "EFSMH5678E", "123456789005", "m.hussain@gmail.com",           80000,  "SELF_EMPLOYED", "Business Owner",        "1979-02-28"},
            {"Lakshmi Iyer",          "9876500006", "FGTLI6789F", "123456789006", "lakshmi.iyer@gmail.com",        55000,  "SALARIED",      "Nurse",                 "1992-09-18"},
            {"Suresh Reddy",          "9876500007", "GHKSR7890G", "123456789007", "suresh.reddy@gmail.com",       120000,  "SELF_EMPLOYED", "Doctor",                "1975-04-12"},
            {"Anita Desai",           "9876500008", "HIDAD8901H", "123456789008", "anita.desai@gmail.com",         50000,  "SALARIED",      "HR Executive",          "1991-12-30"},
            {"Vijay Malhotra",        "9876500009", "IJSVM9012I", "123456789009", "vijay.malhotra@gmail.com",     150000,  "SELF_EMPLOYED", "Architect",             "1978-08-07"},
            {"Kavitha Nair",          "9876500010", "JKAKN0123J", "123456789010", "kavitha.nair@gmail.com",        42000,  "SALARIED",      "Accountant",            "1993-05-25"},
            {"Deepak Joshi",          "9876500011", "KLBDJ1234K", "123456789011", "deepak.joshi@gmail.com",        68000,  "SALARIED",      "Marketing Manager",     "1986-01-14"},
            {"Meera Krishnan",        "9876500012", "LMCMK2345L", "123456789012", "meera.krishnan@gmail.com",      38000,  "SALARIED",      "Graphic Designer",      "1994-10-08"},
            {"Ravi Shankar Gupta",    "9876500013", "MNDRG3456M", "123456789013", "ravi.gupta@gmail.com",          95000,  "SALARIED",      "IAS Officer",           "1981-07-19"},
            {"Pooja Agarwal",         "9876500014", "NOEPA4567N", "123456789014", "pooja.agarwal@gmail.com",       72000,  "SALARIED",      "Lawyer",                "1987-03-03"},
            {"Kiran Kumar Rao",       "9876500015", "OPFKR5678O", "123456789015", "kiran.rao@gmail.com",          110000,  "SELF_EMPLOYED", "IT Consultant",         "1980-11-27"},
            {"Neha Saxena",           "9876500016", "PQGNS6789P", "123456789016", "neha.saxena@gmail.com",         48000,  "SALARIED",      "Data Analyst",          "1995-06-11"},
            {"Sanjay Bhat",           "9876500017", "QRHSB7890Q", "123456789017", "sanjay.bhat@gmail.com",         88000,  "SALARIED",      "Product Manager",       "1983-09-23"},
            {"Rekha Menon",           "9876500018", "RSIRM8901R", "123456789018", "rekha.menon@gmail.com",         58000,  "SALARIED",      "Pharmacist",            "1989-04-16"},
            {"Prakash Choudhary",     "9876500019", "STJPC9012S", "123456789019", "prakash.c@gmail.com",           62000,  "SELF_EMPLOYED", "Contractor",            "1976-12-02"},
            {"Divya Pillai",          "9876500020", "TUKDP0123T", "123456789020", "divya.pillai@gmail.com",        53000,  "SALARIED",      "School Principal",      "1972-08-31"},
            {"Arun Tiwari",           "9876500021", "UVLAT1234U", "123456789021", "arun.tiwari@gmail.com",         78000,  "SALARIED",      "Sales Director",        "1984-02-17"},
            {"Bhavna Shah",           "9876500022", "VWMBS2345V", "123456789022", "bhavna.shah@gmail.com",         44000,  "SALARIED",      "Interior Designer",     "1992-07-09"},
            {"Ganesh Kulkarni",       "9876500023", "WXNGK3456W", "123456789023", "ganesh.kulkarni@gmail.com",    100000,  "SELF_EMPLOYED", "Chartered Engineer",    "1977-05-04"},
            {"Shobha Varghese",       "9876500024", "XYOSV4567X", "123456789024", "shobha.v@gmail.com",            36000,  "SALARIED",      "Administrative Asst",   "1996-11-20"},
            {"Harish Pandey",         "9876500025", "YZPHP5678Y", "123456789025", "harish.pandey@gmail.com",       85000,  "SALARIED",      "Branch Manager",        "1983-03-13"},
            {"Indira Narayanan",      "9876500026", "ZAQIN6789Z", "123456789026", "indira.n@gmail.com",            47000,  "SALARIED",      "Radiologist",           "1971-10-07"},
            {"Ramesh Yadav",          "9876500027", "ABRRY7890A", "123456789027", "ramesh.yadav@gmail.com",        55000,  "SELF_EMPLOYED", "Transport Operator",    "1973-01-29"},
            {"Chitra Subramaniam",    "9876500028", "BCSCS8901B", "123456789028", "chitra.s@gmail.com",            61000,  "SALARIED",      "Quality Engineer",      "1988-06-24"},
            {"Nitin Gokhale",         "9876500029", "CDTNG9012C", "123456789029", "nitin.gokhale@gmail.com",       92000,  "SALARIED",      "Investment Banker",     "1980-04-18"},
            {"Farzana Shaikh",        "9876500030", "DEFFS0123D", "123456789030", "farzana.s@gmail.com",           43000,  "SALARIED",      "Physiotherapist",       "1993-08-05"},
        };

        List<CustomerEntity> customers = new ArrayList<>();
        for (int i = 0; i < data.length; i++) {
            Object[] row = data[i];
            CustomerEntity c = new CustomerEntity();
            c.setCustomerNumber("CUST-SEED-" + String.format("%03d", i + 1));
            c.setName((String) row[0]);
            c.setPhone((String) row[1]);
            c.setPan((String) row[2]);
            c.setAadhar((String) row[3]);
            c.setEmail((String) row[4]);
            c.setMonthlySalary(((Number) row[5]).doubleValue());
            c.setEmploymentType(EmploymentType.valueOf((String) row[6]));
            c.setOccupation((String) row[7]);
            c.setDob(java.time.LocalDate.parse((String) row[8]).atStartOfDay());
            c.setCreditScore(758.5);
            c.setHomeBranchCode("BR001");
            c.setAddress("123 Seed Street, Demo City - 400001");
            c.setIsActive(true);
            c.setCreatedBy("SEED");
            customers.add(c);
        }
        return customers;
    }
}
