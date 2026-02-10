package com.moneymoment.lending.master;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moneymoment.lending.common.response.ApiResponse;
import com.moneymoment.lending.master.entities.DisbursementModesEntity;
import com.moneymoment.lending.master.entities.DocumentTypesEntity;
import com.moneymoment.lending.master.entities.LoanPurposesEntity;
import com.moneymoment.lending.master.entities.LoanStatusesEntity;
import com.moneymoment.lending.master.entities.LoanTypesEntity;
import com.moneymoment.lending.master.entities.ProcessingFeeConfigEntity;
import com.moneymoment.lending.master.entities.TenureMasterEntity;

@RestController
@RequestMapping("/api/masters")
public class MasterController {

    private final MasterService masterService;

    MasterController(MasterService masterService) {
        this.masterService = masterService;
    }

    @GetMapping("/loan-types")
    public ResponseEntity<ApiResponse<List<LoanTypesEntity>>> getLoanTypes() {
        return ResponseEntity.ok(ApiResponse.success(
                masterService.getAllLoanTypes(),
                "Loan types fetched successfully"));
    }

    @GetMapping("/loan-purposes")
    public ResponseEntity<ApiResponse<List<LoanPurposesEntity>>> getLoanPurposes() {
        return ResponseEntity.ok(ApiResponse.success(
                masterService.getAllLoanPurposes(),
                "Loan purposes fetched successfully"));
    }

    @GetMapping("/disbursement-modes")
    public ResponseEntity<ApiResponse<List<DisbursementModesEntity>>> getDisbursementModes() {
        return ResponseEntity.ok(ApiResponse.success(
                masterService.getAllDisbursementModes(),
                "Disbursement modes fetched successfully"));
    }

    @GetMapping("/loan-statuses")
    public ResponseEntity<ApiResponse<List<LoanStatusesEntity>>> getLoanStatuses() {
        return ResponseEntity.ok(ApiResponse.success(
                masterService.getAllLoanStatuses(),
                "Loan statuses fetched successfully"));
    }

    @GetMapping("/tenures/{loanTypeCode}")
    public ResponseEntity<ApiResponse<List<TenureMasterEntity>>> getAvailableTenures(
            @PathVariable String loanTypeCode) {
        return ResponseEntity.ok(ApiResponse.success(
                masterService.getAvailableTenures(loanTypeCode),
                "Tenures fetched successfully"));
    }

    @GetMapping("/processing-fee/{loanTypeCode}")
    public ResponseEntity<ApiResponse<ProcessingFeeConfigEntity>> getProcessingFeeConfig(
            @PathVariable String loanTypeCode) {
        return ResponseEntity.ok(ApiResponse.success(
                masterService.getProcessingFeeConfig(loanTypeCode),
                "Processing fee config fetched successfully"));
    }

    @GetMapping("/interest-rate")
    public ResponseEntity<ApiResponse<Double>> getInterestRate(
            @RequestParam String loanTypeCode,
            @RequestParam Integer creditScore,
            @RequestParam Double loanAmount,
            @RequestParam Integer tenureMonths) {
        return ResponseEntity.ok(ApiResponse.success(
                masterService.getApplicableInterestRate(loanTypeCode, creditScore * 1.0, loanAmount, tenureMonths),
                "Interest rate calculated successfully"));
    }

    @GetMapping("/document-types")
    public ResponseEntity<ApiResponse<List<DocumentTypesEntity>>> getDocumentTypes() {
        return ResponseEntity.ok(ApiResponse.success(
                masterService.getAllDocumentTypes(),
                "Document types fetched successfully"));
    }

    @GetMapping("/document-types/{applicableFor}")
    public ResponseEntity<ApiResponse<List<DocumentTypesEntity>>> getDocumentTypesByApplicableFor(
            @PathVariable String applicableFor) {

        return ResponseEntity.ok(ApiResponse.success(
                masterService.getDocumentTypesByApplicableFor(applicableFor),
                "Document types fetched successfully"));
    }

    // doc by code @GetMapping("/document-types/code/{code}")
    @GetMapping("/document-types/code/{code}")
    public ResponseEntity<ApiResponse<DocumentTypesEntity>> getDocumentTypeByCode(
            @PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.success(
                masterService.getDocumentTypesByCode(code),
                "Document type fetched successfully"));
    }
}