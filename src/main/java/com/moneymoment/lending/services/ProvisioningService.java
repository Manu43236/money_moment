package com.moneymoment.lending.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moneymoment.lending.entities.LoanEntity;
import com.moneymoment.lending.repos.LoanRepo;

import lombok.extern.slf4j.Slf4j;

/**
 * RBI NPA Provisioning — calculates required provision per RBI circular DBOD.No.BP.BC.37/21.04.048/2001-02
 *
 * DPD Bucket       | Category        | Provision Rate
 * 0                | Standard        | 0.4%
 * 1 – 89           | Sub-Standard    | 15%
 * 90 – 365         | Doubtful-1      | 25%
 * 366 – 730        | Doubtful-2      | 40%
 * > 730            | Loss            | 100%
 */
@Service
@Slf4j
public class ProvisioningService {

    private final LoanRepo loanRepo;

    public ProvisioningService(LoanRepo loanRepo) {
        this.loanRepo = loanRepo;
    }

    @Transactional
    public Map<String, Object> run() {
        Map<String, Object> metrics = new HashMap<>();

        List<LoanEntity> loans = loanRepo.findAll().stream()
                .filter(loan -> !loan.getLoanStatus().getCode().equals("CLOSED")
                        && !loan.getLoanStatus().getCode().equals("INITIATED")
                        && !loan.getLoanStatus().getCode().equals("UNDER_ASSESSMENT")
                        && !loan.getLoanStatus().getCode().equals("APPROVED"))
                .toList();

        double totalProvision = 0.0;
        int standardCount = 0, subStandardCount = 0, doubtful1Count = 0, doubtful2Count = 0, lossCount = 0;

        for (LoanEntity loan : loans) {
            int dpd = loan.getCurrentDpd() != null ? loan.getCurrentDpd() : 0;
            double outstanding = loan.getOutstandingAmount() != null ? loan.getOutstandingAmount() : 0.0;

            double rate;
            String category;

            if (dpd == 0) {
                rate = 0.004; category = "STANDARD"; standardCount++;
            } else if (dpd < 90) {
                rate = 0.15; category = "SUB_STANDARD"; subStandardCount++;
            } else if (dpd <= 365) {
                rate = 0.25; category = "DOUBTFUL_1"; doubtful1Count++;
            } else if (dpd <= 730) {
                rate = 0.40; category = "DOUBTFUL_2"; doubtful2Count++;
            } else {
                rate = 1.00; category = "LOSS"; lossCount++;
            }

            double provision = Math.round(outstanding * rate * 100.0) / 100.0;
            loan.setProvisionAmount(provision);
            loan.setProvisionRate(rate * 100);
            loanRepo.save(loan);

            totalProvision += provision;
            log.debug("Provision: Loan {} | DPD {} | {} | Rate {}% | ₹{}", loan.getLoanNumber(), dpd, category, rate * 100, provision);
        }

        metrics.put("loansProvisioned", loans.size());
        metrics.put("totalProvisionAmount", Math.round(totalProvision * 100.0) / 100.0);
        metrics.put("standard", standardCount);
        metrics.put("subStandard", subStandardCount);
        metrics.put("doubtful1", doubtful1Count);
        metrics.put("doubtful2", doubtful2Count);
        metrics.put("loss", lossCount);

        // Mock GL entries
        int glEntries = loans.size() * 2; // Debit + Credit per loan
        metrics.put("glEntriesGenerated", glEntries);

        log.info("Provisioning: ₹{} total provision | Standard={}, SubStd={}, D1={}, D2={}, Loss={} | {} GL entries",
                Math.round(totalProvision), standardCount, subStandardCount, doubtful1Count, doubtful2Count, lossCount, glEntries);
        return metrics;
    }
}
