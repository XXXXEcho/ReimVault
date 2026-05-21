<script setup lang="ts">
import { computed } from 'vue';
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

async function chooseFiles(event: Event) {
  const input = event.target as HTMLInputElement;
  const files = Array.from(input.files ?? []);
  emit('update:modelValue', files);
  if (props.recordId && props.attachmentType) {
    await Promise.all(files.map((file) => uploadAttachment(props.recordId as number, props.attachmentType as AttachmentType, file)));
    emit('update:modelValue', []);
    input.value = '';
  }
}
</script>

<template>
  <section class="uploader">
    <strong>{{ title }}</strong>
    <input :data-test="dataTest" type="file" multiple @change="chooseFiles" />
  </section>
</template>

<style scoped>
.uploader { display: grid; gap: 8px; padding: 10px; border: 1px dashed #bbb; border-radius: 4px; }
</style>
