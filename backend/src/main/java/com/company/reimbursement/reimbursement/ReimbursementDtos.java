package com.company.reimbursement.reimbursement;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class ReimbursementDtos {
    public record SaveRecordRequest(BigDecimal amount, Long categoryId, String purpose, Instant paymentTime) {
    }

    public record AdminRemarkRequest(String adminRemark) {
    }

    public record AdminListFilter(Long employeeId, Long categoryId, ReimbursementStatus status, LocalDate from, LocalDate to) {
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
            Instant submittedAt,
            Instant archivedAt
    ) {
        public static RecordResponse from(ReimbursementRecord record) {
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
                    record.getSubmittedAt(),
                    record.getArchivedAt()
            );
        }
    }
}
