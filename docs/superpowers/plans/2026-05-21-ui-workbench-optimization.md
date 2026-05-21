# UI Workbench Optimization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade the existing reimbursement UI from basic CRUD screens into a modern enterprise SaaS material workbench with row-click drawers and full-screen attachment preview.

**Architecture:** Keep Vue 3 + Element Plus as the UI foundation, but introduce focused reusable components: shell, status tags, metric cards, material completeness, record drawer, material list, and material previewer. Add attachment metadata to reimbursement record API responses so the workbench can render material state without extra per-row API calls. Preserve the current backend business rules and local file storage.

**Tech Stack:** Java 21, Spring Boot 3, Spring Data JPA, Maven, Vue 3, TypeScript, Vite, Element Plus, Pinia, Axios, Vitest, Vue Test Utils.

---

## Boundaries

Implement only the UI/UX workbench optimization from `docs/superpowers/specs/2026-05-21-ui-workbench-design.md`.

Do not implement approval flow, payment management, OCR, external finance system integration, mobile app, complex BI charts, or custom logo design.

Do not push to GitHub.

---

## File Structure

```text
F:\Code\报销\
  backend/
    src/main/java/com/company/reimbursement/
      attachment/
        AttachmentController.java                     # switch upload response to shared attachment DTO
      reimbursement/
        ReimbursementDtos.java                        # add AttachmentResponse and attachments list on RecordResponse
        ReimbursementService.java                     # populate attachment metadata in all record responses
    src/test/java/com/company/reimbursement/
      reimbursement/
        ReimbursementAttachmentMetadataTest.java       # verifies list/get responses include attachments
  frontend/
    src/
      styles/
        tokens.css                                     # semantic SaaS design tokens and base classes
      layouts/
        AppShell.vue                                   # sidebar + topbar + content layout
      components/
        StatusTag.vue                                  # unified DRAFT/SUBMITTED/ARCHIVED tags
        MetricCard.vue                                 # dashboard metric card
        MaterialCompleteness.vue                       # material count/missing status display
        EmptyState.vue                                 # consistent empty state
        ConfirmAction.vue                              # confirm wrapper for destructive actions
        MaterialList.vue                               # grouped attachments with upload/delete/preview/download
        MaterialPreviewer.vue                          # full-screen image/PDF/material preview overlay
        RecordDrawer.vue                               # right drawer for summary, edit form, materials, actions
        WorkbenchFilters.vue                           # status/category/date/keyword/employee filters
        WorkbenchRecordTable.vue                       # enterprise record list table with row-click drawer behavior
      api/
        reimbursements.ts                              # add attachment DTOs, query params, blob download helper reuse
      views/
        employee/ReimbursementListView.vue             # employee workbench page
        admin/ReimbursementAdminView.vue               # admin workbench page
        admin/BatchAdminView.vue                       # visual consistency pass for batch page
      App.vue                                          # mount AppShell
    tests/
      backend-contract.spec.ts                         # frontend type/API contract expectations
      app-shell.spec.ts                                # role-aware shell and nav
      workbench.spec.ts                                # metrics, filters, row click drawer
      record-drawer.spec.ts                            # drawer editing/status/material action behavior
      material-previewer.spec.ts                       # full-screen preview behavior
      admin-batch.spec.ts                              # update existing assertions for visual/action feedback
```

---

## Task 1: Backend attachment metadata in reimbursement responses

**Files:**
- Modify: `backend/src/main/java/com/company/reimbursement/reimbursement/ReimbursementDtos.java`
- Modify: `backend/src/main/java/com/company/reimbursement/reimbursement/ReimbursementService.java`
- Modify: `backend/src/main/java/com/company/reimbursement/attachment/AttachmentController.java`
- Create: `backend/src/test/java/com/company/reimbursement/reimbursement/ReimbursementAttachmentMetadataTest.java`

- [ ] **Step 1: Write failing backend test for employee list attachment metadata**

Create `backend/src/test/java/com/company/reimbursement/reimbursement/ReimbursementAttachmentMetadataTest.java`:

```java
package com.company.reimbursement.reimbursement;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.reimbursement.attachment.AttachmentType;
import com.company.reimbursement.attachment.ReimbursementAttachment;
import com.company.reimbursement.attachment.ReimbursementAttachmentRepository;
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
        "spring.datasource.url=jdbc:h2:mem:record_attachments;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class ReimbursementAttachmentMetadataTest {
    @Autowired UserRepository users;
    @Autowired ExpenseCategoryRepository categories;
    @Autowired ReimbursementRepository records;
    @Autowired ReimbursementAttachmentRepository attachments;
    @Autowired ReimbursementService service;
    @Autowired PasswordEncoder passwordEncoder;

    private ReimbursementRecord record;

    @BeforeEach
    void setUp() {
        attachments.deleteAll();
        records.deleteAll();
        categories.deleteAll();
        users.deleteAll();
        User employee = users.save(User.create("employee", "员工一", "研发部", passwordEncoder.encode("secret123"), UserRole.EMPLOYEE));
        ExpenseCategory category = categories.save(ExpenseCategory.create("差旅费", true, 1, null));
        record = records.save(ReimbursementRecord.createDraft(employee, new BigDecimal("123.45"), category, "客户拜访", Instant.parse("2026-05-21T02:30:00Z")));
        attachments.save(ReimbursementAttachment.create(record, AttachmentType.PAYMENT_VOUCHER, "pay.png", "1/payment_voucher/pay.png", "image/png", 11));
        attachments.save(ReimbursementAttachment.create(record, AttachmentType.INVOICE, "invoice.pdf", "1/invoice/invoice.pdf", "application/pdf", 22));
    }

    @Test
    void listMineIncludesAttachmentMetadata() {
        ReimbursementDtos.RecordResponse response = service.listMine("employee").getFirst();

        assertThat(response.attachments()).hasSize(2);
        assertThat(response.attachments()).extracting(ReimbursementDtos.AttachmentResponse::type)
                .containsExactlyInAnyOrder(AttachmentType.PAYMENT_VOUCHER, AttachmentType.INVOICE);
        assertThat(response.attachments()).extracting(ReimbursementDtos.AttachmentResponse::originalFilename)
                .containsExactlyInAnyOrder("pay.png", "invoice.pdf");
    }
}
```

- [ ] **Step 2: Run backend test to verify it fails**

Run:

```bash
JAVA_HOME="D:\software\jdk-21" "/d/software/apache-maven-3.9.11/bin/mvn.cmd" -q -pl backend test -Dtest=ReimbursementAttachmentMetadataTest
```

Expected: FAIL because `RecordResponse` has no `attachments()` accessor.

- [ ] **Step 3: Add shared attachment DTO to reimbursement DTOs**

Modify `backend/src/main/java/com/company/reimbursement/reimbursement/ReimbursementDtos.java` so it includes attachment metadata:

```java
package com.company.reimbursement.reimbursement;

import com.company.reimbursement.attachment.AttachmentType;
import com.company.reimbursement.attachment.ReimbursementAttachment;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class ReimbursementDtos {
    public record SaveRecordRequest(BigDecimal amount, Long categoryId, String purpose, Instant paymentTime) {
    }

    public record AdminRemarkRequest(String adminRemark) {
    }

    public record AdminListFilter(Long employeeId, Long categoryId, ReimbursementStatus status, LocalDate from, LocalDate to) {
    }

    public record AttachmentResponse(Long id, AttachmentType type, String originalFilename, String contentType, long sizeBytes, Instant createdAt) {
        public static AttachmentResponse from(ReimbursementAttachment attachment) {
            return new AttachmentResponse(
                    attachment.getId(),
                    attachment.getType(),
                    attachment.getOriginalFilename(),
                    attachment.getContentType(),
                    attachment.getSizeBytes(),
                    attachment.getCreatedAt()
            );
        }
    }

    public record RecordResponse(
            Long id,
            Long employeeId,
            String employeeName,
            BigDecimal amount,
            Long categoryId,
            String categoryName,
            String purpose,
            Instant paymentTime,
            ReimbursementStatus status,
            String adminRemark,
            Instant submittedAt,
            Instant archivedAt,
            List<AttachmentResponse> attachments
    ) {
        public static RecordResponse from(ReimbursementRecord record, List<ReimbursementAttachment> attachments) {
            return new RecordResponse(
                    record.getId(),
                    record.getEmployee().getId(),
                    record.getEmployee().getDisplayName(),
                    record.getAmount(),
                    record.getCategory().getId(),
                    record.getCategory().getName(),
                    record.getPurpose(),
                    record.getPaymentTime(),
                    record.getStatus(),
                    record.getAdminRemark(),
                    record.getSubmittedAt(),
                    record.getArchivedAt(),
                    attachments.stream().map(AttachmentResponse::from).toList()
            );
        }
    }
}
```

- [ ] **Step 4: Populate attachments in reimbursement service responses**

Modify `backend/src/main/java/com/company/reimbursement/reimbursement/ReimbursementService.java` to use a helper:

```java
private ReimbursementDtos.RecordResponse response(ReimbursementRecord record) {
    return ReimbursementDtos.RecordResponse.from(record, attachments.findByRecord(record));
}
```

Replace every `ReimbursementDtos.RecordResponse.from(record)` call with `response(record)`. Replace stream mappings with `.map(this::response)`.

- [ ] **Step 5: Use shared attachment DTO in attachment controller**

Modify `backend/src/main/java/com/company/reimbursement/attachment/AttachmentController.java`:

```java
import com.company.reimbursement.reimbursement.ReimbursementDtos;
```

Change upload signature and return:

```java
ReimbursementDtos.AttachmentResponse upload(@PathVariable Long id, @RequestParam AttachmentType type, @RequestParam MultipartFile file, Authentication authentication) {
    User user = findUser(authentication.getName());
    ReimbursementRecord record = records.findById(id).orElseThrow(() -> new EntityNotFoundException("报销记录不存在"));
    ensureOwner(record, user);
    if (record.getStatus() != ReimbursementStatus.DRAFT) {
        throw new IllegalArgumentException("只能给草稿记录上传附件");
    }
    FileStorageService.StoredFile stored = storage.save(record.getId(), type, file);
    ReimbursementAttachment attachment = attachments.save(ReimbursementAttachment.create(
            record, type, stored.originalFilename(), stored.storagePath(), stored.contentType(), stored.sizeBytes()
    ));
    return ReimbursementDtos.AttachmentResponse.from(attachment);
}
```

Remove the local `record AttachmentResponse` from the bottom of `AttachmentController`.

- [ ] **Step 6: Run backend tests**

Run:

```bash
JAVA_HOME="D:\software\jdk-21" "/d/software/apache-maven-3.9.11/bin/mvn.cmd" -q -pl backend test -Dtest=ReimbursementAttachmentMetadataTest,AttachmentControllerTest,ReimbursementServiceTest,AdminReimbursementControllerTest
```

