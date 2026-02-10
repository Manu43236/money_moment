package com.moneymoment.lending.loans.repos;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moneymoment.lending.loans.Entity.LoanEntity;

@Repository
public interface LoanRepo extends JpaRepository<LoanEntity, Long> {

    Optional<LoanEntity> findByLoanNumber(String loanNumber);

    List<LoanEntity> findByCustomerId(Long customerId);

}
