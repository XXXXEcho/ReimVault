# 管理端新增按钮与保存提示 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让用户管理、分类管理在编辑后能一键切回新增态，并在保存后明确提示成功或失败。

**Architecture:** 保持现有 Vue 单文件组件结构，不引入弹窗、路由或新组件。新增按钮只负责重置本页 reactive form 和 `editingId`；保存逻辑继续复用现有 create/update API，并在页面顶部显示 notice。

**Tech Stack:** Vue 3 Composition API、TypeScript、Vitest、@vue/test-utils、现有 `http` API mock。

---

## File Structure

- Modify: `frontend/tests/admin-batch.spec.ts`
  - 增加用户管理编辑后切回新增态的回归测试。
  - 增加分类管理保存成功与编辑后切回新增态的回归测试。
- Modify: `frontend/src/views/admin/UserAdminView.vue`
  - 新增 `resetForm()`。
  - 标题区新增“新增用户”按钮。
  - 保存成功后保留现有“用户保存成功”提示。
- Modify: `frontend/src/views/admin/CategoryAdminView.vue`
  - 新增 `notice`、`resetForm()`、`errorMessage()`。
  - 标题区新增“新增分类”按钮。
  - 保存成功/失败后显示提示。
- Modify: `tasks/todo.md`
  - 记录本轮执行计划和验证结果。

---

### Task 1: Add failing frontend tests

**Files:**
- Modify: `frontend/tests/admin-batch.spec.ts`

- [ ] **Step 1: Add user reset-to-create regression test**

Append this test inside `describe('admin views', () => { ... })`, after the existing user edit test:

```ts
  it('resets edited user form before creating another user', async () => {
    vi.mocked(http.get).mockResolvedValue({
      data: [{ id: 7, username: 'lisi', displayName: '李四', department: '销售部', role: 'EMPLOYEE', enabled: true }]
    });
    vi.mocked(http.post).mockResolvedValue({ data: { id: 8 } });
    const wrapper = mount(UserAdminView, { global: { stubs: ['el-table', 'el-table-column', 'el-drawer', 'el-form', 'el-form-item', 'el-input', 'el-select', 'el-option', 'el-switch', 'el-button'] } });
    await vi.waitFor(() => expect(wrapper.text()).toContain('lisi'));

    await wrapper.find('tbody button').trigger('click');
    await wrapper.find('[data-test="new-user"]').trigger('click');
    await wrapper.find('[aria-label="用户名"]').setValue('wangwu');
    await wrapper.find('[aria-label="姓名"]').setValue('王五');
    await wrapper.find('[aria-label="部门"]').setValue('财务部');
    await wrapper.find('[aria-label="密码"]').setValue('secret');
    await wrapper.find('[data-test="create-user"]').trigger('click');

    expect(http.post).toHaveBeenCalledWith('/admin/users', {
      username: 'wangwu',
      displayName: '王五',
      department: '财务部',
      password: 'secret',
      role: 'EMPLOYEE'
    });
    expect(http.patch).not.toHaveBeenCalled();
    expect(wrapper.find('[role="status"]').text()).toContain('用户保存成功');
  });
```

- [ ] **Step 2: Add category success notice test**

Append this test after `creates categories through the admin categories endpoint`:

```ts
  it('shows a success message after saving a category', async () => {
    vi.mocked(http.post).mockResolvedValue({ data: { id: 2 } });
    const wrapper = mount(CategoryAdminView, { global: { stubs: ['el-table', 'el-table-column', 'el-form', 'el-form-item', 'el-input', 'el-input-number', 'el-switch', 'el-button'] } });

    await wrapper.find('[aria-label="分类名称"]').setValue('办公用品');
    await wrapper.find('[aria-label="排序"]').setValue('3');
    await wrapper.find('[data-test="create-category"]').trigger('click');
    await vi.waitFor(() => expect(wrapper.text()).toContain('分类保存成功'));

    expect(wrapper.find('[role="status"]').text()).toContain('分类保存成功');
  });
```

- [ ] **Step 3: Add category reset-to-create regression test**

Append this test after the category success notice test:

```ts
  it('resets edited category form before creating another category', async () => {
    vi.mocked(http.get).mockResolvedValue({
      data: [{ id: 3, name: '交通费', enabled: false, sortOrder: 9, remark: '旧分类' }]
    });
    vi.mocked(http.post).mockResolvedValue({ data: { id: 4 } });
    const wrapper = mount(CategoryAdminView, { global: { stubs: ['el-table', 'el-table-column', 'el-form', 'el-form-item', 'el-input', 'el-input-number', 'el-switch', 'el-button'] } });
    await vi.waitFor(() => expect(wrapper.text()).toContain('交通费'));

    await wrapper.find('tbody button').trigger('click');
    await wrapper.find('[data-test="new-category"]').trigger('click');
    await wrapper.find('[aria-label="分类名称"]').setValue('差旅费');
    await wrapper.find('[aria-label="排序"]').setValue('1');
    await wrapper.find('[aria-label="备注"]').setValue('新分类');
    await wrapper.find('[data-test="create-category"]').trigger('click');

    expect(http.post).toHaveBeenCalledWith('/admin/categories', {
      name: '差旅费',
      enabled: true,
      sortOrder: 1,
      remark: '新分类'
    });
    expect(http.patch).not.toHaveBeenCalled();
    expect(wrapper.find('[role="status"]').text()).toContain('分类保存成功');
  });
```

- [ ] **Step 4: Run target tests and verify failure**

Run:

```bash
npm test --prefix "F:/Code/报销/frontend" -- admin-batch.spec.ts
```

Expected: FAIL because `[data-test="new-user"]`, `[data-test="new-category"]`, and category notice do not exist yet.

---

### Task 2: Implement user reset button

