<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getAdminReimbursement, type ReimbursementRecord, type ReimbursementAttachment, type AttachmentType, statusLabel, formatTime } from '../../api/reimbursements';

const route = useRoute();
const router = useRouter();
const record = ref<ReimbursementRecord | null>(null);
const largePreview = ref<{ name: string; url: string } | null>(null);

function attachmentsByType(type: AttachmentType) {
  return record.value?.attachments?.filter((a) => a.type === type) ?? [];
}

function attachmentUrl(id: number) {
  return `/api/attachments/${id}`;
}

function isImage(a: ReimbursementAttachment) {
  return a.contentType.startsWith('image/');
}

function openLargePreview(name: string, url: string) {
  largePreview.value = { name, url };
}

function closeLargePreview() {
  largePreview.value = null;
}

onMounted(async () => {
  const id = Number(route.params.id);
  const response = await getAdminReimbursement(id);
  record.value = response.data;
});
</script>

<template>
  <section v-if="record">
    <div class="page-header">
      <h1>报销详情</h1>
      <button type="button" @click="router.push('/admin/reimbursements')">返回列表</button>
    </div>
    <div class="detail-grid">
      <div class="detail-card">
        <h2>基本信息</h2>
        <dl>
          <dt>员工</dt><dd>{{ record.employeeName }}</dd>
          <dt>金额</dt><dd>{{ record.amount }}</dd>
          <dt>状态</dt><dd><span class="status-tag" :class="record.reimbursedAt ? 'reimbursed' : record.status.toLowerCase()">{{ statusLabel(record.status, record.reimbursedAt) }}</span></dd>
          <dt>用途分类</dt><dd>{{ record.categoryName }}</dd>
          <dt>用途说明</dt><dd>{{ record.purpose }}</dd>
          <dt>支付时间</dt><dd>{{ formatTime(record.paymentTime) }}</dd>
          <dt>提交时间</dt><dd>{{ formatTime(record.submittedAt) }}</dd>
          <dt>报销时间</dt><dd>{{ formatTime(record.reimbursedAt) }}</dd>
          <dt>批次</dt><dd>{{ record.batchName ?? '未分配' }}</dd>
          <dt>管理员备注</dt><dd>{{ record.adminRemark || '—' }}</dd>
          <dt>OA编号</dt><dd>{{ record.oaNumber || '—' }}</dd>
        </dl>
      </div>
      <div class="detail-card">
        <h2>支付凭证</h2>
        <div v-if="attachmentsByType('PAYMENT_VOUCHER').length" class="preview-list">
          <div v-for="a in attachmentsByType('PAYMENT_VOUCHER')" :key="a.id" class="preview-card">
            <button v-if="isImage(a)" type="button" class="image-preview-button" @click="openLargePreview(a.originalFilename, attachmentUrl(a.id))">
              <img :src="attachmentUrl(a.id)" :alt="a.originalFilename" />
            </button>
            <a v-else :href="attachmentUrl(a.id)" target="_blank" rel="noreferrer" class="file-link">{{ a.originalFilename }}</a>
          </div>
        </div>
        <p v-else class="empty">暂无</p>

        <h2>订单截图</h2>
        <div v-if="attachmentsByType('ORDER_SCREENSHOT').length" class="preview-list">
          <div v-for="a in attachmentsByType('ORDER_SCREENSHOT')" :key="a.id" class="preview-card">
            <button v-if="isImage(a)" type="button" class="image-preview-button" @click="openLargePreview(a.originalFilename, attachmentUrl(a.id))">
              <img :src="attachmentUrl(a.id)" :alt="a.originalFilename" />
            </button>
            <a v-else :href="attachmentUrl(a.id)" target="_blank" rel="noreferrer" class="file-link">{{ a.originalFilename }}</a>
          </div>
        </div>
        <p v-else class="empty">暂无</p>

        <h2>发票</h2>
        <div v-if="attachmentsByType('INVOICE').length" class="preview-list">
          <div v-for="a in attachmentsByType('INVOICE')" :key="a.id" class="preview-card">
            <button v-if="isImage(a)" type="button" class="image-preview-button" @click="openLargePreview(a.originalFilename, attachmentUrl(a.id))">
              <img :src="attachmentUrl(a.id)" :alt="a.originalFilename" />
            </button>
            <a v-else :href="attachmentUrl(a.id)" target="_blank" rel="noreferrer" class="file-link">{{ a.originalFilename }}</a>
          </div>
        </div>
        <p v-else class="empty">暂无</p>
      </div>
    </div>
    <div v-if="largePreview" class="large-preview" role="dialog" aria-modal="true" @click.self="closeLargePreview">
      <div class="large-preview-panel">
        <div class="large-preview-header">
          <strong>{{ largePreview.name }}</strong>
          <button type="button" aria-label="关闭大图预览" @click="closeLargePreview">关闭</button>
        </div>
        <img class="expanded-preview-image" :src="largePreview.url" :alt="largePreview.name" />
        <a :href="largePreview.url" target="_blank" rel="noreferrer">查看原图</a>
      </div>
    </div>
  </section>
