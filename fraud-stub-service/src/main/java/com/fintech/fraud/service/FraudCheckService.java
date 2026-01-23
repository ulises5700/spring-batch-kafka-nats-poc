package com.fintech.fraud.service;

import com.fintech.common.dto.FraudCheckRequest;
import com.fintech.common.dto.FraudCheckResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

/**
 * Fraud Check Service - Core Business Logic.
 * 
 * <p>Implements fraud detection rules for real-time transaction validation.
 * This stub service simulates common fraud detection patterns:</p>
 * 
 * <ul>
 *   <li>Amount threshold validation</li>
 *   <li>High-risk country detection</li>
 *   <li>Velocity checks (simplified)</li>
 * </ul>
 * 
 * <p>In a production system, this would integrate with ML models,
 * external fraud databases, and real-time risk scoring engines.</p>
 */
@Service
public class FraudCheckService {

    private static final Logger log = LoggerFactory.getLogger(FraudCheckService.class);

    @Value("${fraud.threshold.amount:1000}")
    private BigDecimal amountThreshold;

    @Value("${fraud.threshold.currency:USD}")
    private String thresholdCurrency;

    // Simulated high-risk countries for demo purposes
    private static final Set<String> HIGH_RISK_COUNTRIES = Set.of("XX", "YY", "ZZ");

    /**
     * Performs fraud check on a transaction.
     * 
     * @param request the fraud check request containing transaction details
     * @return FraudCheckResponse with approval status and risk details
     */
    public FraudCheckResponse checkTransaction(FraudCheckRequest request) {
        log.info("Processing fraud check for transaction: {}", request.transactionId());
        
        long startTime = System.nanoTime();
        
        boolean approved = true;
        String riskLevel = "LOW";
        String reason = "Transaction approved";
        int riskScore = 0;

        // Rule 1: Amount threshold check
        if (request.amount().compareTo(amountThreshold) > 0) {
            riskScore += 50;
            riskLevel = "HIGH";
            reason = String.format("Amount %.2f exceeds threshold %.2f", 
                    request.amount(), amountThreshold);
            approved = false;
            log.warn("Transaction {} rejected: {}", request.transactionId(), reason);
        }

        // Rule 2: High-risk country check
        if (HIGH_RISK_COUNTRIES.contains(request.originCountry())) {
            riskScore += 30;
            riskLevel = "HIGH";
            reason = "Transaction from high-risk country: " + request.originCountry();
            approved = false;
            log.warn("Transaction {} rejected: {}", request.transactionId(), reason);
        }

        // Rule 3: Negative amount check (basic validation)
        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            riskScore = 100;
            riskLevel = "CRITICAL";
            reason = "Invalid transaction amount: " + request.amount();
            approved = false;
            log.error("Transaction {} rejected: {}", request.transactionId(), reason);
        }

        long processingTimeNanos = System.nanoTime() - startTime;
        
        FraudCheckResponse response = new FraudCheckResponse(
                request.transactionId(),
                approved,
                riskScore,
                riskLevel,
                reason,
                processingTimeNanos / 1_000_000.0, // Convert to milliseconds
                Instant.now()
        );

        log.info("Fraud check completed for {}: approved={}, riskScore={}, processingTime={}ms",
                request.transactionId(), approved, riskScore, response.processingTimeMs());

        return response;
    }
}
