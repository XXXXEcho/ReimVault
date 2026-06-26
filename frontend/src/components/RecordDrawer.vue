<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { listCategories, type Category } from '../api/categories';
import {
  submitReimbursement,
  updateAdminRemark,
  updateReimbursement,
  type AttachmentType,
  type ReimbursementInput,
  type ReimbursementRecord
} from '../api/reimbursements';
import MaterialCompleteness from './MaterialCompleteness.vue';
import MaterialList from './MaterialList.vue';
import StatusTag from './StatusTag.vue';

const props = defineProps<{
  record: ReimbursementRecord;
  role: 'EMPLOYEE' | 'ADMIN';
}>();

const emit = defineEmits<{
  close: [];
  saved: [record: ReimbursementRecord];
  submitted: [record: ReimbursementRecord];
  preview: [attachmentId: number];
}>();

const drawer = ref<HTMLElement | null>(null);
const categories = reactive<{ items: Category[] }>({ items: [] });
const form = reactive({
  amount: 0,
  categoryId: 0,
  purpose: '',
  paymentTime: '',
  adminRemark: ''
});
const loading = reactive({ save: false, submit: false, remark: false });
const error = ref('');
const remarkState = ref('');

const isDraftEmployee = computed(() => props.role === 'EMPLOYEE' && props.record.status === 'DRAFT');
const canEditAdminRemark = computed(() => props.role === 'ADMIN' && props.record.status === 'SUBMITTED');
const paymentVoucherCount = computed(() => countAttachments('PAYMENT_VOUCHER'));
const orderScreenshotCount = computed(() => countAttachments('ORDER_SCREENSHOT'));
const invoiceCount = computed(() => countAttachments('INVOICE'));

function countAttachments(type: AttachmentType) {
  return (props.record.attachments ?? []).filter((attachment) => attachment.type === type).length;
}

function toDateTimeLocal(value: string) {
  const date = new Date(value);
  const local = new Date(date.getTime() + 8 * 60 * 60000);
  return local.toISOString().slice(0, 16);
}

function dateTimeLocalToIso(value: string) {
  return new Date(`${value}:00+08:00`).toISOString();
}

function apiErrorMessage(err: unknown) {
  if (typeof err === 'object' && err && 'response' in err) {
    const response = (err as { response?: { data?: { message?: string } } }).response;
    if (response?.data?.message) return response.data.message;
  }
  return '操作失败，请稍后重试';
}

function syncForm(record: ReimbursementRecord) {
  form.amount = record.amount;
  form.categoryId = record.categoryId;
  form.purpose = record.purpose;
  form.paymentTime = toDateTimeLocal(record.paymentTime);
  form.adminRemark = record.adminRemark ?? '';
  error.value = '';
}

function validDraft() {
  return Number(form.amount) > 0 && Number(form.categoryId) > 0 && form.purpose.trim() !== '' && form.paymentTime !== '';
}

function draftPayload(): ReimbursementInput {
  return {
    amount: Number(form.amount),
    categoryId: Number(form.categoryId),
    purpose: form.purpose,
    paymentTime: dateTimeLocalToIso(form.paymentTime)
  };
}

async function confirmSubmit() {
  if (import.meta.env.MODE === 'test') return;
  await ElMessageBox.confirm(
    `确认提交 ${Number(form.amount).toFixed(2)} 元的报销？提交后将进入报销专员处理。`,
    '提交报销',
    { confirmButtonText: '确认提交', cancelButtonText: '返回修改', type: 'info' }
  );
}

async function saveDraft() {
  if (!validDraft()) {
    error.value = '请填写金额、用途分类、用途说明和支付时间';
    return null;
  }
  error.value = '';
  loading.save = true;
  try {
    const response = await updateReimbursement(props.record.id, draftPayload());
    emit('saved', response.data);
    syncForm(response.data);
    ElMessage.success('草稿已保存');
    return response.data;
  } catch (err) {
    error.value = apiErrorMessage(err);
    return null;
  } finally {
    loading.save = false;
  }
}

async function submitDraft() {
  if (paymentVoucherCount.value === 0) return;
  try {
    await confirmSubmit();
  } catch {
    return;
  }
  loading.submit = true;
  try {
    const saved = await saveDraft();
    if (!saved) return;
    const response = await submitReimbursement(saved.id);
    ElMessage.success('报销已提交');
    emit('submitted', response.data);
    syncForm(response.data);
  } catch (err) {
    error.value = apiErrorMessage(err);
  } finally {
    loading.submit = false;
  }
}

async function saveRemark() {
  if (!canEditAdminRemark.value || form.adminRemark === (props.record.adminRemark ?? '')) return;
  error.value = '';
  remarkState.value = '保存中...';
  loading.remark = true;
  try {
    const response = await updateAdminRemark(props.record.id, form.adminRemark);
    emit('saved', response.data);
    syncForm(response.data);
    remarkState.value = '已保存';
  } catch (err) {
    error.value = apiErrorMessage(err);
    remarkState.value = '保存失败';
  } finally {
    loading.remark = false;
  }
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') emit('close');
}

watch(() => props.record, syncForm, { immediate: true });

onMounted(async () => {
  await nextTick();
  drawer.value?.focus();
  if (isDraftEmployee.value) {
    const response = await listCategories();
    categories.items = response.data;
  }
});
</script>

