package com.company.reimbursement.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin_users;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
class AdminUserControllerTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;

    @BeforeEach
    void setUp() {
        users.deleteAll();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCreatesListsAndDisablesUsers() throws Exception {
        String createBody = """
                {
                  "username":"employee1",
                  "displayName":"员工一",
                  "department":"研发部",
                  "password":"secret123",
                  "role":"EMPLOYEE"
                }
                """;

        String updateBody = """
                {
                  "displayName":"员工一",
                  "department":"研发部",
                  "role":"EMPLOYEE",
                  "enabled":false
                }
                """;

        mvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("employee1"))
                .andExpect(jsonPath("$.enabled").value(true));
        Long id = users.findByUsername("employee1").orElseThrow().getId();

        mvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("employee1"));

        mvc.perform(patch("/api/admin/users/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    @WithMockUser(username = "employee", roles = "EMPLOYEE")
    void employeeCannotAccessAdminUsers() throws Exception {
        mvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "specialist", roles = "SPECIALIST")
    void specialistCannotAccessUserManagement() throws Exception {
        mvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }
}
