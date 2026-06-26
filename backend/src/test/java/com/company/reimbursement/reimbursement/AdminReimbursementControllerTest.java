package com.company.reimbursement.reimbursement;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.reimbursement.batch.BatchDtos;
import com.company.reimbursement.batch.BatchService;
import com.company.reimbursement.batch.ReimbursementBatchItemRepository;
import com.company.reimbursement.batch.ReimbursementBatchRepository;
import com.company.reimbursement.category.ExpenseCategory;
import com.company.reimbursement.category.ExpenseCategoryRepository;
import com.company.reimbursement.oa.OaNumber;
import com.company.reimbursement.oa.OaNumberRepository;
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
    @Autowired OaNumberRepository oaNumbers;
    @Autowired BatchService batchService;
    @Autowired ReimbursementBatchItemRepository batchItems;
    @Autowired ReimbursementBatchRepository batches;
    @Autowired PasswordEncoder passwordEncoder;

    private User employee;
    private User otherEmployee;
    private ExpenseCategory travel;
    private ExpenseCategory office;

    @BeforeEach
    void setUp() {
        batchItems.deleteAll();
        records.deleteAll();
        batches.deleteAll();
        oaNumbers.deleteAll();
        categories.deleteAll();
        users.deleteAll();
        employee = users.save(User.create("employee", "员工", "研发部", passwordEncoder.encode("secret123"), UserRole.EMPLOYEE));
        users.save(User.create("admin", "管理员", "财务部", passwordEncoder.encode("secret123"), UserRole.ADMIN));
        otherEmployee = users.save(User.create("other", "其他员工", "市场部", passwordEncoder.encode("secret123"), UserRole.EMPLOYEE));
        travel = categories.save(ExpenseCategory.create("差旅", true, 1, null));
        office = categories.save(ExpenseCategory.create("办公用品", true, 2, null));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void previewsFilteredExportRecordsBeforeDownload() throws Exception {
        OaNumber targetOa = oaNumbers.save(OaNumber.create("JF-001"));
        OaNumber otherOa = oaNumbers.save(OaNumber.create("JF-002"));

        ReimbursementRecord matching = ReimbursementRecord.createDraft(employee, new BigDecimal("128.00"), travel, "五月命中", Instant.parse("2026-05-10T10:00:00Z"));
        matching.setOa(targetOa);
        matching.submit(1);
        records.save(matching);

        ReimbursementRecord wrongOa = ReimbursementRecord.createDraft(employee, new BigDecimal("129.00"), travel, "经费不符", Instant.parse("2026-05-10T10:00:00Z"));
        wrongOa.setOa(otherOa);
        wrongOa.submit(1);
        records.save(wrongOa);

        ReimbursementRecord wrongMonth = ReimbursementRecord.createDraft(employee, new BigDecimal("130.00"), travel, "月份不符", Instant.parse("2026-06-01T10:00:00Z"));
        wrongMonth.setOa(targetOa);
        wrongMonth.submit(1);
        records.save(wrongMonth);

        mvc.perform(get("/api/admin/batches/export/preview")
                        .param("oaIds", targetOa.getId().toString())
                        .param("months", "2026-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(matching.getId()))
                .andExpect(jsonPath("$[0].purpose").value("五月命中"))
                .andExpect(jsonPath("$[0].oaNumber").value("JF-001"))
                .andExpect(jsonPath("$[1]").doesNotExist());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void computesStatsBucketsByOaAndBatchFilters() throws Exception {
        OaNumber targetOa = oaNumbers.save(OaNumber.create("JF-001"));
        OaNumber otherOa = oaNumbers.save(OaNumber.create("JF-002"));
        BatchDtos.BatchResponse batch = batchService.create("admin", new BatchDtos.CreateBatchRequest("统计批次", "测试"));

        ReimbursementRecord unreimbursed = draft("100", "未报销", targetOa);
        unreimbursed.submit(1);
        records.save(unreimbursed);

        ReimbursementRecord reimbursed = draft("200", "已报销", targetOa);
        reimbursed.submit(1);
        reimbursed.markReimbursed();
        records.save(reimbursed);

        ReimbursementRecord draftRecord = draft("50", "草稿", targetOa);
        records.save(draftRecord);

        ReimbursementRecord wrongOa = draft("999", "其他经费", otherOa);
        wrongOa.submit(1);
        records.save(wrongOa);

        ReimbursementRecord inBatchUnreimbursed = draft("80", "批次未报销", targetOa);
        inBatchUnreimbursed.submit(1);
        records.save(inBatchUnreimbursed);

        ReimbursementRecord inBatchReimbursed = draft("30", "批次已报销", targetOa);
        inBatchReimbursed.submit(1);
        inBatchReimbursed.markReimbursed();
        records.save(inBatchReimbursed);

        batchService.addItem(batch.id(), inBatchUnreimbursed.getId());
        batchService.addItem(batch.id(), inBatchReimbursed.getId());

        mvc.perform(get("/api/admin/reimbursements/stats").param("oaIds", targetOa.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(5))
                .andExpect(jsonPath("$.totalAmount").value(460))
                .andExpect(jsonPath("$.unreimbursedCount").value(2))
                .andExpect(jsonPath("$.unreimbursedAmount").value(180))
                .andExpect(jsonPath("$.reimbursedCount").value(2))
                .andExpect(jsonPath("$.reimbursedAmount").value(230))
                .andExpect(jsonPath("$.draftCount").value(1))
                .andExpect(jsonPath("$.draftAmount").value(50));

        mvc.perform(get("/api/admin/reimbursements/stats")
                        .param("oaIds", targetOa.getId().toString())
                        .param("batchIds", batch.id().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.totalAmount").value(110))
                .andExpect(jsonPath("$.unreimbursedCount").value(1))
                .andExpect(jsonPath("$.unreimbursedAmount").value(80))
                .andExpect(jsonPath("$.reimbursedCount").value(1))
                .andExpect(jsonPath("$.reimbursedAmount").value(30))
                .andExpect(jsonPath("$.draftCount").value(0))
                .andExpect(jsonPath("$.draftAmount").value(0));
    }

    private ReimbursementRecord draft(String amount, String purpose, OaNumber oa) {
        ReimbursementRecord record = ReimbursementRecord.createDraft(employee, new BigDecimal(amount), travel, purpose, Instant.parse("2026-05-10T10:00:00Z"));
        record.setOa(oa);
        return record;
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

        ReimbursementRecord wrongKeyword = ReimbursementRecord.createDraft(employee, new BigDecimal("133.00"), travel, "内部培训", Instant.parse("2026-05-10T10:00:00Z"));
        wrongKeyword.submit(1);
        records.save(wrongKeyword);

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
                        .param("to", "2026-05-31")
                        .param("keyword", "客户"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(matching.getId()))
                .andExpect(jsonPath("$[0].purpose").value("客户拜访"))
                .andExpect(jsonPath("$[1]").doesNotExist());
    }
}
