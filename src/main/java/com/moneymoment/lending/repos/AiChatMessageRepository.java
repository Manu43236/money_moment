package com.moneymoment.lending.repos;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moneymoment.lending.entities.AiChatMessageEntity;
import com.moneymoment.lending.entities.AiChatSessionEntity;

@Repository
public interface AiChatMessageRepository extends JpaRepository<AiChatMessageEntity, Long> {
    List<AiChatMessageEntity> findBySessionOrderByCreatedAtAsc(AiChatSessionEntity session);
}
