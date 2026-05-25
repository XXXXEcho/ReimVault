package com.company.reimbursement.batch;

import com.company.reimbursement.reimbursement.ReimbursementRecord;
import com.company.reimbursement.reimbursement.ReimbursementRepository;
import com.company.reimbursement.reimbursement.ReimbursementStatus;
import com.company.reimbursement.user.User;
import com.company.reimbursement.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BatchService {
    private final ReimbursementBatchRepository batches;
    private final ReimbursementBatchItemRepository items;
    private final ReimbursementRepository records;
    private final UserRepository users;

    public BatchService(ReimbursementBatchRepository batches, ReimbursementBatchItemRepository items, ReimbursementRepository records, UserRepository users) {
        this.batches = batches;
        this.items = items;
        this.records = records;
        this.users = users;
    }

    @Transactional
    public BatchDtos.BatchResponse create(String username, BatchDtos.CreateBatchRequest request) {
        if (batches.existsByName(request.name())) {
            throw new IllegalArgumentException("批次名称已存在");
        }
        User creator = users.findByUsername(username).orElseThrow(() -> new EntityNotFoundException("用户不存在"));
        ReimbursementBatch batch = batches.save(ReimbursementBatch.create(request.name(), request.description(), creator));
        return toResponse(batch);
    }

    @Transactional(readOnly = true)
    public List<BatchDtos.BatchResponse> list() {
        return batches.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BatchDtos.BatchResponse get(Long id) {
        return toResponse(findBatch(id));
    }

    @Transactional
    public BatchDtos.BatchResponse addItem(Long batchId, Long recordId) {
        ReimbursementBatch batch = findBatch(batchId);
        ReimbursementRecord record = records.findById(recordId).orElseThrow(() -> new EntityNotFoundException("报销记录不存在"));
        if (record.getStatus() != ReimbursementStatus.SUBMITTED) {
            throw new IllegalStateException("只能添加已提交记录");
        }
        if (items.existsByRecordId(recordId)) {
            throw new IllegalArgumentException("记录已加入批次");
        }
        items.save(ReimbursementBatchItem.create(batch, record));
        record.setBatch(batch);
        return toResponse(batch);
    }

    @Transactional
    public BatchDtos.BatchResponse removeItem(Long batchId, Long recordId) {
        ReimbursementBatch batch = findBatch(batchId);
        ReimbursementBatchItem item = items.findByBatchIdAndRecordId(batchId, recordId).orElseThrow(() -> new EntityNotFoundException("批次记录不存在"));
        if (item.getRecord().getStatus() == ReimbursementStatus.ARCHIVED) {
            throw new IllegalStateException("已归档记录不能移出批次");
        }
        items.delete(item);
        item.getRecord().setBatch(null);
        return toResponse(batch);
    }

    @Transactional
    public BatchDtos.BatchResponse archive(Long batchId) {
        ReimbursementBatch batch = findBatch(batchId);
        List<ReimbursementBatchItem> batchItems = items.findByBatchId(batchId);
        if (batchItems.isEmpty()) {
            throw new IllegalStateException("批次没有报销记录");
        }
        batchItems.forEach(item -> item.getRecord().archive());
        batch.archive();
        return BatchDtos.BatchResponse.from(batch, batchItems);
    }

    private ReimbursementBatch findBatch(Long id) {
        return batches.findById(id).orElseThrow(() -> new EntityNotFoundException("批次不存在"));
    }

    private BatchDtos.BatchResponse toResponse(ReimbursementBatch batch) {
        return BatchDtos.BatchResponse.from(batch, items.findByBatchId(batch.getId()));
    }

    @Transactional
    public ReimbursementBatch ensureMonthlyBatch(YearMonth month) {
        String name = month.getYear() + "年" + month.getMonthValue() + "月报销批次";
        return batches.findByName(name).orElseGet(() -> {
            User system = users.findByUsername("admin").orElseThrow(() -> new EntityNotFoundException("系统管理员不存在"));
            return batches.save(ReimbursementBatch.create(name, month.getYear() + "年" + month.getMonthValue() + "月自动创建", system));
        });
    }

    public Long findMonthlyBatchId(YearMonth month) {
        String name = month.getYear() + "年" + month.getMonthValue() + "月报销批次";
        return batches.findByName(name).map(ReimbursementBatch::getId).orElse(null);
    }
}
