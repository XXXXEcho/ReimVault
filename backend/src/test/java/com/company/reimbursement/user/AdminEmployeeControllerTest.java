package com.company.reimbursement.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin_employees;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
class AdminEmployeeControllerTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        users.deleteAll();
        users.save(User.create("zhangsan", "张三", "研发部", passwordEncoder.encode("secret123"), UserRole.EMPLOYEE));
        users.save(User.create("lisi", "李四", "市场部", passwordEncoder.encode("secret123"), UserRole.EMPLOYEE));
        users.save(User.create("admin", "张管理员", "财务部", passwordEncoder.encode("secret123"), UserRole.ADMIN));
    }

    @Test
    @WithMockUser(username = "specialist", roles = "SPECIALIST")
    void specialistSearchesEmployeesByDisplayName() throws Exception {
        mvc.perform(get("/api/admin/employees").param("keyword", "张"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("zhangsan"))
                .andExpect(jsonPath("$[0].displayName").value("张三"))
                .andExpect(jsonPath("$[1]").doesNotExist());
    }

    @Test
    @WithMockUser(username = "employee", roles = "EMPLOYEE")
    void employeeCannotSearchEmployees() throws Exception {
        mvc.perform(get("/api/admin/employees"))
                .andExpect(status().isForbidden());
    }
}
