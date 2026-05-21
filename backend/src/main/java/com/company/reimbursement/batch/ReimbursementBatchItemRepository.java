package com.company.reimbursement.batch;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReimbursementBatchItemRepository extends JpaRepository<ReimbursementBatchItem, Long> {
    boolean existsByRecordId(Long recordId);

    List<ReimbursementBatchItem> findByBatchId(Long batchId);

    Optional<ReimbursementBatchItem> findByBatchIdAndRecordId(Long batchId, Long recordId);
}
