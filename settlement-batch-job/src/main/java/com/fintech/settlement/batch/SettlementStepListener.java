package com.fintech.settlement.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

/**
 * Step Execution Listener for Settlement Batch.
 * 
 * <p>Provides detailed metrics and logging for step-level processing.</p>
 */
public class SettlementStepListener implements StepExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(SettlementStepListener.class);

    @Override
    public void beforeStep(StepExecution stepExecution) {
        log.info("Starting step: {}", stepExecution.getStepName());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        log.info("Step {} completed", stepExecution.getStepName());
        log.info("  Read Count: {}", stepExecution.getReadCount());
        log.info("  Write Count: {}", stepExecution.getWriteCount());
        log.info("  Skip Count: {}", stepExecution.getSkipCount());
        log.info("  Commit Count: {}", stepExecution.getCommitCount());
        log.info("  Rollback Count: {}", stepExecution.getRollbackCount());
        
        if (stepExecution.getSkipCount() > 0) {
            log.warn("  {} items were skipped during processing", stepExecution.getSkipCount());
        }
        
        return stepExecution.getExitStatus();
    }
}
