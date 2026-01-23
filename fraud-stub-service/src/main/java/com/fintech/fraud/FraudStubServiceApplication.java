package com.fintech.fraud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Fraud Stub Service Application.
 * 
 * <p>A lightweight microservice that simulates fraud detection capabilities.
 * Listens on NATS for fraud check requests and responds with approval/rejection
 * decisions based on configurable business rules.</p>
 * 
 * <p>This service demonstrates ultra-low latency synchronous communication
 * patterns suitable for real-time transaction validation in financial systems.</p>
 * 
 * @author FinTech Architecture Team
 * @version 1.0.0
 */
@SpringBootApplication
public class FraudStubServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FraudStubServiceApplication.class, args);
    }
}
