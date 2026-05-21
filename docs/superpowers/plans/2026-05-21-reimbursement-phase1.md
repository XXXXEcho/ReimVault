# 报销材料管理系统第一期 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建第一期可用的 BS 报销材料管理系统：员工提交材料，管理员管理用户/分类/批次，并导出 Excel 与附件压缩包。

**Architecture:** 采用前后端分离单体架构。后端 Spring Boot 提供 REST API、会话认证、业务规则、文件存储、Excel/Zip 导出；前端 Vue 3 提供员工端与管理员端页面。文件先落本地 `storage/`，数据库用 MySQL，测试用 H2。

**Tech Stack:** Java 21、Spring Boot 3、Spring Security、Spring Data JPA、Flyway、Apache POI、MySQL、H2、Maven、Vue 3、TypeScript、Vite、Element Plus、Vitest、Playwright。

---

## 边界

第一期实现：登录、用户管理、分类管理、报销记录、附件上传、批次管理、Excel 导出、附件压缩包导出、基础前端页面、核心验收测试。

第一期不实现：审批流、付款、OCR、外部财务系统、企业微信/钉钉登录、移动 App。

GitHub 规则：本计划只允许本地文件变更和本地提交；不创建 GitHub 仓库，不添加 remote，不 push。

---

## 文件结构

```text
F:\Code\报销\
  pom.xml
  backend/
    pom.xml
    src/main/java/com/company/reimbursement/
      ReimbursementApplication.java
      common/
        ApiError.java
        GlobalExceptionHandler.java
        PageResponse.java
      config/
        SecurityConfig.java
        StorageProperties.java
      auth/
        AuthController.java
        LoginRequest.java
        MeResponse.java
      user/
        User.java
        UserRole.java
        UserRepository.java
        UserService.java
        AdminUserController.java
        UserDtos.java
      category/
        ExpenseCategory.java
        ExpenseCategoryRepository.java
        ExpenseCategoryService.java
        CategoryController.java
        CategoryDtos.java
      reimbursement/
        ReimbursementRecord.java
        ReimbursementStatus.java
        ReimbursementRepository.java
        ReimbursementService.java
        EmployeeReimbursementController.java
        AdminReimbursementController.java
        ReimbursementDtos.java
      attachment/
        AttachmentType.java
        ReimbursementAttachment.java
        ReimbursementAttachmentRepository.java
        FileStorageService.java
        LocalFileStorageService.java
        AttachmentController.java
      batch/
        ReimbursementBatch.java
        ReimbursementBatchItem.java
        ReimbursementBatchRepository.java
        ReimbursementBatchItemRepository.java
        BatchService.java
        BatchController.java
        BatchDtos.java
      export/
        ExcelExportService.java
        ZipExportService.java
        ExportController.java
    src/main/resources/
      application.yml
      db/migration/V1__init_schema.sql
    src/test/java/com/company/reimbursement/
      auth/AuthControllerTest.java
      user/AdminUserControllerTest.java
      category/CategoryControllerTest.java
      reimbursement/ReimbursementServiceTest.java
      attachment/AttachmentControllerTest.java
      batch/BatchServiceTest.java
      export/ExportServiceTest.java
  frontend/
    package.json
    index.html
    vite.config.ts
    tsconfig.json
    src/
      main.ts
      App.vue
      router.ts
      api/http.ts
      api/auth.ts
      api/users.ts
      api/categories.ts
      api/reimbursements.ts
      api/batches.ts
      stores/auth.ts
      layouts/AppLayout.vue
      views/LoginView.vue
      views/employee/ReimbursementListView.vue
      views/employee/ReimbursementEditView.vue
      views/admin/UserAdminView.vue
      views/admin/CategoryAdminView.vue
      views/admin/ReimbursementAdminView.vue
      views/admin/BatchAdminView.vue
      components/AttachmentUploader.vue
      components/ReimbursementForm.vue
    tests/
      auth.spec.ts
      reimbursement-form.spec.ts
      admin-batch.spec.ts
```

---

## Task 1: 本地项目骨架

**Files:**
- Create: `pom.xml`
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/company/reimbursement/ReimbursementApplication.java`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/test/java/com/company/reimbursement/ApplicationSmokeTest.java`

- [ ] **Step 1: 初始化本地 Git，但不连接 GitHub**

Run:

```bash
git init
```

Expected: output contains `Initialized empty Git repository` or `Reinitialized existing Git repository`.

- [ ] **Step 2: 创建根 Maven 聚合文件**

Create `pom.xml`:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.company</groupId>
  <artifactId>reimbursement-system</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <packaging>pom</packaging>
  <modules>
    <module>backend</module>
  </modules>
</project>
```

- [ ] **Step 3: 创建后端 Maven 文件**

Create `backend/pom.xml`:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.5</version>
    <relativePath/>
  </parent>
  <groupId>com.company</groupId>
  <artifactId>reimbursement-backend</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <properties>
    <java.version>21</java.version>
  </properties>
  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
      <groupId>org.flywaydb</groupId>
      <artifactId>flyway-core</artifactId>
    </dependency>
    <dependency>
      <groupId>com.mysql</groupId>
      <artifactId>mysql-connector-j</artifactId>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>com.h2database</groupId>
      <artifactId>h2</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.apache.poi</groupId>
      <artifactId>poi-ooxml</artifactId>
      <version>5.3.0</version>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.security</groupId>
      <artifactId>spring-security-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>
</project>
```

- [ ] **Step 4: 写启动类和配置**

Create `backend/src/main/java/com/company/reimbursement/ReimbursementApplication.java`:

```java
package com.company.reimbursement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ReimbursementApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReimbursementApplication.class, args);
    }
}
```

Create `backend/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/reimbursement?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: reimbursement
    password: reimbursement
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  flyway:
    enabled: true
server:
  port: 8080
app:
  storage:
    root: storage/reimbursements
```