Expected: PASS.

- [ ] **Step 7: Commit backend attachment metadata**

Run:

```bash
git add backend/src/main/java/com/company/reimbursement/reimbursement/ReimbursementDtos.java backend/src/main/java/com/company/reimbursement/reimbursement/ReimbursementService.java backend/src/main/java/com/company/reimbursement/attachment/AttachmentController.java backend/src/test/java/com/company/reimbursement/reimbursement/ReimbursementAttachmentMetadataTest.java
git commit -m "feat: expose reimbursement attachment metadata"
```

---

## Task 2: Design tokens and enterprise app shell

**Files:**
- Create: `frontend/src/styles/tokens.css`
- Create: `frontend/src/layouts/AppShell.vue`
- Modify: `frontend/src/main.ts`
- Modify: `frontend/src/App.vue`
- Modify: `frontend/src/router.ts`
- Create: `frontend/tests/app-shell.spec.ts`

- [ ] **Step 1: Write failing shell tests**

Create `frontend/tests/app-shell.spec.ts`:

```ts
// @vitest-environment jsdom
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { flushPromises, mount } from '@vue/test-utils';
import { createMemoryHistory, createRouter } from 'vue-router';
import { createPinia, setActivePinia } from 'pinia';
import AppShell from '../src/layouts/AppShell.vue';
import { useAuthStore } from '../src/stores/auth';

const routes = [
  { path: '/reimbursements', component: { template: '<div>我的报销页</div>' }, meta: { title: '我的报销', description: '提交和管理自己的报销材料' } },
  { path: '/admin/reimbursements', component: { template: '<div>报销工作台页</div>' }, meta: { title: '报销工作台', description: '集中处理员工提交的材料' } }
];

describe('AppShell', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  it('shows employee navigation without admin-only destinations', async () => {
    const router = createRouter({ history: createMemoryHistory(), routes });
    await router.push('/reimbursements');
    const auth = useAuthStore();
    auth.user = { id: 1, username: 'employee', displayName: '员工一', department: '研发部', role: 'EMPLOYEE' };

    const wrapper = mount(AppShell, { global: { plugins: [router] } });
    await flushPromises();

    expect(wrapper.text()).toContain('我的报销');
    expect(wrapper.text()).not.toContain('报销工作台');
    expect(wrapper.text()).toContain('员工一');
    expect(wrapper.text()).toContain('EMPLOYEE');
  });

  it('shows admin navigation and current page description', async () => {
    const router = createRouter({ history: createMemoryHistory(), routes });
    await router.push('/admin/reimbursements');
    const auth = useAuthStore();
    auth.user = { id: 2, username: 'admin', displayName: '系统管理员', department: '财务部', role: 'ADMIN' };

    const wrapper = mount(AppShell, { global: { plugins: [router] } });
    await flushPromises();

    expect(wrapper.text()).toContain('报销工作台');
    expect(wrapper.text()).toContain('集中处理员工提交的材料');
    expect(wrapper.text()).toContain('批次管理');
    expect(wrapper.text()).toContain('系统管理员');
    expect(wrapper.text()).toContain('ADMIN');
  });
});
```

- [ ] **Step 2: Run shell test to verify it fails**

Run:

```bash
npm test --prefix "F:/Code/报销/frontend" -- app-shell.spec.ts
```

Expected: FAIL because `frontend/src/layouts/AppShell.vue` does not exist.

- [ ] **Step 3: Add design tokens**

Create `frontend/src/styles/tokens.css`:

```css
:root {
  --color-primary: #1d4ed8;
  --color-primary-soft: #dbeafe;
  --color-success: #15803d;
  --color-success-soft: #dcfce7;
  --color-warning: #b45309;
  --color-warning-soft: #fef3c7;
  --color-danger: #b91c1c;
  --color-danger-soft: #fee2e2;
  --color-background: #f4f7fb;
  --color-surface: #ffffff;
  --color-surface-muted: #f8fafc;
  --color-border: #d8e0ea;
  --color-text-primary: #0f172a;
  --color-text-secondary: #64748b;
  --shadow-card: 0 10px 30px rgba(15, 23, 42, 0.06);
  --radius-lg: 16px;
  --radius-md: 10px;
  --space-1: 4px;
  --space-2: 8px;
  --space-3: 12px;
  --space-4: 16px;
  --space-5: 24px;
  --space-6: 32px;
}

* { box-sizing: border-box; }
body {
  margin: 0;
  min-width: 320px;
  color: var(--color-text-primary);
  background: var(--color-background);
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
}
button, input, select, textarea { font: inherit; }
button { cursor: pointer; }
button:disabled { cursor: not-allowed; opacity: 0.6; }
a { color: inherit; }
.enterprise-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}
```

- [ ] **Step 4: Import tokens in main**

Modify `frontend/src/main.ts`:

```ts
import { createApp } from 'vue';
import { createPinia } from 'pinia';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import './styles/tokens.css';
import App from './App.vue';
import router from './router';

createApp(App).use(createPinia()).use(router).use(ElementPlus).mount('#app');
```

- [ ] **Step 5: Create AppShell layout**

Create `frontend/src/layouts/AppShell.vue`:

```vue
<script setup lang="ts">
import { computed } from 'vue';
import { RouterLink, RouterView, useRoute } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const route = useRoute();
const auth = useAuthStore();

const navItems = computed(() => {
  const base = [{ to: '/reimbursements', label: '我的报销', icon: 'M4 6h16M4 12h16M4 18h10' }];
  if (auth.user?.role === 'ADMIN') {
    base.push(
      { to: '/admin/reimbursements', label: '报销工作台', icon: 'M4 5h16v14H4z' },
      { to: '/admin/batches', label: '批次管理', icon: 'M5 7h14M5 12h14M5 17h14' },
      { to: '/admin/users', label: '用户管理', icon: 'M12 12a4 4 0 100-8 4 4 0 000 8z' },
      { to: '/admin/categories', label: '分类管理', icon: 'M6 6h12v12H6z' }
    );
  }
  return base;
});

const pageTitle = computed(() => String(route.meta.title ?? '报销材料管理系统'));
const pageDescription = computed(() => String(route.meta.description ?? '企业级报销材料工作台'));
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar" aria-label="主导航">
      <div class="brand">
        <div class="brand-mark">R</div>
        <div>
          <strong>报销材料</strong>
          <span>Workbench</span>
        </div>
      </div>
      <nav class="nav-list">
        <RouterLink v-for="item in navItems" :key="item.to" :to="item.to" class="nav-item" active-class="active">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path :d="item.icon" /></svg>
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>
    </aside>
    <div class="shell-body">
      <header class="topbar">
        <div>
          <h1>{{ pageTitle }}</h1>
          <p>{{ pageDescription }}</p>
        </div>
        <div v-if="auth.user" class="user-pill">
          <span>{{ auth.user.displayName }}</span>
          <strong>{{ auth.user.role }}</strong>
        </div>
      </header>
      <main class="content" tabindex="-1">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.app-shell { min-height: 100dvh; display: flex; background: var(--color-background); }
.sidebar { width: 240px; padding: var(--space-5) var(--space-4); background: #0f172a; color: #e2e8f0; }
.brand { display: flex; align-items: center; gap: var(--space-3); margin-bottom: var(--space-6); }
.brand-mark { width: 40px; height: 40px; border-radius: 12px; display: grid; place-items: center; background: var(--color-primary); color: white; font-weight: 700; }
.brand span { display: block; color: #94a3b8; font-size: 12px; margin-top: 2px; }
.nav-list { display: grid; gap: var(--space-2); }
.nav-item { display: flex; align-items: center; gap: var(--space-3); min-height: 44px; padding: 0 var(--space-3); border-radius: var(--radius-md); color: #cbd5e1; text-decoration: none; }
.nav-item svg { width: 20px; height: 20px; fill: none; stroke: currentColor; stroke-width: 2; stroke-linecap: round; stroke-linejoin: round; }
.nav-item:hover, .nav-item:focus-visible { background: rgba(255,255,255,0.08); outline: none; }
.nav-item.active { background: var(--color-primary); color: white; }
.shell-body { flex: 1; min-width: 0; }
.topbar { height: 80px; display: flex; align-items: center; justify-content: space-between; padding: 0 var(--space-5); background: rgba(255,255,255,0.8); border-bottom: 1px solid var(--color-border); backdrop-filter: blur(12px); }
.topbar h1 { margin: 0; font-size: 24px; font-weight: 600; }
.topbar p { margin: 4px 0 0; color: var(--color-text-secondary); font-size: 13px; }
.user-pill { display: flex; align-items: center; gap: var(--space-2); padding: var(--space-2) var(--space-3); border: 1px solid var(--color-border); border-radius: 999px; background: var(--color-surface); }
.user-pill strong { color: var(--color-primary); font-size: 12px; }
.content { padding: var(--space-5); }
@media (max-width: 767px) {
  .app-shell { display: block; }
  .sidebar { width: 100%; padding: var(--space-3); }
  .nav-list { display: flex; overflow-x: auto; }
  .topbar { height: auto; padding: var(--space-4); align-items: flex-start; gap: var(--space-3); }
  .content { padding: var(--space-4); }
}
</style>
```

- [ ] **Step 6: Mount AppShell from App**

Modify `frontend/src/App.vue`:

```vue
<script setup lang="ts">
import AppShell from './layouts/AppShell.vue';
</script>

<template>
  <AppShell />
</template>
```

- [ ] **Step 7: Add route titles**

Modify `frontend/src/router.ts` route definitions so each route has meta:

```ts
{ path: '/reimbursements', component: ReimbursementListView, meta: { title: '我的报销', description: '提交和查看自己的报销材料' } },
{ path: '/reimbursements/new', component: ReimbursementEditView, meta: { title: '新建报销', description: '创建草稿并上传材料' } },
{ path: '/reimbursements/:id', component: ReimbursementEditView, meta: { title: '编辑报销', description: '维护草稿材料' } },
{ path: '/admin/users', component: UserAdminView, meta: { title: '用户管理', description: '维护员工和管理员账号', requiresAdmin: true } },
{ path: '/admin/categories', component: CategoryAdminView, meta: { title: '分类管理', description: '维护报销用途分类', requiresAdmin: true } },
{ path: '/admin/reimbursements', component: ReimbursementAdminView, meta: { title: '报销工作台', description: '集中处理员工提交的材料', requiresAdmin: true } },
{ path: '/admin/batches', component: BatchAdminView, meta: { title: '批次管理', description: '整理、导出和归档材料', requiresAdmin: true } }
```

- [ ] **Step 8: Run shell test and full frontend tests**

Run:

```bash
npm test --prefix "F:/Code/报销/frontend" -- app-shell.spec.ts
npm test --prefix "F:/Code/报销/frontend"
```

Expected: PASS.

- [ ] **Step 9: Commit shell and tokens**

