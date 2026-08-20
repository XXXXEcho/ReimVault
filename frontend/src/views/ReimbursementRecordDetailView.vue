<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import MaterialPreviewer from '../components/MaterialPreviewer.vue';
import RecordDrawer from '../components/RecordDrawer.vue';
import { getAdminReimbursement, getReimbursement, type ReimbursementRecord } from '../api/reimbursements';
import { useAuthStore } from '../stores/auth';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const record = ref<ReimbursementRecord | null>(null);
const previewAttachmentId = ref<number | null>(null);
const loading = ref(true);
const error = ref('');
const isManagement = computed(() => route.path.startsWith('/admin/'));
const role = computed(() => auth.user?.role ?? (isManagement.value ? 'ADMIN' : 'EMPLOYEE'));
const backPath = computed(() => isManagement.value ? '/admin/reimbursements' : '/reimbursements');

async function load() {
  loading.value = true;
  error.value = '';
  try {
    const id = Number(route.params.id);
    const response = isManagement.value ? await getAdminReimbursement(id) : await getReimbursement(id);
    record.value = response.data;
  } catch {
    error.value = '报销记录加载失败，请返回列表后重试。';
  } finally {
    loading.value = false;
  }
}

function back() {
  router.push(backPath.value);
}

function updateRecord(updated: ReimbursementRecord) {
  record.value = updated;
}

onMounted(load);
</script>

<template>
  <section class="record-detail-page">
    <p v-if="loading" class="record-detail-page__state">正在加载报销详情…</p>
    <div v-else-if="error" class="record-detail-page__state record-detail-page__state--error">
      <p>{{ error }}</p>
      <button type="button" @click="back">返回列表</button>
    </div>
    <template v-else-if="record">
      <RecordDrawer
        presentation="page"
        :record="record"
        :role="role"
        @close="back"
        @saved="updateRecord"
        @submitted="updateRecord"
        @preview="previewAttachmentId = $event"
      />
      <MaterialPreviewer
        v-if="previewAttachmentId"
        :attachments="record.attachments"
        :active-id="previewAttachmentId"
        @close="previewAttachmentId = null"
      />
    </template>
  </section>
</template>

<style scoped>
.record-detail-page { min-width: 0; }
.record-detail-page__state { margin: 0; padding: var(--space-6); color: var(--color-text-muted); font-weight: 700; }
.record-detail-page__state--error { border-radius: var(--radius-md); background: var(--color-danger-soft); color: var(--color-danger); }
.record-detail-page__state button { min-height: 44px; border: 1px solid var(--color-border); border-radius: var(--radius-md); padding: 0 var(--space-4); background: var(--color-surface); color: var(--color-text); font-weight: 700; cursor: pointer; }
</style>
