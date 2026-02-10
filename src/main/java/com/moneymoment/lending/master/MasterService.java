package com.moneymoment.lending.master;

import java.util.List;

import org.springframework.stereotype.Service;

import com.moneymoment.lending.common.exception.ResourceNotFoundException;
import com.moneymoment.lending.master.entities.DisbursementModesEntity;
import com.moneymoment.lending.master.entities.DocumentTypesEntity;
import com.moneymoment.lending.master.entities.InterestRateConfigEntity;
import com.moneymoment.lending.master.entities.LoanPurposesEntity;
import com.moneymoment.lending.master.entities.LoanStatusesEntity;
import com.moneymoment.lending.master.entities.LoanTypesEntity;
import com.moneymoment.lending.master.entities.ProcessingFeeConfigEntity;
import com.moneymoment.lending.master.entities.TenureMasterEntity;
import com.moneymoment.lending.master.repos.DisbursementModesRepo;
import com.moneymoment.lending.master.repos.DocumentTypesRepo;
import com.moneymoment.lending.master.repos.InterestRateConfigRepository;
import com.moneymoment.lending.master.repos.LoanPurposesRepo;
import com.moneymoment.lending.master.repos.LoanStatusesRepo;
import com.moneymoment.lending.master.repos.LoneTypeRepo;
import com.moneymoment.lending.master.repos.ProcessingFeeConfigRepository;
import com.moneymoment.lending.master.repos.TenureMasterRepository;

import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MasterService {

        private final LoneTypeRepo loanTypeRepo;
        private final LoanPurposesRepo loanPurposeRepo;
        private final DisbursementModesRepo disbursementModeRepo;
        private final LoanStatusesRepo loanStatusRepo;

        private final TenureMasterRepository tenureMasterRepo;
        private final ProcessingFeeConfigRepository processingFeeConfigRepo;

        private final InterestRateConfigRepository interestRateConfigRepo;

        private final DocumentTypesRepo documentTypesRepo;

        // Constructor with all 4 repos
        MasterService(LoneTypeRepo loanTypeRepo, LoanPurposesRepo loanPurposeRepo,
                        DisbursementModesRepo disbursementModeRepo, LoanStatusesRepo loanStatusRepo,
                        InterestRateConfigRepository interestRateConfigRepo,
                        TenureMasterRepository tenureMasterRepo,
                        DocumentTypesRepo documentTypesRepo,
                        ProcessingFeeConfigRepository processingFeeConfigRepo) {
                this.loanTypeRepo = loanTypeRepo;
                this.loanPurposeRepo = loanPurposeRepo;
                this.disbursementModeRepo = disbursementModeRepo;
                this.loanStatusRepo = loanStatusRepo;
                this.interestRateConfigRepo = interestRateConfigRepo;
                this.tenureMasterRepo = tenureMasterRepo;
                this.processingFeeConfigRepo = processingFeeConfigRepo;
                this.documentTypesRepo = documentTypesRepo;
        }

        public List<LoanTypesEntity> getAllLoanTypes() {
                return loanTypeRepo.findAll();
        }

        public List<LoanPurposesEntity> getAllLoanPurposes() {
                return loanPurposeRepo.findAll();
        }

        public List<DisbursementModesEntity> getAllDisbursementModes() {
                return disbursementModeRepo.findAll();
        }

        public List<LoanStatusesEntity> getAllLoanStatuses() {
                return loanStatusRepo.findAll();
        }

        // Get available tenures for a loan type
        public List<TenureMasterEntity> getAvailableTenures(String loanTypeCode) {
                return tenureMasterRepo.findByLoanType_CodeAndIsActive(loanTypeCode, true);
        }

        // Get processing fee config for a loan type
        public ProcessingFeeConfigEntity getProcessingFeeConfig(String loanTypeCode) {
                return processingFeeConfigRepo.findByLoanType_CodeAndIsActive(loanTypeCode, true)
                                .orElseThrow(() -> new RuntimeException("Processing fee config not found"));
        }

        // Get all interest rate configs (we'll add smart lookup later)
        public List<InterestRateConfigEntity> getAllInterestRateConfigs() {
                return interestRateConfigRepo.findAll();
        }

        // Find applicable interest rate (based on loan type, credit score, amount,
        // tenure)
        public Double getApplicableInterestRate(String loanTypeCode, Double creditScore,
                        Double loanAmount, Integer tenureMonths) {

                List<InterestRateConfigEntity> allConfigs = interestRateConfigRepo.findAll();

                List<InterestRateConfigEntity> matches = allConfigs.stream()
                                .filter(config -> config.getLoanType().getCode().equals(loanTypeCode))
                                .filter(config -> config.getIsActive())
                                .filter(config -> creditScore >= config.getMinCreditScore()
                                                && creditScore <= config.getMaxCreditScore())
                                .filter(config -> loanAmount >= config.getMinLoanAmount()
                                                && loanAmount <= config.getMaxLoanAmount())
                                .filter(config -> config.getTenureMonths().equals(tenureMonths))

                                .collect(Collectors.toList());
                System.out.println("Matching configs: " + matches.size());
                matches.forEach(m -> System.out.println(
                                "Match: " + m.getId() + ", Score range: " + m.getMinCreditScore() + "-"
                                                + m.getMaxCreditScore()));

                // Filter matching configs
                Optional<InterestRateConfigEntity> matchingConfig = allConfigs.stream()
                                .filter(config -> config.getLoanType().getCode().equals(loanTypeCode))
                                .filter(config -> config.getIsActive())
                                .filter(config -> creditScore >= config.getMinCreditScore()
                                                && creditScore <= config.getMaxCreditScore())
                                .filter(config -> loanAmount >= config.getMinLoanAmount()
                                                && loanAmount <= config.getMaxLoanAmount())
                                .filter(config -> config.getTenureMonths().equals(tenureMonths))
                                .findFirst();

                return matchingConfig
                                .map(InterestRateConfigEntity::getInterestRate)
                                .orElseThrow(() -> new RuntimeException(
                                                "No interest rate config found for given criteria"));
        }

        // Get all document types
        public List<DocumentTypesEntity> getAllDocumentTypes() {
                return documentTypesRepo.findAll();
        }

        // Get document types by applicable for (e.g., "KYC", "Income Proof")
        public List<DocumentTypesEntity> getDocumentTypesByApplicableFor(String applicableFor) {
                return documentTypesRepo.findByApplicableFor(applicableFor);
        }

        public DocumentTypesEntity getDocumentTypesByCode(String code) {
                return documentTypesRepo.findByCode(code)
                                .orElseThrow(() -> new ResourceNotFoundException("DocumentType", "code", code));
        }

}