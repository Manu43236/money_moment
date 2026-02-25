package com.moneymoment.lending.entities;

import com.moneymoment.lending.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(name = "penalty_config")
public class PenaltyConfigEntity extends BaseEntity {

    @Column(name = "penalty_code", unique = true, nullable = false, length = 30)
    private String penaltyCode;

    @Column(name = "penalty_name", nullable = false, length = 100)
    private String penaltyName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "charge_type", nullable = false, length = 20)
    private String chargeType; // FLAT, PERCENTAGE

    @Column(name = "charge_value", nullable = false)
    private Double chargeValue;

    @Column(name = "applicable_after_days")
    private Integer applicableAfterDays;

    @Column(name = "max_penalty_amount")
    private Double maxPenaltyAmount;

    @Column(name = "penalty_type", length = 20)
    private String penaltyType; // PENALTY, SERVICE_CHARGE

    @Column(name = "is_active")
    private Boolean isActive;
}