Run:

```bash
git add frontend/src/styles/tokens.css frontend/src/layouts/AppShell.vue frontend/src/main.ts frontend/src/App.vue frontend/src/router.ts frontend/tests/app-shell.spec.ts
git commit -m "feat: add enterprise app shell"
```

---

## Task 3: Shared UI primitives

**Files:**
- Create: `frontend/src/components/StatusTag.vue`
- Create: `frontend/src/components/MetricCard.vue`
- Create: `frontend/src/components/MaterialCompleteness.vue`
- Create: `frontend/src/components/EmptyState.vue`
- Create: `frontend/src/components/ConfirmAction.vue`
- Create: `frontend/tests/ui-primitives.spec.ts`

- [ ] **Step 1: Write failing primitive tests**

Create `frontend/tests/ui-primitives.spec.ts`:

```ts
// @vitest-environment jsdom
import { describe, expect, it, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import StatusTag from '../src/components/StatusTag.vue';
import MetricCard from '../src/components/MetricCard.vue';
import MaterialCompleteness from '../src/components/MaterialCompleteness.vue';
import EmptyState from '../src/components/EmptyState.vue';
import ConfirmAction from '../src/components/ConfirmAction.vue';

describe('UI primitives', () => {
  it('renders status labels with semantic class', () => {
    const wrapper = mount(StatusTag, { props: { status: 'SUBMITTED' } });
    expect(wrapper.text()).toBe('已提交');
    expect(wrapper.classes()).toContain('status-submitted');
  });

  it('renders metric card title and value', () => {
    const wrapper = mount(MetricCard, { props: { title: '已提交', value: 8, tone: 'primary' } });
    expect(wrapper.text()).toContain('已提交');
    expect(wrapper.text()).toContain('8');
  });

  it('shows missing required payment voucher', () => {
    const wrapper = mount(MaterialCompleteness, { props: { paymentVoucherCount: 0, orderScreenshotCount: 1, invoiceCount: 0 } });
    expect(wrapper.text()).toContain('支付凭证缺失');
    expect(wrapper.classes()).toContain('is-incomplete');
  });

  it('renders empty state with action slot', () => {
    const wrapper = mount(EmptyState, { props: { title: '暂无记录', description: '创建第一条报销记录' }, slots: { action: '<button>新建报销</button>' } });
    expect(wrapper.text()).toContain('暂无记录');
    expect(wrapper.text()).toContain('新建报销');
  });

  it('confirms before emitting confirm action', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const wrapper = mount(ConfirmAction, { props: { message: '确认删除？' }, slots: { default: '<button>删除</button>' } });
    await wrapper.find('button').trigger('click');
    expect(wrapper.emitted('confirm')).toHaveLength(1);
  });
});
```

- [ ] **Step 2: Run primitive test to verify it fails**

Run:

```bash
npm test --prefix "F:/Code/报销/frontend" -- ui-primitives.spec.ts
```

Expected: FAIL because the new primitive components do not exist.

- [ ] **Step 3: Create StatusTag**

Create `frontend/src/components/StatusTag.vue`:

```vue
<script setup lang="ts">
import type { ReimbursementStatus } from '../api/reimbursements';

const props = defineProps<{ status: ReimbursementStatus }>();

const labels: Record<ReimbursementStatus, string> = {
  DRAFT: '草稿',
  SUBMITTED: '已提交',
  ARCHIVED: '已归档'
};
</script>

<template>
  <span class="status-tag" :class="`status-${props.status.toLowerCase()}`">{{ labels[props.status] }}</span>
</template>

<style scoped>
.status-tag { display: inline-flex; align-items: center; min-height: 24px; padding: 0 10px; border-radius: 999px; font-size: 12px; font-weight: 600; }
.status-draft { color: var(--color-warning); background: var(--color-warning-soft); }
.status-submitted { color: var(--color-primary); background: var(--color-primary-soft); }
.status-archived { color: var(--color-success); background: var(--color-success-soft); }
</style>
```

- [ ] **Step 4: Create MetricCard**

Create `frontend/src/components/MetricCard.vue`:

```vue
<script setup lang="ts">
defineProps<{ title: string; value: number | string; tone?: 'primary' | 'success' | 'warning' | 'danger' }>();
</script>

<template>
  <article class="metric-card enterprise-card" :class="tone ?? 'primary'">
    <span>{{ title }}</span>
    <strong>{{ value }}</strong>
  </article>
</template>

<style scoped>
.metric-card { padding: var(--space-4); display: grid; gap: var(--space-2); }
.metric-card span { color: var(--color-text-secondary); font-size: 13px; }
.metric-card strong { font-size: 28px; font-variant-numeric: tabular-nums; }
.primary strong { color: var(--color-primary); }
.success strong { color: var(--color-success); }
.warning strong { color: var(--color-warning); }
.danger strong { color: var(--color-danger); }
</style>
```

- [ ] **Step 5: Create MaterialCompleteness**

Create `frontend/src/components/MaterialCompleteness.vue`:

```vue
<script setup lang="ts">
const props = defineProps<{ paymentVoucherCount: number; orderScreenshotCount: number; invoiceCount: number }>();
</script>

<template>
  <div class="material-completeness" :class="{ 'is-incomplete': props.paymentVoucherCount === 0 }">
    <span v-if="props.paymentVoucherCount === 0" class="missing">支付凭证缺失</span>
    <span v-else>支付凭证 {{ props.paymentVoucherCount }}</span>
    <span>订单截图 {{ props.orderScreenshotCount }}</span>
    <span>发票 {{ props.invoiceCount }}</span>
  </div>
</template>

<style scoped>
.material-completeness { display: flex; flex-wrap: wrap; gap: var(--space-2); color: var(--color-text-secondary); font-size: 12px; }
.material-completeness span { padding: 3px 8px; border: 1px solid var(--color-border); border-radius: 999px; background: var(--color-surface-muted); }
.material-completeness .missing { color: var(--color-danger); border-color: var(--color-danger-soft); background: var(--color-danger-soft); }
.is-incomplete { color: var(--color-danger); }
</style>
```

- [ ] **Step 6: Create EmptyState**

Create `frontend/src/components/EmptyState.vue`:

```vue
<script setup lang="ts">
defineProps<{ title: string; description: string }>();
</script>

<template>
  <section class="empty-state enterprise-card">
    <div class="empty-icon" aria-hidden="true">∅</div>
    <h2>{{ title }}</h2>
    <p>{{ description }}</p>
    <div class="empty-action"><slot name="action" /></div>
  </section>
</template>

<style scoped>
.empty-state { min-height: 220px; display: grid; place-items: center; align-content: center; gap: var(--space-3); text-align: center; color: var(--color-text-secondary); }
.empty-icon { width: 48px; height: 48px; display: grid; place-items: center; border-radius: 50%; background: var(--color-primary-soft); color: var(--color-primary); font-size: 24px; }
h2 { margin: 0; color: var(--color-text-primary); font-size: 18px; }
p { margin: 0; }
.empty-action { margin-top: var(--space-2); }
</style>
```

- [ ] **Step 7: Create ConfirmAction**

Create `frontend/src/components/ConfirmAction.vue`:

```vue
<script setup lang="ts">
const props = defineProps<{ message: string }>();
const emit = defineEmits<{ confirm: [] }>();

function onClick() {
  if (window.confirm(props.message)) {
    emit('confirm');
  }
}
</script>

<template>
  <span class="confirm-action" @click="onClick"><slot /></span>
</template>
```

- [ ] **Step 8: Run primitive tests**

Run:

```bash
npm test --prefix "F:/Code/报销/frontend" -- ui-primitives.spec.ts
```

Expected: PASS.

- [ ] **Step 9: Commit primitives**

Run:

```bash
git add frontend/src/components/StatusTag.vue frontend/src/components/MetricCard.vue frontend/src/components/MaterialCompleteness.vue frontend/src/components/EmptyState.vue frontend/src/components/ConfirmAction.vue frontend/tests/ui-primitives.spec.ts
git commit -m "feat: add workbench UI primitives"
```

---

## Task 4: Workbench record list and metrics

**Files:**
- Create: `frontend/src/components/WorkbenchFilters.vue`
- Create: `frontend/src/components/WorkbenchRecordTable.vue`
- Modify: `frontend/src/views/employee/ReimbursementListView.vue`
- Modify: `frontend/src/views/admin/ReimbursementAdminView.vue`
- Modify: `frontend/src/api/reimbursements.ts`
- Create: `frontend/tests/workbench.spec.ts`

- [ ] **Step 1: Write failing workbench tests**

Create `frontend/tests/workbench.spec.ts`:

```ts
// @vitest-environment jsdom
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { flushPromises, mount } from '@vue/test-utils';
import EmployeeWorkbench from '../src/views/employee/ReimbursementListView.vue';
import AdminWorkbench from '../src/views/admin/ReimbursementAdminView.vue';
import http from '../src/api/http';

vi.mock('../src/api/http', () => ({
  default: { get: vi.fn(), post: vi.fn(), patch: vi.fn(), delete: vi.fn() }
}));

const records = [
  { id: 1, employeeId: 2, employeeName: '员工一', amount: 123.45, categoryId: 1, categoryName: '差旅费', purpose: '客户拜访', paymentTime: '2026-05-21T02:30:00Z', status: 'DRAFT', adminRemark: null, submittedAt: null, archivedAt: null, attachments: [] },
  { id: 2, employeeId: 2, employeeName: '员工一', amount: 88, categoryId: 1, categoryName: '差旅费', purpose: '午餐', paymentTime: '2026-05-20T02:30:00Z', status: 'SUBMITTED', adminRemark: null, submittedAt: '2026-05-20T03:00:00Z', archivedAt: null, attachments: [{ id: 8, type: 'PAYMENT_VOUCHER', originalFilename: 'pay.png', contentType: 'image/png', sizeBytes: 11, createdAt: '2026-05-20T03:00:00Z' }] }
];

describe('workbench pages', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(http.get).mockResolvedValue({ data: records });
  });

  it('renders employee metrics and opens drawer by row click', async () => {
    const wrapper = mount(EmployeeWorkbench, { global: { stubs: ['RouterLink'] } });
    await flushPromises();

    expect(wrapper.text()).toContain('草稿');
    expect(wrapper.text()).toContain('已提交');
    expect(wrapper.text()).toContain('材料不完整');
    await wrapper.find('[data-test="record-row-1"]').trigger('click');
    expect(wrapper.text()).toContain('记录详情');
    expect(wrapper.text()).toContain('客户拜访');
  });

  it('passes admin filters and opens drawer by row click', async () => {
    const wrapper = mount(AdminWorkbench);
    await flushPromises();
    await wrapper.find('[aria-label="员工ID"]').setValue('2');
    await wrapper.find('[aria-label="状态"]').setValue('SUBMITTED');
    await wrapper.find('[data-test="apply-filters"]').trigger('click');

    expect(http.get).toHaveBeenLastCalledWith('/admin/reimbursements', { params: expect.objectContaining({ employeeId: 2, status: 'SUBMITTED' }) });
    await wrapper.find('[data-test="record-row-2"]').trigger('click');
    expect(wrapper.text()).toContain('记录详情');
  });
});
```

