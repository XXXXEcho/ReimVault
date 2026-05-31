package com.company.reimbursement.export;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.reimbursement.attachment.AttachmentType;
import com.company.reimbursement.attachment.ReimbursementAttachment;
import com.company.reimbursement.attachment.ReimbursementAttachmentRepository;
import com.company.reimbursement.batch.BatchDtos;
import com.company.reimbursement.batch.BatchService;
import com.company.reimbursement.batch.ReimbursementBatchItemRepository;
import com.company.reimbursement.batch.ReimbursementBatchRepository;
import com.company.reimbursement.category.ExpenseCategory;
import com.company.reimbursement.category.ExpenseCategoryRepository;
import com.company.reimbursement.reimbursement.ReimbursementRecord;
import com.company.reimbursement.reimbursement.ReimbursementRepository;
import com.company.reimbursement.user.User;
import com.company.reimbursement.user.UserRepository;
import com.company.reimbursement.user.UserRole;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.zip.ZipInputStream;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:exports;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "app.storage.root=target/test-storage/exports"
})
class ExportServiceTest {
    @Autowired ExcelExportService excelExportService;
    @Autowired ZipExportService zipExportService;
    @Autowired BatchService batchService;
    @Autowired ReimbursementBatchItemRepository batchItems;
    @Autowired ReimbursementBatchRepository batches;
    @Autowired ReimbursementAttachmentRepository attachments;
    @Autowired ReimbursementRepository records;
    @Autowired ExpenseCategoryRepository categories;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;

    private Long batchId;

    @BeforeEach
    void setUp() {
        batchItems.deleteAll();
        attachments.deleteAll();
        records.deleteAll();
        batches.deleteAll();
        categories.deleteAll();
        users.deleteAll();
        User admin = users.save(User.create("admin", "管理员", "财务部", passwordEncoder.encode("secret123"), UserRole.ADMIN));
        User employee = users.save(User.create("employee", "员工一", "研发部", passwordEncoder.encode("secret123"), UserRole.EMPLOYEE));
        ExpenseCategory category = categories.save(ExpenseCategory.create("办公用品", true, 1, null));
        ReimbursementRecord record = records.save(ReimbursementRecord.createDraft(employee, new BigDecimal("128.00"), category, "购买键盘", Instant.parse("2026-05-01T10:00:00Z")));
        attachments.save(ReimbursementAttachment.create(record, AttachmentType.PAYMENT_VOUCHER, "pay.png", "missing/pay.png", "image/png", 10));
        attachments.save(ReimbursementAttachment.create(record, AttachmentType.INVOICE, "invoice.pdf", "missing/invoice.pdf", "application/pdf", 10));
        record.submit(1);
        record.setAdminRemark("材料齐全");
        records.save(record);
        BatchDtos.BatchResponse batch = batchService.create(admin.getUsername(), new BatchDtos.CreateBatchRequest("2026-05报销", "五月报销"));
        batchService.addItem(batch.id(), record.getId());
        batchId = batch.id();
    }

    @Test
    void exportsBatchAttachmentZipWithExpectedEntriesAndMissingList() throws Exception {
        Files.createDirectories(Path.of("target/test-storage/exports/real/payment_voucher"));
        Files.writeString(Path.of("target/test-storage/exports/real/payment_voucher/pay.png"), "pay-content");
        ReimbursementAttachment attachment = attachments.findAll().stream()
                .filter(item -> item.getType() == AttachmentType.PAYMENT_VOUCHER)
                .findFirst()
                .orElseThrow();
        attachments.delete(attachment);
        attachments.save(ReimbursementAttachment.create(attachment.getRecord(), AttachmentType.PAYMENT_VOUCHER, "pay.png", "real/payment_voucher/pay.png", "image/png", 11));

        byte[] bytes = zipExportService.exportBatchAttachments(batchId);

        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            List<String> names = new java.util.ArrayList<>();
            java.util.zip.ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                names.add(entry.getName());
            }
            assertThat(names).contains(
                    "报销批次-2026-05报销/员工一/001-办公用品-128.00/支付凭证/pay.png",
                    "报销批次-2026-05报销/附件缺失清单.txt"
            );
        }
    }

    @Test
    void exportsBatchWorkbookWithHeadersCountsAndMissingAttachmentMark() throws Exception {
        byte[] bytes = excelExportService.exportBatch(batchId);

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            assertThat(workbook.getSheetName(0)).isEqualTo("报销清单");
            Row header = workbook.getSheetAt(0).getRow(0);
            List<String> headers = List.of("批次名称", "员工姓名", "部门", "金额", "用途分类", "用途说明", "经费编码", "支付时间", "支付凭证数量", "订单截图数量", "发票数量", "提交时间", "报销状态", "报销时间", "管理员备注", "附件目录路径");
            for (int i = 0; i < headers.size(); i++) {
                assertThat(header.getCell(i).getStringCellValue()).isEqualTo(headers.get(i));
            }

            Row row = workbook.getSheetAt(0).getRow(1);
            assertThat(row.getCell(0).getStringCellValue()).isEqualTo("2026-05报销");
            assertThat(row.getCell(1).getStringCellValue()).isEqualTo("员工一");
            assertThat(row.getCell(2).getStringCellValue()).isEqualTo("研发部");
            assertThat(row.getCell(3).getNumericCellValue()).isEqualTo(128.00);
            assertThat(row.getCell(4).getStringCellValue()).isEqualTo("办公用品");
            assertThat(row.getCell(6).getStringCellValue()).isEmpty();
            assertThat(row.getCell(8).getNumericCellValue()).isEqualTo(1);
            assertThat(row.getCell(9).getNumericCellValue()).isEqualTo(0);
            assertThat(row.getCell(10).getNumericCellValue()).isEqualTo(1);
            assertThat(row.getCell(12).getStringCellValue()).isEqualTo("已提交未报销");
            assertThat(row.getCell(13).getStringCellValue()).isEmpty();
            assertThat(row.getCell(14).getStringCellValue()).isEqualTo("材料齐全");
            assertThat(row.getCell(15).getStringCellValue()).contains("员工一/001-办公用品-128.00").contains("附件缺失");
        }
    }
}
