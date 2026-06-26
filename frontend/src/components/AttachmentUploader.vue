<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import { deleteAttachment, uploadAttachment, type AttachmentRecord, type AttachmentType } from '../api/reimbursements';

const props = defineProps<{
  label: string;
  required?: boolean;
  recordId?: number | null;
  attachmentType?: AttachmentType;
  modelValue?: File[];
  attachments?: AttachmentRecord[];
  dataTest?: string;
  readonly?: boolean;
}>();

const emit = defineEmits<{
  'update:modelValue': [File[]];
  uploaded: [AttachmentRecord];
  deleted: [number];
}>();

const title = computed(() => `${props.label}${props.required ? '（必填）' : '（选填）'}`);
const localPreviews = ref<{ name: string; url: string; image: boolean }[]>([]);
const largePreview = ref<{ name: string; url: string } | null>(null);
const uploading = ref(false);
const error = ref('');

function clearLocalPreviews() {
  for (const preview of localPreviews.value) URL.revokeObjectURL(preview.url);
  localPreviews.value = [];
}

watch(() => props.modelValue, (files = []) => {
  clearLocalPreviews();
  localPreviews.value = files.map((file) => ({
    name: file.name,
    url: URL.createObjectURL(file),
    image: file.type.startsWith('image/')
  }));
}, { immediate: true });

onBeforeUnmount(clearLocalPreviews);

function attachmentUrl(id: number) {
  return `/api/attachments/${id}`;
}

function isImage(attachment: AttachmentRecord) {
  return attachment.contentType.startsWith('image/');
}

function openLargePreview(name: string, url: string) {
  largePreview.value = { name, url };
}

function closeLargePreview() {
  largePreview.value = null;
}

function apiErrorMessage(err: unknown) {
  if (typeof err === 'object' && err && 'response' in err) {
    const response = (err as { response?: { data?: { message?: string } } }).response;
    if (response?.data?.message) return response.data.message;
  }
  return '上传失败，请稍后重试';
}

async function chooseFiles(event: Event) {
  const input = event.target as HTMLInputElement;
  const files = Array.from(input.files ?? []);
  emit('update:modelValue', files);
  if (props.recordId && props.attachmentType && files.length) {
    uploading.value = true;
    error.value = '';
    try {
      const responses = await Promise.all(files.map((file) => uploadAttachment(props.recordId as number, props.attachmentType as AttachmentType, file)));
      for (const response of responses) emit('uploaded', response.data);
      emit('update:modelValue', []);
      input.value = '';
    } catch (err) {
      error.value = apiErrorMessage(err);
    } finally {
      uploading.value = false;
    }
  }
}

async function removeAttachment(attachment: AttachmentRecord) {
  await deleteAttachment(attachment.id);
  emit('deleted', attachment.id);
}
</script>

<template>
  <section class="uploader">
    <strong>{{ title }}</strong>
    <p class="uploader__hint">支持 JPG、PNG、WebP、PDF，可一次选择多份。</p>
    <div v-if="attachments?.length || localPreviews.length" class="attachment-preview-list masonry-preview-list">
      <div v-for="attachment in attachments" :key="attachment.id" class="attachment-preview masonry-preview-card">
        <button v-if="isImage(attachment)" type="button" class="image-preview-button" @click="openLargePreview(attachment.originalFilename, attachmentUrl(attachment.id))">
          <img class="adaptive-preview-image" :src="attachmentUrl(attachment.id)" :alt="attachment.originalFilename" />
        </button>
        <a v-else :href="attachmentUrl(attachment.id)" target="_blank" rel="noreferrer">{{ attachment.originalFilename }}</a>
        <button v-if="!readonly" type="button" class="btn-remove" @click="removeAttachment(attachment)">删除</button>
      </div>
      <div v-for="preview in localPreviews" :key="preview.url" class="attachment-preview masonry-preview-card">
        <button v-if="preview.image" type="button" class="image-preview-button" @click="openLargePreview(preview.name, preview.url)">
          <img class="adaptive-preview-image" :src="preview.url" :alt="preview.name" />
        </button>
        <span v-else>{{ preview.name }}</span>
      </div>
    </div>
    <input
      v-if="!readonly"
      :data-test="dataTest"
      type="file"
      accept="image/png,image/jpeg,image/webp,application/pdf,.png,.jpg,.jpeg,.webp,.pdf"
      multiple
      :disabled="uploading"
      @change="chooseFiles"
    />
    <p v-if="uploading" class="uploader__hint">上传中...</p>
    <p v-if="error" class="uploader__error" role="alert">{{ error }}</p>
    <ul v-if="modelValue?.length" class="uploader__files">
      <li v-for="file in modelValue" :key="`${file.name}-${file.size}`">{{ file.name }}</li>
    </ul>
    <div v-if="largePreview" class="large-preview" role="dialog" aria-modal="true" @click.self="closeLargePreview">
      <div class="large-preview-panel expanded-preview-panel">
        <div class="large-preview-header">
          <strong>{{ largePreview.name }}</strong>
          <button type="button" aria-label="关闭大图预览" @click="closeLargePreview">关闭</button>
        </div>
        <img class="expanded-preview-image" :src="largePreview.url" :alt="largePreview.name" />
        <a :href="largePreview.url" target="_blank" rel="noreferrer">打开原图</a>
      </div>
    </div>
  </section>
</template>

<style scoped>
.uploader { display: grid; gap: 8px; padding: 10px; border: 1px dashed #bbb; border-radius: 4px; }
.uploader__hint, .uploader__error, .uploader__files { margin: 0; }
.uploader__hint { color: var(--color-text-muted); font-size: 0.875rem; }
.uploader__error { color: var(--color-danger); font-weight: 700; }
.uploader__files { padding-left: 18px; color: var(--color-text-muted); }
.attachment-preview-list { column-count: 3; column-gap: 12px; }
.attachment-preview { display: inline-grid; width: 100%; margin: 0 0 12px; break-inside: avoid; padding: 8px; border: 1px solid #dbe3ef; border-radius: 10px; background: #f8fafc; color: #2563eb; font-size: 13px; text-decoration: none; }
.image-preview-button { display: grid; width: 100%; padding: 0; border: 0; background: transparent; cursor: zoom-in; }
.attachment-preview img { width: 100%; max-height: 220px; object-fit: contain; border-radius: 8px; background: #fff; }
.large-preview { position: fixed; inset: 0; z-index: 1000; display: grid; place-items: center; padding: 24px; background: rgba(15, 23, 42, 0.72); }
.large-preview-panel { display: grid; gap: 14px; width: min(1280px, 94vw); max-height: 94vh; padding: 18px; border-radius: 18px; background: #fff; }
.large-preview-header { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.large-preview-header button { min-height: 36px; padding: 0 12px; border: 0; border-radius: 8px; background: #0f172a; color: #fff; cursor: pointer; }
.large-preview-panel > img { justify-self: center; max-width: 100%; max-height: 88vh; object-fit: contain; border-radius: 12px; }
.large-preview-panel > a { color: #2563eb; font-weight: 700; }
.btn-remove { margin-top: 6px; padding: 0 8px; min-height: 26px; border: 1px solid #fca5a5; border-radius: 6px; background: #fff; color: #dc2626; font-size: 11px; font-weight: 700; cursor: pointer; }
.btn-remove:hover { background: #dc2626; color: #fff; }
@media (max-width: 760px) { .attachment-preview-list { column-count: 2; } }
@media (max-width: 520px) { .attachment-preview-list { column-count: 1; } }
</style>
