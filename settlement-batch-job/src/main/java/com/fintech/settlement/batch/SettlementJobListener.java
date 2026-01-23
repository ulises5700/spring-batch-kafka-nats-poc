package com.fintech.settlement.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

/**
 * Job Execution Listener for Settlement Batch.
 * 
 * <p>Provides logging and monitoring callbacks for job lifecycle events.</p>
 */
public class SettlementJobListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(SettlementJobListener.class);

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("=== Settlement Batch Job Starting ===");
        log.info("Job ID: {}", jobExecution.getJobId());
        log.info("Job Parameters: {}", jobExecution.getJobParameters());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        log.info("=== Settlement Batch Job Completed ===");
        log.info("Job ID: {}", jobExecution.getJobId());
        log.info("Status: {}", jobExecution.getStatus());
        log.info("Exit Status: {}", jobExecution.getExitStatus().getExitCode());
        
        long duration = jobExecution.getEndTime().toEpochSecond(java.time.ZoneOffset.UTC) - 
                        jobExecution.getStartTime().toEpochSecond(java.time.ZoneOffset.UTC);
        log.info("Duration: {} seconds", duration);
        
        if (jobExecution.getStatus().isUnsuccessful()) {
            log.error("Job failed with exceptions:");
            jobExecution.getAllFailureExceptions()
                    .forEach(e -> log.error("  - {}", e.getMessage()));
        }
    }
}
