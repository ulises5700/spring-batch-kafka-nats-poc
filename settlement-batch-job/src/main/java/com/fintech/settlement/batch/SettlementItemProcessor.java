package com.fintech.settlement.batch;

import com.fintech.settlement.entity.StagedTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

/**
 * Item Processor for Settlement Batch.
 * 
 * <p>Transforms staged transactions into settlement records,
 * applying any necessary business logic during the conversion.</p>
 */
public class SettlementItemProcessor implements ItemProcessor<StagedTransaction, SettlementRecord> {

    private static final Logger log = LoggerFactory.getLogger(SettlementItemProcessor.class);

    @Override
    public SettlementRecord process(StagedTransaction transaction) throws Exception {
        log.debug("Processing transaction: {} for issuer: {}", 
                transaction.getTransactionId(), 
                transaction.getIssuerBankId());

        // Transform to settlement record
        SettlementRecord record = SettlementRecord.fromTransaction(
                transaction.getTransactionId(),
                transaction.getIssuerBankId(),
                transaction.getAcquirerBankId(),
                transaction.getAmount(),
                transaction.getCurrency()
        );

        log.debug("Settlement record created: {}", record.transactionId());
        return record;
    }
}