- [ ] **Step 2: Run workbench test to verify it fails**

Run:

```bash
npm test --prefix "F:/Code/报销/frontend" -- workbench.spec.ts
```

Expected: FAIL because workbench components and drawer behavior do not exist.

- [ ] **Step 3: Update frontend reimbursement API types**

Modify `frontend/src/api/reimbursements.ts`:

```ts
import http from './http';

export type ReimbursementStatus = 'DRAFT' | 'SUBMITTED' | 'ARCHIVED';
export type AttachmentType = 'PAYMENT_VOUCHER' | 'ORDER_SCREENSHOT' | 'INVOICE';

export interface AttachmentRecord {
  id: number;
  type: AttachmentType;
  originalFilename: string;
  contentType: string;
  sizeBytes: number;
  createdAt: string;
}

export interface ReimbursementInput {
  amount: number;
  categoryId: number;
  purpose: string;
  paymentTime: string;
}

export interface ReimbursementRecord extends ReimbursementInput {
  id: number;
  employeeId: number;
  employeeName: string;
  categoryName: string;
  status: ReimbursementStatus;
  adminRemark: string | null;
  submittedAt: string | null;
  archivedAt: string | null;
  attachments: AttachmentRecord[];
}

export interface EmployeeReimbursementFilters {
  status?: ReimbursementStatus;
  categoryId?: number;
  from?: string;
  to?: string;
  keyword?: string;
}

export interface AdminReimbursementFilters extends EmployeeReimbursementFilters {
  employeeId?: number;
}

export function listReimbursements(params: EmployeeReimbursementFilters = {}) {
  return http.get<ReimbursementRecord[]>('/reimbursements', { params });
}

export function createReimbursement(payload: ReimbursementInput) {
  return http.post<ReimbursementRecord>('/reimbursements', payload);
}

export function getReimbursement(id: number) {
  return http.get<ReimbursementRecord>(`/reimbursements/${id}`);
}

export function updateReimbursement(id: number, payload: ReimbursementInput) {
  return http.patch<ReimbursementRecord>(`/reimbursements/${id}`, payload);
}

export function submitReimbursement(id: number) {
  return http.post<ReimbursementRecord>(`/reimbursements/${id}/submit`);
}

export function uploadAttachment(recordId: number, type: AttachmentType, file: File) {
  const form = new FormData();
  form.append('file', file);
  return http.post<AttachmentRecord>(`/reimbursements/${recordId}/attachments?type=${type}`, form);
}

export function deleteAttachment(attachmentId: number) {
  return http.delete(`/attachments/${attachmentId}`);
}

export function listAdminReimbursements(params: AdminReimbursementFilters = { status: 'SUBMITTED' }) {
  return http.get<ReimbursementRecord[]>('/admin/reimbursements', { params });
}

export function updateAdminRemark(id: number, adminRemark: string) {
  return http.patch<ReimbursementRecord>(`/admin/reimbursements/${id}/remark`, { adminRemark });
}
```

- [ ] **Step 4: Create WorkbenchFilters**

Create `frontend/src/components/WorkbenchFilters.vue`:

```vue
<script setup lang="ts">
import type { ReimbursementStatus } from '../api/reimbursements';

const props = defineProps<{ admin?: boolean }>();
const filters = defineModel<{ employeeId?: string; categoryId?: string; status?: ReimbursementStatus | ''; from?: string; to?: string; keyword?: string }>({ required: true });
const emit = defineEmits<{ apply: []; reset: [] }>();
</script>

<template>
  <form class="workbench-filters enterprise-card" @submit.prevent="emit('apply')">
    <input v-if="props.admin" aria-label="员工ID" v-model="filters.employeeId" type="number" min="1" placeholder="员工ID" />
    <input aria-label="分类ID" v-model="filters.categoryId" type="number" min="1" placeholder="分类ID" />
    <select aria-label="状态" v-model="filters.status">
      <option value="">全部状态</option>
      <option value="DRAFT">草稿</option>
      <option value="SUBMITTED">已提交</option>
      <option value="ARCHIVED">已归档</option>
    </select>
    <input aria-label="开始日期" v-model="filters.from" type="date" />
    <input aria-label="结束日期" v-model="filters.to" type="date" />
    <input aria-label="关键词" v-model="filters.keyword" placeholder="用途说明关键词" />
    <button data-test="apply-filters" type="submit">筛选</button>
    <button type="button" @click="emit('reset')">重置</button>
  </form>
</template>

<style scoped>
.workbench-filters { display: grid; grid-template-columns: repeat(6, minmax(120px, 1fr)) auto auto; gap: var(--space-3); padding: var(--space-4); margin-bottom: var(--space-4); }
input, select, button { min-height: 40px; border: 1px solid var(--color-border); border-radius: var(--radius-md); padding: 0 var(--space-3); background: white; }
button[type="submit"] { background: var(--color-primary); color: white; border-color: var(--color-primary); }
@media (max-width: 1023px) { .workbench-filters { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 767px) { .workbench-filters { grid-template-columns: 1fr; } }
</style>
```

- [ ] **Step 5: Create WorkbenchRecordTable**

Create `frontend/src/components/WorkbenchRecordTable.vue`:

```vue
<script setup lang="ts">
import type { ReimbursementRecord } from '../api/reimbursements';
import StatusTag from './StatusTag.vue';
import MaterialCompleteness from './MaterialCompleteness.vue';

const props = defineProps<{ records: ReimbursementRecord[]; admin?: boolean }>();
const emit = defineEmits<{ open: [ReimbursementRecord] }>();

function count(record: ReimbursementRecord, type: string) {
  return record.attachments.filter((attachment) => attachment.type === type).length;
}
</script>

<template>
  <div class="record-table enterprise-card">
    <table>
      <thead>
        <tr>
          <th v-if="props.admin">员工</th>
          <th>金额</th>
          <th>用途分类</th>
          <th>用途说明</th>
          <th>支付时间</th>
          <th>状态</th>
          <th>材料完整度</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="record in props.records" :key="record.id" :data-test="`record-row-${record.id}`" tabindex="0" @click="emit('open', record)" @keydown.enter="emit('open', record)">
          <td v-if="props.admin"><strong>{{ record.employeeName }}</strong></td>
          <td class="amount">¥{{ Number(record.amount).toFixed(2) }}</td>
          <td>{{ record.categoryName }}</td>
          <td>{{ record.purpose }}</td>
          <td>{{ record.paymentTime }}</td>
          <td><StatusTag :status="record.status" /></td>
          <td><MaterialCompleteness :payment-voucher-count="count(record, 'PAYMENT_VOUCHER')" :order-screenshot-count="count(record, 'ORDER_SCREENSHOT')" :invoice-count="count(record, 'INVOICE')" /></td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.record-table { overflow: hidden; }
table { width: 100%; border-collapse: collapse; }
th { text-align: left; padding: var(--space-3) var(--space-4); color: var(--color-text-secondary); font-size: 12px; background: var(--color-surface-muted); }
td { min-height: 52px; padding: var(--space-3) var(--space-4); border-top: 1px solid var(--color-border); vertical-align: middle; }
tr { cursor: pointer; }
tbody tr:hover, tbody tr:focus-visible { background: #f8fbff; outline: none; }
.amount { font-variant-numeric: tabular-nums; font-weight: 700; color: var(--color-text-primary); }
@media (max-width: 767px) { th:nth-child(4), td:nth-child(4), th:nth-child(5), td:nth-child(5) { display: none; } }
</style>
```

- [ ] **Step 6: Add workbench page logic to employee view**

Modify `frontend/src/views/employee/ReimbursementListView.vue` to use metrics, filters, table, and drawer stub:

```vue
<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { listReimbursements, type ReimbursementRecord, type ReimbursementStatus } from '../../api/reimbursements';
import MetricCard from '../../components/MetricCard.vue';
import WorkbenchFilters from '../../components/WorkbenchFilters.vue';
import WorkbenchRecordTable from '../../components/WorkbenchRecordTable.vue';
import EmptyState from '../../components/EmptyState.vue';

const records = ref<ReimbursementRecord[]>([]);
const selected = ref<ReimbursementRecord | null>(null);
const filters = reactive<{ status: ReimbursementStatus | ''; categoryId: string; from: string; to: string; keyword: string }>({ status: '', categoryId: '', from: '', to: '', keyword: '' });

const metrics = computed(() => ({
  draft: records.value.filter((record) => record.status === 'DRAFT').length,
  submitted: records.value.filter((record) => record.status === 'SUBMITTED').length,
  archived: records.value.filter((record) => record.status === 'ARCHIVED').length,
  incomplete: records.value.filter((record) => !record.attachments.some((attachment) => attachment.type === 'PAYMENT_VOUCHER')).length
}));

function params() {
  return { status: filters.status || undefined, categoryId: filters.categoryId ? Number(filters.categoryId) : undefined, from: filters.from || undefined, to: filters.to || undefined, keyword: filters.keyword || undefined };
}

async function load() {
  const response = await listReimbursements(params());
  records.value = response.data;
}

function reset() {
  Object.assign(filters, { status: '', categoryId: '', from: '', to: '', keyword: '' });
  load();
}

onMounted(load);
</script>

<template>
  <section class="workbench-page">
    <div class="page-actions"><RouterLink class="primary-link" to="/reimbursements/new">新建报销</RouterLink></div>
    <div class="metrics-grid">
      <MetricCard title="草稿" :value="metrics.draft" tone="warning" />
      <MetricCard title="已提交" :value="metrics.submitted" tone="primary" />
      <MetricCard title="已归档" :value="metrics.archived" tone="success" />
      <MetricCard title="材料不完整" :value="metrics.incomplete" tone="danger" />
    </div>
    <WorkbenchFilters v-model="filters" @apply="load" @reset="reset" />
    <WorkbenchRecordTable v-if="records.length" :records="records" @open="selected = $event" />
    <EmptyState v-else title="暂无报销记录" description="创建第一条报销记录并上传支付凭证">
      <template #action><RouterLink class="primary-link" to="/reimbursements/new">新建报销</RouterLink></template>
    </EmptyState>
    <aside v-if="selected" class="drawer-stub" role="dialog" aria-label="记录详情">
      <button @click="selected = null">关闭</button>
      <h2>记录详情</h2>
      <p>{{ selected.purpose }}</p>
    </aside>
  </section>
</template>

<style scoped>
.workbench-page { display: grid; gap: var(--space-4); }
.page-actions { display: flex; justify-content: flex-end; }
.primary-link { min-height: 40px; display: inline-flex; align-items: center; padding: 0 var(--space-4); border-radius: var(--radius-md); color: white; background: var(--color-primary); text-decoration: none; }
.metrics-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: var(--space-4); }
.drawer-stub { position: fixed; top: 0; right: 0; width: min(640px, 100vw); height: 100dvh; padding: var(--space-5); background: white; box-shadow: -20px 0 60px rgba(15, 23, 42, 0.16); z-index: 40; }
@media (max-width: 1023px) { .metrics-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 767px) { .metrics-grid { grid-template-columns: 1fr; } }
</style>
```