- [ ] **Step 5: 写启动测试**

Create `backend/src/test/java/com/company/reimbursement/ApplicationSmokeTest.java`:

```java
package com.company.reimbursement;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:smoke;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none"
})
class ApplicationSmokeTest {
    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 6: 运行测试**

Run:

```bash
mvn -pl backend test -Dtest=ApplicationSmokeTest
```

Expected: build ends with `BUILD SUCCESS`.

- [ ] **Step 7: 本地提交**

Run:

```bash
git add pom.xml backend/pom.xml backend/src/main/java backend/src/main/resources backend/src/test/java
git commit -m "chore: initialize reimbursement project"
```

Expected: local commit created. Do not run `git remote add` or `git push`.

---

## Task 2: 数据库迁移与核心表

**Files:**
- Create: `backend/src/main/resources/db/migration/V1__init_schema.sql`
- Create: `backend/src/test/java/com/company/reimbursement/DatabaseMigrationTest.java`

- [ ] **Step 1: 写迁移测试**

Create `backend/src/test/java/com/company/reimbursement/DatabaseMigrationTest.java`:

```java
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
```

- [ ] **Step 2: 运行测试验证失败**

Run:

```bash
mvn -pl backend test -Dtest=DatabaseMigrationTest
```

Expected: FAIL because table `users` does not exist.

- [ ] **Step 3: 创建迁移 SQL**

Create `backend/src/main/resources/db/migration/V1__init_schema.sql`:

```sql
create table users (
  id bigint generated by default as identity primary key,
  username varchar(80) not null unique,
  display_name varchar(80) not null,
  department varchar(120),
  password_hash varchar(120) not null,
  role varchar(20) not null,
  enabled boolean not null default true,
  created_at timestamp not null,
  updated_at timestamp not null
);

create table expense_categories (
  id bigint generated by default as identity primary key,
  name varchar(80) not null unique,
  enabled boolean not null default true,
  sort_order int not null default 0,
  remark varchar(255),
  created_at timestamp not null,
  updated_at timestamp not null
);

create table reimbursement_records (
  id bigint generated by default as identity primary key,
  employee_id bigint not null,
  amount decimal(12,2) not null,
  category_id bigint not null,
  purpose varchar(255) not null,
  payment_time timestamp not null,
  status varchar(20) not null,
  admin_remark varchar(500),
  created_at timestamp not null,
  updated_at timestamp not null,
  submitted_at timestamp,
  archived_at timestamp,
  constraint fk_records_employee foreign key (employee_id) references users(id),
  constraint fk_records_category foreign key (category_id) references expense_categories(id)
);

create table reimbursement_attachments (
  id bigint generated by default as identity primary key,
  record_id bigint not null,
  type varchar(40) not null,
  original_filename varchar(255) not null,
  storage_path varchar(500) not null,
  content_type varchar(120) not null,
  size_bytes bigint not null,
  created_at timestamp not null,
  constraint fk_attachments_record foreign key (record_id) references reimbursement_records(id)
);

create table reimbursement_batches (
  id bigint generated by default as identity primary key,
  name varchar(120) not null unique,
  description varchar(500),
  created_by bigint not null,
  created_at timestamp not null,
  archived_at timestamp,
  constraint fk_batches_creator foreign key (created_by) references users(id)
);

create table reimbursement_batch_items (
  id bigint generated by default as identity primary key,
  batch_id bigint not null,
  record_id bigint not null unique,
  created_at timestamp not null,
  constraint fk_batch_items_batch foreign key (batch_id) references reimbursement_batches(id),
  constraint fk_batch_items_record foreign key (record_id) references reimbursement_records(id)
);
```

- [ ] **Step 4: 运行测试验证通过**

Run:

```bash
mvn -pl backend test -Dtest=DatabaseMigrationTest
```

Expected: PASS and build ends with `BUILD SUCCESS`.

- [ ] **Step 5: 本地提交**

Run:

```bash
git add backend/src/main/resources/db/migration/V1__init_schema.sql backend/src/test/java/com/company/reimbursement/DatabaseMigrationTest.java
git commit -m "feat: add initial database schema"
```

---

## Task 3: 后端通用错误与安全基础

**Files:**
- Create: `backend/src/main/java/com/company/reimbursement/common/ApiError.java`
- Create: `backend/src/main/java/com/company/reimbursement/common/GlobalExceptionHandler.java`
- Create: `backend/src/main/java/com/company/reimbursement/common/PageResponse.java`
- Create: `backend/src/main/java/com/company/reimbursement/config/SecurityConfig.java`
- Create: `backend/src/test/java/com/company/reimbursement/auth/SecurityRulesTest.java`

- [ ] **Step 1: 写未登录访问测试**

Create `backend/src/test/java/com/company/reimbursement/auth/SecurityRulesTest.java`:

```java
package com.company.reimbursement.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.reimbursement.ReimbursementApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = ReimbursementApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:security;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
class SecurityRulesTest {
    @Autowired MockMvc mvc;

