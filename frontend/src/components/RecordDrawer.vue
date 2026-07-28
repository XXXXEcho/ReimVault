<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { listCategories, type Category } from '../api/categories';
import { listOaNumbers, type OaNumber } from '../api/oa';
import {
  formatTime,
  submitReimbursement,
  updateAdminRemark,
  updateOaNumber,
  updateReimbursement,
  withdrawReimbursement,
  type AttachmentType,
  type ReimbursementInput,
  type ReimbursementRecord
} from '../api/reimbursements';
import MaterialCompleteness from './MaterialCompleteness.vue';
import MaterialList from './MaterialList.vue';
import StatusTag from './StatusTag.vue';

const props = defineProps<{
  record: ReimbursementRecord;
  role: 'EMPLOYEE' | 'SPECIALIST' | 'ADMIN';
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
const oaNumbers = ref<OaNumber[]>([]);
const selectedOaId = ref<number | null>(null);
const oaLoading = ref(false);

const isManagement = computed(() => props.role === 'ADMIN' || props.role === 'SPECIALIST');
const isDraftEmployee = computed(() => props.role === 'EMPLOYEE' && props.record.status === 'DRAFT');
const canEditAdminRemark = computed(() => isManagement.value && props.record.status === 'SUBMITTED');
const canEditOa = computed(() => isManagement.value && props.record.status === 'SUBMITTED');
const canWithdraw = computed(() => props.role === 'EMPLOYEE' && props.record.status === 'SUBMITTED');
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

async function withdraw() {
  try {
    await ElMessageBox.confirm('确认撤回此报销？记录将退回草稿，可重新修改后提交。', '撤回报销', {
      confirmButtonText: '确认撤回',
      cancelButtonText: '取消',
      type: 'warning'
    });
  } catch { return; }
  error.value = '';
  try {
    const response = await withdrawReimbursement(props.record.id);
    ElMessage.success('已撤回，可重新编辑');
    emit('submitted', response.data);
    syncForm(response.data);
  } catch (err) {
    error.value = apiErrorMessage(err);
  }
}

async function saveOa() {
  error.value = '';
  oaLoading.value = true;
  try {
    const response = await updateOaNumber(props.record.id, selectedOaId.value);
    ElMessage.success('经费编码已更新');
    emit('saved', response.data);
    syncForm(response.data);
  } catch (err) {
    error.value = apiErrorMessage(err);
  } finally {
    oaLoading.value = false;
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
  if (isManagement.value) {
    const [oaResponse, catResponse] = await Promise.all([listOaNumbers(), listCategories()]);
    oaNumbers.value = oaResponse.data;
    selectedOaId.value = props.record.oaId ?? null;
    if (!isDraftEmployee.value) categories.items = catResponse.data;
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
      <StatusTag v-if="isManagement || props.record.status !== 'SUBMITTED'" :status="props.record.status" />
    </header>

    <dl class="record-drawer__meta" data-test="record-meta">
      <div>
        <dt>员工</dt>
        <dd>{{ props.record.employeeName }}</dd>
      </div>
      <div>
        <dt>经费编码</dt>
        <dd>{{ props.record.oaNumber || '—' }}</dd>
      </div>
      <div>
        <dt>提交时间</dt>
        <dd>{{ formatTime(props.record.submittedAt) }}</dd>
      </div>
      <div>
        <dt>报销时间</dt>
        <dd>{{ formatTime(props.record.reimbursedAt) }}</dd>
      </div>
    </dl>

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

    <div v-if="canWithdraw" class="record-drawer__actions record-drawer__withdraw">
      <button type="button" class="ghost-btn" @click="withdraw">撤回修改</button>
    </div>

    <MaterialList
      :record-id="props.record.id"
      :status="props.record.status"
      :attachments="props.record.attachments"
      @preview="emit('preview', $event)"
      @changed="emit('saved', props.record)"
    />

    <div v-if="canEditOa" class="record-drawer__field">
      <span>经费编码</span>
      <div class="record-drawer__oa-row">
        <select v-model="selectedOaId" aria-label="经费编码" class="field-input">
          <option :value="null">未选择</option>
          <option v-for="oa in oaNumbers" :key="oa.id" :value="oa.id">{{ oa.number }}</option>
        </select>
        <button class="primary-btn" style="flex:none" :disabled="oaLoading" @click="saveOa">保存</button>
      </div>
    </div>

    <div class="record-drawer__field">
      <span>管理员备注</span>
      <textarea v-if="canEditAdminRemark" v-model="form.adminRemark" aria-label="管理员备注" rows="4" @blur="saveRemark" />
      <p v-else class="record-drawer__readonly-remark" data-test="admin-remark-readonly">{{ props.record.adminRemark || '—' }}</p>
    </div>
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
  width: min(620px, 100vw);
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

.record-drawer__meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-2) var(--space-5);
  margin: 0;
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface-muted);
}

.record-drawer__meta div {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.record-drawer__meta dt {
  color: var(--color-text-subtle);
  font-size: 0.75rem;
  font-weight: 700;
}

.record-drawer__meta dd {
  margin: 0;
  color: var(--color-text);
  font-size: 0.9375rem;
  font-weight: 600;
  word-break: break-word;
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

.record-drawer__oa-row {
  display: flex;
  gap: var(--space-2);
  align-items: center;
}

.record-drawer__withdraw {
  padding-top: 0;
}

.record-drawer__readonly-remark {
  margin: 0;
  min-height: 44px;
  padding: var(--space-2) var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface-muted);
  color: var(--color-text);
  font-size: 0.9375rem;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
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