- [ ] **Step 7: Add workbench page logic to admin view**

Modify `frontend/src/views/admin/ReimbursementAdminView.vue` similarly, using `listAdminReimbursements`, `admin` filters, and `WorkbenchRecordTable admin`. Keep `saveRemark` function for later drawer integration.

Use this script block:

```vue
<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { listAdminReimbursements, updateAdminRemark, type ReimbursementRecord, type ReimbursementStatus } from '../../api/reimbursements';
import MetricCard from '../../components/MetricCard.vue';
import WorkbenchFilters from '../../components/WorkbenchFilters.vue';
import WorkbenchRecordTable from '../../components/WorkbenchRecordTable.vue';
import EmptyState from '../../components/EmptyState.vue';

const records = ref<ReimbursementRecord[]>([]);
const selected = ref<ReimbursementRecord | null>(null);
const filters = reactive<{ employeeId: string; categoryId: string; status: ReimbursementStatus | ''; from: string; to: string; keyword: string }>({ employeeId: '', categoryId: '', status: 'SUBMITTED', from: '', to: '', keyword: '' });
const remarks = reactive<Record<number, string>>({});

const metrics = computed(() => ({
  draft: records.value.filter((record) => record.status === 'DRAFT').length,
  submitted: records.value.filter((record) => record.status === 'SUBMITTED').length,
  archived: records.value.filter((record) => record.status === 'ARCHIVED').length,
  incomplete: records.value.filter((record) => !record.attachments.some((attachment) => attachment.type === 'PAYMENT_VOUCHER')).length
}));

function params() {
  return { employeeId: filters.employeeId ? Number(filters.employeeId) : undefined, categoryId: filters.categoryId ? Number(filters.categoryId) : undefined, status: filters.status || undefined, from: filters.from || undefined, to: filters.to || undefined, keyword: filters.keyword || undefined };
}

async function load() {
  const response = await listAdminReimbursements(params());
  records.value = response.data;
  for (const record of records.value) remarks[record.id] = record.adminRemark ?? '';
}

async function saveRemark(id: number) {
  await updateAdminRemark(id, remarks[id] ?? '');
  await load();
}

function reset() {
  Object.assign(filters, { employeeId: '', categoryId: '', status: 'SUBMITTED', from: '', to: '', keyword: '' });
  load();
}

onMounted(load);
</script>
```

Use this template block:

```vue
<template>
  <section class="workbench-page">
    <div class="metrics-grid">
      <MetricCard title="草稿" :value="metrics.draft" tone="warning" />
      <MetricCard title="已提交" :value="metrics.submitted" tone="primary" />
      <MetricCard title="已归档" :value="metrics.archived" tone="success" />
      <MetricCard title="材料不完整" :value="metrics.incomplete" tone="danger" />
    </div>
    <WorkbenchFilters v-model="filters" admin @apply="load" @reset="reset" />
    <WorkbenchRecordTable v-if="records.length" :records="records" admin @open="selected = $event" />
    <EmptyState v-else title="暂无匹配记录" description="调整筛选条件后重新查询" />
    <aside v-if="selected" class="drawer-stub" role="dialog" aria-label="记录详情">
      <button @click="selected = null">关闭</button>
      <h2>记录详情</h2>
      <p>{{ selected.purpose }}</p>
      <label>管理员备注<input :aria-label="`备注${selected.id}`" v-model="remarks[selected.id]" /></label>
      <button @click="saveRemark(selected.id)">保存备注</button>
    </aside>
  </section>
</template>
```

Use this style block:

```vue
<style scoped>
.workbench-page { display: grid; gap: var(--space-4); }
.metrics-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: var(--space-4); }
.drawer-stub { position: fixed; top: 0; right: 0; width: min(640px, 100vw); height: 100dvh; padding: var(--space-5); background: white; box-shadow: -20px 0 60px rgba(15, 23, 42, 0.16); z-index: 40; }
.drawer-stub label { display: grid; gap: var(--space-2); margin: var(--space-4) 0; }
.drawer-stub input { min-height: 40px; border: 1px solid var(--color-border); border-radius: var(--radius-md); padding: 0 var(--space-3); }
@media (max-width: 1023px) { .metrics-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 767px) { .metrics-grid { grid-template-columns: 1fr; } }
</style>
```

- [ ] **Step 8: Run workbench tests**

Run:

```bash
npm test --prefix "F:/Code/报销/frontend" -- workbench.spec.ts
```

Expected: PASS.

- [ ] **Step 9: Commit workbench list and metrics**

Run:

```bash
git add frontend/src/api/reimbursements.ts frontend/src/components/WorkbenchFilters.vue frontend/src/components/WorkbenchRecordTable.vue frontend/src/views/employee/ReimbursementListView.vue frontend/src/views/admin/ReimbursementAdminView.vue frontend/tests/workbench.spec.ts
git commit -m "feat: add reimbursement workbench lists"
```

---

## Task 5: Record drawer with editable fields and status-aware actions

**Files:**
- Create: `frontend/src/components/RecordDrawer.vue`
- Modify: `frontend/src/views/employee/ReimbursementListView.vue`
- Modify: `frontend/src/views/admin/ReimbursementAdminView.vue`
- Create: `frontend/tests/record-drawer.spec.ts`

- [ ] **Step 1: Write failing drawer tests**

Create `frontend/tests/record-drawer.spec.ts`:

```ts
// @vitest-environment jsdom
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { flushPromises, mount } from '@vue/test-utils';
import RecordDrawer from '../src/components/RecordDrawer.vue';
import http from '../src/api/http';

vi.mock('../src/api/http', () => ({
  default: { get: vi.fn(), post: vi.fn(), patch: vi.fn(), delete: vi.fn() }
}));

const draft = { id: 1, employeeId: 2, employeeName: '员工一', amount: 123.45, categoryId: 1, categoryName: '差旅费', purpose: '客户拜访', paymentTime: '2026-05-21T02:30:00Z', status: 'DRAFT', adminRemark: null, submittedAt: null, archivedAt: null, attachments: [] };
const submitted = { ...draft, id: 2, status: 'SUBMITTED', submittedAt: '2026-05-21T03:00:00Z', attachments: [{ id: 9, type: 'PAYMENT_VOUCHER', originalFilename: 'pay.png', contentType: 'image/png', sizeBytes: 11, createdAt: '2026-05-21T03:00:00Z' }] };

describe('RecordDrawer', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(http.get).mockResolvedValue({ data: [{ id: 1, name: '差旅费', enabled: true, sortOrder: 1, remark: '' }] });
  });

  it('edits and saves draft fields inside the drawer', async () => {
    vi.mocked(http.patch).mockResolvedValue({ data: { ...draft, amount: 200, purpose: '新用途' } });
    const wrapper = mount(RecordDrawer, { props: { record: draft, role: 'EMPLOYEE' } });
    await flushPromises();

    await wrapper.find('[aria-label="金额"]').setValue('200');
    await wrapper.find('[aria-label="用途说明"]').setValue('新用途');
    await wrapper.find('[data-test="save-draft"]').trigger('click');

    expect(http.patch).toHaveBeenCalledWith('/reimbursements/1', expect.objectContaining({ amount: 200, purpose: '新用途' }));
    expect(wrapper.emitted('saved')).toHaveLength(1);
  });

  it('renders submitted employee record as read-only', async () => {
    const wrapper = mount(RecordDrawer, { props: { record: submitted, role: 'EMPLOYEE' } });
    await flushPromises();

    expect(wrapper.find('[aria-label="金额"]').attributes('disabled')).toBeDefined();
    expect(wrapper.text()).not.toContain('提交');
  });

  it('allows admin to save remark for submitted record', async () => {
    vi.mocked(http.patch).mockResolvedValue({ data: { ...submitted, adminRemark: '已核对' } });
    const wrapper = mount(RecordDrawer, { props: { record: submitted, role: 'ADMIN' } });
    await flushPromises();

    await wrapper.find('[aria-label="管理员备注"]').setValue('已核对');
    await wrapper.find('[data-test="save-remark"]').trigger('click');

    expect(http.patch).toHaveBeenCalledWith('/admin/reimbursements/2/remark', { adminRemark: '已核对' });
  });
});
```

- [ ] **Step 2: Run drawer test to verify it fails**

Run:

```bash
npm test --prefix "F:/Code/报销/frontend" -- record-drawer.spec.ts
```

Expected: FAIL because `RecordDrawer.vue` does not exist.

- [ ] **Step 3: Implement RecordDrawer**

Create `frontend/src/components/RecordDrawer.vue`:

