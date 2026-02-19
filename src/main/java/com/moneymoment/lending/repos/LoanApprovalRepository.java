package com.moneymoment.lending.repos;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moneymoment.lending.entities.LoanApprovalEntity;

@Repository
public interface LoanApprovalRepository extends JpaRepository<LoanApprovalEntity, Long> {

    // Get all approvals for a loan (approval history)
    List<LoanApprovalEntity> findByLoanIdOrderByCreatedAtAsc(Long loanId);

    // Get approvals by user (who approved what)
    List<LoanApprovalEntity> findByApprovedByUserId(Long userId);

    // Count approvals for a loan
    Long countByLoanIdAndAction(Long loanId, String action);
}