    @Test
    void apiRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run:

```bash
mvn -pl backend test -Dtest=SecurityRulesTest
```

Expected: FAIL because security/API error shape is not configured.

- [ ] **Step 3: 写通用响应类**

Create `backend/src/main/java/com/company/reimbursement/common/ApiError.java`:

```java
package com.company.reimbursement.common;

import java.time.Instant;

public record ApiError(String code, String message, Instant timestamp) {
    public static ApiError of(String code, String message) {
        return new ApiError(code, message, Instant.now());
    }
}
```

Create `backend/src/main/java/com/company/reimbursement/common/PageResponse.java`:

```java
package com.company.reimbursement.common;

import java.util.List;

public record PageResponse<T>(List<T> items, long total, int page, int size) {
}
```

Create `backend/src/main/java/com/company/reimbursement/common/GlobalExceptionHandler.java`:

```java
package com.company.reimbursement.common;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ApiError.of("BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    ResponseEntity<ApiError> validation(Exception ex) {
        return ResponseEntity.badRequest().body(ApiError.of("VALIDATION_ERROR", "请求参数不合法"));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    ResponseEntity<ApiError> notFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of("NOT_FOUND", ex.getMessage()));
    }
}
```

- [ ] **Step 4: 写安全配置**

Create `backend/src/main/java/com/company/reimbursement/config/SecurityConfig.java`:

```java
package com.company.reimbursement.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.company.reimbursement.common.ApiError;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        http.csrf(csrf -> csrf.disable());
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login").permitAll()
                .anyRequest().authenticated());
        http.exceptionHandling(handler -> handler.authenticationEntryPoint((request, response, ex) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), ApiError.of("UNAUTHORIZED", "请先登录"));
        }));
        http.logout(logout -> logout.logoutUrl("/api/auth/logout"));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

- [ ] **Step 5: 运行测试验证通过**

Run:

```bash
mvn -pl backend test -Dtest=SecurityRulesTest
```

Expected: PASS.

- [ ] **Step 6: 本地提交**

Run:

```bash
git add backend/src/main/java/com/company/reimbursement/common backend/src/main/java/com/company/reimbursement/config backend/src/test/java/com/company/reimbursement/auth/SecurityRulesTest.java
git commit -m "feat: add API error handling and security baseline"
```

---

## Task 4: 用户实体、登录与当前用户接口

**Files:**
- Create: `backend/src/main/java/com/company/reimbursement/user/UserRole.java`
- Create: `backend/src/main/java/com/company/reimbursement/user/User.java`
- Create: `backend/src/main/java/com/company/reimbursement/user/UserRepository.java`
- Create: `backend/src/main/java/com/company/reimbursement/auth/LoginRequest.java`
- Create: `backend/src/main/java/com/company/reimbursement/auth/MeResponse.java`
- Create: `backend/src/main/java/com/company/reimbursement/auth/AuthController.java`
- Create: `backend/src/test/java/com/company/reimbursement/auth/AuthControllerTest.java`

- [ ] **Step 1: 写登录测试**

Create `backend/src/test/java/com/company/reimbursement/auth/AuthControllerTest.java` with tests for successful login, bad password, and `/api/auth/me`.

```java
package com.company.reimbursement.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.reimbursement.user.User;
import com.company.reimbursement.user.UserRepository;
import com.company.reimbursement.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
class AuthControllerTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        users.deleteAll();
        users.save(User.create("admin", "管理员", "财务部", passwordEncoder.encode("secret123"), UserRole.ADMIN));
    }

    @Test
    void loginAndReadCurrentUser() throws Exception {
        MvcResult login = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andReturn();

        mvc.perform(get("/api/auth/me").session((org.springframework.mock.web.MockHttpSession) login.getRequest().getSession(false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("管理员"));
    }

    @Test
    void rejectsBadPassword() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"bad\"}"))
                .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run:

```bash
mvn -pl backend test -Dtest=AuthControllerTest
```

Expected: FAIL because user/auth classes are missing.

- [ ] **Step 3: 实现用户模型**

Create `UserRole.java`:

```java
package com.company.reimbursement.user;

public enum UserRole {
    EMPLOYEE,
    ADMIN
}
```

Create `User.java`:

```java
package com.company.reimbursement.user;

import jakarta.persistence.*;
import java.time.Instant;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
public class User implements UserDetails {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String displayName;
    private String department;
    private String passwordHash;
    @Enumerated(EnumType.STRING)
    private UserRole role;
    private boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;

    protected User() {}

    public static User create(String username, String displayName, String department, String passwordHash, UserRole role) {
        User user = new User();
        user.username = username;
        user.displayName = displayName;
        user.department = department;
        user.passwordHash = passwordHash;
        user.role = role;
        user.enabled = true;
        user.createdAt = Instant.now();
        user.updatedAt = user.createdAt;
        return user;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getDisplayName() { return displayName; }
    public String getDepartment() { return department; }
    public UserRole getRole() { return role; }
    public void update(String displayName, String department, UserRole role, boolean enabled) {
        this.displayName = displayName;
        this.department = department;
        this.role = role;
        this.enabled = enabled;
        this.updatedAt = Instant.now();
    }
    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
        this.updatedAt = Instant.now();
    }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
    @Override public String getPassword() { return passwordHash; }
    @Override public boolean isEnabled() { return enabled; }
}
```

Create `UserRepository.java`:

```java
package com.company.reimbursement.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
}
```

- [ ] **Step 4: 实现认证接口**

Create `LoginRequest.java`:

```java
package com.company.reimbursement.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String username, @NotBlank String password) {
}
```

Create `MeResponse.java`:

```java
package com.company.reimbursement.auth;

import com.company.reimbursement.user.User;
import com.company.reimbursement.user.UserRole;

public record MeResponse(Long id, String username, String displayName, String department, UserRole role) {
    public static MeResponse from(User user) {
        return new MeResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getDepartment(), user.getRole());
    }
}
```

Create `AuthController.java`:

```java
package com.company.reimbursement.auth;

import com.company.reimbursement.user.User;
import com.company.reimbursement.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    MeResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        User user = users.findByUsername(request.username()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (!user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        httpRequest.getSession(true).setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return MeResponse.from(user);
    }

    @GetMapping("/me")
    MeResponse me(org.springframework.security.core.Authentication authentication) {
        String username = authentication.getName();
        User user = users.findByUsername(username).orElseThrow(() -> new EntityNotFoundException("用户不存在"));
        return MeResponse.from(user);
    }
}
```

- [ ] **Step 5: 配置 UserDetailsService**

Modify `SecurityConfig.java` to add this bean:

