package com.moneymoment.lending.master.repos;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moneymoment.lending.master.entities.DocumentTypesEntity;

@Repository
public interface DocumentTypesRepo extends JpaRepository<DocumentTypesEntity, Long> {

    Optional<DocumentTypesEntity> findByCode(String code);

    List<DocumentTypesEntity> findByApplicableFor(String applicableFor);
}