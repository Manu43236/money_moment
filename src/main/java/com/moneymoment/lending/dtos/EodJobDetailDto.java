package com.moneymoment.lending.dtos;

import java.util.List;

import lombok.Data;

@Data
public class EodJobDetailDto {
    private EodLogResponseDto summary;
    private List<EodPhaseDetailDto> phases;
    private boolean isLive; // true if this is the current running job
}
