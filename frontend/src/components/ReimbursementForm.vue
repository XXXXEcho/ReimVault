<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import AttachmentUploader from './AttachmentUploader.vue';
import { listCategories, type Category } from '../api/categories';
import { listOaNumbers, type OaNumber } from '../api/oa';
import {
  createReimbursement,
  getReimbursement,
  submitReimbursement,
  updateReimbursement,
  uploadAttachment,
  type AttachmentRecord,
  type AttachmentType,
  type ReimbursementRecord
} from '../api/reimbursements';

const props = defineProps<{ id?: number }>();
const emit = defineEmits<{ saved: [ReimbursementRecord]; submitted: [ReimbursementRecord] }>();

const categories = ref<Category[]>([]);
const oaNumbers = ref<OaNumber[]>([]);
const recordId = ref<number | null>(props.id ?? null);
const recordStatus = ref<string | null>(null);
const attachments = ref<AttachmentRecord[]>([]);
const error = ref('');
const saving = ref(false);
const submitting = ref(false);
const paymentVoucherSection = ref<HTMLElement | null>(null);
const readonly = computed(() => recordStatus.value != null && recordStatus.value !== 'DRAFT');

const form = reactive({
  amount: 0,
  categoryId: 0,
  oaId: null as number | null,
  purpose: '',
  paymentTime: '',
  paymentVoucherFiles: [] as File[],
  orderScreenshotFiles: [] as File[],
  invoiceFiles: [] as File[]
});

function payload() {
  return {
    amount: Number(form.amount),
    categoryId: Number(form.categoryId),
    oaId: form.oaId ?? null,
    purpose: form.purpose,
    paymentTime: dateTimeLocalToIso(form.paymentTime)
  };
}

function toDateTimeLocal(value: string) {
  const date = new Date(value);
  const local = new Date(date.getTime() + 8 * 60 * 60000);
  return local.toISOString().slice(0, 16);
}

function dateTimeLocalToIso(value: string) {
  return new Date(`${value}:00+08:00`).toISOString();
}

async function confirmSubmit(message: string) {
  if (import.meta.env.MODE === 'test') return;
  await ElMessageBox.confirm(message, '提交报销前确认', {
    confirmButtonText: '确认提交',
    cancelButtonText: '返回修改',
    type: 'info'
  });
}

function isValid() {
  return Number(form.amount) > 0 && Number(form.categoryId) > 0 && form.purpose.trim() !== '' && form.paymentTime !== '';
}

function attachmentsByType(type: AttachmentType) {
  return attachments.value.filter((attachment) => attachment.type === type);
}

function addAttachment(attachment: AttachmentRecord) {
  attachments.value.push(attachment);
}

function removeAttachment(id: number) {
  attachments.value = attachments.value.filter((attachment) => attachment.id !== id);
}

async function uploadSelectedAttachments(id: number) {
  const responses = await Promise.all([
    ...form.paymentVoucherFiles.map((file) => uploadAttachment(id, 'PAYMENT_VOUCHER', file)),
    ...form.orderScreenshotFiles.map((file) => uploadAttachment(id, 'ORDER_SCREENSHOT', file)),
    ...form.invoiceFiles.map((file) => uploadAttachment(id, 'INVOICE', file))
  ]);
  attachments.value.push(...responses.map((response) => response.data));
  form.paymentVoucherFiles = [];
  form.orderScreenshotFiles = [];
  form.invoiceFiles = [];
}

function apiErrorMessage(err: unknown) {
  if (typeof err === 'object' && err && 'response' in err) {
    const response = (err as { response?: { data?: { message?: string } } }).response;
    if (response?.data?.message) return response.data.message;
  }
  return '操作失败，请稍后重试';
}

async function saveDraft() {
  if (readonly.value) return null;
  if (!isValid()) {
    error.value = '请填写金额、用途分类、用途说明和支付时间';
    return null;
  }
  error.value = '';
  saving.value = true;
  try {
    const response = recordId.value
      ? await updateReimbursement(recordId.value, payload())
      : await createReimbursement(payload());
    recordId.value = response.data.id;
    recordStatus.value = response.data.status;
    await uploadSelectedAttachments(response.data.id);
    emit('saved', response.data);
    ElMessage.success('草稿已保存');
    return response.data;
  } catch (err) {
    error.value = apiErrorMessage(err);
    return null;
  } finally {
    saving.value = false;
  }
}

