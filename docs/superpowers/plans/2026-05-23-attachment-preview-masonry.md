# Attachment Preview Masonry Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make reimbursement attachment previews use a masonry-like adaptive layout and a larger image preview dialog.

**Architecture:** Keep all behavior inside `AttachmentUploader.vue`; no API changes. Add CSS-driven masonry layout with content-sized preview cards and enlarge the existing dialog. Cover the behavior with component tests in `reimbursement-form.spec.ts`.

**Tech Stack:** Vue 3, TypeScript, Vitest, Vue Test Utils, CSS columns/grid.

---

### Task 1: Attachment Preview Layout

**Files:**
- Modify: `frontend/src/components/AttachmentUploader.vue`
- Test: `frontend/tests/reimbursement-form.spec.ts`

- [ ] **Step 1: Write failing tests**

Add tests asserting the preview list uses masonry layout, preview cards expose adaptive image structure, and the large preview panel uses the expanded dialog class.

- [ ] **Step 2: Run target test to verify RED**

Run: `npm test --prefix "F:/Code/报销/frontend" -- reimbursement-form.spec.ts`

Expected: FAIL because masonry/expanded classes are absent.

- [ ] **Step 3: Implement minimal layout change**

In `AttachmentUploader.vue`, update markup/classes only where needed:
- preview list remains one container but gets masonry CSS behavior.
- image buttons/cards use content-sized cards.
- thumbnails use `max-width`, `max-height`, `width: 100%`, `height: auto`, `object-fit: contain`.
- dialog panel grows to `min(1280px, 94vw)` and image maxes at `88vh`.

- [ ] **Step 4: Run target test to verify GREEN**

Run: `npm test --prefix "F:/Code/报销/frontend" -- reimbursement-form.spec.ts`

Expected: PASS.

- [ ] **Step 5: Run frontend build**

Run: `npm run build --prefix "F:/Code/报销/frontend"`

Expected: exit 0; existing Rolldown warnings are acceptable.

- [ ] **Step 6: Browser verify**

Open the deployed/local reimbursement form in Chrome DevTools, confirm the preview area has no large empty rows and the large preview is visibly bigger.
