package com.company.reimbursement.reimbursement;

import com.company.reimbursement.attachment.AttachmentType;
import com.company.reimbursement.attachment.ReimbursementAttachment;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class ReimbursementDtos {
    public record SaveRecordRequest(BigDecimal amount, Long categoryId, String purpose, Instant paymentTime, Long oaId) {
    }

    public record AdminRemarkRequest(String adminRemark) {
    }

    public record OaNumberRequest(Long oaId) {
    }

    public record AdminListFilter(Long employeeId, Long categoryId, ReimbursementStatus status, LocalDate from, LocalDate to, Boolean reimbursed, Long oaId) {
    }

    public record AttachmentResponse(Long id, AttachmentType type, String originalFilename, String contentType, long sizeBytes, Instant createdAt) {
        public static AttachmentResponse from(ReimbursementAttachment attachment) {
            return new AttachmentResponse(
                    attachment.getId(),
                    attachment.getType(),
                    attachment.getOriginalFilename(),
                    attachment.getContentType(),
                    attachment.getSizeBytes(),
                    attachment.getCreatedAt()
            );
        }
    }

    public record RecordResponse(
            Long id,
            Long employeeId,
            String employeeName,
            BigDecimal amount,
            Long categoryId,
            String categoryName,
            String purpose,
            Instant paymentTime,
            ReimbursementStatus status,
            String adminRemark,
            Long oaId,
            String oaNumber,
            Instant submittedAt,
            Instant archivedAt,
            Instant reimbursedAt,
            Long batchId,
            String batchName,
            List<AttachmentResponse> attachments
    ) {
        public static RecordResponse from(ReimbursementRecord record, List<ReimbursementAttachment> attachments) {
            return new RecordResponse(
                    record.getId(),
                    record.getEmployee().getId(),
                    record.getEmployee().getDisplayName(),
                    record.getAmount(),
                    record.getCategory().getId(),
                    record.getCategory().getName(),
                    record.getPurpose(),
                    record.getPaymentTime(),
                    record.getStatus(),
                    record.getAdminRemark(),
                    record.getOa() != null ? record.getOa().getId() : null,
                    record.getOa() != null ? record.getOa().getNumber() : null,
                    record.getSubmittedAt(),
                    record.getArchivedAt(),
                    record.getReimbursedAt(),
                    record.getBatch() != null ? record.getBatch().getId() : null,
                    record.getBatch() != null ? record.getBatch().getName() : null,
                    attachments.stream().map(AttachmentResponse::from).toList()
            );
        }
    }
}
