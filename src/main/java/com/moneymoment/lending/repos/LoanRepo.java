package com.moneymoment.lending.repos;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.moneymoment.lending.entities.LoanEntity;

@Repository
public interface LoanRepo extends JpaRepository<LoanEntity, Long>, JpaSpecificationExecutor<LoanEntity> {

    Optional<LoanEntity> findByLoanNumber(String loanNumber);

    List<LoanEntity> findByCustomerId(Long customerId);

}
