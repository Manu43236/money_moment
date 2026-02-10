package com.moneymoment.lending.master.repos;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moneymoment.lending.master.entities.LoanStatusesEntity;

@Repository
public interface LoanStatusesRepo extends JpaRepository<LoanStatusesEntity, Long> {

    Optional<LoanStatusesEntity> findByCode(String code);

}