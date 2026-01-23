package com.fintech.gateway.config;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class NatsConfig {

    @Value("${nats.server.url:nats://localhost:4222}")
    private String natsServerUrl;

    @Bean(destroyMethod = "close")
    public Connection natsConnection() throws Exception {
        Options options = new Options.Builder()
            .server(natsServerUrl)
            .connectionName("payment-gateway-service")
            .connectionTimeout(Duration.ofSeconds(5))
            .build();
        return Nats.connect(options);
    }
}
