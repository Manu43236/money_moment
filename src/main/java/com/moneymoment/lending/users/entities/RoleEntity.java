package com.moneymoment.lending.users.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "roles")
public class RoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_code", unique = true, nullable = false, length = 30)
    private String roleCode;

    @Column(name = "role_name", nullable = false, length = 50)
    private String roleName;

    @Column(name = "description")
    private String description;

    @Column(name = "max_approval_amount")
    private Double maxApprovalAmount;

    @Column(name = "can_approve")
    private Boolean canApprove = true;

    @Column(name = "can_recommend")
    private Boolean canRecommend = false;

    @Column(name = "can_veto")
    private Boolean canVeto = false;

    @Column(name = "approval_level")
    private Integer approvalLevel;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}