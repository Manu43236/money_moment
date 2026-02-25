package com.moneymoment.lending.repos;

import com.moneymoment.lending.entities.PenaltyConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PenaltyConfigRepository extends JpaRepository<PenaltyConfigEntity, Long> {

    Optional<PenaltyConfigEntity> findByPenaltyCode(String penaltyCode);

    List<PenaltyConfigEntity> findByIsActive(Boolean isActive);

    List<PenaltyConfigEntity> findByPenaltyType(String penaltyType);

    List<PenaltyConfigEntity> findByChargeType(String chargeType);
}