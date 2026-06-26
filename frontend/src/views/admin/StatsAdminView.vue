<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { getPersonnelMatrix, getStats, type PersonnelMatrix, type ReimbursementStats } from '../../api/stats';
import { listOaNumbers, type OaNumber } from '../../api/oa';
import { listBatches, type Batch } from '../../api/batches';
import { searchEmployees, type UserRecord } from '../../api/users';
import PersonnelMatrixView from '../../components/PersonnelMatrix.vue';

type MetricType = 'amount' | 'count';
type MetricGroup = 'total' | 'reimbursed' | 'unreimbursed' | 'draft';
interface MetricDef {
  key: string;
  label: string;
  type: MetricType;
  field: keyof ReimbursementStats;
  group: MetricGroup;
}
interface Preset {
  key: string;
  label: string;
  metrics: string[];
}

const METRIC_GROUPS: { key: MetricGroup; label: string }[] = [
  { key: 'total', label: '总计' },
  { key: 'reimbursed', label: '已报销' },
  { key: 'unreimbursed', label: '未报销' },
  { key: 'draft', label: '未提交' }
];

const METRICS: MetricDef[] = [
  { key: 'totalAmount', label: '总金额', type: 'amount', field: 'totalAmount', group: 'total' },
  { key: 'totalCount', label: '总笔数', type: 'count', field: 'totalCount', group: 'total' },
  { key: 'reimbursedAmount', label: '已报销金额', type: 'amount', field: 'reimbursedAmount', group: 'reimbursed' },
  { key: 'reimbursedCount', label: '已报销笔数', type: 'count', field: 'reimbursedCount', group: 'reimbursed' },
  { key: 'unreimbursedAmount', label: '未报销金额', type: 'amount', field: 'unreimbursedAmount', group: 'unreimbursed' },
  { key: 'unreimbursedCount', label: '未报销笔数', type: 'count', field: 'unreimbursedCount', group: 'unreimbursed' },
  { key: 'draftAmount', label: '未提交金额', type: 'amount', field: 'draftAmount', group: 'draft' },
  { key: 'draftCount', label: '未提交笔数', type: 'count', field: 'draftCount', group: 'draft' }
];

const PRESETS: Preset[] = [
  { key: 'execution', label: '经费执行概览', metrics: ['reimbursedAmount', 'unreimbursedAmount', 'totalAmount'] },
  { key: 'progress', label: '报销进度', metrics: ['reimbursedCount', 'unreimbursedCount', 'totalCount'] },
  { key: 'all', label: '全量明细', metrics: METRICS.map((m) => m.key) }
];

const oaNumbers = ref<OaNumber[]>([]);
const batches = ref<Batch[]>([]);
const employees = ref<UserRecord[]>([]);
const selectedOaIds = ref<number[]>([]);
const selectedBatchIds = ref<number[]>([]);
const selectedEmployeeIds = ref<number[]>([]);
const selectedMetrics = ref<string[]>(PRESETS[0].metrics);
const stats = ref<ReimbursementStats | null>(null);
const matrix = ref<PersonnelMatrix | null>(null);
const error = ref('');
const loading = ref(false);

const visibleMetrics = computed(() => METRICS.filter((m) => selectedMetrics.value.includes(m.key)));
const activePreset = computed(() => PRESETS.find((p) => sameSet(p.metrics, selectedMetrics.value))?.key);
const metricModeLabel = computed(() => {
  const preset = PRESETS.find((p) => p.key === activePreset.value);
  return preset ? preset.label : '自定义组合';
});
const scopeSummary = computed(() => {
  const parts: string[] = [];
  if (selectedOaIds.value.length) {
    parts.push(oaNumbers.value.filter((o) => selectedOaIds.value.includes(o.id)).map((o) => o.number).join('、'));
  }
  if (selectedBatchIds.value.length) {
    parts.push(batches.value.filter((b) => selectedBatchIds.value.includes(b.id)).map((b) => b.name).join('、'));
  }
  if (selectedEmployeeIds.value.length) {
    parts.push(employees.value.filter((e) => selectedEmployeeIds.value.includes(e.id)).map((e) => e.displayName).join('、'));
  }
  return parts.length ? parts.join(' · ') : '全部记录';
});

function metricsByGroup(group: MetricGroup) {
  return METRICS.filter((m) => m.group === group);
}

function sameSet(a: string[], b: string[]) {
  return a.length === b.length && a.every((x) => b.includes(x));
}

function format(metric: MetricDef) {
  const value = stats.value ? stats.value[metric.field] : 0;
  return metric.type === 'amount' ? Number(value).toFixed(2) : String(value);
}