```vue
<script setup lang="ts">
import { computed, onMounted, reactive, watch } from 'vue';
import { listCategories, type Category } from '../api/categories';
import { submitReimbursement, updateAdminRemark, updateReimbursement, type ReimbursementRecord } from '../api/reimbursements';
import StatusTag from './StatusTag.vue';
import MaterialCompleteness from './MaterialCompleteness.vue';

const props = defineProps<{ record: ReimbursementRecord; role: 'EMPLOYEE' | 'ADMIN' }>();
const emit = defineEmits<{ close: []; saved: [ReimbursementRecord]; submitted: [ReimbursementRecord]; preview: [number] }>();

const categories = reactive<{ items: Category[] }>({ items: [] });
const form = reactive({ amount: 0, categoryId: 0, purpose: '', paymentTime: '', adminRemark: '' });
const loading = reactive({ save: false, submit: false, remark: false });

const editable = computed(() => props.record.status === 'DRAFT' && props.role === 'EMPLOYEE');
const adminSubmitted = computed(() => props.record.status === 'SUBMITTED' && props.role === 'ADMIN');
const paymentVoucherCount = computed(() => props.record.attachments.filter((item) => item.type === 'PAYMENT_VOUCHER').length);
const orderScreenshotCount = computed(() => props.record.attachments.filter((item) => item.type === 'ORDER_SCREENSHOT').length);
const invoiceCount = computed(() => props.record.attachments.filter((item) => item.type === 'INVOICE').length);

function toDateTimeLocal(value: string) {
  const date = new Date(value);
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 16);
}

function syncForm() {
  form.amount = Number(props.record.amount);
  form.categoryId = props.record.categoryId;
  form.purpose = props.record.purpose;
  form.paymentTime = toDateTimeLocal(props.record.paymentTime);
  form.adminRemark = props.record.adminRemark ?? '';
}

function payload() {
  return { amount: Number(form.amount), categoryId: Number(form.categoryId), purpose: form.purpose, paymentTime: new Date(form.paymentTime).toISOString() };
}

async function saveDraft() {
  loading.save = true;
  try {
    const response = await updateReimbursement(props.record.id, payload());
    emit('saved', response.data);
  } finally {
    loading.save = false;
  }
}

async function submitDraft() {
  loading.submit = true;
  try {
    const saved = await updateReimbursement(props.record.id, payload());
    const response = await submitReimbursement(saved.data.id);
    emit('submitted', response.data);
  } finally {
    loading.submit = false;
  }
}

async function saveRemark() {
  loading.remark = true;
  try {
    const response = await updateAdminRemark(props.record.id, form.adminRemark);
    emit('saved', response.data);
  } finally {
    loading.remark = false;
  }
}

watch(() => props.record, syncForm, { immediate: true });
onMounted(async () => {
  const response = await listCategories();
  categories.items = response.data;
});
</script>

<template>
  <aside class="record-drawer" role="dialog" aria-label="记录详情">
    <header class="drawer-header">
      <div>
        <span class="eyebrow">记录详情</span>
        <h2>¥{{ Number(record.amount).toFixed(2) }}</h2>
      </div>
      <button aria-label="关闭详情" @click="emit('close')">×</button>
    </header>

    <section class="summary enterprise-card">
      <StatusTag :status="record.status" />
      <p>{{ record.employeeName }} · {{ record.categoryName }} · {{ record.paymentTime }}</p>
      <MaterialCompleteness :payment-voucher-count="paymentVoucherCount" :order-screenshot-count="orderScreenshotCount" :invoice-count="invoiceCount" />
    </section>

    <section class="drawer-section enterprise-card">
      <h3>报销信息</h3>
      <label>金额<input aria-label="金额" v-model="form.amount" type="number" step="0.01" :disabled="!editable" /></label>
      <label>用途分类<select aria-label="用途分类" v-model.number="form.categoryId" :disabled="!editable"><option v-for="category in categories.items" :key="category.id" :value="category.id">{{ category.name }}</option></select></label>
      <label>用途说明<input aria-label="用途说明" v-model="form.purpose" :disabled="!editable" /></label>
      <label>支付时间<input aria-label="支付时间" v-model="form.paymentTime" type="datetime-local" :disabled="!editable" /></label>
    </section>

    <section v-if="role === 'ADMIN'" class="drawer-section enterprise-card">
      <h3>管理员备注</h3>
      <label>备注<input aria-label="管理员备注" v-model="form.adminRemark" :disabled="record.status === 'ARCHIVED'" /></label>
      <button v-if="adminSubmitted" data-test="save-remark" :disabled="loading.remark" @click="saveRemark">{{ loading.remark ? '保存中...' : '保存备注' }}</button>
    </section>

    <footer class="drawer-actions">
      <button v-if="editable" data-test="save-draft" :disabled="loading.save" @click="saveDraft">{{ loading.save ? '保存中...' : '保存草稿' }}</button>
      <button v-if="editable" data-test="submit-draft" :disabled="loading.submit || paymentVoucherCount === 0" @click="submitDraft">{{ loading.submit ? '提交中...' : '提交' }}</button>
      <span v-if="record.status === 'ARCHIVED'">已归档，只读</span>
    </footer>
  </aside>
</template>

<style scoped>
.record-drawer { position: fixed; top: 0; right: 0; z-index: 50; width: min(640px, 100vw); height: 100dvh; display: grid; grid-template-rows: auto auto 1fr auto; gap: var(--space-4); padding: var(--space-5); overflow: auto; background: var(--color-background); box-shadow: -20px 0 60px rgba(15, 23, 42, 0.18); }
.drawer-header { display: flex; justify-content: space-between; align-items: flex-start; }
.drawer-header h2 { margin: 4px 0 0; font-size: 28px; }
.drawer-header button { width: 44px; height: 44px; border: 1px solid var(--color-border); border-radius: 50%; background: white; }
.eyebrow { color: var(--color-text-secondary); font-size: 12px; }
.summary, .drawer-section { padding: var(--space-4); }
.summary { display: grid; gap: var(--space-3); }
.summary p { margin: 0; color: var(--color-text-secondary); }
.drawer-section { display: grid; gap: var(--space-3); }
.drawer-section h3 { margin: 0; font-size: 18px; }
label { display: grid; gap: var(--space-2); color: var(--color-text-secondary); font-size: 13px; }
input, select { min-height: 40px; border: 1px solid var(--color-border); border-radius: var(--radius-md); padding: 0 var(--space-3); background: white; color: var(--color-text-primary); }
.drawer-actions { position: sticky; bottom: 0; display: flex; gap: var(--space-3); padding-top: var(--space-4); background: var(--color-background); }
.drawer-actions button { min-height: 44px; padding: 0 var(--space-4); border: 1px solid var(--color-primary); border-radius: var(--radius-md); background: var(--color-primary); color: white; }
.drawer-actions span { color: var(--color-text-secondary); }
</style>
```

- [ ] **Step 4: Replace drawer stubs in employee and admin views**

In both `frontend/src/views/employee/ReimbursementListView.vue` and `frontend/src/views/admin/ReimbursementAdminView.vue`, import `RecordDrawer` and replace the `drawer-stub` aside with:

```vue
<RecordDrawer
  v-if="selected"
  :record="selected"
  :role="'EMPLOYEE'"
  @close="selected = null"
  @saved="selected = $event; load()"
  @submitted="selected = $event; load()"
/>
```

Use `:role="'ADMIN'"` in admin view.

Remove `.drawer-stub` CSS from both views.

- [ ] **Step 5: Run drawer tests and workbench tests**

Run:

```bash
npm test --prefix "F:/Code/报销/frontend" -- record-drawer.spec.ts workbench.spec.ts
```

Expected: PASS.

- [ ] **Step 6: Commit drawer**

Run:

```bash
git add frontend/src/components/RecordDrawer.vue frontend/src/views/employee/ReimbursementListView.vue frontend/src/views/admin/ReimbursementAdminView.vue frontend/tests/record-drawer.spec.ts
git commit -m "feat: add record detail drawer"
```

---

## Task 6: Material list with upload, delete, download, and preview events

**Files:**
- Create: `frontend/src/components/MaterialList.vue`
- Modify: `frontend/src/components/RecordDrawer.vue`
- Modify: `frontend/src/api/reimbursements.ts`
- Create: `frontend/tests/material-list.spec.ts`

- [ ] **Step 1: Write failing material list tests**

Create `frontend/tests/material-list.spec.ts`:

```ts
// @vitest-environment jsdom
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';
import MaterialList from '../src/components/MaterialList.vue';
import http from '../src/api/http';

vi.mock('../src/api/http', () => ({
  default: { get: vi.fn(), post: vi.fn(), patch: vi.fn(), delete: vi.fn() }
}));

const attachment = { id: 9, type: 'PAYMENT_VOUCHER', originalFilename: 'pay.png', contentType: 'image/png', sizeBytes: 11, createdAt: '2026-05-21T03:00:00Z' };

describe('MaterialList', () => {
  beforeEach(() => vi.clearAllMocks());

  it('groups attachments and emits preview', async () => {
    const wrapper = mount(MaterialList, { props: { recordId: 1, status: 'SUBMITTED', attachments: [attachment] } });
    expect(wrapper.text()).toContain('支付凭证');
    expect(wrapper.text()).toContain('pay.png');
    await wrapper.find('[data-test="preview-9"]').trigger('click');
    expect(wrapper.emitted('preview')?.[0]).toEqual([9]);
  });

  it('uploads file for draft record', async () => {
    vi.mocked(http.post).mockResolvedValue({ data: attachment });
    const wrapper = mount(MaterialList, { props: { recordId: 1, status: 'DRAFT', attachments: [] } });
    const file = new File(['pay'], 'pay.png', { type: 'image/png' });
    const input = wrapper.find('[data-test="upload-PAYMENT_VOUCHER"]').element as HTMLInputElement;
    Object.defineProperty(input, 'files', { value: [file], configurable: true });
    await wrapper.find('[data-test="upload-PAYMENT_VOUCHER"]').trigger('change');
    expect(http.post).toHaveBeenCalledWith('/reimbursements/1/attachments?type=PAYMENT_VOUCHER', expect.any(FormData));
    expect(wrapper.emitted('changed')).toHaveLength(1);
  });

  it('confirms and deletes draft attachment', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    vi.mocked(http.delete).mockResolvedValue({});
    const wrapper = mount(MaterialList, { props: { recordId: 1, status: 'DRAFT', attachments: [attachment] } });
    await wrapper.find('[data-test="delete-9"]').trigger('click');
    expect(http.delete).toHaveBeenCalledWith('/attachments/9');
    expect(wrapper.emitted('changed')).toHaveLength(1);
  });
});
```

- [ ] **Step 2: Run material list test to verify it fails**

Run:

```bash
npm test --prefix "F:/Code/报销/frontend" -- material-list.spec.ts
```

Expected: FAIL because `MaterialList.vue` does not exist.

- [ ] **Step 3: Create MaterialList**

Create `frontend/src/components/MaterialList.vue`:

