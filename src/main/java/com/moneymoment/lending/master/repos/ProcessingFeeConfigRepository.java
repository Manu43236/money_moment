package com.moneymoment.lending.master.repos;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moneymoment.lending.master.entities.ProcessingFeeConfigEntity;

@Repository
public interface ProcessingFeeConfigRepository extends JpaRepository<ProcessingFeeConfigEntity, Long> {
    Optional<ProcessingFeeConfigEntity> findByLoanType_CodeAndIsActive(String loanTypeCode, Boolean isActive);
}