function applyPreset(preset: Preset) {
  selectedMetrics.value = [...preset.metrics];
}

async function loadStats() {
  loading.value = true;
  error.value = '';
  try {
    const [statsResponse, matrixResponse] = await Promise.all([
      getStats(selectedOaIds.value, selectedBatchIds.value, selectedEmployeeIds.value),
      getPersonnelMatrix(selectedOaIds.value, selectedBatchIds.value, selectedEmployeeIds.value)
    ]);
    stats.value = statsResponse.data;
    matrix.value = matrixResponse.data;
  } catch (err) {
    error.value = errorMessage(err);
    stats.value = null;
    matrix.value = null;
  } finally {
    loading.value = false;
  }
}

function errorMessage(err: unknown) {
  if (typeof err === 'object' && err && 'response' in err) {
    const response = (err as { response?: { data?: { message?: string } } }).response;
    if (response?.data?.message) return response.data.message;
  }
  return '统计加载失败';
}

onMounted(async () => {
  const [oaResponse, batchResponse, employeeResponse] = await Promise.all([listOaNumbers(), listBatches(), searchEmployees('')]);
  oaNumbers.value = oaResponse.data;
  batches.value = batchResponse.data;
  employees.value = employeeResponse.data;
  await loadStats();
});
</script>

<template>
  <section>
    <div class="stats-head">
      <h1>报销统计</h1>
      <p class="stats-desc">选定经费编码与批次范围，多选指标查看费用与笔数（与报销管理状态口径一致）。</p>
    </div>

    <div class="control-deck">
      <div class="control-card">
        <div class="control-title">统计范围</div>
        <div class="scope-cols">
          <div class="scope-col" data-test="oa-filter-group">
            <div class="scope-col-head"><span>经费编码</span><small>已选 {{ selectedOaIds.length }}</small></div>
            <div class="scope-list">
              <label v-for="oa in oaNumbers" :key="oa.id" class="scope-option">
                <input type="checkbox" :value="oa.id" v-model="selectedOaIds" @change="loadStats" />
                <span>{{ oa.number }}</span>
              </label>
              <p v-if="!oaNumbers.length" class="scope-empty">暂无经费编码</p>
            </div>
          </div>
          <div class="scope-col" data-test="batch-filter-group">
            <div class="scope-col-head"><span>批次</span><small>已选 {{ selectedBatchIds.length }}</small></div>
            <div class="scope-list">
              <label v-for="batch in batches" :key="batch.id" class="scope-option">
                <input type="checkbox" :value="batch.id" v-model="selectedBatchIds" @change="loadStats" />
                <span>{{ batch.name }}</span>
              </label>
              <p v-if="!batches.length" class="scope-empty">暂无批次</p>
            </div>
          </div>
          <div class="scope-col" data-test="employee-filter-group">
            <div class="scope-col-head"><span>员工</span><small>已选 {{ selectedEmployeeIds.length }}</small></div>
            <div class="scope-list">
              <label v-for="employee in employees" :key="employee.id" class="scope-option">
                <input type="checkbox" :value="employee.id" v-model="selectedEmployeeIds" @change="loadStats" />
                <span>{{ employee.displayName }}（{{ employee.department }}）</span>
              </label>
              <p v-if="!employees.length" class="scope-empty">暂无员工</p>
            </div>
          </div>
        </div>
      </div>

      <div class="control-card">
        <div class="control-title">指标 <small class="mode-tag">{{ metricModeLabel }}</small></div>
        <div class="preset-bar">
          <button
            v-for="preset in PRESETS"
            :key="preset.key"
            type="button"
            :class="['preset-chip', { active: activePreset === preset.key }]"
            :data-test="`preset-${preset.key}`"
            @click="applyPreset(preset)"
          >{{ preset.label }}</button>
        </div>
        <div class="metric-groups">
          <div v-for="group in METRIC_GROUPS" :key="group.key" class="metric-group">
            <span class="metric-group-label">{{ group.label }}</span>
            <label v-for="metric in metricsByGroup(group.key)" :key="metric.key" class="metric-option">
              <input type="checkbox" :value="metric.key" v-model="selectedMetrics" />
              <span>{{ metric.type === 'amount' ? '金额' : '笔数' }}</span>
            </label>
          </div>
        </div>
      </div>
    </div>

    <div class="result-panel">
      <div class="result-head">
        <span class="result-scope" data-test="scope-summary">统计范围：{{ scopeSummary }}</span>
        <span v-if="loading" class="result-refresh">刷新中…</span>
      </div>
      <p v-if="error" class="stats-error">{{ error }}</p>
      <p v-else-if="!visibleMetrics.length" class="stats-empty">请至少选择一个指标。</p>
      <div v-else class="metric-grid">
        <div
          v-for="metric in visibleMetrics"
          :key="metric.key"
          :class="['metric-card', `accent-${metric.group}`]"
          :data-test="`metric-${metric.key}`"
        >
          <span class="metric-label">{{ metric.label }}</span>
          <span class="metric-value">
            {{ format(metric) }}<small class="metric-unit">{{ metric.type === 'amount' ? '元' : '笔' }}</small>
          </span>
        </div>
      </div>
    </div>

    <PersonnelMatrixView :matrix="matrix" />
  </section>
