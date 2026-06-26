package com.company.reimbursement.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.reimbursement.attachment.AttachmentType;
import com.company.reimbursement.attachment.ReimbursementAttachment;
import com.company.reimbursement.attachment.ReimbursementAttachmentRepository;
import com.company.reimbursement.category.ExpenseCategory;
import com.company.reimbursement.category.ExpenseCategoryRepository;
import com.company.reimbursement.reimbursement.ReimbursementRecord;
import com.company.reimbursement.reimbursement.ReimbursementRepository;
import com.company.reimbursement.reimbursement.ReimbursementStatus;
import com.company.reimbursement.user.User;
import com.company.reimbursement.user.UserRepository;
import com.company.reimbursement.user.UserRole;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:batches;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class BatchServiceTest {
    @Autowired BatchService service;
    @Autowired ReimbursementBatchRepository batches;
    @Autowired ReimbursementBatchItemRepository items;
    @Autowired ReimbursementRepository records;
    @Autowired ReimbursementAttachmentRepository attachments;
    @Autowired UserRepository users;
    @Autowired ExpenseCategoryRepository categories;
    @Autowired PasswordEncoder passwordEncoder;

    private User admin;
    private User employee;
    private ExpenseCategory category;
    private ReimbursementRecord submitted;
    private ReimbursementRecord draft;

    @BeforeEach
    void setUp() {
        items.deleteAll();
        batches.deleteAll();
        attachments.deleteAll();
        records.deleteAll();
        categories.deleteAll();
        users.deleteAll();
        admin = users.save(User.create("admin", "管理员", "财务部", passwordEncoder.encode("secret123"), UserRole.ADMIN));
        employee = users.save(User.create("employee", "员工", "研发部", passwordEncoder.encode("secret123"), UserRole.EMPLOYEE));
        category = categories.save(ExpenseCategory.create("办公用品", true, 1, null));
        submitted = records.save(ReimbursementRecord.createDraft(employee, new BigDecimal("128.00"), category, "购买键盘", Instant.parse("2026-05-01T10:00:00Z")));
        attachments.save(ReimbursementAttachment.create(submitted, AttachmentType.PAYMENT_VOUCHER, "pay.png", "missing/pay.png", "image/png", 10));
        submitted.submit(1);
        submitted = records.save(submitted);
        draft = records.save(ReimbursementRecord.createDraft(employee, new BigDecimal("88.00"), category, "购买鼠标", Instant.parse("2026-05-02T10:00:00Z")));
    }

    @Test
    void adminCreatesBatchAndAddsSubmittedRecord() {
        BatchDtos.BatchResponse batch = service.create(admin.getUsername(), new BatchDtos.CreateBatchRequest("2026-05报销", "五月报销"));

        BatchDtos.BatchResponse withItem = service.addItem(batch.id(), submitted.getId());

        assertThat(withItem.name()).isEqualTo("2026-05报销");
        assertThat(withItem.items()).hasSize(1);
        assertThat(withItem.items().getFirst().recordId()).isEqualTo(submitted.getId());
    }

    @Test
    void cannotAddDraftRecordToBatch() {
        BatchDtos.BatchResponse batch = service.create(admin.getUsername(), new BatchDtos.CreateBatchRequest("2026-05报销", "五月报销"));

        assertThatThrownBy(() -> service.addItem(batch.id(), draft.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("只能添加已提交记录");
    }

    @Test
    void canRemoveNotArchivedRecordFromBatch() {
        BatchDtos.BatchResponse batch = service.create(admin.getUsername(), new BatchDtos.CreateBatchRequest("2026-05报销", "五月报销"));
        service.addItem(batch.id(), submitted.getId());

        BatchDtos.BatchResponse afterRemove = service.removeItem(batch.id(), submitted.getId());

        assertThat(afterRemove.items()).isEmpty();
        assertThat(items.existsByRecordId(submitted.getId())).isFalse();
    }

    @Test
    void archiveBatchArchivesAllItems() {
        BatchDtos.BatchResponse batch = service.create(admin.getUsername(), new BatchDtos.CreateBatchRequest("2026-05报销", "五月报销"));
        service.addItem(batch.id(), submitted.getId());

        BatchDtos.BatchResponse archived = service.archive(batch.id());

        assertThat(archived.archivedAt()).isNotNull();
        assertThat(records.findById(submitted.getId()).orElseThrow().getStatus()).isEqualTo(ReimbursementStatus.ARCHIVED);
    }

    @Test
    void addItemsSkipsDuplicateRecordsAndReturnsBatchDetails() {
        BatchDtos.BatchResponse batch = service.create(admin.getUsername(), new BatchDtos.CreateBatchRequest("2026-05报销", "五月报销"));
        service.addItem(batch.id(), submitted.getId());

        BatchDtos.BatchResponse withItems = service.addItems(batch.id(), java.util.List.of(submitted.getId()));

        assertThat(withItems.items()).hasSize(1);
        assertThat(withItems.items().getFirst().recordId()).isEqualTo(submitted.getId());
    }
}
