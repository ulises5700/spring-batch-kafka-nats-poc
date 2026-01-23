package com.fintech.common.dto;

import java.math.BigDecimal;

/**
 * Payment Request DTO.
 * Represents an incoming payment request from external clients.
 */
public record PaymentRequestDTO(
    String payerId,
    String payeeId,
    BigDecimal amount,
    String currency,
    String issuerBankId,
    String acquirerBankId,
    String merchantCategoryCode,
    String originCountry
) {}
