package com.fintech.settlement.batch;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Settlement Record DTO for batch output.
 * 
 * <p>Represents a processed transaction ready for settlement file output.</p>
 */
public record SettlementRecord(
        String transactionId,
        String issuerBankId,
        String acquirerBankId,
        BigDecimal amount,
        String currency,
        LocalDate settlementDate,
        String status
) {
    /**
     * Creates a settlement record from transaction data.
     */
    public static SettlementRecord fromTransaction(
            String transactionId,
            String issuerBankId,
            String acquirerBankId,
            BigDecimal amount,
            String currency
    ) {
        return new SettlementRecord(
                transactionId,
                issuerBankId,
                acquirerBankId,
                amount,
                currency,
                LocalDate.now(),
                "SETTLED"
        );
    }
}
