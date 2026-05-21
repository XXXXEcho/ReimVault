<script setup lang="ts">
const props = withDefaults(defineProps<{
  title: string;
  value: number | string;
  tone?: 'primary' | 'success' | 'warning' | 'danger';
}>(), {
  tone: 'primary'
});
</script>

<template>
  <article class="metric-card enterprise-card" :class="`metric-card--${props.tone}`">
    <p class="metric-card__title">{{ props.title }}</p>
    <strong class="metric-card__value">{{ props.value }}</strong>
  </article>
</template>

<style scoped>
.metric-card {
  display: grid;
  gap: var(--space-2);
  padding: var(--space-5);
  border-left: 4px solid var(--metric-tone);
}

.metric-card--primary {
  --metric-tone: var(--color-primary);
}

.metric-card--success {
  --metric-tone: var(--color-success);
}

.metric-card--warning {
  --metric-tone: var(--color-warning);
}

.metric-card--danger {
  --metric-tone: var(--color-danger);
}

.metric-card__title {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.875rem;
}

.metric-card__value {
  color: var(--color-text);
  font-size: 2rem;
  line-height: 1;
}
</style>
