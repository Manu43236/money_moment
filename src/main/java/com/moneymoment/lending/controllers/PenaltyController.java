package com.moneymoment.lending.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moneymoment.lending.common.response.ApiResponse;
import com.moneymoment.lending.common.response.PagedResponse;
import com.moneymoment.lending.entities.LoanPenaltyEntity;
import com.moneymoment.lending.services.PenaltyService;

@RestController
@RequestMapping("/api/penalties")
public class PenaltyController {

    private final PenaltyService penaltyService;

    public PenaltyController(PenaltyService penaltyService) {
        this.penaltyService = penaltyService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<LoanPenaltyEntity>>> getAllPenalties(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        penaltyService.getAllPenalties(page, size),
                        "Penalties fetched successfully"));
    }

    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<LoanPenaltyEntity>> applyPenalty(
            @RequestParam Long emiScheduleId,
            @RequestParam String penaltyCode) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        penaltyService.applyPenalty(emiScheduleId, penaltyCode),
                        "Penalty applied successfully"));
    }

    @GetMapping("/loan/{loanNumber}")
    public ResponseEntity<ApiResponse<PagedResponse<LoanPenaltyEntity>>> getPenaltiesByLoan(
            @PathVariable String loanNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        penaltyService.getPenaltiesByLoan(loanNumber, page, size),
                        "Penalties fetched successfully"));
    }

    @GetMapping("/emi/{emiScheduleId}")
    public ResponseEntity<ApiResponse<List<LoanPenaltyEntity>>> getPenaltiesByEmi(
            @PathVariable Long emiScheduleId) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        penaltyService.getPenaltiesByEmi(emiScheduleId),
                        "Penalties fetched successfully"));
    }

    @PostMapping("/waive/{penaltyId}")
    public ResponseEntity<ApiResponse<LoanPenaltyEntity>> waivePenalty(
            @PathVariable Long penaltyId,
            @RequestParam Long waivedByUserId,
            @RequestParam String reason) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        penaltyService.waivePenalty(penaltyId, waivedByUserId, reason),
                        "Penalty waived successfully"));
    }
}