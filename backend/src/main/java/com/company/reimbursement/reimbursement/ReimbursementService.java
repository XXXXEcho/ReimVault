package com.company.reimbursement.reimbursement;

import com.company.reimbursement.attachment.AttachmentType;
import com.company.reimbursement.attachment.ReimbursementAttachmentRepository;
import com.company.reimbursement.category.ExpenseCategory;
import com.company.reimbursement.category.ExpenseCategoryRepository;
import com.company.reimbursement.oa.OaNumber;
import com.company.reimbursement.oa.OaNumberRepository;
import com.company.reimbursement.user.User;
import com.company.reimbursement.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReimbursementService {
    private final ReimbursementRepository records;
    private final UserRepository users;
    private final ExpenseCategoryRepository categories;
    private final ReimbursementAttachmentRepository attachments;
    private final OaNumberRepository oaNumbers;

    public ReimbursementService(ReimbursementRepository records, UserRepository users, ExpenseCategoryRepository categories, ReimbursementAttachmentRepository attachments, OaNumberRepository oaNumbers) {
        this.records = records;
        this.users = users;
        this.categories = categories;
        this.attachments = attachments;
        this.oaNumbers = oaNumbers;
    }

    @Transactional
    public ReimbursementDtos.RecordResponse createDraft(String username, ReimbursementDtos.SaveRecordRequest request) {
        User employee = findUser(username);
        ExpenseCategory category = findCategory(request.categoryId());
        ReimbursementRecord record = ReimbursementRecord.createDraft(employee, request.amount(), category, request.purpose(), request.paymentTime());
        if (request.oaId() != null) record.setOa(oaNumbers.findById(request.oaId()).orElseThrow());
        return response(records.save(record));
    }

    @Transactional
    public ReimbursementDtos.RecordResponse updateDraft(String username, Long id, ReimbursementDtos.SaveRecordRequest request) {
        ReimbursementRecord record = getOwnedRecord(username, id);
        record.updateDraft(request.amount(), findCategory(request.categoryId()), request.purpose(), request.paymentTime());
        record.setOa(request.oaId() != null ? oaNumbers.findById(request.oaId()).orElseThrow() : null);
        return response(record);
    }

    @Transactional(readOnly = true)
    public ReimbursementDtos.RecordResponse getMine(String username, Long id) {
        return response(getOwnedRecord(username, id));
    }

    @Transactional(readOnly = true)
    public List<ReimbursementDtos.RecordResponse> listMine(String username) {
        return records.findByEmployeeOrderByCreatedAtDesc(findUser(username)).stream()
                .map(this::response)
                .toList();
    }

    @Transactional
    public ReimbursementDtos.RecordResponse submit(String username, Long id) {
        ReimbursementRecord record = getOwnedRecord(username, id);
        record.submit(attachments.countByRecordAndType(record, AttachmentType.PAYMENT_VOUCHER));
        return response(record);
    }

    @Transactional
    public ReimbursementDtos.RecordResponse withdraw(String username, Long id) {
        ReimbursementRecord record = getOwnedRecord(username, id);
        record.rejectToDraft();
        return response(record);
    }

    @Transactional
    public ReimbursementDtos.RecordResponse reject(Long id) {
        ReimbursementRecord record = records.findById(id).orElseThrow(() -> new EntityNotFoundException("报销记录不存在"));
        record.rejectToDraft();
        return response(record);
    }

    @Transactional
    public void deleteDraft(String username, Long id) {
        ReimbursementRecord record = getOwnedRecord(username, id);
        record.ensureDraft();
        attachments.deleteByRecord(record);
        records.delete(record);
    }

    @Transactional
    public ReimbursementDtos.RecordResponse markReimbursed(Long id) {
        ReimbursementRecord record = records.findById(id).orElseThrow(() -> new EntityNotFoundException("报销记录不存在"));
        record.markReimbursed();
        return response(record);
    }

    @Transactional
    public ReimbursementDtos.RecordResponse clearReimbursed(Long id) {
        ReimbursementRecord record = records.findById(id).orElseThrow(() -> new EntityNotFoundException("报销记录不存在"));
        record.clearReimbursed();
        return response(record);
    }

    @Transactional(readOnly = true)
    public List<ReimbursementDtos.RecordResponse> listAll(ReimbursementDtos.AdminListFilter filter) {
        return records.findAll(adminFilter(filter)).stream()
                .map(this::response)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReimbursementDtos.RecordResponse getAny(Long id) {
        ReimbursementRecord record = records.findById(id).orElseThrow(() -> new EntityNotFoundException("报销记录不存在"));
        return response(record);
    }

    @Transactional(readOnly = true)
    public List<ReimbursementDtos.RecordResponse> previewFiltered(List<Long> oaIds, List<String> months) {
        return listFiltered(oaIds, months).stream()
                .map(this::response)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReimbursementRecord> listFiltered(List<Long> oaIds, List<String> months) {
        return records.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), ReimbursementStatus.SUBMITTED));
            if (oaIds != null && !oaIds.isEmpty()) {
                predicates.add(root.get("oa").get("id").in(oaIds));
            }
            if (months != null && !months.isEmpty()) {
                List<Predicate> monthPredicates = new ArrayList<>();
                for (String m : months) {
                    YearMonth ym = YearMonth.parse(m);
                    LocalDate start = ym.atDay(1);
                    LocalDate end = ym.plusMonths(1).atDay(1);
                    monthPredicates.add(cb.and(
                            cb.greaterThanOrEqualTo(root.get("paymentTime"), start.atStartOfDay().toInstant(ZoneOffset.UTC)),
                            cb.lessThan(root.get("paymentTime"), end.atStartOfDay().toInstant(ZoneOffset.UTC))
                    ));
                }
                predicates.add(cb.or(monthPredicates.toArray(Predicate[]::new)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        });
    }

    @Transactional(readOnly = true)
    public ReimbursementDtos.StatsResponse computeStats(List<Long> oaIds, List<Long> batchIds) {
        List<ReimbursementRecord> scoped = records.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (oaIds != null && !oaIds.isEmpty()) {
                predicates.add(root.get("oa").get("id").in(oaIds));
            }
            if (batchIds != null && !batchIds.isEmpty()) {
                predicates.add(root.get("batch").get("id").in(batchIds));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        });

        long totalCount = 0, reimbursedCount = 0, unreimbursedCount = 0, draftCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal reimbursedAmount = BigDecimal.ZERO;
        BigDecimal unreimbursedAmount = BigDecimal.ZERO;
        BigDecimal draftAmount = BigDecimal.ZERO;
        for (ReimbursementRecord record : scoped) {
            BigDecimal amount = record.getAmount() != null ? record.getAmount() : BigDecimal.ZERO;
            totalCount++;
            totalAmount = totalAmount.add(amount);
            boolean reimbursed = record.getReimbursedAt() != null || record.getStatus() == ReimbursementStatus.ARCHIVED;
            if (record.getStatus() == ReimbursementStatus.DRAFT) {
                draftCount++;
                draftAmount = draftAmount.add(amount);
            } else if (reimbursed) {
                reimbursedCount++;
                reimbursedAmount = reimbursedAmount.add(amount);
            } else {
                unreimbursedCount++;
                unreimbursedAmount = unreimbursedAmount.add(amount);
            }
        }
        return new ReimbursementDtos.StatsResponse(
                totalCount, totalAmount,
                reimbursedCount, reimbursedAmount,
                unreimbursedCount, unreimbursedAmount,
                draftCount, draftAmount);
    }

    @Transactional
    public void archiveRecords(List<Long> ids) {
        for (Long id : ids) {
            ReimbursementRecord record = records.findById(id).orElseThrow(() -> new EntityNotFoundException("报销记录不存在"));
            record.archive();
        }
    }

    private ReimbursementDtos.RecordResponse response(ReimbursementRecord record) {
        return ReimbursementDtos.RecordResponse.from(record, attachments.findByRecord(record));
    }

    private Specification<ReimbursementRecord> adminFilter(ReimbursementDtos.AdminListFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.employeeId() != null) predicates.add(cb.equal(root.get("employee").get("id"), filter.employeeId()));
            if (filter.categoryId() != null) predicates.add(cb.equal(root.get("category").get("id"), filter.categoryId()));
            if (filter.status() != null) predicates.add(cb.equal(root.get("status"), filter.status()));
            if (filter.from() != null) predicates.add(cb.greaterThanOrEqualTo(root.get("paymentTime"), filter.from().atStartOfDay().toInstant(ZoneOffset.UTC)));
            if (filter.to() != null) predicates.add(cb.lessThan(root.get("paymentTime"), filter.to().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)));
            if (filter.reimbursed() != null) {
                if (filter.reimbursed()) predicates.add(cb.isNotNull(root.get("reimbursedAt")));
                else predicates.add(cb.isNull(root.get("reimbursedAt")));
            }
            if (filter.oaId() != null) {
                predicates.add(cb.equal(root.get("oa").get("id"), filter.oaId()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    @Transactional
    public ReimbursementDtos.RecordResponse updateAdminRemark(Long id, ReimbursementDtos.AdminRemarkRequest request) {
        ReimbursementRecord record = records.findById(id).orElseThrow(() -> new EntityNotFoundException("报销记录不存在"));
        record.setAdminRemark(request.adminRemark());
        return response(record);
    }

    @Transactional
    public ReimbursementDtos.RecordResponse updateOaNumber(Long id, ReimbursementDtos.OaNumberRequest request) {
        ReimbursementRecord record = records.findById(id).orElseThrow(() -> new EntityNotFoundException("报销记录不存在"));
        OaNumber oa = request.oaId() != null ? oaNumbers.findById(request.oaId()).orElseThrow() : null;
        record.setOa(oa);
        return response(record);
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
