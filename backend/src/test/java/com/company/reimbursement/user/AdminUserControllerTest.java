package com.company.reimbursement.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    @Autowired PasswordEncoder passwordEncoder;

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
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminUpdatesUserProfileAndPassword() throws Exception {
        User user = users.save(User.create("employee1", "员工一", "研发部", passwordEncoder.encode("old-secret"), UserRole.EMPLOYEE));
        String updateBody = """
                {
                  "displayName":"员工甲",
                  "department":"财务部",
                  "password":"new-secret",
                  "role":"ADMIN",
                  "enabled":false
                }
                """;

        mvc.perform(patch("/api/admin/users/{id}", user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("员工甲"))
                .andExpect(jsonPath("$.department").value("财务部"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.enabled").value(false));

        User updated = users.findById(user.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("new-secret", updated.getPassword())).isTrue();
    }

    @Test
    @WithMockUser(username = "employee", roles = "EMPLOYEE")
    void employeeCannotAccessAdminUsers() throws Exception {
        mvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }
}
