package com.fintech.gateway.controller;

import com.fintech.common.dto.PaymentRequestDTO;
import com.fintech.common.dto.PaymentResponseDTO;
import com.fintech.gateway.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Traffic Simulator Controller for load testing.
 */
@RestController
@RequestMapping("/api/v1/simulator")
public class TrafficSimulatorController {

    private static final Logger log = LoggerFactory.getLogger(TrafficSimulatorController.class);
    
    private final PaymentService paymentService;
    
    private static final String[] PAYER_IDS = {"CUST-001", "CUST-002", "CUST-003", "CUST-004", "CUST-005"};
    private static final String[] PAYEE_IDS = {"MERCH-001", "MERCH-002", "MERCH-003", "MERCH-004"};
    private static final String[] BANKS = {"BANK-A", "BANK-B", "BANK-C", "BANK-D"};
    private static final String[] COUNTRIES = {"US", "UK", "DE", "FR", "ES"};
    private static final String[] MCC_CODES = {"5411", "5812", "5912", "7011"};

    public TrafficSimulatorController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateTraffic(
            @RequestParam(defaultValue = "10") int count) {
        
        log.info("🚀 Generating {} random payment requests...", count);
        
        List<CompletableFuture<PaymentResponseDTO>> futures = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            PaymentRequestDTO request = generateRandomPayment();
            futures.add(CompletableFuture.supplyAsync(() -> 
                paymentService.processPayment(request)));
        }
        
        // Wait for all to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        long approved = futures.stream()
            .map(CompletableFuture::join)
            .filter(r -> "AUTHORIZED".equals(r.status()))
            .count();
        
        log.info("✅ Traffic generation complete: {}/{} approved", approved, count);
        
        return ResponseEntity.ok(Map.of(
            "totalRequests", count,
            "approved", approved,
            "rejected", count - approved,
            "timestamp", System.currentTimeMillis()
        ));
    }

    private PaymentRequestDTO generateRandomPayment() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        // 70% chance of amount < 1000 (will be approved)
        BigDecimal amount = random.nextDouble() < 0.7 
            ? BigDecimal.valueOf(random.nextDouble(10, 999))
            : BigDecimal.valueOf(random.nextDouble(1000, 5000));
        
        return new PaymentRequestDTO(
            PAYER_IDS[random.nextInt(PAYER_IDS.length)],
            PAYEE_IDS[random.nextInt(PAYEE_IDS.length)],
            amount.setScale(2, java.math.RoundingMode.HALF_UP),
            "USD",
            BANKS[random.nextInt(BANKS.length)],
            BANKS[random.nextInt(BANKS.length)],
            MCC_CODES[random.nextInt(MCC_CODES.length)],
            COUNTRIES[random.nextInt(COUNTRIES.length)]
        );
    }
}
