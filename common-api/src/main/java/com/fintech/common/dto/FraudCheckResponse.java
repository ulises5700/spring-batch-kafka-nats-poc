package com.fintech.common.dto;

import java.time.Instant;

/**
 * Fraud Check Response DTO.
 * Returned via NATS with fraud decision.
 */
public record FraudCheckResponse(
    String transactionId,
    boolean approved,
    int riskScore,
    String riskLevel,
    String reason,
    double processingTimeMs,
    Instant timestamp
) {
    public static FraudCheckResponse error(String transactionId, String reason) {
        return new FraudCheckResponse(
            transactionId, false, 100, "ERROR", reason, 0.0, Instant.now()
        );
    }
}
