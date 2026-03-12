package com.moneymoment.lending.repos;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moneymoment.lending.entities.EodPhaseLogEntity;

@Repository
public interface EodPhaseLogRepository extends JpaRepository<EodPhaseLogEntity, Long> {
    List<EodPhaseLogEntity> findByJobIdOrderByPhaseNumberAsc(String jobId);
    Optional<EodPhaseLogEntity> findByJobIdAndPhaseNumber(String jobId, Integer phaseNumber);
}
