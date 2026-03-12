package com.moneymoment.lending.entities;

import java.time.LocalDateTime;

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
@Table(name = "eod_phase_logs")
public class EodPhaseLogEntity extends BaseEntity {

    @Column(name = "job_id", nullable = false, length = 50)
    private String jobId;

    @Column(name = "phase_number", nullable = false)
    private Integer phaseNumber;

    @Column(name = "phase_name", nullable = false, length = 100)
    private String phaseName;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "duration_seconds")
    private Long durationSeconds;

    @Column(name = "metrics_json", columnDefinition = "TEXT")
    private String metricsJson;

    @Column(name = "error", columnDefinition = "TEXT")
    private String error;
}
