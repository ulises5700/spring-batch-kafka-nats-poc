package com.fintech.gateway.service;

import com.fintech.common.dto.*;
import com.fintech.common.event.TransactionAuthorizedEvent;
import com.fintech.gateway.monitoring.MetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

import static com.fintech.common.constants.KafkaTopics.PAYMENTS_AUTHORIZED;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    
    private final NatsFraudClient natsFraudClient;
    private final KafkaTemplate<String, TransactionAuthorizedEvent> kafkaTemplate;
    private final MetricsService metricsService;

    public PaymentService(NatsFraudClient natsFraudClient,
                          KafkaTemplate<String, TransactionAuthorizedEvent> kafkaTemplate,
                          MetricsService metricsService) {
        this.natsFraudClient = natsFraudClient;
        this.kafkaTemplate = kafkaTemplate;
        this.metricsService = metricsService;
    }

    public PaymentResponseDTO processPayment(PaymentRequestDTO request) {
        String transactionId = UUID.randomUUID().toString();
        log.info("📥 Processing payment: txId={}, amount={} {}", 
                transactionId, request.amount(), request.currency());
        
        try {
            // Step 1: Fraud check via NATS with timing
            long startTime = System.nanoTime();
            
            FraudCheckRequest fraudRequest = new FraudCheckRequest(
                transactionId, request.payerId(), request.amount(),
                request.currency(), request.originCountry(), Instant.now()
            );
            
            FraudCheckResponse fraudResponse = natsFraudClient.checkFraud(fraudRequest);
            
            double latencyMs = (System.nanoTime() - startTime) / 1_000_000.0;
            metricsService.recordFraudLatency(latencyMs);
            
            log.info("🔍 Fraud check complete: txId={}, approved={}, latency={:.2f}ms", 
                    transactionId, fraudResponse.approved(), latencyMs);
            
            if (!fraudResponse.approved()) {
                log.warn("❌ Payment REJECTED: txId={}, reason={}", 
                        transactionId, fraudResponse.reason());
                metricsService.recordTransaction(false);
                metricsService.recordKafkaEvent(transactionId, request.amount(), "REJECTED");
                return PaymentResponseDTO.rejected(transactionId, fraudResponse.reason());
            }
            
            // Step 2: Publish to Kafka
            TransactionAuthorizedEvent event = new TransactionAuthorizedEvent(
                transactionId, request.payerId(), request.payeeId(),
                request.amount(), request.currency(), request.issuerBankId(),
                request.acquirerBankId(), request.merchantCategoryCode(),
                request.originCountry(), fraudResponse.riskScore(), Instant.now()
            );
            
            kafkaTemplate.send(PAYMENTS_AUTHORIZED, transactionId, event);
            
            log.info("✅ Payment AUTHORIZED: txId={}, amount={} {} → Kafka published", 
                    transactionId, request.amount(), request.currency());
            
            metricsService.recordTransaction(true);
            metricsService.recordKafkaEvent(transactionId, request.amount(), "AUTHORIZED");
            
            return PaymentResponseDTO.success(transactionId, request.amount(), request.currency());
            
        } catch (Exception e) {
            log.error("💥 Payment FAILED: txId={}, error={}", transactionId, e.getMessage(), e);
            metricsService.recordTransaction(false);
            return PaymentResponseDTO.error("Processing failed: " + e.getMessage());
        }
    }
}
