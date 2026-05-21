package com.company.reimbursement.batch;

import com.company.reimbursement.user.User;
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
@Table(name = "reimbursement_batches")
public class ReimbursementBatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
    private Instant createdAt;
    private Instant archivedAt;

    protected ReimbursementBatch() {
    }

    public static ReimbursementBatch create(String name, String description, User createdBy) {
        ReimbursementBatch batch = new ReimbursementBatch();
        batch.name = name;
        batch.description = description;
        batch.createdBy = createdBy;
        batch.createdAt = Instant.now();
        return batch;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public User getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getArchivedAt() { return archivedAt; }

    public void archive() {
        this.archivedAt = Instant.now();
    }
}
