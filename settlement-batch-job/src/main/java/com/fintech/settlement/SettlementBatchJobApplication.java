package com.fintech.settlement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Settlement Batch Job Application.
 * 
 * <p>Spring Boot application combining Kafka consumer capabilities with
 * Spring Batch for robust, transaction-safe settlement processing.</p>
 * 
 * <p>This application demonstrates the pattern of:</p>
 * <ul>
 *   <li>Consuming events from Kafka into a staging database</li>
 *   <li>Processing staged records in batches with Spring Batch</li>
 *   <li>Generating settlement files for downstream systems</li>
 * </ul>
 * 
 * <p>The combination of streaming ingestion with batch processing
 * provides both real-time data capture and reliable bulk processing.</p>
 * 
 * @author FinTech Architecture Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableScheduling
public class SettlementBatchJobApplication {

    public static void main(String[] args) {
        SpringApplication.run(SettlementBatchJobApplication.class, args);
    }
}
