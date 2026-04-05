package com.moneymoment.lending.repos;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moneymoment.lending.entities.AiKycExtractionEntity;
import com.moneymoment.lending.entities.AiChatSessionEntity;

@Repository
public interface AiKycExtractionRepository extends JpaRepository<AiKycExtractionEntity, Long> {
    Optional<AiKycExtractionEntity> findBySession(AiChatSessionEntity session);
}
