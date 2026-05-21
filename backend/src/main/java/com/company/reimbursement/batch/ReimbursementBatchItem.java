package com.company.reimbursement.batch;

import com.company.reimbursement.reimbursement.ReimbursementRecord;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "reimbursement_batch_items")
public class ReimbursementBatchItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private ReimbursementBatch batch;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id")
    private ReimbursementRecord record;
    private Instant createdAt;

    protected ReimbursementBatchItem() {
    }

    public static ReimbursementBatchItem create(ReimbursementBatch batch, ReimbursementRecord record) {
        ReimbursementBatchItem item = new ReimbursementBatchItem();
        item.batch = batch;
        item.record = record;
        item.createdAt = Instant.now();
        return item;
    }

    public Long getId() { return id; }
    public ReimbursementBatch getBatch() { return batch; }
    public ReimbursementRecord getRecord() { return record; }
    public Instant getCreatedAt() { return createdAt; }
}
