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
}