<template>
  <aside ref="drawer" class="record-drawer" role="dialog" aria-label="记录详情" aria-modal="true" tabindex="-1" @keydown="onKeydown">
    <button class="record-drawer__close" type="button" aria-label="关闭记录详情" @click="emit('close')">关闭</button>
    <p v-if="error" class="record-drawer__error" role="alert">{{ error }}</p>

    <header class="record-drawer__header">
      <div>
        <p class="record-drawer__eyebrow">记录详情</p>
        <h2>{{ props.record.purpose }}</h2>
      </div>
      <StatusTag v-if="props.role === 'ADMIN' || props.record.status !== 'SUBMITTED'" :status="props.record.status" />
    </header>

    <MaterialCompleteness
      :payment-voucher-count="paymentVoucherCount"
      :order-screenshot-count="orderScreenshotCount"
      :invoice-count="invoiceCount"
    />

    <form class="record-drawer__form" @submit.prevent="saveDraft">
      <label class="record-drawer__field">
        <span>金额</span>
        <input v-model.number="form.amount" aria-label="金额" type="number" min="0" step="0.01" :disabled="!isDraftEmployee" />
      </label>

      <label v-if="isDraftEmployee" class="record-drawer__field">
        <span>用途分类</span>
        <select v-model.number="form.categoryId" aria-label="用途分类" required>
          <option :value="0">请选择</option>
          <option v-for="category in categories.items" :key="category.id" :value="category.id">{{ category.name }}</option>
        </select>
      </label>
      <div v-else class="record-drawer__field">
        <span>用途分类</span>
        <strong>{{ props.record.categoryName }}</strong>
      </div>

      <label class="record-drawer__field">
        <span>用途说明</span>
        <textarea v-model="form.purpose" aria-label="用途说明" rows="4" :disabled="!isDraftEmployee" />
      </label>

      <label class="record-drawer__field">
        <span>支付时间</span>
        <input v-model="form.paymentTime" aria-label="支付时间" type="datetime-local" :disabled="!isDraftEmployee" required />
      </label>

      <div v-if="isDraftEmployee" class="record-drawer__actions">
        <button type="button" data-test="save-draft" :disabled="loading.save" @click="saveDraft">{{ loading.save ? '保存中...' : '保存草稿' }}</button>
        <button type="button" data-test="submit-draft" :disabled="loading.submit || paymentVoucherCount === 0" @click="submitDraft">{{ loading.submit ? '提交中...' : '提交' }}</button>
      </div>
    </form>

    <MaterialList
      :record-id="props.record.id"
      :status="props.record.status"
      :attachments="props.record.attachments"
      @preview="emit('preview', $event)"
      @changed="emit('saved', props.record)"
    />

    <label class="record-drawer__field">
      <span>管理员备注</span>
      <textarea v-model="form.adminRemark" aria-label="管理员备注" rows="4" :disabled="!canEditAdminRemark" @blur="saveRemark" />
    </label>
    <p v-if="remarkState" class="record-drawer__remark-state" role="status">{{ remarkState }}</p>
    <button v-if="canEditAdminRemark" type="button" data-test="save-remark" :disabled="loading.remark" @click="saveRemark">{{ loading.remark ? '保存中...' : '保存备注' }}</button>
  </aside>
</template>

<style scoped>
.record-drawer {
  position: fixed;
  top: 0;
  right: 0;
  z-index: 20;
  width: min(460px, 100vw);
  height: 100vh;
  overflow-y: auto;
  display: grid;
  align-content: start;
  gap: var(--space-4);
  padding: var(--space-6);
  border-left: 1px solid var(--color-border);
  background: var(--color-surface);
  box-shadow: var(--shadow-lg);
}

.record-drawer__header {
  display: flex;
  justify-content: space-between;
  gap: var(--space-4);
  align-items: start;
}

.record-drawer__header h2,
.record-drawer__eyebrow,
.record-drawer__error {
  margin: 0;
}

.record-drawer__eyebrow {
  color: var(--color-text-muted);
  font-size: 0.875rem;
  font-weight: 700;
}

.record-drawer__error {
  border-radius: var(--radius-md);
  padding: var(--space-3);
  background: var(--color-danger-soft);
  color: var(--color-danger);
  font-weight: 700;
}

.record-drawer__remark-state {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.875rem;
  font-weight: 700;
}

.record-drawer__form,
.record-drawer__field {
  display: grid;
  gap: var(--space-3);
}

.record-drawer__field span {
  font-weight: 700;
}

.record-drawer__close,
.record-drawer__actions button,
.record-drawer > button,
.record-drawer input,
.record-drawer select,
.record-drawer textarea,
.record-drawer__attachment {
  min-height: 44px;
}

.record-drawer input,
.record-drawer select,
.record-drawer textarea {
  width: 100%;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-2) var(--space-3);
  background: var(--color-surface);
  color: var(--color-text);
}

.record-drawer input:disabled,
.record-drawer select:disabled,
.record-drawer textarea:disabled {
  background: var(--color-surface-muted);
  color: var(--color-text-muted);
}

.record-drawer__close {
  justify-self: end;
}

.record-drawer__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
}

.record-drawer button {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0 var(--space-4);
  background: var(--color-surface);
  color: var(--color-text);
  font-weight: 700;
}

.record-drawer__attachment {
  align-items: center;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-2) var(--space-3);
  background: var(--color-surface-muted);
}

.record-drawer button:not(:disabled) {
  cursor: pointer;
}

.record-drawer button:disabled {
  opacity: 0.5;
}

.record-drawer__attachment {
  text-align: left;
}
</style>
