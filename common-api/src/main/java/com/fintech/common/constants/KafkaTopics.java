package com.fintech.common.constants;

/**
 * Kafka topic constants.
 */
public final class KafkaTopics {
    private KafkaTopics() {}
    
    public static final String PAYMENTS_AUTHORIZED = "payments.authorized";
    public static final String PAYMENTS_REJECTED = "payments.rejected";
}