**Files:**
- Modify: `frontend/src/views/admin/UserAdminView.vue`

- [ ] **Step 1: Add reset function**

Insert after `const form = reactive(...)`:

```ts
function resetForm() {
  editingId.value = null;
  Object.assign(form, {
    username: '',
    displayName: '',
    department: '',
    password: '',
    role: 'EMPLOYEE' as Role,
    enabled: true
  });
}
```

- [ ] **Step 2: Add title action button**

Replace the current title line:

```vue
<h1>用户管理</h1>
```

with:

```vue
<div class="admin-title-row">
  <h1>用户管理</h1>
  <button data-test="new-user" type="button" @click="resetForm">新增用户</button>
</div>
```

- [ ] **Step 3: Add title row style**

Insert before `.admin-form` in the scoped style block:

```css
.admin-title-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 16px; }
.admin-title-row h1 { margin: 0; }
```

- [ ] **Step 4: Run target tests**

Run:

```bash
npm test --prefix "F:/Code/报销/frontend" -- admin-batch.spec.ts
```

Expected: user reset test passes; category notice/reset tests still fail until Task 3 is implemented.

---

### Task 3: Implement category reset button and notices

**Files:**
- Modify: `frontend/src/views/admin/CategoryAdminView.vue`

- [ ] **Step 1: Add notice state and reset function**

Insert after `const editingId = ref<number | null>(null);`:

```ts
const notice = ref<{ type: 'success' | 'error'; text: string } | null>(null);
```

Insert after `const form = reactive(...)`:

```ts
function resetForm() {
  editingId.value = null;
  Object.assign(form, { name: '', enabled: true, sortOrder: 0, remark: '' });
}

function errorMessage(err: unknown) {
  if (typeof err === 'object' && err && 'response' in err) {
    const response = (err as { response?: { data?: { message?: string } } }).response;
    if (response?.data?.message) return response.data.message;
  }
  return '分类保存失败';
}
```

- [ ] **Step 2: Wrap category save with success/error notice**

Replace the current `save()` function with:

```ts
async function save() {
  notice.value = null;
  const payload = { name: form.name, enabled: form.enabled, sortOrder: Number(form.sortOrder), remark: form.remark };
  try {
    if (editingId.value) await updateCategory(editingId.value, payload);
    else await createCategory(payload);
    await load();
    notice.value = { type: 'success', text: '分类保存成功' };
  } catch (err) {
    notice.value = { type: 'error', text: errorMessage(err) };
  }
}
```

- [ ] **Step 3: Add title action and notice markup**

Replace:

```vue
<h1>分类管理</h1>
```

with:

```vue
<div class="admin-title-row">
  <h1>分类管理</h1>
  <button data-test="new-category" type="button" @click="resetForm">新增分类</button>
</div>
<p v-if="notice" :class="['notice', notice.type]" :role="notice.type === 'error' ? 'alert' : 'status'">{{ notice.text }}</p>
```

- [ ] **Step 4: Add title row and notice styles**

Insert before `.admin-form` in the scoped style block:

```css
.admin-title-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 16px; }
.admin-title-row h1 { margin: 0; }
.notice { margin: 0 0 12px; padding: 10px 12px; border-radius: 8px; font-weight: 700; }
.notice.success { background: #dcfce7; color: #166534; }
.notice.error { background: #fee2e2; color: #991b1b; }
```

- [ ] **Step 5: Run target tests**

Run:

```bash
npm test --prefix "F:/Code/报销/frontend" -- admin-batch.spec.ts
```

Expected: PASS for all admin view tests.

---

### Task 4: Verify and record results

**Files:**
- Modify: `tasks/todo.md`

- [ ] **Step 1: Run frontend full test suite**

Run:

```bash
npm test --prefix "F:/Code/报销/frontend"
```

Expected: PASS for all frontend tests.

- [ ] **Step 2: Run frontend build**

Run:

```bash
npm run build --prefix "F:/Code/报销/frontend"
```

Expected: exit code 0. Existing Rolldown pure annotation or chunk-size warnings are acceptable if unchanged.

- [ ] **Step 3: Browser smoke test**

Start the dev server:

```bash
npm run dev --prefix "F:/Code/报销/frontend"
```

In browser:

1. Open `/login`.
2. Log in as an admin account available in the local environment.
3. Go to 用户管理.
4. Click an existing row's 编辑 button.
5. Click 新增用户.
6. Confirm username, name, department, password are empty, role is 员工, enabled is checked.
7. Go to 分类管理.
8. Click an existing row's 编辑 button.
9. Click 新增分类.
10. Confirm name and remark are empty, sort order is `0`, enabled is checked.

Expected: both pages switch back to create mode without page reload.

- [ ] **Step 4: Update execution tracker**

Append this section to `tasks/todo.md`:

```markdown
## 当前修复计划：管理端新增入口与保存提示

- [x] RED：补充用户/分类编辑后切回新增态、分类保存成功提示测试。
- [x] GREEN：用户管理新增“新增用户”按钮，重置表单并切回新增模式。
- [x] GREEN：分类管理新增“新增分类”按钮，重置表单并增加成功/失败提示。
- [x] 验证：运行前端目标测试、前端全量测试、前端构建，并完成浏览器冒烟验收。
- [x] 未提交，未推送。
```

---

## Self-Review

- Spec coverage: covered新增用户、新增分类、编辑后切回新增态、保存成功/失败提示、测试和浏览器验收。
- Placeholder scan: no TBD/TODO/fill-in-later instructions.
- Type consistency: `Role` already imported in `UserAdminView.vue`; category `notice` type matches existing user notice pattern; `data-test` selectors match tests and implementation.

## Execution Note

Do not create a git commit unless the user explicitly asks for one.
