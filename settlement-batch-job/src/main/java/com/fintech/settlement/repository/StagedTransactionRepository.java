package com.fintech.settlement.repository;

import com.fintech.settlement.entity.StagedTransaction;
import com.fintech.settlement.entity.StagedTransaction.ProcessingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for StagedTransaction entity operations.
 * 
 * <p>Provides data access methods for the transaction staging table,
 * including batch processing support methods.</p>
 */
@Repository
public interface StagedTransactionRepository extends JpaRepository<StagedTransaction, Long> {

    /**
     * Finds all transactions with the specified processing status.
     */
    List<StagedTransaction> findByProcessingStatus(ProcessingStatus status);

    /**
     * Finds pending transactions created before a specific timestamp.
     */
    List<StagedTransaction> findByProcessingStatusAndCreatedAtBefore(
            ProcessingStatus status, 
            Instant createdBefore
    );

    /**
     * Counts transactions by issuer bank.
     */
    long countByIssuerBankIdAndProcessingStatus(String issuerBankId, ProcessingStatus status);

    /**
     * Gets distinct issuer bank IDs for pending transactions.
     */
    @Query("SELECT DISTINCT t.issuerBankId FROM StagedTransaction t WHERE t.processingStatus = :status")
    List<String> findDistinctIssuerBankIdsByStatus(@Param("status") ProcessingStatus status);

    /**
     * Bulk updates status for batch processing.
     */
    @Modifying
    @Query("UPDATE StagedTransaction t SET t.processingStatus = :newStatus, " +
           "t.batchId = :batchId, t.processedAt = :processedAt " +
           "WHERE t.processingStatus = :currentStatus AND t.id IN :ids")
    int updateStatusBatch(
            @Param("currentStatus") ProcessingStatus currentStatus,
            @Param("newStatus") ProcessingStatus newStatus,
            @Param("batchId") String batchId,
            @Param("processedAt") Instant processedAt,
            @Param("ids") List<Long> ids
    );

    /**
     * Checks if a transaction already exists by transaction ID.
     */
    boolean existsByTransactionId(String transactionId);
}
