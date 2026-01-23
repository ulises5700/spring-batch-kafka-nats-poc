package com.fintech.settlement.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entity representing a staged transaction awaiting settlement processing.
 * 
 * <p>Transactions are staged in this table after being consumed from Kafka.
 * The Spring Batch job reads from this table and processes records in batches.</p>
 */
@Entity
@Table(name = "staged_transactions", indexes = {
    @Index(name = "idx_staged_status", columnList = "processing_status"),
    @Index(name = "idx_staged_issuer", columnList = "issuer_bank_id"),
    @Index(name = "idx_staged_created", columnList = "created_at")
})
public class StagedTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, unique = true, length = 64)
    private String transactionId;

    @Column(name = "payer_id", nullable = false, length = 64)
    private String payerId;

    @Column(name = "payee_id", nullable = false, length = 64)
    private String payeeId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "issuer_bank_id", nullable = false, length = 32)
    private String issuerBankId;

    @Column(name = "acquirer_bank_id", nullable = false, length = 32)
    private String acquirerBankId;

    @Column(name = "merchant_category_code", length = 4)
    private String merchantCategoryCode;

    @Column(name = "origin_country", length = 2)
    private String originCountry;

    @Column(name = "authorized_at", nullable = false)
    private Instant authorizedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "processing_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ProcessingStatus processingStatus = ProcessingStatus.PENDING;

    @Column(name = "batch_id")
    private String batchId;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Version
    @Column(name = "version")
    private Long version;

    // Default constructor for JPA
    public StagedTransaction() {
    }

    // Builder pattern constructor
    private StagedTransaction(Builder builder) {
        this.transactionId = builder.transactionId;
        this.payerId = builder.payerId;
        this.payeeId = builder.payeeId;
        this.amount = builder.amount;
        this.currency = builder.currency;
        this.issuerBankId = builder.issuerBankId;
        this.acquirerBankId = builder.acquirerBankId;
        this.merchantCategoryCode = builder.merchantCategoryCode;
        this.originCountry = builder.originCountry;
        this.authorizedAt = builder.authorizedAt;
        this.createdAt = Instant.now();
        this.processingStatus = ProcessingStatus.PENDING;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getPayerId() { return payerId; }
    public void setPayerId(String payerId) { this.payerId = payerId; }

    public String getPayeeId() { return payeeId; }
    public void setPayeeId(String payeeId) { this.payeeId = payeeId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getIssuerBankId() { return issuerBankId; }
    public void setIssuerBankId(String issuerBankId) { this.issuerBankId = issuerBankId; }

    public String getAcquirerBankId() { return acquirerBankId; }
    public void setAcquirerBankId(String acquirerBankId) { this.acquirerBankId = acquirerBankId; }

    public String getMerchantCategoryCode() { return merchantCategoryCode; }
    public void setMerchantCategoryCode(String merchantCategoryCode) { this.merchantCategoryCode = merchantCategoryCode; }

    public String getOriginCountry() { return originCountry; }
    public void setOriginCountry(String originCountry) { this.originCountry = originCountry; }

    public Instant getAuthorizedAt() { return authorizedAt; }
    public void setAuthorizedAt(Instant authorizedAt) { this.authorizedAt = authorizedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public ProcessingStatus getProcessingStatus() { return processingStatus; }
    public void setProcessingStatus(ProcessingStatus processingStatus) { this.processingStatus = processingStatus; }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    /**
     * Processing status enumeration.
     */
    public enum ProcessingStatus {
        PENDING,
        IN_PROGRESS,
        PROCESSED,
        FAILED
    }

    /**
     * Builder class for StagedTransaction.
     */
    public static class Builder {
        private String transactionId;
        private String payerId;
        private String payeeId;
        private BigDecimal amount;
        private String currency;
        private String issuerBankId;
        private String acquirerBankId;
        private String merchantCategoryCode;
        private String originCountry;
        private Instant authorizedAt;

        public Builder transactionId(String transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        public Builder payerId(String payerId) {
            this.payerId = payerId;
            return this;
        }

        public Builder payeeId(String payeeId) {
            this.payeeId = payeeId;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder issuerBankId(String issuerBankId) {
            this.issuerBankId = issuerBankId;
            return this;
        }

        public Builder acquirerBankId(String acquirerBankId) {
            this.acquirerBankId = acquirerBankId;
            return this;
        }

        public Builder merchantCategoryCode(String merchantCategoryCode) {
            this.merchantCategoryCode = merchantCategoryCode;
            return this;
        }

        public Builder originCountry(String originCountry) {
            this.originCountry = originCountry;
            return this;
        }

        public Builder authorizedAt(Instant authorizedAt) {
            this.authorizedAt = authorizedAt;
            return this;
        }

        public StagedTransaction build() {
            return new StagedTransaction(this);
        }
    }
}