</template>

<style scoped>
.page-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 18px; }
.page-header h1 { margin: 0; }
.detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
@media (max-width: 900px) { .detail-grid { grid-template-columns: 1fr; } }
.detail-card { padding: 18px; border: 1px solid #e5e7eb; border-radius: 16px; background: #fff; }
.detail-card h2 { margin: 0 0 12px; font-size: 15px; color: #1e293b; }
dl { display: grid; grid-template-columns: auto 1fr; gap: 8px 16px; margin: 0; }
dt { color: #64748b; font-size: 13px; font-weight: 700; white-space: nowrap; }
dd { margin: 0; color: #1f2937; font-size: 13px; }
.status-tag { display: inline-block; padding: 2px 10px; border-radius: 999px; font-size: 11px; font-weight: 800; letter-spacing: .4px; }
.status-tag.submitted { background: #dbeafe; color: #1d4ed8; }
.status-tag.reimbursed { background: #dcfce7; color: #166534; }
.status-tag.archived { background: #f3f4f6; color: #6b7280; }
.status-tag.draft { background: #fef3c7; color: #b45309; }
.preview-list { column-count: 2; column-gap: 12px; margin-bottom: 16px; }
.preview-card { display: inline-grid; width: 100%; margin: 0 0 12px; break-inside: avoid; padding: 8px; border: 1px solid #dbe3ef; border-radius: 10px; background: #f8fafc; }
.image-preview-button { display: grid; width: 100%; padding: 0; border: 0; background: transparent; cursor: zoom-in; }
.image-preview-button img { width: 100%; max-height: 220px; object-fit: contain; border-radius: 8px; background: #fff; }
.file-link { color: #2563eb; font-size: 13px; font-weight: 600; text-decoration: none; }
.empty { color: #94a3b8; font-size: 13px; margin: 0 0 16px; }
.large-preview { position: fixed; inset: 0; z-index: 1000; display: grid; place-items: center; padding: 24px; background: rgba(15, 23, 42, 0.72); }
.large-preview-panel { display: grid; gap: 14px; width: min(1280px, 94vw); max-height: 94vh; padding: 18px; border-radius: 18px; background: #fff; }
.large-preview-header { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.large-preview-header button { min-height: 36px; padding: 0 12px; border: 0; border-radius: 8px; background: #0f172a; color: #fff; cursor: pointer; }
.large-preview-panel > img { justify-self: center; max-width: 100%; max-height: 88vh; object-fit: contain; border-radius: 12px; }
.large-preview-panel > a { color: #2563eb; font-weight: 700; }
@media (max-width: 760px) { .preview-list { column-count: 1; } }
</style>
