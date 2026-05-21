package com.company.reimbursement.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:bootstrap_admin;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "app.bootstrap.admin.username=admin",
        "app.bootstrap.admin.password=secret123",
        "app.bootstrap.admin.display-name=系统管理员",
        "app.bootstrap.admin.department=财务部"
})
class BootstrapAdminTest {
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void createsConfiguredInitialAdmin() {
        User admin = users.findByUsername("admin").orElseThrow();

        assertThat(admin.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(admin.getDisplayName()).isEqualTo("系统管理员");
        assertThat(passwordEncoder.matches("secret123", admin.getPassword())).isTrue();
    }
}
