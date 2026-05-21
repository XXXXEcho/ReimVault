package com.company.reimbursement.category;

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
        "spring.datasource.url=jdbc:h2:mem:categories;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
class CategoryControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ExpenseCategoryRepository categories;

    @BeforeEach
    void setUp() {
        categories.deleteAll();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCreatesAndUpdatesCategory() throws Exception {
        String createBody = """
                {
                  "name":"办公用品",
                  "enabled":true,
                  "sortOrder":10,
                  "remark":"日常办公采购"
                }
                """;

        mvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("办公用品"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.sortOrder").value(10));

        Long id = categories.findAll().getFirst().getId();
        String updateBody = """
                {
                  "name":"办公用品",
                  "enabled":false,
                  "sortOrder":20,
                  "remark":"停用"
                }
                """;

        mvc.perform(patch("/api/admin/categories/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.sortOrder").value(20));
    }

    @Test
    @WithMockUser(username = "employee", roles = "EMPLOYEE")
    void employeeListsOnlyEnabledCategories() throws Exception {
        categories.save(ExpenseCategory.create("办公用品", true, 1, null));
        categories.save(ExpenseCategory.create("停用分类", false, 2, null));

        mvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("办公用品"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser(username = "employee", roles = "EMPLOYEE")
    void employeeCannotCreateCategory() throws Exception {
        mvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"办公用品\",\"enabled\":true,\"sortOrder\":1}"))
                .andExpect(status().isForbidden());
    }
}