```java
@Bean
org.springframework.security.core.userdetails.UserDetailsService userDetailsService(com.company.reimbursement.user.UserRepository users) {
    return username -> users.findByUsername(username)
            .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException(username));
}
```

- [ ] **Step 6: 运行测试验证通过**

Run:

```bash
mvn -pl backend test -Dtest=AuthControllerTest,SecurityRulesTest
```

Expected: PASS.

- [ ] **Step 7: 本地提交**

Run:

```bash
git add backend/src/main/java/com/company/reimbursement/user backend/src/main/java/com/company/reimbursement/auth backend/src/main/java/com/company/reimbursement/config/SecurityConfig.java backend/src/test/java/com/company/reimbursement/auth
git commit -m "feat: add session login and current user API"
```

---

## Task 5: 管理员用户管理

**Files:**
- Create: `backend/src/main/java/com/company/reimbursement/user/UserDtos.java`
- Create: `backend/src/main/java/com/company/reimbursement/user/UserService.java`
- Create: `backend/src/main/java/com/company/reimbursement/user/AdminUserController.java`
- Create: `backend/src/test/java/com/company/reimbursement/user/AdminUserControllerTest.java`

- [ ] **Step 1: 写管理员用户管理测试**

Test behaviors:

```java
@WithMockUser(username = "admin", roles = "ADMIN")
void adminCreatesListsAndDisablesUsers()
```

Assertions:

- `POST /api/admin/users` with username, displayName, department, password, role returns 200 and id.
- `GET /api/admin/users` returns created user.
- `PATCH /api/admin/users/{id}` can set `enabled=false`.
- Employee role receives 403 for admin endpoint.

- [ ] **Step 2: 运行测试验证失败**

Run:

```bash
mvn -pl backend test -Dtest=AdminUserControllerTest
```

Expected: FAIL because admin user endpoints are missing.

- [ ] **Step 3: 实现 DTO**

Create `UserDtos.java`:

```java
package com.company.reimbursement.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UserDtos {
    public record CreateUserRequest(@NotBlank String username, @NotBlank String displayName, String department,
                                    @NotBlank String password, @NotNull UserRole role) {}
    public record UpdateUserRequest(@NotBlank String displayName, String department, @NotNull UserRole role, boolean enabled) {}
    public record UserResponse(Long id, String username, String displayName, String department, UserRole role, boolean enabled) {
        public static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getDepartment(), user.getRole(), user.isEnabled());
        }
    }
}
```

- [ ] **Step 4: 实现服务与控制器**

Create `UserService.java` with methods `create`, `list`, `update`, and username uniqueness validation.

Create `AdminUserController.java`:

```java
package com.company.reimbursement.user;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    private final UserService service;
    public AdminUserController(UserService service) { this.service = service; }

    @PostMapping
    UserDtos.UserResponse create(@Valid @RequestBody UserDtos.CreateUserRequest request) { return service.create(request); }

    @GetMapping
    List<UserDtos.UserResponse> list() { return service.list(); }

    @PatchMapping("/{id}")
    UserDtos.UserResponse update(@PathVariable Long id, @Valid @RequestBody UserDtos.UpdateUserRequest request) {
        return service.update(id, request);
    }
}
```

- [ ] **Step 5: 运行测试验证通过**

Run:

```bash
mvn -pl backend test -Dtest=AdminUserControllerTest
```

Expected: PASS.

- [ ] **Step 6: 本地提交**

Run:

```bash
git add backend/src/main/java/com/company/reimbursement/user backend/src/test/java/com/company/reimbursement/user/AdminUserControllerTest.java
git commit -m "feat: add admin user management"
```

---

## Task 6: 用途分类管理

**Files:**
- Create: `backend/src/main/java/com/company/reimbursement/category/ExpenseCategory.java`
- Create: `backend/src/main/java/com/company/reimbursement/category/ExpenseCategoryRepository.java`
- Create: `backend/src/main/java/com/company/reimbursement/category/CategoryDtos.java`
- Create: `backend/src/main/java/com/company/reimbursement/category/ExpenseCategoryService.java`
- Create: `backend/src/main/java/com/company/reimbursement/category/CategoryController.java`
- Create: `backend/src/test/java/com/company/reimbursement/category/CategoryControllerTest.java`

- [ ] **Step 1: 写分类测试**

Cover:

- Admin creates category.
- Admin updates enabled state and sort order.
- Employee lists only enabled categories.
- Employee cannot create category.

- [ ] **Step 2: 运行测试验证失败**

Run:

```bash
mvn -pl backend test -Dtest=CategoryControllerTest
```

Expected: FAIL because category endpoints are missing.

- [ ] **Step 3: 实现分类实体与仓库**

Create entity fields matching `expense_categories`: id, name, enabled, sortOrder, remark, createdAt, updatedAt.

Repository methods:

```java
List<ExpenseCategory> findByEnabledTrueOrderBySortOrderAscNameAsc();
boolean existsByName(String name);
```

- [ ] **Step 4: 实现接口**

Endpoints:

```text
GET /api/categories
GET /api/admin/categories
POST /api/admin/categories
PATCH /api/admin/categories/{id}
```

DTO names:

```text
CreateCategoryRequest(name, enabled, sortOrder, remark)
UpdateCategoryRequest(name, enabled, sortOrder, remark)
CategoryResponse(id, name, enabled, sortOrder, remark)
```

- [ ] **Step 5: 运行测试验证通过**

Run:

```bash
mvn -pl backend test -Dtest=CategoryControllerTest
```

Expected: PASS.

- [ ] **Step 6: 本地提交**

Run:

```bash
git add backend/src/main/java/com/company/reimbursement/category backend/src/test/java/com/company/reimbursement/category/CategoryControllerTest.java
git commit -m "feat: add expense category management"
```

---

## Task 7: 报销记录草稿、提交、归档规则

