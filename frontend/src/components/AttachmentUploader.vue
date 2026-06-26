<script setup lang="ts">
import { computed, ref } from 'vue';
import { uploadAttachment, type AttachmentType } from '../api/reimbursements';

const props = defineProps<{
  label: string;
  required?: boolean;
  recordId?: number | null;
  attachmentType?: AttachmentType;
  modelValue?: File[];
  dataTest?: string;
}>();

const emit = defineEmits<{ 'update:modelValue': [File[]] }>();

const title = computed(() => `${props.label}${props.required ? '（必填）' : '（选填）'}`);
const uploading = ref(false);
const error = ref('');

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
  if (props.recordId && props.attachmentType) {
    uploading.value = true;
    error.value = '';
    try {
      await Promise.all(files.map((file) => uploadAttachment(props.recordId as number, props.attachmentType as AttachmentType, file)));
      emit('update:modelValue', []);
      input.value = '';
    } catch (err) {
      error.value = apiErrorMessage(err);
    } finally {
      uploading.value = false;
    }
  }
}
</script>

<template>
  <section class="uploader">
    <strong>{{ title }}</strong>
    <p class="uploader__hint">支持 JPG、PNG、WebP、PDF，可一次选择多份。</p>
    <input :data-test="dataTest" type="file" accept="image/png,image/jpeg,image/webp,application/pdf,.png,.jpg,.jpeg,.webp,.pdf" multiple :disabled="uploading" @change="chooseFiles" />
    <p v-if="uploading" class="uploader__hint">上传中...</p>
    <p v-if="error" class="uploader__error" role="alert">{{ error }}</p>
    <ul v-if="modelValue?.length" class="uploader__files">
      <li v-for="file in modelValue" :key="`${file.name}-${file.size}`">{{ file.name }}</li>
    </ul>
  </section>
</template>

<style scoped>
.uploader { display: grid; gap: 8px; padding: 10px; border: 1px dashed #bbb; border-radius: 4px; }
.uploader__hint, .uploader__error, .uploader__files { margin: 0; }
.uploader__hint { color: var(--color-text-muted); font-size: 0.875rem; }
.uploader__error { color: var(--color-danger); font-weight: 700; }
.uploader__files { padding-left: 18px; color: var(--color-text-muted); }
</style>
