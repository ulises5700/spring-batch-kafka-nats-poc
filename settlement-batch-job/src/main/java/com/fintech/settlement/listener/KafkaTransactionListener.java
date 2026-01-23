package com.fintech.settlement.listener;

import com.fintech.common.event.TransactionAuthorizedEvent;
import com.fintech.settlement.entity.StagedTransaction;
import com.fintech.settlement.repository.StagedTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.fintech.common.constants.KafkaTopics.PAYMENTS_AUTHORIZED;

/**
 * Kafka Consumer for Transaction Authorized Events.
 * 
 * <p>Listens to the payments.authorized topic and stages incoming
 * transactions in the H2 database for batch processing.</p>
 * 
 * <p>Implements idempotency checking to handle potential message
 * redelivery scenarios.</p>
 */
@Component
public class KafkaTransactionListener {

    private static final Logger log = LoggerFactory.getLogger(KafkaTransactionListener.class);

    private final StagedTransactionRepository stagingRepository;

    public KafkaTransactionListener(StagedTransactionRepository stagingRepository) {
        this.stagingRepository = stagingRepository;
    }

    /**
     * Consumes transaction authorized events and stages them for batch processing.
     */
    @KafkaListener(
            topics = PAYMENTS_AUTHORIZED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handleTransactionAuthorized(
            @Payload TransactionAuthorizedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        log.info("Received authorized transaction: {} from partition: {}, offset: {}",
                event.transactionId(), partition, offset);

        try {
            // Idempotency check
            if (stagingRepository.existsByTransactionId(event.transactionId())) {
                log.warn("Transaction {} already staged, skipping duplicate", event.transactionId());
                acknowledgment.acknowledge();
                return;
            }

            // Map event to staging entity
            StagedTransaction stagedTransaction = StagedTransaction.builder()
                    .transactionId(event.transactionId())
                    .payerId(event.payerId())
                    .payeeId(event.payeeId())
                    .amount(event.amount())
                    .currency(event.currency())
                    .issuerBankId(event.issuerBankId())
                    .acquirerBankId(event.acquirerBankId())
                    .merchantCategoryCode(event.merchantCategoryCode())
                    .originCountry(event.originCountry())
                    .authorizedAt(event.authorizedAt())
                    .build();

            stagingRepository.save(stagedTransaction);
            
            log.info("Transaction {} staged successfully", event.transactionId());
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error staging transaction {}: {}", event.transactionId(), e.getMessage(), e);
            // Don't acknowledge - message will be redelivered
            throw e;
        }
    }
}
