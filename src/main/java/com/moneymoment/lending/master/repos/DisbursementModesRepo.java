package com.moneymoment.lending.master.repos;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moneymoment.lending.master.entities.DisbursementModesEntity;

@Repository
public interface DisbursementModesRepo extends JpaRepository<DisbursementModesEntity, Long> {

    Optional<DisbursementModesEntity> findByCode(String code);
    
}