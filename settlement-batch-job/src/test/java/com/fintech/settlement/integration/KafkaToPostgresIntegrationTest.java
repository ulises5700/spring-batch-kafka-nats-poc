package com.fintech.settlement.integration;

import com.fintech.common.event.TransactionAuthorizedEvent;
import com.fintech.settlement.entity.StagedTransaction;
import com.fintech.settlement.repository.StagedTransactionRepository;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test using Testcontainers with real Kafka and PostgreSQL.
 * 
 * <p>
 * This test verifies the complete flow:
 * <ol>
 * <li>Publish a TransactionAuthorizedEvent to Kafka</li>
 * <li>Verify the Kafka listener consumes it</li>
 * <li>Verify the transaction is staged in PostgreSQL</li>
 * </ol>
 * 
 * <p>
 * <strong>Podman Compatibility:</strong>
 * Set the environment variable TESTCONTAINERS_RYUK_DISABLED=true for rootless
 * Podman.
 * Also set DOCKER_HOST=unix:///run/user/$(id -u)/podman/podman.sock
 */
@SpringBootTest
@Testcontainers
class KafkaToPostgresIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("docker.io/library/postgres:16-alpine").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("settlement_test_db")
            .withUsername("test")
            .withPassword("test");

    @Container
    @SuppressWarnings("resource")
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("docker.io/confluentinc/cp-kafka:7.5.0")
                    .asCompatibleSubstituteFor("confluentinc/cp-kafka"));

    @Autowired
    private StagedTransactionRepository repository;

    private KafkaTemplate<String, TransactionAuthorizedEvent> kafkaTemplate;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL configuration
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        // Flyway configuration
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");

        // JPA configuration
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");

        // Kafka configuration
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("spring.kafka.consumer.group-id", () -> "test-settlement-group");

        // Disable tracing for tests
        registry.add("management.tracing.enabled", () -> false);
    }

    @BeforeEach
    void setUp() {
        // Create Kafka producer for sending test events
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        DefaultKafkaProducerFactory<String, TransactionAuthorizedEvent> producerFactory = new DefaultKafkaProducerFactory<>(
                producerProps);
        kafkaTemplate = new KafkaTemplate<>(producerFactory);

        // Clean database before each test
        repository.deleteAll();
    }

    @Test
    @DisplayName("Should consume Kafka event and stage transaction in PostgreSQL")
    void shouldConsumeKafkaEventAndStageInDatabase() {
        // Given: A transaction authorized event
        String transactionId = UUID.randomUUID().toString();
        TransactionAuthorizedEvent event = new TransactionAuthorizedEvent(
                transactionId,
                "PAYER-TEST-001",
                "PAYEE-TEST-001",
                new BigDecimal("250.00"),
                "USD",
                "BANK-TEST-A",
                "BANK-TEST-B",
                "5411",
                "US",
                25,
                Instant.now());

        // When: We publish to Kafka
        kafkaTemplate.send("payments.authorized", transactionId, event);

        // Then: The transaction should appear in the database (with timeout)
        await()
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Optional<StagedTransaction> staged = repository.findByTransactionId(transactionId);

                    assertThat(staged)
                            .isPresent()
                            .hasValueSatisfying(tx -> {
                                assertThat(tx.getPayerId()).isEqualTo("PAYER-TEST-001");
                                assertThat(tx.getPayeeId()).isEqualTo("PAYEE-TEST-001");
                                assertThat(tx.getAmount()).isEqualByComparingTo(new BigDecimal("250.00"));
                                assertThat(tx.getCurrency()).isEqualTo("USD");
                                assertThat(tx.getStatus()).isEqualTo("PENDING");
                            });
                });
    }

    @Test
    @DisplayName("Should handle multiple transactions in sequence")
    void shouldHandleMultipleTransactionsInSequence() {
        // Given: Multiple transaction events
        for (int i = 0; i < 5; i++) {
            String transactionId = UUID.randomUUID().toString();
            TransactionAuthorizedEvent event = new TransactionAuthorizedEvent(
                    transactionId,
                    "PAYER-" + i,
                    "PAYEE-" + i,
                    new BigDecimal(100 + i * 50),
                    "USD",
                    "BANK-A",
                    "BANK-B",
                    "5411",
                    "US",
                    10 + i,
                    Instant.now());

            // When: We publish each event
            kafkaTemplate.send("payments.authorized", transactionId, event);
        }

        // Then: All transactions should be staged
        await()
                .atMost(60, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    long count = repository.count();
                    assertThat(count).isGreaterThanOrEqualTo(5L);
                });
    }
}
