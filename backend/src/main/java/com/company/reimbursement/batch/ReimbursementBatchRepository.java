package com.company.reimbursement.batch;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReimbursementBatchRepository extends JpaRepository<ReimbursementBatch, Long> {
    boolean existsByName(String name);
    Optional<ReimbursementBatch> findByName(String name);
}
