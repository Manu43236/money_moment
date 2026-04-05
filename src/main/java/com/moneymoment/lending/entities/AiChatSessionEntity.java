package com.moneymoment.lending.entities;

import java.time.LocalDateTime;

import com.moneymoment.lending.common.enums.AiSessionIntent;
import com.moneymoment.lending.common.enums.AiSessionStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ai_chat_sessions")
public class AiChatSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 36)
    private String sessionUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AiSessionIntent intent = AiSessionIntent.UNKNOWN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AiSessionStatus status = AiSessionStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_customer_id")
    private CustomerEntity createdCustomer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_loan_id")
    private LoanEntity createdLoan;

    @Column
    private LocalDateTime expiresAt;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        expiresAt = LocalDateTime.now().plusMinutes(30);
        if (intent == null) intent = AiSessionIntent.UNKNOWN;
        if (status == null) status = AiSessionStatus.ACTIVE;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        expiresAt = LocalDateTime.now().plusMinutes(30);
    }
}
