package com.fintech.gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.common.dto.FraudCheckRequest;
import com.fintech.common.dto.FraudCheckResponse;
import io.nats.client.Connection;
import io.nats.client.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

import static com.fintech.common.constants.NatsSubjects.FRAUD_CHECK;

@Component
public class NatsFraudClient {

    private static final Logger log = LoggerFactory.getLogger(NatsFraudClient.class);
    
    private final Connection natsConnection;
    private final ObjectMapper objectMapper;
    
    @Value("${nats.timeout.ms:2000}")
    private long timeoutMs;

    public NatsFraudClient(Connection natsConnection, ObjectMapper objectMapper) {
        this.natsConnection = natsConnection;
        this.objectMapper = objectMapper;
    }

    public FraudCheckResponse checkFraud(FraudCheckRequest request) throws Exception {
        log.debug("Sending fraud check request for transaction: {}", request.transactionId());
        
        byte[] requestBytes = objectMapper.writeValueAsBytes(request);
        
        Message response = natsConnection.request(
            FRAUD_CHECK, requestBytes, Duration.ofMillis(timeoutMs)
        );
        
        if (response == null) {
            throw new RuntimeException("Fraud check timeout");
        }
        
        FraudCheckResponse fraudResponse = objectMapper.readValue(
            response.getData(), FraudCheckResponse.class
        );
        
        log.debug("Fraud check response: approved={}, riskScore={}", 
            fraudResponse.approved(), fraudResponse.riskScore());
        
        return fraudResponse;
    }
}
