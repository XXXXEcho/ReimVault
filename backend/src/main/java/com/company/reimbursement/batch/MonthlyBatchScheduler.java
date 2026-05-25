package com.company.reimbursement.batch;

import java.time.YearMonth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MonthlyBatchScheduler {
    private static final Logger log = LoggerFactory.getLogger(MonthlyBatchScheduler.class);
    private final BatchService batchService;

    public MonthlyBatchScheduler(BatchService batchService) {
        this.batchService = batchService;
    }

    @Scheduled(cron = "0 0 1 * * ")
    public void createMonthlyBatch() {
        YearMonth current = YearMonth.now();
        batchService.ensureMonthlyBatch(current);
        log.info("月度批次已就绪: {}年{}月", current.getYear(), current.getMonthValue());
    }
}
