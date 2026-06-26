<script setup lang="ts">
import type { ReimbursementStatus } from '../api/reimbursements';

const props = defineProps<{ status: ReimbursementStatus }>();

const labels: Record<ReimbursementStatus, string> = {
  DRAFT: '草稿',
  SUBMITTED: '待报销',
  ARCHIVED: '已归档'
};
</script>

<template>
  <span class="status-tag" :class="`status-${props.status.toLowerCase()}`">{{ labels[props.status] }}</span>
</template>

<style scoped>
.status-tag {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  border-radius: 999px;
  padding: 2px var(--space-2);
  font-size: 0.875rem;
  font-weight: 700;
}

.status-draft {
  background: var(--color-surface-muted);
  color: var(--color-text-muted);
}

.status-submitted {
  background: var(--color-primary-soft);
  color: var(--color-primary-strong);
}

.status-archived {
  background: var(--color-success-soft);
  color: var(--color-success);
}
</style>