**Files:**
- Create: `backend/src/main/java/com/company/reimbursement/reimbursement/ReimbursementStatus.java`
- Create: `backend/src/main/java/com/company/reimbursement/reimbursement/ReimbursementRecord.java`
- Create: `backend/src/main/java/com/company/reimbursement/reimbursement/ReimbursementRepository.java`
- Create: `backend/src/main/java/com/company/reimbursement/reimbursement/ReimbursementDtos.java`
- Create: `backend/src/main/java/com/company/reimbursement/reimbursement/ReimbursementService.java`
- Create: `backend/src/main/java/com/company/reimbursement/reimbursement/EmployeeReimbursementController.java`
- Create: `backend/src/main/java/com/company/reimbursement/reimbursement/AdminReimbursementController.java`
- Create: `backend/src/test/java/com/company/reimbursement/reimbursement/ReimbursementServiceTest.java`

- [ ] **Step 1: 写业务规则测试**

Cover:

- Employee creates draft.
- Employee updates own draft.
- Employee cannot update submitted record.
- Employee cannot read another employee's record.
- Submit requires payment voucher count greater than zero.
- Admin can archive a submitted record only after it belongs to a batch; this final assertion will pass after Task 10.

- [ ] **Step 2: 运行测试验证失败**

Run:

```bash
mvn -pl backend test -Dtest=ReimbursementServiceTest
```

Expected: FAIL because reimbursement domain is missing.

- [ ] **Step 3: 实现状态枚举**

Create `ReimbursementStatus.java`:

```java
package com.company.reimbursement.reimbursement;

public enum ReimbursementStatus {
    DRAFT,
    SUBMITTED,
    ARCHIVED
}
```

- [ ] **Step 4: 实现记录实体**

Entity fields:

```text
id, employee, amount, category, purpose, paymentTime, status, adminRemark, createdAt, updatedAt, submittedAt, archivedAt
```

Methods:

```text
createDraft(User employee, BigDecimal amount, ExpenseCategory category, String purpose, Instant paymentTime)
updateDraft(BigDecimal amount, ExpenseCategory category, String purpose, Instant paymentTime)
submit(int paymentVoucherCount)
archive()
setAdminRemark(String adminRemark)
```

Rule in `submit`: if `paymentVoucherCount < 1`, throw `IllegalArgumentException("至少上传一张支付凭证")`.

- [ ] **Step 5: 实现员工接口**

Endpoints:

```text
GET /api/reimbursements
POST /api/reimbursements
GET /api/reimbursements/{id}
PATCH /api/reimbursements/{id}
POST /api/reimbursements/{id}/submit
```

Employees only access their own records.

- [ ] **Step 6: 实现管理员查询接口**

Endpoints:

```text
GET /api/admin/reimbursements?employeeId=&categoryId=&status=&from=&to=
PATCH /api/admin/reimbursements/{id}/remark
```

Admin can query all records and write remarks.

- [ ] **Step 7: 运行测试验证通过**

Run:

```bash
mvn -pl backend test -Dtest=ReimbursementServiceTest
```

Expected: PASS except the batch-dependent archive assertion if it was added. If that assertion exists, mark it disabled with name `archiveRequiresBatchMembershipAfterBatchModule` and enable it in Task 10.

- [ ] **Step 8: 本地提交**

Run:

```bash
git add backend/src/main/java/com/company/reimbursement/reimbursement backend/src/test/java/com/company/reimbursement/reimbursement/ReimbursementServiceTest.java
git commit -m "feat: add reimbursement record workflow"
```

---

## Task 8: 附件上传、下载与本地存储

**Files:**
- Create: `backend/src/main/java/com/company/reimbursement/attachment/AttachmentType.java`
- Create: `backend/src/main/java/com/company/reimbursement/attachment/ReimbursementAttachment.java`
- Create: `backend/src/main/java/com/company/reimbursement/attachment/ReimbursementAttachmentRepository.java`
- Create: `backend/src/main/java/com/company/reimbursement/config/StorageProperties.java`
- Create: `backend/src/main/java/com/company/reimbursement/attachment/FileStorageService.java`
- Create: `backend/src/main/java/com/company/reimbursement/attachment/LocalFileStorageService.java`
- Create: `backend/src/main/java/com/company/reimbursement/attachment/AttachmentController.java`
- Create: `backend/src/test/java/com/company/reimbursement/attachment/AttachmentControllerTest.java`

- [ ] **Step 1: 写附件测试**

Cover:

- Employee uploads `payment_voucher` to own draft.
- Employee uploads `order_screenshot` and `invoice` to own draft.
- Upload rejects non-image/non-PDF content type.
- Upload rejects file above 10MB.
- Employee cannot upload to submitted record.
- Employee cannot download another employee's attachment.
- Admin can download any attachment.

- [ ] **Step 2: 运行测试验证失败**

Run:

```bash
mvn -pl backend test -Dtest=AttachmentControllerTest
```

Expected: FAIL because attachment module is missing.

- [ ] **Step 3: 实现附件类型**

Create `AttachmentType.java`:

```java
package com.company.reimbursement.attachment;

public enum AttachmentType {
    PAYMENT_VOUCHER,
    ORDER_SCREENSHOT,
    INVOICE
}
```

- [ ] **Step 4: 实现存储配置与服务**

Create `StorageProperties.java`:

```java
package com.company.reimbursement.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(String root) {
}
```

Enable properties in `ReimbursementApplication`:

```java
@org.springframework.boot.context.properties.EnableConfigurationProperties(com.company.reimbursement.config.StorageProperties.class)
```

`FileStorageService` methods:

```text
StoredFile save(Long recordId, AttachmentType type, MultipartFile file)
Resource load(String storagePath)
boolean exists(String storagePath)
```

Store files under:

```text
storage/reimbursements/{recordId}/{type-lowercase}/{uuid}-{safe-original-name}
```

Allowed content types:

```text
image/jpeg
image/png
image/webp
application/pdf
```

