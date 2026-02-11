package com.moneymoment.lending.users.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleResponseDto {

    private Long id;
    private String roleCode;
    private String roleName;
    private String description;
    private Double maxApprovalAmount;
    private Boolean canApprove;
    private Boolean canRecommend;
    private Boolean canVeto;
    private Integer approvalLevel;
}