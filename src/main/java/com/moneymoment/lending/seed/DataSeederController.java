package com.moneymoment.lending.seed;

import com.moneymoment.lending.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/seed")
public class DataSeederController {

    private final DataSeederService seederService;

    public DataSeederController(DataSeederService seederService) {
        this.seederService = seederService;
    }

    /**
     * POST /api/seed/demo-data
     * Seeds 30 customers + 300 loans (180 ACTIVE, 60 OVERDUE, 30 NPA, 30 CLOSED)
     * with full EMI schedules — ~7–10 EMIs falling per day in the current month.
     * Idempotent: skips if 20+ customers already exist.
     */
    @PostMapping("/demo-data")
    public ResponseEntity<ApiResponse<Map<String, Object>>> seedDemoData() {
        log.info("[SEED] Demo data seed requested");
        Map<String, Object> result = seederService.seedDemoData();
        String status = (String) result.get("status");
        String message = switch (status) {
            case "SUCCESS" -> "Demo data seeded successfully";
            case "SKIPPED" -> (String) result.get("message");
            case "ERROR"   -> (String) result.get("message");
            default        -> "Unknown result";
        };
        return ResponseEntity.ok(ApiResponse.success(result, message));
    }

    /**
     * GET /api/seed/status
     * Check if demo data is already seeded.
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> seedStatus() {
        boolean seeded = seederService.isAlreadySeeded();
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("seeded", seeded),
                seeded ? "Demo data is already present" : "Demo data not yet seeded"));
    }
}