- [ ] **Step 5: 实现附件接口**

Endpoints:

```text
POST /api/reimbursements/{id}/attachments?type=PAYMENT_VOUCHER
GET /api/attachments/{attachmentId}
DELETE /api/attachments/{attachmentId}
```

Delete is allowed only for draft owner and draft records.

- [ ] **Step 6: 运行附件测试和提交测试**

Run:

```bash
mvn -pl backend test -Dtest=AttachmentControllerTest,ReimbursementServiceTest
```

Expected: PASS. Submit test must now count `PAYMENT_VOUCHER` attachments from repository.

- [ ] **Step 7: 本地提交**

Run:

```bash
git add backend/src/main/java/com/company/reimbursement/attachment backend/src/main/java/com/company/reimbursement/config/StorageProperties.java backend/src/main/java/com/company/reimbursement/ReimbursementApplication.java backend/src/test/java/com/company/reimbursement/attachment/AttachmentControllerTest.java
git commit -m "feat: add reimbursement attachment storage"
```

---

## Task 9: 批次管理

**Files:**
- Create: `backend/src/main/java/com/company/reimbursement/batch/ReimbursementBatch.java`
- Create: `backend/src/main/java/com/company/reimbursement/batch/ReimbursementBatchItem.java`
- Create: `backend/src/main/java/com/company/reimbursement/batch/ReimbursementBatchRepository.java`
- Create: `backend/src/main/java/com/company/reimbursement/batch/ReimbursementBatchItemRepository.java`
- Create: `backend/src/main/java/com/company/reimbursement/batch/BatchDtos.java`
- Create: `backend/src/main/java/com/company/reimbursement/batch/BatchService.java`
- Create: `backend/src/main/java/com/company/reimbursement/batch/BatchController.java`
- Create: `backend/src/test/java/com/company/reimbursement/batch/BatchServiceTest.java`

- [ ] **Step 1: 写批次测试**

Cover:

- Admin creates batch.
- Admin adds submitted record to batch.
- Admin cannot add draft record.
- Admin can remove not-yet-archived record from batch.
- Admin archives batch and records become `ARCHIVED`.
- Admin cannot archive a record outside any batch.

- [ ] **Step 2: 运行测试验证失败**

Run:

```bash
mvn -pl backend test -Dtest=BatchServiceTest
```

Expected: FAIL because batch module is missing.

- [ ] **Step 3: 实现批次实体**

`ReimbursementBatch` fields:

```text
id, name, description, createdBy, createdAt, archivedAt
```

`ReimbursementBatchItem` fields:

```text
id, batch, record, createdAt
```

Repository methods:

```text
boolean existsByRecordId(Long recordId)
List<ReimbursementBatchItem> findByBatchId(Long batchId)
```

- [ ] **Step 4: 实现批次接口**

Endpoints:

```text
POST /api/admin/batches
GET /api/admin/batches
GET /api/admin/batches/{id}
POST /api/admin/batches/{id}/items/{recordId}
DELETE /api/admin/batches/{id}/items/{recordId}
POST /api/admin/batches/{id}/archive
```

All endpoints require admin role.

- [ ] **Step 5: 启用归档规则测试**

Enable the disabled archive assertion from Task 7. The passing rule is:

```text
Only records in a batch can be archived, and batch archive archives all its items.
```

- [ ] **Step 6: 运行测试验证通过**

Run:

```bash
mvn -pl backend test -Dtest=BatchServiceTest,ReimbursementServiceTest
```

Expected: PASS.

- [ ] **Step 7: 本地提交**

Run:

```bash
git add backend/src/main/java/com/company/reimbursement/batch backend/src/main/java/com/company/reimbursement/reimbursement backend/src/test/java/com/company/reimbursement/batch/BatchServiceTest.java backend/src/test/java/com/company/reimbursement/reimbursement/ReimbursementServiceTest.java
git commit -m "feat: add reimbursement batch management"
```

---

## Task 10: Excel 导出

**Files:**
- Create: `backend/src/main/java/com/company/reimbursement/export/ExcelExportService.java`
- Create: `backend/src/main/java/com/company/reimbursement/export/ExportController.java`
- Create: `backend/src/test/java/com/company/reimbursement/export/ExportServiceTest.java`

- [ ] **Step 1: 写 Excel 导出测试**

Cover:

- Export batch produces `.xlsx` workbook.
- Header row equals: 批次名称、员工姓名、部门、金额、用途分类、用途说明、支付时间、支付凭证数量、订单截图数量、发票数量、提交时间、管理员备注、附件目录路径。
- Missing attachment file marks path with `附件缺失`.

- [ ] **Step 2: 运行测试验证失败**

Run:

```bash
mvn -pl backend test -Dtest=ExportServiceTest
```

Expected: FAIL because export service is missing.

- [ ] **Step 3: 实现 Excel 服务**

`ExcelExportService` public method:

```java
public byte[] exportBatch(Long batchId)
```

Workbook rules:

```text
sheet name: 报销清单
row 0: fixed headers
one data row per batch item
amount as numeric cell
attachment counts grouped by AttachmentType
attachment directory path: {employee-display-name}/{sequence}-{category-name}-{amount}
if any storage file is missing: append "（附件缺失）"
```

- [ ] **Step 4: 实现导出接口**

`ExportController` endpoint:

```text
GET /api/admin/batches/{id}/export/excel
```

Response headers:

```text
Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
Content-Disposition: attachment; filename="batch-{id}.xlsx"
```

- [ ] **Step 5: 运行测试验证通过**

Run:

```bash
mvn -pl backend test -Dtest=ExportServiceTest
```

Expected: PASS.

- [ ] **Step 6: 本地提交**

Run:

```bash
git add backend/src/main/java/com/company/reimbursement/export backend/src/test/java/com/company/reimbursement/export/ExportServiceTest.java
git commit -m "feat: add batch Excel export"
```

---

