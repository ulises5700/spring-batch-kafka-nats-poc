package com.fintech.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Payment Gateway Application.
 * 
 * <p>Main entry point for the Payment Gateway microservice.
 * Provides REST API for payment processing, NATS integration for
 * fraud detection, and Kafka publishing for authorized transactions.</p>
 * 
 * <p>Includes a real-time monitoring dashboard accessible at the root URL.</p>
 */
@SpringBootApplication
@EnableScheduling
public class PaymentGatewayApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(PaymentGatewayApplication.class, args);
    }
}
