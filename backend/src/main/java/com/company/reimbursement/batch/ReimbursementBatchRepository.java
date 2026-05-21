package com.company.reimbursement.batch;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReimbursementBatchRepository extends JpaRepository<ReimbursementBatch, Long> {
    boolean existsByName(String name);
}
