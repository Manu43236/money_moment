package com.moneymoment.lending.master.repos;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moneymoment.lending.master.entities.TenureMasterEntity;

@Repository
public interface TenureMasterRepository extends JpaRepository<TenureMasterEntity, Long> {
List<TenureMasterEntity> findByLoanType_CodeAndIsActive(String loanTypeCode, Boolean isActive);
}