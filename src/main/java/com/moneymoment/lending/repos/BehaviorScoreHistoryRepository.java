package com.moneymoment.lending.repos;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moneymoment.lending.entities.BehaviorScoreHistoryEntity;

public interface BehaviorScoreHistoryRepository extends JpaRepository<BehaviorScoreHistoryEntity, Long> {
    List<BehaviorScoreHistoryEntity> findTop20ByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
