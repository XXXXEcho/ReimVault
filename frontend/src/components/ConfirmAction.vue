<script setup lang="ts">
const props = defineProps<{ message: string }>();
const emit = defineEmits<{ confirm: [] }>();

function confirmAction() {
  if (window.confirm(props.message)) {
    emit('confirm');
  }
}
</script>

<template>
  <button type="button" class="confirm-action" @click="confirmAction">
    <slot />
  </button>
</template>

<style scoped>
.confirm-action {
  display: inline-flex;
  min-height: 44px;
  align-items: center;
  justify-content: center;
  border: 0;
  padding: 0;
  background: transparent;
  color: inherit;
}
</style>
