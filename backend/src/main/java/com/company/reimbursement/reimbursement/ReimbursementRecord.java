package com.company.reimbursement.reimbursement;

import com.company.reimbursement.batch.ReimbursementBatch;
import com.company.reimbursement.category.ExpenseCategory;
import com.company.reimbursement.oa.OaNumber;
import com.company.reimbursement.user.User;
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
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "reimbursement_records")
public class ReimbursementRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private User employee;
    private BigDecimal amount;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ExpenseCategory category;
    private String purpose;
    private Instant paymentTime;
    @Enumerated(EnumType.STRING)
    private ReimbursementStatus status;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private ReimbursementBatch batch;
    private String adminRemark;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oa_id")
    private OaNumber oa;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant submittedAt;
    private Instant archivedAt;
    private Instant reimbursedAt;

    protected ReimbursementRecord() {
    }

    public static ReimbursementRecord createDraft(User employee, BigDecimal amount, ExpenseCategory category, String purpose, Instant paymentTime) {
        ReimbursementRecord record = new ReimbursementRecord();
        record.employee = employee;
        record.amount = amount;
        record.category = category;
        record.purpose = purpose;
        record.paymentTime = paymentTime;
        record.status = ReimbursementStatus.DRAFT;
        record.createdAt = Instant.now();
        record.updatedAt = record.createdAt;
        return record;
    }

    public Long getId() { return id; }
    public User getEmployee() { return employee; }
    public BigDecimal getAmount() { return amount; }
    public ExpenseCategory getCategory() { return category; }
    public String getPurpose() { return purpose; }
    public Instant getPaymentTime() { return paymentTime; }
    public ReimbursementStatus getStatus() { return status; }
    public ReimbursementBatch getBatch() { return batch; }
    public void setBatch(ReimbursementBatch batch) { this.batch = batch; }
    public String getAdminRemark() { return adminRemark; }
    public OaNumber getOa() { return oa; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getArchivedAt() { return archivedAt; }
    public Instant getReimbursedAt() { return reimbursedAt; }

    public void updateDraft(BigDecimal amount, ExpenseCategory category, String purpose, Instant paymentTime) {
        ensureDraft();
        this.amount = amount;
        this.category = category;
        this.purpose = purpose;
        this.paymentTime = paymentTime;
        this.updatedAt = Instant.now();
    }

    public void submit(int paymentVoucherCount) {
        ensureDraft();
        if (paymentVoucherCount < 1) {
            throw new IllegalArgumentException("至少上传一张支付凭证");
        }
        this.status = ReimbursementStatus.SUBMITTED;
        this.submittedAt = Instant.now();
        this.updatedAt = this.submittedAt;
    }

    public void archive() {
        if (status != ReimbursementStatus.SUBMITTED) {
            throw new IllegalStateException("只能归档已提交记录");
        }
        this.status = ReimbursementStatus.ARCHIVED;
        this.archivedAt = Instant.now();
        this.updatedAt = this.archivedAt;
    }

    public void restoreSubmitted() {
        if (status != ReimbursementStatus.ARCHIVED) {
            throw new IllegalStateException("只能撤销已归档记录");
        }
        this.status = ReimbursementStatus.SUBMITTED;
        this.archivedAt = null;
        this.updatedAt = Instant.now();
    }

    public void setAdminRemark(String adminRemark) {
        this.adminRemark = adminRemark;
        this.updatedAt = Instant.now();
    }

    public void setOa(OaNumber oa) {
        this.oa = oa;
        this.updatedAt = Instant.now();
    }

    public void rejectToDraft() {
        if (status != ReimbursementStatus.SUBMITTED) {
            throw new IllegalStateException("只能打回已提交记录");
        }
        if (batch != null) {
            throw new IllegalStateException("已入批次的记录不能打回，请先从批次中移除");
        }
        this.status = ReimbursementStatus.DRAFT;
        this.submittedAt = null;
        this.updatedAt = Instant.now();
    }

    public void markReimbursed() {
        if (status != ReimbursementStatus.SUBMITTED) {
            throw new IllegalStateException("只能标记已提交记录为已报销");
        }
        this.reimbursedAt = Instant.now();
        this.updatedAt = this.reimbursedAt;
    }

    public void clearReimbursed() {
        this.reimbursedAt = null;
        this.updatedAt = Instant.now();
    }

    public void ensureDraft() {
        if (status != ReimbursementStatus.DRAFT) {
            throw new IllegalStateException("只能修改草稿记录");
        }
    }
}