## Task 11: 附件压缩包导出

**Files:**
- Modify: `backend/src/main/java/com/company/reimbursement/export/ZipExportService.java`
- Modify: `backend/src/main/java/com/company/reimbursement/export/ExportController.java`
- Modify: `backend/src/test/java/com/company/reimbursement/export/ExportServiceTest.java`

- [ ] **Step 1: 写 Zip 导出测试**

Cover expected entries:

```text
报销批次-{batchName}/{employeeName}/001-{categoryName}-{amount}/支付凭证/{originalFilename}
报销批次-{batchName}/{employeeName}/001-{categoryName}-{amount}/订单截图/{originalFilename}
报销批次-{batchName}/{employeeName}/001-{categoryName}-{amount}/发票/{originalFilename}
```

Missing physical files must not break export; add text entry:

```text
报销批次-{batchName}/附件缺失清单.txt
```

- [ ] **Step 2: 运行测试验证失败**

Run:

```bash
mvn -pl backend test -Dtest=ExportServiceTest
```

Expected: FAIL because Zip export is missing.

- [ ] **Step 3: 实现 Zip 服务**

`ZipExportService` public method:

```java
public byte[] exportBatchAttachments(Long batchId)
```

Folder name mapping:

```text
PAYMENT_VOUCHER -> 支付凭证
ORDER_SCREENSHOT -> 订单截图
INVOICE -> 发票
```

Path sanitization: replace `\`, `/`, `:`, `*`, `?`, `"`, `<`, `>`, `|` with `_`.

- [ ] **Step 4: 实现 Zip 接口**

`ExportController` endpoint:

```text
GET /api/admin/batches/{id}/export/attachments
```

Response headers:

```text
Content-Type: application/zip
Content-Disposition: attachment; filename="batch-{id}-attachments.zip"
```

- [ ] **Step 5: 运行测试验证通过**

Run:

```bash
mvn -pl backend test -Dtest=ExportServiceTest
```

Expected: PASS.

- [ ] **Step 6: 本地提交**

Run:

```bash
git add backend/src/main/java/com/company/reimbursement/export backend/src/test/java/com/company/reimbursement/export/ExportServiceTest.java
git commit -m "feat: add batch attachment zip export"
```

---

## Task 12: 前端项目骨架、路由与认证

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/index.html`
- Create: `frontend/vite.config.ts`
- Create: `frontend/tsconfig.json`
- Create: `frontend/src/main.ts`
- Create: `frontend/src/App.vue`
- Create: `frontend/src/router.ts`
- Create: `frontend/src/api/http.ts`
- Create: `frontend/src/api/auth.ts`
- Create: `frontend/src/stores/auth.ts`
- Create: `frontend/src/views/LoginView.vue`
- Create: `frontend/tests/auth.spec.ts`

- [ ] **Step 1: 创建前端 package**

Create `frontend/package.json`:

```json
{
  "scripts": {
    "dev": "vite --host 127.0.0.1",
    "build": "vue-tsc -b && vite build",
    "test": "vitest run"
  },
  "dependencies": {
    "@vitejs/plugin-vue": "latest",
    "axios": "latest",
    "element-plus": "latest",
    "pinia": "latest",
    "vue": "latest",
    "vue-router": "latest"
  },
  "devDependencies": {
    "@vue/test-utils": "latest",
    "jsdom": "latest",
    "typescript": "latest",
    "vite": "latest",
    "vitest": "latest",
    "vue-tsc": "latest"
  }
}
```

- [ ] **Step 2: 写登录组件测试**

Create `frontend/tests/auth.spec.ts` to verify:

- Login view renders username/password inputs.
- Submit calls `POST /api/auth/login`.
- Auth store stores current user.

- [ ] **Step 3: 运行测试验证失败**

Run:

```bash
cd frontend && npm install && npm test
```

Expected: FAIL because source files are missing.

- [ ] **Step 4: 实现前端入口与认证**

Create HTTP client with cookies:

```ts
import axios from 'axios'

