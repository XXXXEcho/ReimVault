package com.company.reimbursement.reimbursement;

import com.company.reimbursement.attachment.AttachmentType;
import com.company.reimbursement.attachment.ReimbursementAttachmentRepository;
import com.company.reimbursement.category.ExpenseCategory;
import com.company.reimbursement.category.ExpenseCategoryRepository;
import com.company.reimbursement.user.User;
import com.company.reimbursement.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReimbursementService {
    private final ReimbursementRepository records;
    private final UserRepository users;
    private final ExpenseCategoryRepository categories;
    private final ReimbursementAttachmentRepository attachments;

    public ReimbursementService(ReimbursementRepository records, UserRepository users, ExpenseCategoryRepository categories, ReimbursementAttachmentRepository attachments) {
        this.records = records;
        this.users = users;
        this.categories = categories;
        this.attachments = attachments;
    }

    @Transactional
    public ReimbursementDtos.RecordResponse createDraft(String username, ReimbursementDtos.SaveRecordRequest request) {
        User employee = findUser(username);
        ExpenseCategory category = findCategory(request.categoryId());
        ReimbursementRecord record = ReimbursementRecord.createDraft(employee, request.amount(), category, request.purpose(), request.paymentTime());
        return ReimbursementDtos.RecordResponse.from(records.save(record));
    }

    @Transactional
    public ReimbursementDtos.RecordResponse updateDraft(String username, Long id, ReimbursementDtos.SaveRecordRequest request) {
        ReimbursementRecord record = getOwnedRecord(username, id);
        record.updateDraft(request.amount(), findCategory(request.categoryId()), request.purpose(), request.paymentTime());
        return ReimbursementDtos.RecordResponse.from(record);
    }

    @Transactional(readOnly = true)
    public ReimbursementDtos.RecordResponse getMine(String username, Long id) {
        return ReimbursementDtos.RecordResponse.from(getOwnedRecord(username, id));
    }

    @Transactional(readOnly = true)
    public List<ReimbursementDtos.RecordResponse> listMine(String username) {
        return records.findByEmployeeOrderByCreatedAtDesc(findUser(username)).stream()
                .map(ReimbursementDtos.RecordResponse::from)
                .toList();
    }

    @Transactional
    public ReimbursementDtos.RecordResponse submit(String username, Long id) {
        ReimbursementRecord record = getOwnedRecord(username, id);
        record.submit(attachments.countByRecordAndType(record, AttachmentType.PAYMENT_VOUCHER));
        return ReimbursementDtos.RecordResponse.from(record);
    }

    @Transactional(readOnly = true)
    public List<ReimbursementDtos.RecordResponse> listAll() {
        return records.findAll().stream()
                .map(ReimbursementDtos.RecordResponse::from)
                .toList();
    }

    @Transactional
    public ReimbursementDtos.RecordResponse updateAdminRemark(Long id, ReimbursementDtos.AdminRemarkRequest request) {
        ReimbursementRecord record = records.findById(id).orElseThrow(() -> new EntityNotFoundException("报销记录不存在"));
        record.setAdminRemark(request.adminRemark());
        return ReimbursementDtos.RecordResponse.from(record);
    }

    private ReimbursementRecord getOwnedRecord(String username, Long id) {
        User user = findUser(username);
        ReimbursementRecord record = records.findById(id).orElseThrow(() -> new EntityNotFoundException("报销记录不存在"));
        if (!record.getEmployee().getId().equals(user.getId())) {
            throw new SecurityException("不能访问他人的报销记录");
        }
        return record;
    }

    private User findUser(String username) {
        return users.findByUsername(username).orElseThrow(() -> new EntityNotFoundException("用户不存在"));
    }

    private ExpenseCategory findCategory(Long categoryId) {
        ExpenseCategory category = categories.findById(categoryId).orElseThrow(() -> new EntityNotFoundException("分类不存在"));
        if (!category.isEnabled()) {
            throw new IllegalArgumentException("分类未启用");
        }
        return category;
    }
}
