# Fast Batch Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make batch creation and assignment obvious from the batch management page without letting employees choose batches during reimbursement entry.

**Architecture:** Keep batch ownership in the admin/specialist workflow. Reuse existing backend `POST /api/admin/batches/monthly` and frontend `ensureMonthlyBatch()`; only improve `BatchAdminView.vue` and its Vitest coverage. No backend schema or employee form changes.

**Tech Stack:** Vue 3, TypeScript, Vitest, existing Spring Boot batch API.

---

## File Structure

- Modify: `frontend/src/views/admin/BatchAdminView.vue`
  - Import `ensureMonthlyBatch`.
  - Add one-click “创建/使用本月批次”.
  - Add current batch summary so users know where selected records will go.
  - Keep batch assignment restricted to selected submitted records.
- Modify: `frontend/tests/admin-batch.spec.ts`
  - Test monthly batch button calls `/admin/batches/monthly` and selects returned batch.
  - Test selected preview records are added to current batch through `/admin/batches/{id}/items`.

---

### Task 1: Expose Monthly Batch Quick Action

- [ ] **Step 1: Write failing test**

Add a test in `frontend/tests/admin-batch.spec.ts` that mounts `BatchAdminView`, clicks `[data-test="ensure-monthly-batch"]`, and expects:

```ts
expect(http.post).toHaveBeenCalledWith('/admin/batches/monthly');
expect(wrapper.text()).toContain('2026年7月报销批次');
```

- [ ] **Step 2: Implement minimal UI**

In `frontend/src/views/admin/BatchAdminView.vue`:

```ts
import { ensureMonthlyBatch } from '../../api/batches';

async function useMonthlyBatch() {
  try {
    const response = await ensureMonthlyBatch();
    current.value = response.data;
    advanced.batchId = response.data.id;
    await loadBatches();
    ElMessage.success('已切换到本月批次');
  } catch (err) {
    ElMessage.error(apiErrorMessage(err));
  }
}
```

Add button:

```vue
<button type="button" class="ghost-btn" data-test="ensure-monthly-batch" @click="useMonthlyBatch">创建/使用本月批次</button>
```

- [ ] **Step 3: Verify**

Run:

```bash
npm test --prefix "F:/Code/报销/frontend" -- admin-batch.spec.ts
```

Expected: batch tests pass.

---

### Task 2: Clarify Current Batch Assignment

- [ ] **Step 1: Write failing test**

Extend existing batch-management test to assert the page shows:

```ts
expect(wrapper.text()).toContain('当前批次');
expect(wrapper.text()).toContain('勾选记录后加入当前批次');
```

- [ ] **Step 2: Implement current batch summary**

Add near the join action:

```vue
<div class="current-batch enterprise-card" data-test="current-batch-summary">
  <p class="eyebrow">当前批次</p>
  <h2>{{ current?.name ?? '未选择批次' }}</h2>
  <p>{{ current ? '勾选记录后加入当前批次' : '先创建或选择一个批次，再勾选待报销记录加入。' }}</p>
</div>
```

- [ ] **Step 3: Verify**

Run:

```bash
npm test --prefix "F:/Code/报销/frontend" -- admin-batch.spec.ts
npm test --prefix "F:/Code/报销/frontend"
npm run build --prefix "F:/Code/报销/frontend"
```

Expected: all pass; only existing build warnings allowed.

---

## Self-Review

- Scope is intentionally frontend-only because backend/API already supports monthly batch creation and multi-record assignment.
- Employee reimbursement form remains unchanged; employees do not select batches.
- No placeholders or ambiguous ownership: admins/specialists own batch assignment.
