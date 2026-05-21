<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import {
  deleteAttachment,
  uploadAttachment,
  type AttachmentRecord,
  type AttachmentType,
  type ReimbursementStatus
} from '../api/reimbursements';

type MaterialAttachment = Omit<AttachmentRecord, 'type'> & { type: AttachmentType | string };

const props = defineProps<{
  recordId: number;
  status: ReimbursementStatus;
  attachments: MaterialAttachment[];
}>();

const emit = defineEmits<{
  preview: [attachmentId: number];
  changed: [];
}>();

const groups: { type: AttachmentType; label: string; required: boolean }[] = [
  { type: 'PAYMENT_VOUCHER', label: '支付凭证', required: true },
  { type: 'ORDER_SCREENSHOT', label: '订单截图', required: false },
  { type: 'INVOICE', label: '发票', required: false }
];

const isDraft = computed(() => props.status === 'DRAFT');
const error = ref('');
const loading = reactive<Record<string, boolean>>({});

function attachmentsByType(type: AttachmentType) {
  return (props.attachments ?? []).filter((attachment) => attachment.type === type);
}

function apiErrorMessage(err: unknown) {
  if (typeof err === 'object' && err && 'response' in err) {
    const response = (err as { response?: { data?: { message?: string } } }).response;
    if (response?.data?.message) return response.data.message;
  }
  return '操作失败，请稍后重试';
}

function uploadKey(type: AttachmentType) {
  return `upload-${type}`;
}

function deleteKey(attachmentId: number) {
  return `delete-${attachmentId}`;
}

async function onUpload(type: AttachmentType, event: Event) {
  const input = event.target as HTMLInputElement;
  const files = Array.from(input.files ?? []);
  if (!files.length || loading[uploadKey(type)]) return;

  error.value = '';
  loading[uploadKey(type)] = true;
  try {
    for (const file of files) {
      await uploadAttachment(props.recordId, type, file);
    }
    emit('changed');
  } catch (err) {
    error.value = apiErrorMessage(err);
  } finally {
    loading[uploadKey(type)] = false;
    input.value = '';
  }
}

async function onDelete(attachmentId: number) {
  if (loading[deleteKey(attachmentId)] || !window.confirm('确认删除该附件？')) return;
  error.value = '';
  loading[deleteKey(attachmentId)] = true;
  try {
    await deleteAttachment(attachmentId);
    emit('changed');
  } catch (err) {
    error.value = apiErrorMessage(err);
  } finally {
    loading[deleteKey(attachmentId)] = false;
  }
}
</script>

<template>
  <section class="material-list" aria-label="材料列表">
    <p v-if="error" class="material-list__error" role="alert">{{ error }}</p>
    <div v-for="group in groups" :key="group.type" class="material-list__group">
      <header class="material-list__header">
        <div>
          <h3>{{ group.label }}</h3>
          <p v-if="group.required && attachmentsByType(group.type).length === 0" class="material-list__required">缺少必填支付凭证</p>
          <p v-else-if="attachmentsByType(group.type).length === 0" class="material-list__empty">暂无材料</p>
        </div>
        <label v-if="isDraft" class="material-list__upload" :class="{ 'material-list__upload--disabled': loading[uploadKey(group.type)] }" :aria-label="`上传${group.label}`">
          <span>{{ loading[uploadKey(group.type)] ? '上传中...' : '上传' }}</span>
          <input
            :data-test="`upload-${group.type}`"
            :aria-label="`上传${group.label}`"
            :disabled="loading[uploadKey(group.type)]"
            type="file"
            accept="image/png,image/jpeg,image/webp,application/pdf,.png,.jpg,.jpeg,.webp,.pdf"
            multiple
            @change="onUpload(group.type, $event)"
          />
        </label>
      </header>

      <span
        v-for="attachment in attachmentsByType(group.type)"
        :key="attachment.id"
        class="record-drawer__attachment material-list__item"
      >
        <span class="material-list__meta">
          <strong>{{ attachment.originalFilename }}</strong>
          <small>{{ attachment.contentType }} · {{ attachment.sizeBytes }} bytes</small>
        </span>
        <span class="material-list__actions">
          <button type="button" :data-test="`preview-${attachment.id}`" @click="emit('preview', attachment.id)">预览</button>
          <a :href="`/api/attachments/${attachment.id}`" target="_blank" rel="noopener">下载</a>
          <button v-if="isDraft" type="button" :data-test="`delete-${attachment.id}`" :disabled="loading[deleteKey(attachment.id)]" @click="onDelete(attachment.id)">{{ loading[deleteKey(attachment.id)] ? '删除中...' : '删除' }}</button>
        </span>
      </span>
    </div>
  </section>
</template>

<style scoped>
.material-list,
.material-list__group {
  display: grid;
  gap: var(--space-3);
}

.material-list__header,
.material-list__item,
.material-list__actions {
  display: flex;
  gap: var(--space-3);
}

.material-list__header,
.material-list__item {
  align-items: center;
  justify-content: space-between;
}

.material-list h3,
.material-list p {
  margin: 0;
}

.material-list__required {
  color: var(--color-danger);
  font-weight: 700;
}

.material-list__error {
  margin: 0;
  border-radius: var(--radius-md);
  padding: var(--space-3);
  background: var(--color-danger-soft);
  color: var(--color-danger);
  font-weight: 700;
}

.material-list__empty,
.material-list__meta small {
  color: var(--color-text-muted);
}

.material-list__upload,
.material-list__actions button,
.material-list__actions a {
  min-height: 44px;
}

.material-list__upload,
.material-list__actions a {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.material-list__upload {
  position: relative;
  overflow: hidden;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0 var(--space-4);
  background: var(--color-surface);
  font-weight: 700;
  cursor: pointer;
}

.material-list__upload input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.material-list__upload--disabled,
.material-list__actions button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.material-list__item {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-2) var(--space-3);
  background: var(--color-surface-muted);
}

.material-list__meta {
  display: grid;
  gap: var(--space-1);
}

.material-list__actions {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.material-list__actions button,
.material-list__actions a {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0 var(--space-3);
  background: var(--color-surface);
  color: var(--color-text);
  font-weight: 700;
  text-decoration: none;
}

.material-list__actions button:not(:disabled) {
  cursor: pointer;
}
</style>
