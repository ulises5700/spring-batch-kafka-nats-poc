package com.fintech.fraud.config;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Duration;

/**
 * NATS Connection Configuration for Fraud Service.
 * 
 * <p>Configures the NATS client connection with optimized settings
 * for low-latency fraud detection responses.</p>
 */
@Configuration
public class NatsConfig {

    private static final Logger log = LoggerFactory.getLogger(NatsConfig.class);

    @Value("${nats.server.url:nats://localhost:4222}")
    private String natsServerUrl;

    @Value("${nats.connection.name:fraud-stub-service}")
    private String connectionName;

    /**
     * Creates and configures the NATS connection.
     * 
     * @return configured NATS Connection
     * @throws IOException if connection fails
     * @throws InterruptedException if connection is interrupted
     */
    @Bean(destroyMethod = "close")
    public Connection natsConnection() throws IOException, InterruptedException {
        log.info("Establishing NATS connection to: {}", natsServerUrl);
        
        Options options = new Options.Builder()
                .server(natsServerUrl)
                .connectionName(connectionName)
                .connectionTimeout(Duration.ofSeconds(5))
                .pingInterval(Duration.ofSeconds(10))
                .reconnectWait(Duration.ofSeconds(1))
                .maxReconnects(-1) // Unlimited reconnects
                .build();
        
        Connection connection = Nats.connect(options);
        log.info("NATS connection established successfully. Status: {}", connection.getStatus());
        
        return connection;
    }
}
