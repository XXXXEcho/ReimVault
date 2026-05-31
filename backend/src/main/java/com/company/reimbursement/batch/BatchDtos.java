package com.company.reimbursement.batch;

import com.company.reimbursement.reimbursement.ReimbursementRecord;
import java.time.Instant;
import java.util.List;

public class BatchDtos {
    public record CreateBatchRequest(String name, String description) {
    }

    public record BatchItemResponse(Long id, Long recordId, String employeeName, String categoryName, String oaNumber) {
        public static BatchItemResponse from(ReimbursementBatchItem item) {
            ReimbursementRecord record = item.getRecord();
            return new BatchItemResponse(item.getId(), record.getId(), record.getEmployee().getDisplayName(), record.getCategory().getName(), record.getOa() != null ? record.getOa().getNumber() : null);
        }
    }

    public record BatchResponse(Long id, String name, String description, Instant createdAt, Instant archivedAt, List<BatchItemResponse> items) {
        public static BatchResponse from(ReimbursementBatch batch, List<ReimbursementBatchItem> items) {
            return new BatchResponse(
                    batch.getId(),
                    batch.getName(),
                    batch.getDescription(),
                    batch.getCreatedAt(),
                    batch.getArchivedAt(),
                    items.stream().map(BatchItemResponse::from).toList()
            );
        }
    }
}
