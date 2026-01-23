package com.fintech.gateway.monitoring;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service for tracking dashboard metrics.
 */
@Service
public class MetricsService {

    private final AtomicLong totalTransactions = new AtomicLong(0);
    private final AtomicLong approvedTransactions = new AtomicLong(0);
    private final AtomicLong rejectedTransactions = new AtomicLong(0);
    private final AtomicReference<Double> avgFraudLatency = new AtomicReference<>(0.0);
    private final AtomicReference<Double> lastFraudLatency = new AtomicReference<>(0.0);
    
    private final List<KafkaEventInfo> recentKafkaEvents = 
        Collections.synchronizedList(new ArrayList<>());
    
    private long latencySampleCount = 0;
    private double latencySum = 0.0;

    public void recordTransaction(boolean approved) {
        totalTransactions.incrementAndGet();
        if (approved) {
            approvedTransactions.incrementAndGet();
        } else {
            rejectedTransactions.incrementAndGet();
        }
    }

    public synchronized void recordFraudLatency(double latencyMs) {
        lastFraudLatency.set(latencyMs);
        latencySampleCount++;
        latencySum += latencyMs;
        avgFraudLatency.set(latencySum / latencySampleCount);
    }

    public void recordKafkaEvent(String transactionId, BigDecimal amount, String status) {
        KafkaEventInfo event = new KafkaEventInfo(
            transactionId, amount, status, Instant.now().toString()
        );
        recentKafkaEvents.add(0, event);
        // Keep only last 10 events
        while (recentKafkaEvents.size() > 10) {
            recentKafkaEvents.remove(recentKafkaEvents.size() - 1);
        }
    }

    public DashboardMetrics getMetrics() {
        return new DashboardMetrics(
            totalTransactions.get(),
            approvedTransactions.get(),
            rejectedTransactions.get(),
            lastFraudLatency.get(),
            avgFraudLatency.get(),
            new ArrayList<>(recentKafkaEvents)
        );
    }

    public record DashboardMetrics(
        long totalTransactions,
        long approvedTransactions,
        long rejectedTransactions,
        double lastFraudLatencyMs,
        double avgFraudLatencyMs,
        List<KafkaEventInfo> recentKafkaEvents
    ) {}

    public record KafkaEventInfo(
        String transactionId,
        BigDecimal amount,
        String status,
        String timestamp
    ) {}
}