```vue
<script setup lang="ts">
import { computed, reactive } from 'vue';
import { deleteAttachment, uploadAttachment, type AttachmentRecord, type AttachmentType, type ReimbursementStatus } from '../api/reimbursements';

const props = defineProps<{ recordId: number; status: ReimbursementStatus; attachments: AttachmentRecord[] }>();
const emit = defineEmits<{ preview: [number]; changed: [] }>();
const loading = reactive<Record<string, boolean>>({});

const groups: { type: AttachmentType; label: string; required: boolean }[] = [
  { type: 'PAYMENT_VOUCHER', label: '支付凭证', required: true },
  { type: 'ORDER_SCREENSHOT', label: '订单截图', required: false },
  { type: 'INVOICE', label: '发票', required: false }
];

const byType = computed(() => Object.fromEntries(groups.map((group) => [group.type, props.attachments.filter((attachment) => attachment.type === group.type)])) as Record<AttachmentType, AttachmentRecord[]>);
const editable = computed(() => props.status === 'DRAFT');

async function upload(type: AttachmentType, event: Event) {
  const input = event.target as HTMLInputElement;
  const files = Array.from(input.files ?? []);
  if (!files.length) return;
  loading[`upload-${type}`] = true;
  try {
    for (const file of files) {
      await uploadAttachment(props.recordId, type, file);
    }
    emit('changed');
  } finally {
    loading[`upload-${type}`] = false;
    input.value = '';
  }
}

async function remove(id: number) {
  if (!window.confirm('确认删除该附件？')) return;
  loading[`delete-${id}`] = true;
  try {
    await deleteAttachment(id);
    emit('changed');
  } finally {
    loading[`delete-${id}`] = false;
  }
}
</script>

<template>
  <section class="material-list">
    <article v-for="group in groups" :key="group.type" class="material-group enterprise-card">
      <header>
        <h3>{{ group.label }}<span>{{ group.required ? '必填' : '选填' }}</span></h3>
        <label v-if="editable" class="upload-button">
          {{ loading[`upload-${group.type}`] ? '上传中...' : '上传' }}
          <input :data-test="`upload-${group.type}`" type="file" multiple accept="image/png,image/jpeg,image/webp,application/pdf" @change="upload(group.type, $event)" />
        </label>
      </header>
      <p v-if="group.required && byType[group.type].length === 0" class="missing">提交前必须上传支付凭证</p>
      <p v-else-if="byType[group.type].length === 0" class="empty">未上传</p>
      <ul v-else>
        <li v-for="attachment in byType[group.type]" :key="attachment.id">
          <button :data-test="`preview-${attachment.id}`" class="file-main" @click="emit('preview', attachment.id)">
            <strong>{{ attachment.originalFilename }}</strong>
            <span>{{ attachment.contentType }} · {{ attachment.sizeBytes }} bytes</span>
          </button>
          <a :href="`/api/attachments/${attachment.id}`" download>下载</a>
          <button v-if="editable" :data-test="`delete-${attachment.id}`" :disabled="loading[`delete-${attachment.id}`]" @click="remove(attachment.id)">删除</button>
        </li>
      </ul>
    </article>
  </section>
</template>

<style scoped>
.material-list { display: grid; gap: var(--space-4); }
.material-group { padding: var(--space-4); display: grid; gap: var(--space-3); }
header { display: flex; align-items: center; justify-content: space-between; gap: var(--space-3); }
h3 { margin: 0; font-size: 16px; display: flex; align-items: center; gap: var(--space-2); }
h3 span { color: var(--color-text-secondary); font-size: 12px; font-weight: 500; }
.upload-button { position: relative; min-height: 36px; display: inline-flex; align-items: center; padding: 0 var(--space-3); border-radius: var(--radius-md); background: var(--color-primary-soft); color: var(--color-primary); cursor: pointer; }
.upload-button input { position: absolute; inset: 0; opacity: 0; cursor: pointer; }
.missing { color: var(--color-danger); margin: 0; }
.empty { color: var(--color-text-secondary); margin: 0; }
ul { list-style: none; display: grid; gap: var(--space-2); padding: 0; margin: 0; }
li { display: grid; grid-template-columns: 1fr auto auto; align-items: center; gap: var(--space-2); padding: var(--space-2); border: 1px solid var(--color-border); border-radius: var(--radius-md); }
.file-main { text-align: left; border: 0; background: transparent; color: var(--color-text-primary); }
.file-main span { display: block; margin-top: 2px; color: var(--color-text-secondary); font-size: 12px; }
a, li > button:not(.file-main) { min-height: 36px; display: inline-flex; align-items: center; padding: 0 var(--space-3); border: 1px solid var(--color-border); border-radius: var(--radius-md); background: white; color: var(--color-text-primary); text-decoration: none; }
</style>
```

- [ ] **Step 4: Integrate MaterialList in RecordDrawer**

In `frontend/src/components/RecordDrawer.vue`, import `MaterialList`:

```ts
import MaterialList from './MaterialList.vue';
```

Add this section before the footer:

```vue
<section class="drawer-section materials-section">
  <h3>报销材料</h3>
  <MaterialList :record-id="record.id" :status="record.status" :attachments="record.attachments" @preview="emit('preview', $event)" @changed="emit('saved', record)" />
</section>
```

- [ ] **Step 5: Refresh selected record after material changes**

In both workbench views, when `RecordDrawer` emits `saved`, reload the list and keep the selected record synced:

```ts
async function refreshSelected(record: ReimbursementRecord) {
  await load();
  selected.value = records.value.find((item) => item.id === record.id) ?? record;
}
```

Use:

```vue
<RecordDrawer @saved="refreshSelected" @submitted="refreshSelected" />
```

- [ ] **Step 6: Run material list and drawer tests**

Run:

```bash
npm test --prefix "F:/Code/报销/frontend" -- material-list.spec.ts record-drawer.spec.ts
```

Expected: PASS.

- [ ] **Step 7: Commit material list**

Run:

```bash
git add frontend/src/components/MaterialList.vue frontend/src/components/RecordDrawer.vue frontend/src/views/employee/ReimbursementListView.vue frontend/src/views/admin/ReimbursementAdminView.vue frontend/tests/material-list.spec.ts
git commit -m "feat: add material list interactions"
```

---

## Task 7: Full-screen material previewer

**Files:**
- Create: `frontend/src/components/MaterialPreviewer.vue`
- Modify: `frontend/src/components/RecordDrawer.vue`
- Modify: `frontend/src/views/employee/ReimbursementListView.vue`
- Modify: `frontend/src/views/admin/ReimbursementAdminView.vue`
- Create: `frontend/tests/material-previewer.spec.ts`

- [ ] **Step 1: Write failing previewer tests**

Create `frontend/tests/material-previewer.spec.ts`:

```ts
// @vitest-environment jsdom
import { describe, expect, it } from 'vitest';
import { mount } from '@vue/test-utils';
import MaterialPreviewer from '../src/components/MaterialPreviewer.vue';

const attachments = [
  { id: 1, type: 'PAYMENT_VOUCHER', originalFilename: 'pay.png', contentType: 'image/png', sizeBytes: 11, createdAt: '2026-05-21T03:00:00Z' },
  { id: 2, type: 'INVOICE', originalFilename: 'invoice.pdf', contentType: 'application/pdf', sizeBytes: 22, createdAt: '2026-05-21T03:00:00Z' }
];

describe('MaterialPreviewer', () => {
  it('renders image preview and switches to next attachment', async () => {
    const wrapper = mount(MaterialPreviewer, { props: { attachments, activeId: 1 } });
    expect(wrapper.find('img').attributes('src')).toBe('/api/attachments/1');
    await wrapper.find('[data-test="next-preview"]').trigger('click');
    expect(wrapper.find('object').attributes('data')).toBe('/api/attachments/2');
  });

  it('emits close and shows download link', async () => {
    const wrapper = mount(MaterialPreviewer, { props: { attachments, activeId: 1 } });
    expect(wrapper.find('[data-test="download-active"]').attributes('href')).toBe('/api/attachments/1');
    await wrapper.find('[data-test="close-preview"]').trigger('click');
    expect(wrapper.emitted('close')).toHaveLength(1);
  });
});
```

- [ ] **Step 2: Run previewer test to verify it fails**

Run:

```bash
npm test --prefix "F:/Code/报销/frontend" -- material-previewer.spec.ts
```

Expected: FAIL because `MaterialPreviewer.vue` does not exist.

- [ ] **Step 3: Create MaterialPreviewer**

Create `frontend/src/components/MaterialPreviewer.vue`:

```vue
<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import type { AttachmentRecord } from '../api/reimbursements';

const props = defineProps<{ attachments: AttachmentRecord[]; activeId: number }>();
const emit = defineEmits<{ close: [] }>();
const currentId = ref(props.activeId);

const currentIndex = computed(() => Math.max(0, props.attachments.findIndex((attachment) => attachment.id === currentId.value)));
const current = computed(() => props.attachments[currentIndex.value]);
const src = computed(() => current.value ? `/api/attachments/${current.value.id}` : '');
const isImage = computed(() => current.value?.contentType.startsWith('image/') ?? false);
const isPdf = computed(() => current.value?.contentType === 'application/pdf');

function previous() {
  if (!props.attachments.length) return;
  currentId.value = props.attachments[(currentIndex.value - 1 + props.attachments.length) % props.attachments.length].id;
}

function next() {
  if (!props.attachments.length) return;
  currentId.value = props.attachments[(currentIndex.value + 1) % props.attachments.length].id;
}

watch(() => props.activeId, (id) => currentId.value = id);
</script>

<template>
  <div class="previewer" role="dialog" aria-label="材料预览">
    <header class="previewer-bar">
      <div>
        <strong>{{ current?.originalFilename }}</strong>
        <span>{{ current?.type }}</span>
      </div>
      <nav>
        <a v-if="current" data-test="download-active" :href="src" download>下载</a>
        <button data-test="close-preview" @click="emit('close')">关闭</button>
      </nav>
    </header>
    <main class="previewer-stage">
      <button data-test="previous-preview" class="switch left" @click="previous">上一份</button>
      <img v-if="isImage" :src="src" :alt="current?.originalFilename" />
      <object v-else-if="isPdf" :data="src" type="application/pdf"><a :href="src" download>下载 PDF</a></object>
      <section v-else class="unsupported enterprise-card"><p>当前文件无法在线预览</p><a :href="src" download>下载文件</a></section>
      <button data-test="next-preview" class="switch right" @click="next">下一份</button>
    </main>
    <footer class="thumbnail-strip">
      <button v-for="attachment in attachments" :key="attachment.id" :class="{ active: attachment.id === current?.id }" @click="currentId = attachment.id">
        <span>{{ attachment.type }}</span>
        <strong>{{ attachment.originalFilename }}</strong>
      </button>
    </footer>
  </div>
</template>

<style scoped>
.previewer { position: fixed; inset: 0; z-index: 100; display: grid; grid-template-rows: 56px 1fr 96px; background: rgba(15, 23, 42, 0.96); color: white; }
.previewer-bar { display: flex; align-items: center; justify-content: space-between; padding: 0 var(--space-5); border-bottom: 1px solid rgba(255,255,255,0.12); }
.previewer-bar span { display: block; color: #cbd5e1; font-size: 12px; margin-top: 2px; }
.previewer-bar nav { display: flex; gap: var(--space-2); }
.previewer-bar a, .previewer-bar button { min-height: 40px; display: inline-flex; align-items: center; padding: 0 var(--space-3); border: 1px solid rgba(255,255,255,0.24); border-radius: var(--radius-md); background: rgba(255,255,255,0.08); color: white; text-decoration: none; }
.previewer-stage { position: relative; display: grid; place-items: center; min-width: 0; min-height: 0; }
.previewer-stage img, .previewer-stage object { max-width: calc(100vw - 160px); max-height: calc(100dvh - 180px); width: auto; height: auto; }
.previewer-stage object { width: calc(100vw - 160px); height: calc(100dvh - 180px); background: white; }
.switch { position: absolute; top: 50%; transform: translateY(-50%); min-height: 44px; padding: 0 var(--space-3); border: 1px solid rgba(255,255,255,0.24); border-radius: var(--radius-md); background: rgba(255,255,255,0.08); color: white; }
.left { left: var(--space-5); }
.right { right: var(--space-5); }
.unsupported { padding: var(--space-5); color: var(--color-text-primary); }
.thumbnail-strip { display: flex; gap: var(--space-2); align-items: center; overflow-x: auto; padding: var(--space-3) var(--space-5); border-top: 1px solid rgba(255,255,255,0.12); }
.thumbnail-strip button { min-width: 180px; text-align: left; padding: var(--space-2); border: 1px solid rgba(255,255,255,0.16); border-radius: var(--radius-md); background: rgba(255,255,255,0.08); color: white; }
.thumbnail-strip button.active { border-color: var(--color-primary); background: rgba(29, 78, 216, 0.45); }
.thumbnail-strip span { display: block; color: #cbd5e1; font-size: 11px; }
.thumbnail-strip strong { display: block; margin-top: 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
@media (max-width: 767px) { .previewer-stage img, .previewer-stage object { max-width: 100vw; width: 100vw; } .switch { top: auto; bottom: var(--space-4); } }
</style>
```

