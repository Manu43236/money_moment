package com.moneymoment.lending.master.repos;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moneymoment.lending.master.entities.InterestRateConfigEntity;

@Repository
public interface InterestRateConfigRepository extends JpaRepository<InterestRateConfigEntity, Long> {

    List<InterestRateConfigEntity> findByLoanTypeCodeAndIsActive(String loanTypeCode, Boolean isActive);
    // We'll add custom query later for finding rate based on criteria
}