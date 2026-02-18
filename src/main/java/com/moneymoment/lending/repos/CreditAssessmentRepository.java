package com.moneymoment.lending.repos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moneymoment.lending.entities.CreditAssessmentEntity;
import com.moneymoment.lending.entities.LoanEntity;

import java.util.Optional;

@Repository
public interface CreditAssessmentRepository extends JpaRepository<CreditAssessmentEntity, Long> {

    Optional<CreditAssessmentEntity> findByAssessmentNumber(String assessmentNumber);

    Optional<CreditAssessmentEntity> findByLoanId(Long loanId);

    Optional<CreditAssessmentEntity> findTopByLoanIdOrderByCreatedAtDesc(Long id);
}
