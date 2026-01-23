package com.fintech.fraud.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.common.dto.FraudCheckRequest;
import com.fintech.common.dto.FraudCheckResponse;
import com.fintech.fraud.service.FraudCheckService;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static com.fintech.common.constants.NatsSubjects.FRAUD_CHECK;

/**
 * NATS Message Listener for Fraud Check Requests.
 * 
 * <p>
 * Subscribes to the fraud check subject and processes incoming
 * validation requests with ultra-low latency responses.
 * </p>
 * 
 * <p>
 * This component demonstrates the Request-Reply pattern in NATS,
 * which is ideal for synchronous validations in financial workflows.
 * </p>
 */
@Component
public class NatsFraudListener {

    private static final Logger log = LoggerFactory.getLogger(NatsFraudListener.class);

    private final Connection natsConnection;
    private final FraudCheckService fraudCheckService;
    private final ObjectMapper objectMapper;

    @Value("${nats.subject.fraud-check:" + FRAUD_CHECK + "}")
    private String fraudCheckSubject;

    private Dispatcher dispatcher;

    public NatsFraudListener(Connection natsConnection,
            FraudCheckService fraudCheckService,
            ObjectMapper objectMapper) {
        this.natsConnection = natsConnection;
        this.fraudCheckService = fraudCheckService;
        this.objectMapper = objectMapper;
    }

    /**
     * Initializes the NATS subscription after bean construction.
     */
    @PostConstruct
    public void subscribe() {
        log.info("Subscribing to NATS subject: {}", fraudCheckSubject);

        dispatcher = natsConnection.createDispatcher(this::handleMessage);
        dispatcher.subscribe(fraudCheckSubject);

        log.info("Successfully subscribed to fraud check subject. Ready to process requests.");
    }

    /**
     * Handles incoming fraud check request messages.
     * 
     * @param message the NATS message containing the fraud check request
     */
    private void handleMessage(Message message) {
        try {
            log.debug("Received message on subject: {}", message.getSubject());

            // Deserialize request
            FraudCheckRequest request = objectMapper.readValue(
                    message.getData(),
                    FraudCheckRequest.class);

            // Process fraud check
            FraudCheckResponse response = fraudCheckService.checkTransaction(request);

            // Send reply if replyTo is set (Request-Reply pattern)
            if (message.getReplyTo() != null) {
                byte[] responseBytes = objectMapper.writeValueAsBytes(response);
                natsConnection.publish(message.getReplyTo(), responseBytes);
                log.debug("Reply sent to: {}", message.getReplyTo());
            }

        } catch (Exception e) {
            log.error("Error processing fraud check message: {}", e.getMessage(), e);

            // Send error response if replyTo is available
            if (message.getReplyTo() != null) {
                try {
                    FraudCheckResponse errorResponse = FraudCheckResponse.error(
                            "UNKNOWN",
                            "Error processing request: " + e.getMessage());
                    byte[] responseBytes = objectMapper.writeValueAsBytes(errorResponse);
                    natsConnection.publish(message.getReplyTo(), responseBytes);
                } catch (Exception ex) {
                    log.error("Failed to send error response: {}", ex.getMessage());
                }
            }
        }
    }

    /**
     * Cleanup NATS subscription on shutdown.
     */
    @PreDestroy
    public void cleanup() {
        if (dispatcher != null) {
            log.info("Draining NATS dispatcher...");
            try {
                dispatcher.drain(java.time.Duration.ofSeconds(5));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while draining dispatcher", e);
            }
        }
    }
}
