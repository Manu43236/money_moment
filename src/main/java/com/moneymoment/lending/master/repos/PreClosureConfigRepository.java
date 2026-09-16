package com.moneymoment.lending.master.repos;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moneymoment.lending.master.entities.PreClosureConfigEntity;

@Repository
public interface PreClosureConfigRepository extends JpaRepository<PreClosureConfigEntity, Long> {
    Optional<PreClosureConfigEntity> findByLoanType_CodeAndIsActive(String loanTypeCode, Boolean isActive);
}