</template>

<style scoped>
.stats-head { margin-bottom: 18px; }
.stats-head h1 { margin-bottom: 4px; }
.stats-desc { margin: 0; color: #64748b; font-size: 13px; }

.control-deck { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 18px; }
.control-card { padding: 16px 18px; border: 1px solid #e5e7eb; border-radius: 16px; background: #fff; box-shadow: 0 6px 18px rgba(15, 23, 42, 0.04); }
.control-title { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; color: #0f172a; font-size: 14px; font-weight: 800; }
.mode-tag { padding: 2px 10px; border-radius: 999px; background: #eef2ff; color: #4338ca; font-size: 11px; font-weight: 800; }

.scope-cols { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; }
.scope-col { display: flex; flex-direction: column; min-width: 0; }
.scope-col-head { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 6px; color: #334155; font-size: 12px; font-weight: 800; }
.scope-col-head small { color: #94a3b8; font-weight: 700; }
.scope-list { display: flex; flex-direction: column; gap: 2px; max-height: 168px; overflow-y: auto; padding: 6px; border: 1px solid #eef2f7; border-radius: 12px; background: #f8fafc; }
.scope-option { display: flex; align-items: center; gap: 8px; padding: 6px 8px; font-size: 13px; color: #334155; cursor: pointer; border-radius: 8px; }
.scope-option:hover { background: #eaf2ff; }
.scope-option input { accent-color: #2563eb; }
.scope-empty { margin: 6px; color: #94a3b8; font-size: 12px; }

.preset-bar { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 14px; }
.preset-chip { min-height: 32px; padding: 0 14px; border: 1px solid #bfdbfe; border-radius: 999px; background: #fff; color: #2563eb; font-size: 12px; font-weight: 700; cursor: pointer; transition: background 160ms ease, color 160ms ease; }
.preset-chip:hover { background: #2563eb; color: #fff; }
.preset-chip.active { background: #2563eb; color: #fff; border-color: #2563eb; }

.metric-groups { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px 16px; }
.metric-group { display: flex; align-items: center; gap: 4px 8px; flex-wrap: wrap; }
.metric-group-label { width: 48px; flex-shrink: 0; color: #64748b; font-size: 12px; font-weight: 800; }
.metric-option { display: inline-flex; align-items: center; gap: 5px; font-size: 13px; color: #334155; cursor: pointer; }
.metric-option input { accent-color: #2563eb; }

.result-panel { padding: 18px 20px; border: 1px solid #e5e7eb; border-radius: 16px; background: #fff; box-shadow: 0 10px 26px rgba(15, 23, 42, 0.05); }
.result-head { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 14px; }
.result-scope { color: #475569; font-size: 13px; font-weight: 700; }
.result-refresh { color: #94a3b8; font-size: 12px; }

.metric-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(190px, 1fr)); gap: 14px; }
.metric-card { display: flex; flex-direction: column; gap: 8px; padding: 18px 20px; border: 1px solid #e5e7eb; border-left-width: 4px; border-radius: 14px; background: #fff; box-shadow: 0 6px 16px rgba(15, 23, 42, 0.04); }
.metric-card.accent-total { border-left-color: #2563eb; }
.metric-card.accent-reimbursed { border-left-color: #16a34a; }
.metric-card.accent-unreimbursed { border-left-color: #d97706; }
.metric-card.accent-draft { border-left-color: #94a3b8; }
.metric-label { color: #64748b; font-size: 13px; font-weight: 700; }
.metric-value { display: flex; align-items: baseline; gap: 4px; color: #0f172a; font-size: 28px; font-weight: 800; letter-spacing: -0.02em; }
.metric-unit { font-size: 13px; font-weight: 700; color: #64748b; }

.stats-error { margin: 0; color: #dc2626; font-size: 13px; font-weight: 700; }
.stats-empty { margin: 0; color: #94a3b8; font-size: 13px; }

@media (max-width: 900px) {
  .control-deck { grid-template-columns: 1fr; }
  .scope-cols { grid-template-columns: 1fr; }
}
</style>