- [ ] **Step 4: Integrate previewer in workbench views**

In both workbench views, add:

```ts
import MaterialPreviewer from '../../components/MaterialPreviewer.vue';

const previewAttachmentId = ref<number | null>(null);
const previewAttachments = computed(() => selected.value?.attachments ?? []);
```

Pass preview event to `RecordDrawer`:

```vue
<RecordDrawer @preview="previewAttachmentId = $event" />
<MaterialPreviewer v-if="previewAttachmentId" :attachments="previewAttachments" :active-id="previewAttachmentId" @close="previewAttachmentId = null" />
```

- [ ] **Step 5: Run preview tests and workbench tests**

Run:

```bash
npm test --prefix "F:/Code/报销/frontend" -- material-previewer.spec.ts record-drawer.spec.ts workbench.spec.ts
```

Expected: PASS.

- [ ] **Step 6: Commit previewer**

Run:

```bash
git add frontend/src/components/MaterialPreviewer.vue frontend/src/views/employee/ReimbursementListView.vue frontend/src/views/admin/ReimbursementAdminView.vue frontend/tests/material-previewer.spec.ts
git commit -m "feat: add full-screen material previewer"
```

---

## Task 8: Batch page visual consistency and action feedback

**Files:**
- Modify: `frontend/src/views/admin/BatchAdminView.vue`
- Modify: `frontend/tests/admin-batch.spec.ts`

- [ ] **Step 1: Update batch test for loading and enterprise layout**

Modify `frontend/tests/admin-batch.spec.ts` with assertions:

```ts
expect(wrapper.text()).toContain('批次列表');
expect(wrapper.find('.enterprise-card').exists()).toBe(true);
await wrapper.find('[data-test="export-excel"]').trigger('click');
expect(http.get).toHaveBeenCalledWith('/admin/batches/1/export/excel', { responseType: 'blob' });
```

- [ ] **Step 2: Run batch test to verify it fails if layout is not updated**

Run:

```bash
npm test --prefix "F:/Code/报销/frontend" -- admin-batch.spec.ts
```

Expected: FAIL if `.enterprise-card` is absent from the batch page.

- [ ] **Step 3: Update BatchAdminView layout**

Modify `frontend/src/views/admin/BatchAdminView.vue` so forms and tables are wrapped in cards:

```vue
<template>
  <section class="batch-page">
    <section class="enterprise-card batch-card">
      <h2>创建批次</h2>
      <form class="admin-form" @submit.prevent="saveBatch">
        <input aria-label="批次名称" v-model="form.name" placeholder="批次名称" />
        <input aria-label="批次描述" v-model="form.description" placeholder="批次描述" />
        <button type="submit">创建批次</button>
      </form>
    </section>

    <section class="enterprise-card batch-card">
      <h2>批次操作</h2>
      <form class="admin-form" @submit.prevent="loadBatch()">
        <input aria-label="批次ID" v-model="batchId" type="number" placeholder="批次ID" />
        <button data-test="load-batch" type="submit">加载批次</button>
        <input aria-label="报销记录ID" v-model="recordId" type="number" placeholder="报销记录ID" />
        <button data-test="add-record" type="button" @click="addRecord">加入批次</button>
        <button data-test="export-excel" type="button" @click="exportExcel">导出 Excel</button>
        <button data-test="export-attachments" type="button" @click="exportAttachments">导出附件</button>
        <button data-test="archive-batch" type="button" @click="archiveCurrent">归档</button>
      </form>
    </section>

    <section class="enterprise-card batch-card">
      <h2>批次列表</h2>
      <table>
        <thead><tr><th>ID</th><th>名称</th><th>说明</th><th>归档时间</th><th>操作</th></tr></thead>
        <tbody><tr v-for="batch in batches" :key="batch.id"><td>{{ batch.id }}</td><td>{{ batch.name }}</td><td>{{ batch.description }}</td><td>{{ batch.archivedAt }}</td><td><button @click="loadBatch(batch.id)">查看</button></td></tr></tbody>
      </table>
    </section>

    <section class="enterprise-card batch-card">
      <h2>批次明细</h2>
      <table>
        <thead><tr><th>记录ID</th><th>员工</th><th>分类</th><th>操作</th></tr></thead>
        <tbody><tr v-for="item in current?.items ?? []" :key="item.id"><td>{{ item.recordId }}</td><td>{{ item.employeeName }}</td><td>{{ item.categoryName }}</td><td><button @click="removeRecord(item.recordId)">移除</button></td></tr></tbody>
      </table>
    </section>
  </section>
</template>
```

Use this style:

```vue
<style scoped>
.batch-page { display: grid; gap: var(--space-4); }
.batch-card { padding: var(--space-4); }
h2 { margin: 0 0 var(--space-4); font-size: 18px; }
.admin-form { display: flex; flex-wrap: wrap; gap: var(--space-3); }
input, button { min-height: 40px; border: 1px solid var(--color-border); border-radius: var(--radius-md); padding: 0 var(--space-3); background: white; }
button { color: var(--color-primary); }
table { width: 100%; border-collapse: collapse; }
th { text-align: left; padding: var(--space-3); background: var(--color-surface-muted); color: var(--color-text-secondary); font-size: 12px; }
td { padding: var(--space-3); border-top: 1px solid var(--color-border); }
</style>
```

- [ ] **Step 4: Run batch tests**

Run:

```bash
npm test --prefix "F:/Code/报销/frontend" -- admin-batch.spec.ts
```

Expected: PASS.

- [ ] **Step 5: Commit batch visual consistency**

Run:

```bash
git add frontend/src/views/admin/BatchAdminView.vue frontend/tests/admin-batch.spec.ts
git commit -m "style: align batch page with workbench UI"
```

---

## Task 9: Final responsive, accessibility, and acceptance verification

**Files:**
- Modify: `tasks/todo.md`
- Modify: `README.md` if run instructions changed

- [ ] **Step 1: Run frontend full test and build**

Run:

```bash
npm test --prefix "F:/Code/报销/frontend" && npm run build --prefix "F:/Code/报销/frontend"
```

Expected: tests PASS and build exits 0. Existing third-party Rolldown warnings are acceptable if unchanged.

- [ ] **Step 2: Run backend full tests**

Run:

```bash
JAVA_HOME="D:\software\jdk-21" "/d/software/apache-maven-3.9.11/bin/mvn.cmd" -q -pl backend test
```

Expected: exit code 0. JVM/ByteBuddy warnings are acceptable if tests pass.

- [ ] **Step 3: Start backend for browser acceptance**

Run:

```bash
JAVA_HOME="D:\software\jdk-21" "D:/software/jdk-21/bin/java.exe" -cp "F:/Code/报销/backend/target/classes;F:/Code/报销/backend/target/test-classes;$(tr '\n' ';' < "F:/Code/报销/backend/target/classpath.txt")" com.company.reimbursement.ReimbursementApplication --server.port=18083 --spring.datasource.url=jdbc:h2:mem:ui_acceptance\;MODE=MySQL\;DATABASE_TO_LOWER=TRUE --spring.datasource.username=sa --spring.datasource.password= --spring.jpa.hibernate.ddl-auto=validate --app.storage.root=target/ui-acceptance-storage --app.bootstrap.admin.username=admin --app.bootstrap.admin.password=secret123 --app.bootstrap.admin.display-name=系统管理员 --app.bootstrap.admin.department=财务部
```

Expected: backend listens on port 18083.

- [ ] **Step 4: Start frontend for browser acceptance**

Run:

```bash
VITE_API_TARGET="http://127.0.0.1:18083" npm run dev --prefix "F:/Code/报销/frontend" -- --port 5183
```

Expected: Vite prints `http://127.0.0.1:5183/`.

- [ ] **Step 5: Browser acceptance checklist**

Use the browser to verify:

```text
1. Admin logs in with admin / secret123.
2. Sidebar and topbar render with admin-only nav.
3. Admin creates an employee.
4. Admin creates an enabled category.
5. Employee logs in in isolated browser context.
6. Employee sees workbench metrics and empty state.
7. Employee creates a draft and uploads payment voucher.
8. Employee list row shows material completeness.
9. Employee clicks row and drawer opens.
10. Employee previews attachment in full-screen previewer.
11. Employee submits the record.
12. Admin sees submitted record in workbench.
13. Admin clicks row and drawer opens.
14. Admin saves remark.
15. Admin creates batch, adds record, exports Excel and attachment Zip, archives batch.
16. Admin filters ARCHIVED and sees record status ARCHIVED.
17. At 375px viewport, sidebar/nav remains usable and drawer becomes full-width.
```

Expected: all steps pass.

- [ ] **Step 6: Update task tracker**

Modify `tasks/todo.md` and append:

```markdown
## UI/UX 工作台优化验收

- [x] 前端全量测试与构建通过。
- [x] 后端全量测试通过。
- [x] 浏览器验收通过：侧边栏、工作台指标、筛选、行点击抽屉、材料上传、全屏预览器、批次导出和归档。
- [x] 未推送 GitHub。
```

- [ ] **Step 7: Commit final verification notes**

Run:

```bash
git add tasks/todo.md README.md
git commit -m "docs: record UI workbench verification"
```

---

## Spec Coverage Review

- Global shell, sidebar, topbar: Task 2.
- Enterprise SaaS tokens, spacing, card surface: Task 2 and Task 3.
- Status tags, metric cards, material completeness, empty states: Task 3.
- Employee and admin workbench list with filters and metrics: Task 4.
- Row click right-side drawer: Task 4 and Task 5.
- Status-aware editing and remarks: Task 5.
- Material groups, upload, delete, download: Task 6.
- Full-screen image/PDF previewer with next/previous and thumbnails: Task 7.
- Batch page consistency and action feedback: Task 8.
- Responsive and browser acceptance: Task 9.

## Placeholder Scan Result

This plan contains no unresolved placeholders, no GitHub push step, and no scope outside the approved UI/UX workbench optimization.
