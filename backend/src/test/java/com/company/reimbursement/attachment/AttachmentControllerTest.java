package com.company.reimbursement.attachment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.reimbursement.category.ExpenseCategory;
import com.company.reimbursement.category.ExpenseCategoryRepository;
import com.company.reimbursement.reimbursement.ReimbursementRecord;
import com.company.reimbursement.reimbursement.ReimbursementRepository;
import com.company.reimbursement.reimbursement.ReimbursementService;
import com.company.reimbursement.reimbursement.ReimbursementStatus;
import com.company.reimbursement.user.User;
import com.company.reimbursement.user.UserRepository;
import com.company.reimbursement.user.UserRole;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:attachments;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "app.storage.root=target/test-storage/attachments"
})
@AutoConfigureMockMvc
class AttachmentControllerTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired ExpenseCategoryRepository categories;
    @Autowired ReimbursementRepository records;
    @Autowired ReimbursementAttachmentRepository attachments;
    @Autowired ReimbursementService reimbursementService;
    @Autowired PasswordEncoder passwordEncoder;

    private User employee;
    private User other;
    private ExpenseCategory category;
    private ReimbursementRecord draft;

    @BeforeEach
    void setUp() throws Exception {
        attachments.deleteAll();
        records.deleteAll();
        categories.deleteAll();
        users.deleteAll();
        Path storage = Path.of("target/test-storage/attachments");
        if (Files.exists(storage)) {
            try (var paths = Files.walk(storage)) {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
            }
        }
        users.save(User.create("admin", "管理员", "财务部", passwordEncoder.encode("secret123"), UserRole.ADMIN));
        employee = users.save(User.create("employee", "员工", "研发部", passwordEncoder.encode("secret123"), UserRole.EMPLOYEE));
        other = users.save(User.create("other", "其他员工", "市场部", passwordEncoder.encode("secret123"), UserRole.EMPLOYEE));
        category = categories.save(ExpenseCategory.create("办公用品", true, 1, null));
        draft = records.save(ReimbursementRecord.createDraft(employee, new BigDecimal("128.00"), category, "购买键盘", Instant.parse("2026-05-01T10:00:00Z")));
    }

    @Test
    @WithMockUser(username = "employee", roles = "EMPLOYEE")
    void employeeUploadsAndDownloadsOwnDraftAttachment() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "pay.png", MediaType.IMAGE_PNG_VALUE, "image-bytes".getBytes());

        mvc.perform(multipart("/api/reimbursements/{id}/attachments", draft.getId())
                        .file(file)
                        .param("type", "PAYMENT_VOUCHER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("PAYMENT_VOUCHER"))
                .andExpect(jsonPath("$.originalFilename").value("pay.png"));

        ReimbursementAttachment attachment = attachments.findAll().getFirst();
        mvc.perform(get("/api/attachments/{id}", attachment.getId()))
                .andExpect(status().isOk())
                .andExpect(content().bytes("image-bytes".getBytes()));

        reimbursementService.submit("employee", draft.getId());
        assertThat(records.findById(draft.getId()).orElseThrow().getStatus()).isEqualTo(ReimbursementStatus.SUBMITTED);
    }

    @Test
    @WithMockUser(username = "employee", roles = "EMPLOYEE")
    void employeeUploadsOptionalAttachmentTypes() throws Exception {
        MockMultipartFile order = new MockMultipartFile("file", "order.jpg", MediaType.IMAGE_JPEG_VALUE, "order".getBytes());
        MockMultipartFile invoice = new MockMultipartFile("file", "invoice.pdf", MediaType.APPLICATION_PDF_VALUE, "invoice".getBytes());

        mvc.perform(multipart("/api/reimbursements/{id}/attachments", draft.getId()).file(order).param("type", "ORDER_SCREENSHOT"))
                .andExpect(status().isOk());
        mvc.perform(multipart("/api/reimbursements/{id}/attachments", draft.getId()).file(invoice).param("type", "INVOICE"))
                .andExpect(status().isOk());

        assertThat(attachments.findAll()).hasSize(2);
    }

    @Test
    @WithMockUser(username = "employee", roles = "EMPLOYEE")
    void uploadRejectsUnsupportedTypeAndLargeFile() throws Exception {
        MockMultipartFile text = new MockMultipartFile("file", "note.txt", MediaType.TEXT_PLAIN_VALUE, "bad".getBytes());
        MockMultipartFile large = new MockMultipartFile("file", "large.png", MediaType.IMAGE_PNG_VALUE, new byte[10 * 1024 * 1024 + 1]);

        mvc.perform(multipart("/api/reimbursements/{id}/attachments", draft.getId()).file(text).param("type", "PAYMENT_VOUCHER"))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/reimbursements/{id}/attachments", draft.getId()).file(large).param("type", "PAYMENT_VOUCHER"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "employee", roles = "EMPLOYEE")
    void employeeCannotUploadToSubmittedRecord() throws Exception {
        draft.submit(1);
        records.save(draft);
        MockMultipartFile file = new MockMultipartFile("file", "pay.png", MediaType.IMAGE_PNG_VALUE, "image".getBytes());

        mvc.perform(multipart("/api/reimbursements/{id}/attachments", draft.getId()).file(file).param("type", "PAYMENT_VOUCHER"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "other", roles = "EMPLOYEE")
    void employeeCannotDownloadAnotherEmployeesAttachment() throws Exception {
        ReimbursementAttachment attachment = attachments.save(ReimbursementAttachment.create(draft, AttachmentType.PAYMENT_VOUCHER, "pay.png", "missing/pay.png", MediaType.IMAGE_PNG_VALUE, 10));

        mvc.perform(get("/api/attachments/{id}", attachment.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanDownloadAnyAttachment() throws Exception {
        attachments.save(ReimbursementAttachment.create(draft, AttachmentType.PAYMENT_VOUCHER, "pay.png", "1/payment_voucher/pay.png", MediaType.IMAGE_PNG_VALUE, 10));
        Files.createDirectories(Path.of("target/test-storage/attachments/1/payment_voucher"));
        Files.writeString(Path.of("target/test-storage/attachments/1/payment_voucher/pay.png"), "admin-download");

        mvc.perform(get("/api/attachments/{id}", attachments.findAll().getFirst().getId()))
                .andExpect(status().isOk())
                .andExpect(content().bytes("admin-download".getBytes()));
    }
}
