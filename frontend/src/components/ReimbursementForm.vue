<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import AttachmentUploader from './AttachmentUploader.vue';
import { listCategories, type Category } from '../api/categories';
import { createReimbursement, getReimbursement, submitReimbursement, updateReimbursement, uploadAttachment, type AttachmentType, type ReimbursementAttachment, type ReimbursementRecord } from '../api/reimbursements';

const props = defineProps<{ id?: number }>();
const emit = defineEmits<{ saved: [ReimbursementRecord]; submitted: [ReimbursementRecord] }>();

const categories = ref<Category[]>([]);
const recordId = ref<number | null>(props.id ?? null);
const recordStatus = ref<string | null>(null);
const attachments = ref<ReimbursementAttachment[]>([]);
const error = ref('');

const readonly = computed(() => recordStatus.value != null && recordStatus.value !== 'DRAFT');
const form = reactive({
  amount: 0,
  categoryId: 0,
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
    purpose: form.purpose,
    paymentTime: new Date(form.paymentTime).toISOString()
  };
}

function toDateTimeLocal(value: string) {
  const date = new Date(value);
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 16);
}

function isValid() {
  return Number(form.amount) > 0 && Number(form.categoryId) > 0 && form.purpose.trim() !== '' && form.paymentTime !== '';
}

function attachmentsByType(type: AttachmentType) {
  return attachments.value.filter((attachment) => attachment.type === type);
}

function addAttachment(attachment: ReimbursementAttachment) {
  attachments.value.push(attachment);
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
  if (!isValid()) {
    error.value = '请填写金额、用途分类、用途说明和支付时间';
    return null;
  }
  error.value = '';
  try {
    const response = recordId.value
      ? await updateReimbursement(recordId.value, payload())
      : await createReimbursement(payload());
    recordId.value = response.data.id;
    await uploadSelectedAttachments(response.data.id);
    emit('saved', response.data);
    return response.data;
  } catch (err) {
    error.value = apiErrorMessage(err);
    return null;
  }
}

async function submitRecord() {
  if (!recordId.value && form.paymentVoucherFiles.length === 0) {
    error.value = '请先选择至少一张支付凭证';
    return;
  }
  const draft = await saveDraft();
  const id = draft?.id ?? recordId.value;
  if (!id) return;
  try {
    const response = await submitReimbursement(id);
    emit('submitted', response.data);
  } catch (err) {
    error.value = apiErrorMessage(err);
  }
}

onMounted(async () => {
  const categoryResponse = await listCategories();
  categories.value = categoryResponse.data;
  if (props.id) {
    const response = await getReimbursement(props.id);
    recordStatus.value = response.data.status;
    Object.assign(form, {
      amount: response.data.amount,
      categoryId: response.data.categoryId,
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
    <label>金额<input aria-label="金额" v-model="form.amount" type="number" min="0" step="0.01" required :disabled="readonly" /></label>
    <label>用途分类
      <select aria-label="用途分类" v-model.number="form.categoryId" required :disabled="readonly">
        <option :value="0">请选择</option>
        <option v-for="category in categories" :key="category.id" :value="category.id">{{ category.name }}</option>
      </select>
    </label>
    <label>用途说明<input aria-label="用途说明" v-model="form.purpose" required :disabled="readonly" /></label>
    <label>支付时间<input aria-label="支付时间" v-model="form.paymentTime" type="datetime-local" required :disabled="readonly" /></label>
    <AttachmentUploader v-model="form.paymentVoucherFiles" label="支付凭证" required attachment-type="PAYMENT_VOUCHER" :record-id="recordId" :attachments="attachmentsByType('PAYMENT_VOUCHER')" :readonly="readonly" data-test="payment-voucher-files" @uploaded="addAttachment" />
    <AttachmentUploader v-model="form.orderScreenshotFiles" label="订单截图" attachment-type="ORDER_SCREENSHOT" :record-id="recordId" :attachments="attachmentsByType('ORDER_SCREENSHOT')" :readonly="readonly" data-test="order-screenshot-files" @uploaded="addAttachment" />
    <AttachmentUploader v-model="form.invoiceFiles" label="发票" attachment-type="INVOICE" :record-id="recordId" :attachments="attachmentsByType('INVOICE')" :readonly="readonly" data-test="invoice-files" @uploaded="addAttachment" />
    <div v-if="!readonly" class="actions">
      <button type="button" data-test="save-draft" @click="saveDraft">保存草稿</button>
      <button type="button" data-test="submit-reimbursement" @click="submitRecord">提交</button>
    </div>
  </form>
</template>

<style scoped>
.reimbursement-form { display: grid; gap: 14px; max-width: 720px; }
label { display: grid; gap: 6px; }
input, select { padding: 8px; border: 1px solid #ccc; border-radius: 4px; }
.actions { display: flex; gap: 12px; }
.error { color: #b00020; margin: 0; }
</style>
