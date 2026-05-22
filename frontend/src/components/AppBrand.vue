<template>
  <div class="app-brand">
    <span class="brand-mark">RV</span>
    <div>
      <strong class="brand-title">报销归档通</strong>
      <span class="brand-subtitle">ReimVault</span>
    </div>
  </div>
</template>

<style scoped>
.app-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-mark {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: 14px;
  background: linear-gradient(135deg, #3b82f6, #22c55e);
  color: #fff;
  font-weight: 800;
  letter-spacing: -0.04em;
  box-shadow: 0 12px 28px rgba(37, 99, 235, 0.24);
}

.brand-title,
.brand-subtitle {
  display: block;
  line-height: 1.2;
}

.brand-title {
  font-size: 17px;
  letter-spacing: 0.02em;
}

.brand-subtitle {
  margin-top: 3px;
  color: #94a3b8;
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}
</style>
