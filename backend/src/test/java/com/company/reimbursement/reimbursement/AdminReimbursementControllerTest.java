package com.company.reimbursement.reimbursement;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin_reimbursements;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
class AdminReimbursementControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ReimbursementRepository records;
    @Autowired UserRepository users;
    @Autowired ExpenseCategoryRepository categories;
    @Autowired PasswordEncoder passwordEncoder;

    private User employee;
    private User otherEmployee;
    private ExpenseCategory travel;
    private ExpenseCategory office;

    @BeforeEach
    void setUp() {
        records.deleteAll();
        categories.deleteAll();
        users.deleteAll();
        employee = users.save(User.create("employee", "员工", "研发部", passwordEncoder.encode("secret123"), UserRole.EMPLOYEE));
        otherEmployee = users.save(User.create("other", "其他员工", "市场部", passwordEncoder.encode("secret123"), UserRole.EMPLOYEE));
        travel = categories.save(ExpenseCategory.create("差旅", true, 1, null));
        office = categories.save(ExpenseCategory.create("办公用品", true, 2, null));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void filtersAdminReimbursementsByEmployeeCategoryStatusAndPaymentDateRange() throws Exception {
        ReimbursementRecord matching = ReimbursementRecord.createDraft(employee, new BigDecimal("128.00"), travel, "客户拜访", Instant.parse("2026-05-10T10:00:00Z"));
        matching.submit(1);
        records.save(matching);

        ReimbursementRecord wrongEmployee = ReimbursementRecord.createDraft(otherEmployee, new BigDecimal("129.00"), travel, "其他员工", Instant.parse("2026-05-10T10:00:00Z"));
        wrongEmployee.submit(1);
        records.save(wrongEmployee);

        ReimbursementRecord wrongCategory = ReimbursementRecord.createDraft(employee, new BigDecimal("130.00"), office, "办公用品", Instant.parse("2026-05-10T10:00:00Z"));
        wrongCategory.submit(1);
        records.save(wrongCategory);

        ReimbursementRecord wrongStatus = ReimbursementRecord.createDraft(employee, new BigDecimal("131.00"), travel, "草稿", Instant.parse("2026-05-10T10:00:00Z"));
        records.save(wrongStatus);

        ReimbursementRecord wrongDate = ReimbursementRecord.createDraft(employee, new BigDecimal("132.00"), travel, "日期不符", Instant.parse("2026-06-01T00:00:00Z"));
        wrongDate.submit(1);
        records.save(wrongDate);

        mvc.perform(get("/api/admin/reimbursements")
                        .param("employeeId", employee.getId().toString())
                        .param("categoryId", travel.getId().toString())
                        .param("status", "SUBMITTED")
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(matching.getId()))
                .andExpect(jsonPath("$[0].purpose").value("客户拜访"))
                .andExpect(jsonPath("$[1]").doesNotExist());
    }
}
