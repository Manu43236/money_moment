package com.moneymoment.lending.credit_assessment.repos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moneymoment.lending.credit_assessment.entities.CreditAssessmentEntity;

import java.util.Optional;

@Repository
public interface CreditAssessmentRepository extends JpaRepository<CreditAssessmentEntity, Long> {

    Optional<CreditAssessmentEntity> findByAssessmentNumber(String assessmentNumber);

    Optional<CreditAssessmentEntity> findByLoanId(Long loanId);
}
