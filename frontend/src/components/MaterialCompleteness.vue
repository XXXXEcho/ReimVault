<script setup lang="ts">
const props = defineProps<{
  paymentVoucherCount: number;
  orderScreenshotCount: number;
  invoiceCount: number;
}>();
</script>

<template>
  <div class="material-completeness" :class="{ 'is-incomplete': props.paymentVoucherCount === 0 }">
    <span>{{ props.paymentVoucherCount === 0 ? '支付凭证缺失' : `支付凭证 ${props.paymentVoucherCount}` }}</span>
    <span>订单截图 {{ props.orderScreenshotCount }}</span>
    <span>发票 {{ props.invoiceCount }}</span>
  </div>
</template>

<style scoped>
.material-completeness {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  color: var(--color-text-muted);
}

.material-completeness span {
  border-radius: 999px;
  padding: 2px var(--space-2);
  background: var(--color-surface-muted);
}

.material-completeness.is-incomplete span:first-child {
  background: var(--color-danger-soft);
  color: var(--color-danger);
  font-weight: 700;
}
</style>
