package com.fintech.common.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Transaction Authorized Event.
 * Published to Kafka when a payment passes fraud validation.
 */
public record TransactionAuthorizedEvent(
    String transactionId,
    String payerId,
    String payeeId,
    BigDecimal amount,
    String currency,
    String issuerBankId,
    String acquirerBankId,
    String merchantCategoryCode,
    String originCountry,
    int fraudRiskScore,
    Instant authorizedAt
) {}
