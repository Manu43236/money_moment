package com.moneymoment.lending.controllers;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moneymoment.lending.common.response.ApiResponse;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        ZonedDateTime istNow = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
        String nextEod = istNow.toLocalDate()
                .atTime(23, 59)
                .atZone(ZoneId.of("Asia/Kolkata"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"));

        Map<String, String> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("serverTime", istNow.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")));
        data.put("nextEodSchedule", nextEod);

        return ResponseEntity.ok(ApiResponse.success(data, "Service is healthy"));
    }
}
