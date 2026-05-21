package com.company.reimbursement.attachment;

import com.company.reimbursement.reimbursement.ReimbursementRecord;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "reimbursement_attachments")
public class ReimbursementAttachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id")
    private ReimbursementRecord record;
    @Enumerated(EnumType.STRING)
    private AttachmentType type;
    private String originalFilename;
    private String storagePath;
    private String contentType;
    private long sizeBytes;
    private Instant createdAt;

    protected ReimbursementAttachment() {
    }

    public static ReimbursementAttachment create(ReimbursementRecord record, AttachmentType type, String originalFilename, String storagePath, String contentType, long sizeBytes) {
        ReimbursementAttachment attachment = new ReimbursementAttachment();
        attachment.record = record;
        attachment.type = type;
        attachment.originalFilename = originalFilename;
        attachment.storagePath = storagePath;
        attachment.contentType = contentType;
        attachment.sizeBytes = sizeBytes;
        attachment.createdAt = Instant.now();
        return attachment;
    }

    public Long getId() { return id; }
    public ReimbursementRecord getRecord() { return record; }
    public AttachmentType getType() { return type; }
    public String getOriginalFilename() { return originalFilename; }
    public String getStoragePath() { return storagePath; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public Instant getCreatedAt() { return createdAt; }
}
