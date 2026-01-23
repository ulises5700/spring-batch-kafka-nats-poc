package com.fintech.gateway.service;

import com.fintech.common.dto.*;
import com.fintech.common.event.TransactionAuthorizedEvent;
import com.fintech.gateway.monitoring.MetricsService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static com.fintech.common.constants.KafkaTopics.PAYMENTS_AUTHORIZED;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final NatsFraudClient natsFraudClient;
    private final KafkaTemplate<String, TransactionAuthorizedEvent> kafkaTemplate;
    private final MetricsService metricsService;

    @Value("${resilience.fallback.max-amount:500}")
    private BigDecimal fallbackMaxAmount;

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
            // Step 1: Fraud check via NATS with Circuit Breaker protection
            long startTime = System.nanoTime();

            FraudCheckResponse fraudResponse = performFraudCheck(transactionId, request);

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
                    request.originCountry(), fraudResponse.riskScore(), Instant.now());

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

    /**
     * Fraud check with Circuit Breaker protection.
     * Falls back to a simple amount-based rule if NATS is unavailable.
     */
    @CircuitBreaker(name = "fraudCheck", fallbackMethod = "fraudCheckFallback")
    public FraudCheckResponse performFraudCheck(String transactionId, PaymentRequestDTO request) throws Exception {
        FraudCheckRequest fraudRequest = new FraudCheckRequest(
                transactionId, request.payerId(), request.amount(),
                request.currency(), request.originCountry(), Instant.now());

        return natsFraudClient.checkFraud(fraudRequest);
    }

    /**
     * Fallback method when fraud check circuit is open or NATS is unavailable.
     * Applies conservative business rule: approve only small transactions.
     */
    public FraudCheckResponse fraudCheckFallback(String transactionId, PaymentRequestDTO request, Throwable t) {
        log.warn("🛡️ Circuit Breaker FALLBACK activated for txId={}: {}", transactionId, t.getMessage());

        // Conservative fallback: only approve transactions below safety threshold
        boolean approved = request.amount().compareTo(fallbackMaxAmount) <= 0;

        String reason = approved
                ? "Fallback approved (low amount)"
                : "Fallback rejected (amount exceeds safety threshold: " + fallbackMaxAmount + ")";

        log.info("🛡️ Fallback decision for txId={}: approved={}, reason={}",
                transactionId, approved, reason);

        return new FraudCheckResponse(
                transactionId,
                approved,
                approved ? 50 : 100, // Risk score
                approved ? "FALLBACK_LOW" : "FALLBACK_HIGH",
                reason,
                0.0,
                Instant.now());
    }
}
