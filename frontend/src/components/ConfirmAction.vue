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
  <span class="confirm-action" @click="confirmAction">
    <slot />
  </span>
</template>

<style scoped>
.confirm-action {
  display: inline-flex;
}
</style>
