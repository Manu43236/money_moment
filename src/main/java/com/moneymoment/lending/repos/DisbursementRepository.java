package com.moneymoment.lending.repos;

import com.moneymoment.lending.entities.DisbursementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DisbursementRepository extends JpaRepository<DisbursementEntity, Long> {
    
    Optional<DisbursementEntity> findByDisbursementNumber(String disbursementNumber);
    
    Optional<DisbursementEntity> findByLoanId(Long loanId);
    
    List<DisbursementEntity> findByStatus(String status);
    
    List<DisbursementEntity> findByDisbursedByUserId(Long userId);
}