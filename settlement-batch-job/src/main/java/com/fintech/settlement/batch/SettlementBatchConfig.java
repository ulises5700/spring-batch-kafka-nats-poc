package com.fintech.settlement.batch;

import com.fintech.settlement.entity.StagedTransaction;
import com.fintech.settlement.entity.StagedTransaction.ProcessingStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Spring Batch Configuration for Settlement Processing.
 * 
 * <p>Configures the batch job that reads staged transactions from H2,
 * processes them (grouping by issuer bank), and writes settlement
 * files to the output directory.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Chunk-based processing for memory efficiency</li>
 *   <li>Transaction management per chunk</li>
 *   <li>Configurable batch size</li>
 *   <li>Timestamped output files</li>
 * </ul>
 */
@Configuration
public class SettlementBatchConfig {

    private static final Logger log = LoggerFactory.getLogger(SettlementBatchConfig.class);

    @Value("${settlement.batch.chunk-size:100}")
    private int chunkSize;

    @Value("${settlement.output.directory:./output}")
    private String outputDirectory;

    /**
     * Defines the settlement processing job.
     */
    @Bean
    public Job settlementJob(JobRepository jobRepository, Step settlementStep) {
        return new JobBuilder("settlementJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(new SettlementJobListener())
                .start(settlementStep)
                .build();
    }

    /**
     * Defines the main processing step.
     */
    @Bean
    public Step settlementStep(JobRepository jobRepository,
                               PlatformTransactionManager transactionManager,
                               JdbcCursorItemReader<StagedTransaction> reader,
                               SettlementItemProcessor processor,
                               FlatFileItemWriter<SettlementRecord> writer) {
        return new StepBuilder("settlementStep", jobRepository)
                .<StagedTransaction, SettlementRecord>chunk(chunkSize, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .skipLimit(10)
                .skip(Exception.class)
                .listener(new SettlementStepListener())
                .build();
    }

    /**
     * JDBC cursor reader for staged transactions.
     */
    @Bean
    @StepScope
    public JdbcCursorItemReader<StagedTransaction> stagedTransactionReader(DataSource dataSource) {
        return new JdbcCursorItemReaderBuilder<StagedTransaction>()
                .name("stagedTransactionReader")
                .dataSource(dataSource)
                .sql("SELECT * FROM staged_transactions " +
                     "WHERE processing_status = 'PENDING' " +
                     "ORDER BY issuer_bank_id, created_at")
                .rowMapper(new BeanPropertyRowMapper<>(StagedTransaction.class))
                .fetchSize(chunkSize)
                .build();
    }

    /**
     * Item processor for settlement transformation.
     */
    @Bean
    public SettlementItemProcessor settlementProcessor() {
        return new SettlementItemProcessor();
    }

    /**
     * Flat file writer for settlement output.
     */
    @Bean
    @StepScope
    public FlatFileItemWriter<SettlementRecord> settlementFileWriter(
            @Value("#{jobParameters['batchId']}") String batchId) {
        
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = String.format("settlement_%s_%s.csv", 
                batchId != null ? batchId : "batch", timestamp);

        DelimitedLineAggregator<SettlementRecord> lineAggregator = new DelimitedLineAggregator<>();
        lineAggregator.setDelimiter(",");
        
        BeanWrapperFieldExtractor<SettlementRecord> fieldExtractor = new BeanWrapperFieldExtractor<>();
        fieldExtractor.setNames(new String[]{
                "transactionId", "issuerBankId", "acquirerBankId", 
                "amount", "currency", "settlementDate", "status"
        });
        lineAggregator.setFieldExtractor(fieldExtractor);

        return new FlatFileItemWriterBuilder<SettlementRecord>()
                .name("settlementFileWriter")
                .resource(new FileSystemResource(outputDirectory + "/" + filename))
                .headerCallback(writer -> writer.write(
                        "TransactionId,IssuerBankId,AcquirerBankId,Amount,Currency,SettlementDate,Status"))
                .lineAggregator(lineAggregator)
                .build();
    }
}
