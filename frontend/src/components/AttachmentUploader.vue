<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import { uploadAttachment, type AttachmentType, type ReimbursementAttachment } from '../api/reimbursements';

const props = defineProps<{
  label: string;
  required?: boolean;
  recordId?: number | null;
  attachmentType?: AttachmentType;
  modelValue?: File[];
  attachments?: ReimbursementAttachment[];
  dataTest?: string;
}>();

const emit = defineEmits<{
  'update:modelValue': [File[]];
  uploaded: [ReimbursementAttachment];
}>();

const title = computed(() => `${props.label}${props.required ? '（必填）' : '（选填）'}`);
const localPreviews = ref<{ name: string; url: string; image: boolean }[]>([]);
const largePreview = ref<{ name: string; url: string } | null>(null);

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

function isImage(attachment: ReimbursementAttachment) {
  return attachment.contentType.startsWith('image/');
}

function openLargePreview(name: string, url: string) {
  largePreview.value = { name, url };
}

function closeLargePreview() {
  largePreview.value = null;
}

async function chooseFiles(event: Event) {
  const input = event.target as HTMLInputElement;
  const files = Array.from(input.files ?? []);
  emit('update:modelValue', files);
  if (props.recordId && props.attachmentType) {
    const responses = await Promise.all(files.map((file) => uploadAttachment(props.recordId as number, props.attachmentType as AttachmentType, file)));
    for (const response of responses) emit('uploaded', response.data);
    emit('update:modelValue', []);
    input.value = '';
  }
}
</script>

<template>
  <section class="uploader">
    <strong>{{ title }}</strong>
    <div v-if="attachments?.length || localPreviews.length" class="attachment-preview-list">
      <div v-for="attachment in attachments" :key="attachment.id" class="attachment-preview">
        <button v-if="isImage(attachment)" type="button" class="image-preview-button" @click="openLargePreview(attachment.originalFilename, attachmentUrl(attachment.id))">
          <img :src="attachmentUrl(attachment.id)" :alt="attachment.originalFilename" />
        </button>
        <a v-else :href="attachmentUrl(attachment.id)" target="_blank" rel="noreferrer">{{ attachment.originalFilename }}</a>
      </div>
      <div v-for="preview in localPreviews" :key="preview.url" class="attachment-preview">
        <button v-if="preview.image" type="button" class="image-preview-button" @click="openLargePreview(preview.name, preview.url)">
          <img :src="preview.url" :alt="preview.name" />
        </button>
        <span v-else>{{ preview.name }}</span>
      </div>
    </div>
    <input :data-test="dataTest" type="file" multiple @change="chooseFiles" />
    <div v-if="largePreview" class="large-preview" role="dialog" aria-modal="true" @click.self="closeLargePreview">
      <div class="large-preview-panel">
        <div class="large-preview-header">
          <strong>{{ largePreview.name }}</strong>
          <button type="button" aria-label="关闭大图预览" @click="closeLargePreview">关闭</button>
        </div>
        <img :src="largePreview.url" :alt="largePreview.name" />
        <a :href="largePreview.url" target="_blank" rel="noreferrer">打开原图</a>
      </div>
    </div>
  </section>
</template>

<style scoped>
.uploader { display: grid; gap: 8px; padding: 10px; border: 1px dashed #bbb; border-radius: 4px; }
.attachment-preview-list { display: flex; flex-wrap: wrap; gap: 10px; }
.attachment-preview { display: grid; place-items: center; min-width: 96px; min-height: 72px; padding: 8px; border: 1px solid #dbe3ef; border-radius: 10px; background: #f8fafc; color: #2563eb; font-size: 13px; text-decoration: none; }
.attachment-preview img { width: 96px; height: 72px; object-fit: cover; border-radius: 8px; }
.image-preview-button { padding: 0; border: 0; background: transparent; cursor: zoom-in; }
.large-preview { position: fixed; inset: 0; z-index: 1000; display: grid; place-items: center; padding: 32px; background: rgba(15, 23, 42, 0.72); }
.large-preview-panel { display: grid; gap: 14px; max-width: min(960px, 92vw); max-height: 92vh; padding: 18px; border-radius: 18px; background: #fff; }
.large-preview-header { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.large-preview-header button { min-height: 36px; padding: 0 12px; border: 0; border-radius: 8px; background: #0f172a; color: #fff; cursor: pointer; }
.large-preview-panel > img { max-width: 100%; max-height: 72vh; object-fit: contain; border-radius: 12px; }
.large-preview-panel > a { color: #2563eb; font-weight: 700; }
</style>
