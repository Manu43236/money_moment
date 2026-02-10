package com.moneymoment.lending.master.repos;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moneymoment.lending.master.entities.LoanTypesEntity;

@Repository
public interface LoneTypeRepo extends JpaRepository<LoanTypesEntity, Long> {

    Optional<LoanTypesEntity> findByCode(String code);
    
}