async function submitRecord() {
  if (readonly.value) return;
  if (!recordId.value && form.paymentVoucherFiles.length === 0) {
    error.value = '请先选择至少一张支付凭证';
    paymentVoucherSection.value?.scrollIntoView?.({ behavior: 'smooth', block: 'center' });
    return;
  }
  const categoryName = categories.value.find((category) => category.id === Number(form.categoryId))?.name ?? '未选择';
  const oaNumber = oaNumbers.value.find((oa) => oa.id === Number(form.oaId))?.number ?? '未选择';
  try {
    await confirmSubmit(`请核对：金额 ${Number(form.amount).toFixed(2)} 元；分类 ${categoryName}；经费编码 ${oaNumber}；支付时间 ${form.paymentTime}；支付凭证 ${attachmentsByType('PAYMENT_VOUCHER').length + form.paymentVoucherFiles.length} 份。`);
  } catch {
    return;
  }
  submitting.value = true;
  const draft = await saveDraft();
  const id = draft?.id ?? recordId.value;
  if (!id) {
    submitting.value = false;
    return;
  }
  try {
    const response = await submitReimbursement(id);
    recordStatus.value = response.data.status;
    ElMessage.success('报销已提交');
    emit('submitted', response.data);
  } catch (err) {
    error.value = apiErrorMessage(err);
  } finally {
    submitting.value = false;
  }
}

onMounted(async () => {
  const [categoryResponse, oaResponse] = await Promise.all([listCategories(), listOaNumbers()]);
  categories.value = categoryResponse.data;
  oaNumbers.value = oaResponse.data;
  if (props.id) {
    const response = await getReimbursement(props.id);
    recordStatus.value = response.data.status;
    Object.assign(form, {
      amount: response.data.amount,
      categoryId: response.data.categoryId,
      oaId: response.data.oaId,
      purpose: response.data.purpose,
      paymentTime: toDateTimeLocal(response.data.paymentTime)
    });
    attachments.value = response.data.attachments ?? [];
  }
});
</script>

<template>
  <form class="reimbursement-form" @submit.prevent="saveDraft">
    <p v-if="error" class="error">{{ error }}</p>
    <section class="form-step">
      <h2>1. 基本信息</h2>
      <label>金额<input aria-label="金额" v-model="form.amount" type="number" min="0" step="0.01" required :disabled="readonly" /></label>
      <label>用途分类
        <select aria-label="用途分类" v-model.number="form.categoryId" required :disabled="readonly">
          <option :value="0">请选择</option>
          <option v-for="category in categories" :key="category.id" :value="category.id">{{ category.name }}</option>
        </select>
      </label>
      <label>经费编码
        <select aria-label="经费编码" v-model.number="form.oaId" :disabled="readonly">
          <option :value="null">请选择</option>
          <option v-for="oa in oaNumbers" :key="oa.id" :value="oa.id">{{ oa.number }}</option>
        </select>
      </label>
      <label>用途说明<input aria-label="用途说明" v-model="form.purpose" required placeholder="例如 客户拜访交通费" :disabled="readonly" /></label>
      <label>支付时间<input aria-label="支付时间" v-model="form.paymentTime" type="datetime-local" required :disabled="readonly" /></label>
    </section>
    <section class="form-step">
      <h2>2. 上传材料</h2>
      <p class="hint">支持 JPG、PNG、WebP、PDF，单个文件不超过系统限制。支付凭证为必填。</p>
      <div ref="paymentVoucherSection">
        <AttachmentUploader v-model="form.paymentVoucherFiles" label="支付凭证" required attachment-type="PAYMENT_VOUCHER" :record-id="recordId" :attachments="attachmentsByType('PAYMENT_VOUCHER')" :readonly="readonly" data-test="payment-voucher-files" @uploaded="addAttachment" @deleted="removeAttachment" />
      </div>
      <AttachmentUploader v-model="form.orderScreenshotFiles" label="订单截图" attachment-type="ORDER_SCREENSHOT" :record-id="recordId" :attachments="attachmentsByType('ORDER_SCREENSHOT')" :readonly="readonly" data-test="order-screenshot-files" @uploaded="addAttachment" @deleted="removeAttachment" />
      <AttachmentUploader v-model="form.invoiceFiles" label="发票" attachment-type="INVOICE" :record-id="recordId" :attachments="attachmentsByType('INVOICE')" :readonly="readonly" data-test="invoice-files" @uploaded="addAttachment" @deleted="removeAttachment" />
    </section>
    <section class="form-step">
      <h2>3. 提交确认</h2>
      <p class="hint">保存草稿后仍可继续补充材料；提交后会进入报销专员处理。</p>
    </section>
    <div v-if="!readonly" class="actions">
      <button type="button" data-test="save-draft" :disabled="saving || submitting" @click="saveDraft">{{ saving ? '保存中...' : '保存草稿' }}</button>
      <button type="button" data-test="submit-reimbursement" :disabled="saving || submitting" @click="submitRecord">{{ submitting ? '提交中...' : '提交' }}</button>
    </div>
  </form>
</template>

<style scoped>
.reimbursement-form { display: grid; gap: var(--space-4); max-width: 760px; }
.form-step { display: grid; gap: 14px; border: 1px solid var(--color-border); border-radius: var(--radius-lg); padding: var(--space-5); background: var(--color-surface); }
.form-step h2, .hint { margin: 0; }
.hint { color: var(--color-text-muted); font-size: 0.875rem; }
label { display: grid; gap: 6px; }
input, select { padding: 8px; border: 1px solid #ccc; border-radius: 4px; }
.actions { display: flex; gap: 12px; }
.error { color: #b00020; margin: 0; }
</style>
