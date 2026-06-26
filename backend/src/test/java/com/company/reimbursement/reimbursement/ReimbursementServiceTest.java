package com.company.reimbursement.reimbursement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.reimbursement.category.ExpenseCategory;
import com.company.reimbursement.category.ExpenseCategoryRepository;
import com.company.reimbursement.user.User;
import com.company.reimbursement.user.UserRepository;
import com.company.reimbursement.user.UserRole;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:reimbursements;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class ReimbursementServiceTest {
    @Autowired ReimbursementService service;
    @Autowired ReimbursementRepository records;
    @Autowired UserRepository users;
    @Autowired ExpenseCategoryRepository categories;
    @Autowired PasswordEncoder passwordEncoder;

    private User employee;
    private User anotherEmployee;
    private ExpenseCategory category;

    @BeforeEach
    void setUp() {
        records.deleteAll();
        categories.deleteAll();
        users.deleteAll();
        employee = users.save(User.create("employee", "员工", "研发部", passwordEncoder.encode("secret123"), UserRole.EMPLOYEE));
        anotherEmployee = users.save(User.create("other", "其他员工", "市场部", passwordEncoder.encode("secret123"), UserRole.EMPLOYEE));
        category = categories.save(ExpenseCategory.create("办公用品", true, 1, null));
    }

    @Test
    void employeeCreatesAndUpdatesOwnDraft() {
        ReimbursementDtos.RecordResponse created = service.createDraft(employee.getUsername(), new ReimbursementDtos.SaveRecordRequest(
                new BigDecimal("128.00"), category.getId(), "购买键盘", Instant.parse("2026-05-01T10:00:00Z")));

        assertThat(created.status()).isEqualTo(ReimbursementStatus.DRAFT);
        assertThat(created.amount()).isEqualByComparingTo("128.00");

        ReimbursementDtos.RecordResponse updated = service.updateDraft(employee.getUsername(), created.id(), new ReimbursementDtos.SaveRecordRequest(
                new BigDecimal("168.00"), category.getId(), "购买鼠标", Instant.parse("2026-05-02T10:00:00Z")));

        assertThat(updated.amount()).isEqualByComparingTo("168.00");
        assertThat(updated.purpose()).isEqualTo("购买鼠标");
    }

    @Test
    void employeeFiltersOwnReimbursementsByCategoryStatusPaymentDateRangeAndKeyword() {
        ExpenseCategory travel = categories.save(ExpenseCategory.create("差旅", true, 2, null));

        ReimbursementRecord matching = ReimbursementRecord.createDraft(employee, new BigDecimal("128.00"), category, "客户键盘采购", Instant.parse("2026-05-10T10:00:00Z"));
        matching.submit(1);
        records.save(matching);

        ReimbursementRecord wrongCategory = ReimbursementRecord.createDraft(employee, new BigDecimal("129.00"), travel, "客户差旅", Instant.parse("2026-05-10T10:00:00Z"));
        wrongCategory.submit(1);
        records.save(wrongCategory);

        ReimbursementRecord wrongStatus = ReimbursementRecord.createDraft(employee, new BigDecimal("130.00"), category, "客户草稿", Instant.parse("2026-05-10T10:00:00Z"));
        records.save(wrongStatus);

        ReimbursementRecord wrongDate = ReimbursementRecord.createDraft(employee, new BigDecimal("131.00"), category, "客户日期", Instant.parse("2026-06-01T00:00:00Z"));
        wrongDate.submit(1);
        records.save(wrongDate);

        ReimbursementRecord wrongKeyword = ReimbursementRecord.createDraft(employee, new BigDecimal("132.00"), category, "内部键盘采购", Instant.parse("2026-05-10T10:00:00Z"));
        wrongKeyword.submit(1);
        records.save(wrongKeyword);

        ReimbursementRecord wrongEmployee = ReimbursementRecord.createDraft(anotherEmployee, new BigDecimal("133.00"), category, "客户键盘采购", Instant.parse("2026-05-10T10:00:00Z"));
        wrongEmployee.submit(1);
        records.save(wrongEmployee);

        assertThat(service.listMine(employee.getUsername(), new ReimbursementDtos.EmployeeListFilter(
                category.getId(), ReimbursementStatus.SUBMITTED, LocalDate.parse("2026-05-01"), LocalDate.parse("2026-05-31"), "客户")))
                .extracting(ReimbursementDtos.RecordResponse::id)
                .containsExactly(matching.getId());
    }

    @Test
    void employeeCannotReadAnotherEmployeesRecord() {
        ReimbursementDtos.RecordResponse created = service.createDraft(employee.getUsername(), new ReimbursementDtos.SaveRecordRequest(
                new BigDecimal("128.00"), category.getId(), "购买键盘", Instant.parse("2026-05-01T10:00:00Z")));

        assertThatThrownBy(() -> service.getMine(anotherEmployee.getUsername(), created.id()))
                .isInstanceOf(SecurityException.class)
                .hasMessage("不能访问他人的报销记录");
    }

    @Test
    void submitRequiresPaymentVoucher() {
        ReimbursementDtos.RecordResponse created = service.createDraft(employee.getUsername(), new ReimbursementDtos.SaveRecordRequest(
                new BigDecimal("128.00"), category.getId(), "购买键盘", Instant.parse("2026-05-01T10:00:00Z")));

        assertThatThrownBy(() -> service.submit(employee.getUsername(), created.id()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("至少上传一张支付凭证");
    }

    @Test
    void employeeCannotUpdateSubmittedRecord() {
        ReimbursementRecord record = records.save(ReimbursementRecord.createDraft(employee, new BigDecimal("128.00"), category, "购买键盘", Instant.parse("2026-05-01T10:00:00Z")));
        record.submit(1);
        records.save(record);

        assertThatThrownBy(() -> service.updateDraft(employee.getUsername(), record.getId(), new ReimbursementDtos.SaveRecordRequest(
                new BigDecimal("168.00"), category.getId(), "购买鼠标", Instant.parse("2026-05-02T10:00:00Z"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("只能修改草稿记录");
    }

    @Test
    void adminWritesRemark() {
        ReimbursementDtos.RecordResponse created = service.createDraft(employee.getUsername(), new ReimbursementDtos.SaveRecordRequest(
                new BigDecimal("128.00"), category.getId(), "购买键盘", Instant.parse("2026-05-01T10:00:00Z")));

        ReimbursementDtos.RecordResponse updated = service.updateAdminRemark(created.id(), new ReimbursementDtos.AdminRemarkRequest("缺少订单截图"));

        assertThat(updated.adminRemark()).isEqualTo("缺少订单截图");
    }

    @Test
    void bulkActionRejectsDraftAndArchivedRecordsForInvalidTransitions() {
        ReimbursementRecord draft = records.save(ReimbursementRecord.createDraft(employee, new BigDecimal("128.00"), category, "购买键盘", Instant.parse("2026-05-01T10:00:00Z")));
        ReimbursementRecord archived = ReimbursementRecord.createDraft(employee, new BigDecimal("168.00"), category, "购买鼠标", Instant.parse("2026-05-02T10:00:00Z"));
        archived.submit(1);
        archived.archive();
        archived = records.save(archived);

        ReimbursementRecord finalArchived = archived;
        assertThatThrownBy(() -> service.bulkAction(new ReimbursementDtos.BulkActionRequest(
                java.util.List.of(draft.getId()), ReimbursementDtos.BulkAction.REIMBURSE)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("只能归档已提交记录");

        assertThatThrownBy(() -> service.bulkAction(new ReimbursementDtos.BulkActionRequest(
                java.util.List.of(finalArchived.getId()), ReimbursementDtos.BulkAction.REJECT)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("只能打回待报销记录");
    }

    @Test
    void bulkActionCanRejectSubmittedRecordsBackToDraft() {
        ReimbursementRecord submitted = ReimbursementRecord.createDraft(employee, new BigDecimal("128.00"), category, "购买键盘", Instant.parse("2026-05-01T10:00:00Z"));
        submitted.submit(1);
        submitted = records.save(submitted);

        assertThat(service.bulkAction(new ReimbursementDtos.BulkActionRequest(
                java.util.List.of(submitted.getId()), ReimbursementDtos.BulkAction.REJECT)))
                .extracting(ReimbursementDtos.RecordResponse::status)
                .containsExactly(ReimbursementStatus.DRAFT);
    }
}
