package com.fintech.common.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Payment Response DTO.
 * Returned to clients after payment processing.
 */
public record PaymentResponseDTO(
    String transactionId,
    String status,
    String message,
    BigDecimal amount,
    String currency,
    Instant processedAt
) {
    public static PaymentResponseDTO success(String txId, BigDecimal amount, String currency) {
        return new PaymentResponseDTO(txId, "AUTHORIZED", "Payment authorized", 
            amount, currency, Instant.now());
    }
    
    public static PaymentResponseDTO rejected(String txId, String reason) {
        return new PaymentResponseDTO(txId, "REJECTED", reason, null, null, Instant.now());
    }
    
    public static PaymentResponseDTO error(String message) {
        return new PaymentResponseDTO(null, "ERROR", message, null, null, Instant.now());
    }
}
