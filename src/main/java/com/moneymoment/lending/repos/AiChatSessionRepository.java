package com.moneymoment.lending.repos;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moneymoment.lending.entities.AiChatSessionEntity;

@Repository
public interface AiChatSessionRepository extends JpaRepository<AiChatSessionEntity, Long> {
    Optional<AiChatSessionEntity> findBySessionUuid(String sessionUuid);
}
