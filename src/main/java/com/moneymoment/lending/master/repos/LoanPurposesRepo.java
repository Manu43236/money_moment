package com.moneymoment.lending.master.repos;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moneymoment.lending.master.entities.LoanPurposesEntity;

@Repository
public interface LoanPurposesRepo extends JpaRepository<LoanPurposesEntity, Long> {

    Optional<LoanPurposesEntity> findByCode(String code);
    
}