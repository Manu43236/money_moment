package com.moneymoment.lending.credit_assessment.services;

import com.moneymoment.lending.credit_assessment.repos.CreditAssessmentRepository;
import com.moneymoment.lending.loans.repos.LoanRepo;
import com.moneymoment.lending.master.repos.LoanPurposesRepo;
import org.springframework.stereotype.Service;

@Service
public class CreditAssessmentService {

    private final LoanPurposesRepo loanPurposesRepo;

    private final LoanRepo loanRepo;

    private final CreditAssessmentRepository creditAssessmentRepository;

    CreditAssessmentService(CreditAssessmentRepository creditAssessmentRepository, LoanRepo loanRepo, LoanPurposesRepo loanPurposesRepo){
        this.creditAssessmentRepository = creditAssessmentRepository;
        this.loanRepo = loanRepo;
        this.loanPurposesRepo = loanPurposesRepo;
    }

    
    
}
