<script setup lang="ts">
const props = defineProps<{
  title: string;
  description: string;
}>();
</script>

<template>
  <section class="empty-state enterprise-card">
    <div class="empty-state__icon" aria-hidden="true">∅</div>
    <h2>{{ props.title }}</h2>
    <p>{{ props.description }}</p>
    <div class="empty-state__action">
      <slot name="action" />
    </div>
  </section>
</template>

<style scoped>
.empty-state {
  display: grid;
  justify-items: center;
  gap: var(--space-3);
  padding: var(--space-8);
  text-align: center;
}

.empty-state__icon {
  display: grid;
  place-items: center;
  width: 56px;
  height: 56px;
  border-radius: 999px;
  background: var(--color-surface-muted);
  color: var(--color-text-subtle);
  font-size: 2rem;
  font-weight: 700;
}

.empty-state h2,
.empty-state p {
  margin: 0;
}

.empty-state p {
  color: var(--color-text-muted);
}
</style>
