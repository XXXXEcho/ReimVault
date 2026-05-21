package com.company.reimbursement;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:migration;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class DatabaseMigrationTest {
    @Autowired JdbcTemplate jdbc;

    @Test
    void createsCoreTables() {
        Integer count = jdbc.queryForObject("select count(*) from users", Integer.class);
        assertThat(count).isZero();
    }
}