export const http = axios.create({
  baseURL: '/api',
  withCredentials: true,
})
```

Routes:

```text
/login
/reimbursements
/reimbursements/new
/admin/users
/admin/categories
/admin/reimbursements
/admin/batches
```

Auth guard: if route is not `/login` and no user exists, call `/auth/me`; if it fails, redirect to `/login`.

- [ ] **Step 5: 运行测试和构建**

Run:

```bash
cd frontend && npm test && npm run build
```

Expected: tests PASS and build succeeds.

- [ ] **Step 6: 本地提交**

Run:

```bash
git add frontend
git commit -m "feat: add frontend shell and login"
```

---

## Task 13: 员工报销页面与附件上传组件

**Files:**
- Create: `frontend/src/api/categories.ts`
- Create: `frontend/src/api/reimbursements.ts`
- Create: `frontend/src/components/AttachmentUploader.vue`
- Create: `frontend/src/components/ReimbursementForm.vue`
- Create: `frontend/src/views/employee/ReimbursementListView.vue`
- Create: `frontend/src/views/employee/ReimbursementEditView.vue`
- Create: `frontend/tests/reimbursement-form.spec.ts`

- [ ] **Step 1: 写表单测试**

Cover:

- Form requires amount, category, purpose, paymentTime.
- Payment voucher uploader is visually marked required.
- Order screenshot and invoice uploaders are optional.
- Submit button calls `/reimbursements/{id}/submit` only after draft is saved.

- [ ] **Step 2: 运行测试验证失败**

Run:

```bash
cd frontend && npm test -- reimbursement-form.spec.ts
```

Expected: FAIL because form components are missing.

- [ ] **Step 3: 实现 API 客户端**

Functions:

```text
listEnabledCategories()
listMyReimbursements()
createReimbursement(payload)
updateReimbursement(id, payload)
submitReimbursement(id)
uploadAttachment(recordId, type, file)
deleteAttachment(attachmentId)
```

- [ ] **Step 4: 实现页面**

Employee list columns:

```text
金额、用途分类、用途说明、支付时间、状态、提交时间、操作
```

Form fields:

```text
amount, categoryId, purpose, paymentTime, paymentVoucherFiles, orderScreenshotFiles, invoiceFiles
```

- [ ] **Step 5: 运行测试和构建**

Run:

```bash
cd frontend && npm test -- reimbursement-form.spec.ts && npm run build
```

Expected: PASS and build succeeds.

- [ ] **Step 6: 本地提交**

Run:

```bash
git add frontend/src/api frontend/src/components frontend/src/views/employee frontend/tests/reimbursement-form.spec.ts
git commit -m "feat: add employee reimbursement UI"
```

---

## Task 14: 管理员页面

**Files:**
- Create: `frontend/src/api/users.ts`
- Create: `frontend/src/api/batches.ts`
- Create: `frontend/src/views/admin/UserAdminView.vue`
- Create: `frontend/src/views/admin/CategoryAdminView.vue`
- Create: `frontend/src/views/admin/ReimbursementAdminView.vue`
- Create: `frontend/src/views/admin/BatchAdminView.vue`
- Create: `frontend/tests/admin-batch.spec.ts`

- [ ] **Step 1: 写管理员页面测试**

Cover:

- User admin creates employee.
- Category admin creates enabled category.
- Reimbursement admin filters submitted records.
- Batch admin adds selected record to batch.
- Export buttons use Excel and attachments endpoints.

- [ ] **Step 2: 运行测试验证失败**

Run:

```bash
cd frontend && npm test -- admin-batch.spec.ts
```

Expected: FAIL because admin pages are missing.

- [ ] **Step 3: 实现管理员 API**

Functions:

```text
listUsers(), createUser(), updateUser()
listAdminCategories(), createCategory(), updateCategory()
listAdminReimbursements(filters), updateAdminRemark()
listBatches(), createBatch(), addBatchItem(), removeBatchItem(), archiveBatch(), downloadExcel(), downloadAttachments()
```

- [ ] **Step 4: 实现管理员页面**

Pages:

```text
UserAdminView: user table and create/edit drawer
CategoryAdminView: category table and create/edit drawer
ReimbursementAdminView: filters, record table, remark edit
BatchAdminView: batch table, item table, add record, export buttons, archive button
```

- [ ] **Step 5: 运行测试和构建**

Run:

```bash
cd frontend && npm test -- admin-batch.spec.ts && npm run build
```

Expected: PASS and build succeeds.

- [ ] **Step 6: 本地提交**

Run:

```bash
git add frontend/src/api frontend/src/views/admin frontend/tests/admin-batch.spec.ts
git commit -m "feat: add admin management UI"
```

---

## Task 15: 联调、验收与项目运行说明

**Files:**
- Create: `README.md`
- Modify: `tasks/todo.md`
- Modify: `tasks/lessons.md` only if implementation produces a corrected error pattern

- [ ] **Step 1: 写本地运行说明**

Create `README.md` with:

```markdown
# 报销材料管理系统

## 本地启动

### 后端

```bash
mvn -pl backend spring-boot:run
```

### 前端

```bash
cd frontend
npm install
npm run dev
```

## 数据库

默认连接 MySQL：`jdbc:mysql://localhost:3306/reimbursement`。

## 文件存储

默认目录：`storage/reimbursements`。

## 不包含

第一期不包含审批流、付款、OCR、财务系统对接、企业微信/钉钉登录、移动 App。
```

- [ ] **Step 2: 运行后端全量测试**

Run:

```bash
mvn -pl backend test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 3: 运行前端全量测试与构建**

Run:

```bash
cd frontend && npm test && npm run build
```

Expected: tests PASS and build succeeds.

- [ ] **Step 4: 启动后端和前端**

Run backend:

```bash
mvn -pl backend spring-boot:run
```

Run frontend in a second terminal:

```bash
cd frontend && npm run dev
```

Expected: frontend dev server prints local URL and backend listens on port 8080.

- [ ] **Step 5: 浏览器验收**

Use browser to verify:

```text
1. Admin logs in.
2. Admin creates an employee.
3. Admin creates an enabled category.
4. Employee logs in.
5. Employee creates draft.
6. Employee uploads payment voucher.
7. Employee optionally uploads invoice.
8. Employee submits record.
9. Admin sees submitted record.
10. Admin creates batch.
11. Admin adds record to batch.
12. Admin downloads Excel.
13. Admin downloads attachment Zip.
14. Admin archives batch.
15. Record status becomes archived.
```

- [ ] **Step 6: 更新任务计划**

Modify `tasks/todo.md` so completed implementation tasks are checked. Add a results section with exact verification commands and outcomes.

- [ ] **Step 7: 本地提交**

Run:

```bash
git add README.md tasks/todo.md tasks/lessons.md
git commit -m "docs: add local run and verification notes"
```

Do not push to GitHub.

---

## Spec Coverage Review

- 系统定位：Task 1, Task 15。
- 员工与管理员角色：Task 4, Task 5, Task 12, Task 14。
- 草稿、已提交、已归档状态：Task 7, Task 9。
- 支付凭证必填，订单截图/发票选填：Task 7, Task 8, Task 13。
- 管理员维护用途分类：Task 6, Task 14。
- 批次管理：Task 9, Task 14。
- Excel 导出：Task 10, Task 14, Task 15。
- 附件压缩包导出：Task 11, Task 14, Task 15。
- 本地文件存储：Task 8。
- 异常与权限约束：Task 3, Task 4, Task 5, Task 7, Task 8, Task 9。
- 第一期验收标准：Task 15。
- 第一阶段不做项：README in Task 15 documents the boundary.

## Placeholder Scan Result

This plan intentionally contains no unresolved placeholders and no GitHub push step.
