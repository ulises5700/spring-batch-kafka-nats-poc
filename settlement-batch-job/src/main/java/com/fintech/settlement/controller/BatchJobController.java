package com.fintech.settlement.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for triggering batch jobs on demand.
 * 
 * <p>Provides endpoints for manual job execution and monitoring.</p>
 */
@RestController
@RequestMapping("/api/v1/batch")
public class BatchJobController {

    private static final Logger log = LoggerFactory.getLogger(BatchJobController.class);

    private final JobLauncher jobLauncher;
    private final Job settlementJob;

    public BatchJobController(JobLauncher jobLauncher, Job settlementJob) {
        this.jobLauncher = jobLauncher;
        this.settlementJob = settlementJob;
    }

    /**
     * Triggers the settlement batch job on demand.
     */
    @PostMapping("/settlement/run")
    public ResponseEntity<Map<String, Object>> runSettlementJob(
            @RequestParam(required = false) String batchId
    ) {
        try {
            String effectiveBatchId = batchId != null ? batchId : UUID.randomUUID().toString();
            
            JobParameters params = new JobParametersBuilder()
                    .addString("batchId", effectiveBatchId)
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            log.info("Starting settlement job with batchId: {}", effectiveBatchId);
            
            var execution = jobLauncher.run(settlementJob, params);

            return ResponseEntity.ok(Map.of(
                    "jobId", execution.getJobId(),
                    "batchId", effectiveBatchId,
                    "status", execution.getStatus().toString(),
                    "startTime", execution.getStartTime().toString()
            ));

        } catch (Exception e) {
            log.error("Failed to start settlement job: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to start job",
                    "message", e.getMessage()
            ));
        }
    }
}
