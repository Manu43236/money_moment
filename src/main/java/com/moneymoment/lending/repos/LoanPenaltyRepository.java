package com.moneymoment.lending.repos;

import com.moneymoment.lending.entities.LoanPenaltyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LoanPenaltyRepository extends JpaRepository<LoanPenaltyEntity, Long> {

    List<LoanPenaltyEntity> findByLoanIdOrderByAppliedDateDesc(Long loanId);

    List<LoanPenaltyEntity> findByEmiScheduleId(Long emiScheduleId);

    List<LoanPenaltyEntity> findByCustomerId(Long customerId);

    List<LoanPenaltyEntity> findByIsWaived(Boolean isWaived);

    List<LoanPenaltyEntity> findByIsPaid(Boolean isPaid);

    List<LoanPenaltyEntity> findByAppliedDateBetween(LocalDate startDate, LocalDate endDate);

    List<LoanPenaltyEntity> findByLoanIdAndIsPaid(Long loanId, Boolean isPaid);
}