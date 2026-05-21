package com.company.reimbursement.reimbursement;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.reimbursement.attachment.AttachmentType;
import com.company.reimbursement.attachment.ReimbursementAttachment;
import com.company.reimbursement.attachment.ReimbursementAttachmentRepository;
import com.company.reimbursement.category.ExpenseCategory;
import com.company.reimbursement.category.ExpenseCategoryRepository;
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
        "spring.datasource.url=jdbc:h2:mem:record_attachments;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class ReimbursementAttachmentMetadataTest {
    @Autowired UserRepository users;
    @Autowired ExpenseCategoryRepository categories;
    @Autowired ReimbursementRepository records;
    @Autowired ReimbursementAttachmentRepository attachments;
    @Autowired ReimbursementService service;
    @Autowired PasswordEncoder passwordEncoder;

    private ReimbursementRecord record;

    @BeforeEach
    void setUp() {
        attachments.deleteAll();
        records.deleteAll();
        categories.deleteAll();
        users.deleteAll();
        User employee = users.save(User.create("employee", "员工一", "研发部", passwordEncoder.encode("secret123"), UserRole.EMPLOYEE));
        ExpenseCategory category = categories.save(ExpenseCategory.create("差旅费", true, 1, null));
        record = records.save(ReimbursementRecord.createDraft(employee, new BigDecimal("123.45"), category, "客户拜访", Instant.parse("2026-05-21T02:30:00Z")));
        attachments.save(ReimbursementAttachment.create(record, AttachmentType.PAYMENT_VOUCHER, "pay.png", "1/payment_voucher/pay.png", "image/png", 11));
        attachments.save(ReimbursementAttachment.create(record, AttachmentType.INVOICE, "invoice.pdf", "1/invoice/invoice.pdf", "application/pdf", 22));
    }

    @Test
    void listMineIncludesAttachmentMetadata() {
        ReimbursementDtos.RecordResponse response = service.listMine("employee").getFirst();

        assertThat(response.attachments()).hasSize(2);
        assertThat(response.attachments()).extracting(ReimbursementDtos.AttachmentResponse::type)
                .containsExactlyInAnyOrder(AttachmentType.PAYMENT_VOUCHER, AttachmentType.INVOICE);
        assertThat(response.attachments()).extracting(ReimbursementDtos.AttachmentResponse::originalFilename)
                .containsExactlyInAnyOrder("pay.png", "invoice.pdf");
    }
}
