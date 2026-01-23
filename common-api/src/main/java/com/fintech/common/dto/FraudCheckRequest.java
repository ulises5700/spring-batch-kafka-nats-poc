package com.fintech.common.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Fraud Check Request DTO.
 * Sent via NATS for real-time fraud validation.
 */
public record FraudCheckRequest(
    String transactionId,
    String payerId,
    BigDecimal amount,
    String currency,
    String originCountry,
    Instant timestamp
) {}
