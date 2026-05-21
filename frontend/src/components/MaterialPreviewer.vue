<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import type { AttachmentRecord } from '../api/reimbursements';

const props = defineProps<{
  attachments: AttachmentRecord[];
  activeId: number;
}>();

const emit = defineEmits<{
  close: [];
}>();

const root = ref<HTMLElement | null>(null);
const closeButton = ref<HTMLButtonElement | null>(null);
const currentId = ref(props.activeId);
let previousFocus: Element | null = null;

const currentIndex = computed(() => props.attachments.findIndex((attachment) => attachment.id === currentId.value));
const current = computed(() => props.attachments[currentIndex.value] ?? props.attachments[0] ?? null);
const src = computed(() => (current.value ? `/api/attachments/${current.value.id}` : ''));
const filename = computed(() => current.value?.originalFilename ?? '材料');
const isImage = computed(() => current.value?.contentType.startsWith('image/') ?? false);
const isPdf = computed(() => current.value?.contentType === 'application/pdf');
const hasMultiple = computed(() => props.attachments.length > 1);

function syncActive() {
  if (props.attachments.some((attachment) => attachment.id === props.activeId)) {
    currentId.value = props.activeId;
    return;
  }
  currentId.value = props.attachments[0]?.id ?? props.activeId;
}

function move(delta: number) {
  if (!props.attachments.length) return;
  const start = currentIndex.value >= 0 ? currentIndex.value : 0;
  const next = (start + delta + props.attachments.length) % props.attachments.length;
  currentId.value = props.attachments[next].id;
}

function focusableControls() {
  const selector = 'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';
  return Array.from(root.value?.querySelectorAll<HTMLElement>(selector) ?? []).filter(
    (element) => !element.hasAttribute('disabled') && element.getAttribute('aria-hidden') !== 'true'
  );
}

function trapTab(event: KeyboardEvent) {
  const controls = focusableControls();
  if (!controls.length) return;
  const first = controls[0];
  const last = controls[controls.length - 1];
  const active = document.activeElement;

  if (event.shiftKey && active === first) {
    event.preventDefault();
    last.focus();
    return;
  }

  if (!event.shiftKey && active === last) {
    event.preventDefault();
    first.focus();
  }
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    emit('close');
    return;
  }
  if (event.key === 'ArrowRight') {
    event.preventDefault();
    move(1);
    return;
  }
  if (event.key === 'ArrowLeft') {
    event.preventDefault();
    move(-1);
    return;
  }
  if (event.key === 'Tab') trapTab(event);
}

watch(() => [props.activeId, props.attachments] as const, syncActive, { deep: true });

onMounted(async () => {
  previousFocus = document.activeElement;
  syncActive();
  await nextTick();
  closeButton.value?.focus();
});

onBeforeUnmount(() => {
  if (previousFocus instanceof HTMLElement && document.contains(previousFocus)) {
    previousFocus.focus();
  }
});
</script>

<template>
  <section ref="root" class="material-previewer" role="dialog" aria-label="材料预览" aria-modal="true" tabindex="-1" @keydown="onKeydown">
    <header class="material-previewer__bar">
      <div>
        <p class="material-previewer__eyebrow">材料预览</p>
        <h2>{{ filename }}</h2>
      </div>
      <div class="material-previewer__actions">
        <a v-if="current" data-test="download-active" :href="src" :download="filename" target="_blank" rel="noopener">下载</a>
        <button ref="closeButton" type="button" data-test="close-preview" @click="emit('close')">关闭</button>
      </div>
    </header>

    <main class="material-previewer__body">
      <button type="button" data-test="previous-preview" :disabled="!hasMultiple" aria-label="上一份材料" @click="move(-1)">上一份</button>
      <div class="material-previewer__stage">
        <img v-if="current && isImage" :src="src" :alt="filename" />
        <object v-else-if="current && isPdf" :data="src" type="application/pdf" :title="filename" :aria-label="filename">
          <a :href="src" :download="filename" target="_blank" rel="noopener">下载 {{ filename }}</a>
        </object>
        <div v-else-if="current" class="material-previewer__fallback">
          <p>该文件类型暂不支持在线预览，请下载后查看。</p>
          <a :href="src" :download="filename" target="_blank" rel="noopener">下载 {{ filename }}</a>
        </div>
        <p v-else>暂无可预览材料</p>
      </div>
      <button type="button" data-test="next-preview" :disabled="!hasMultiple" aria-label="下一份材料" @click="move(1)">下一份</button>
    </main>
  </section>
</template>

<style scoped>
.material-previewer {
  position: fixed;
  inset: 0;
  z-index: 40;
  display: grid;
  grid-template-rows: auto 1fr;
  gap: var(--space-4);
  padding: var(--space-5);
  background: rgba(15, 23, 42, 0.92);
  color: white;
}

.material-previewer__bar,
.material-previewer__actions,
.material-previewer__body {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.material-previewer__bar {
  justify-content: space-between;
}

.material-previewer__bar h2,
.material-previewer__eyebrow,
.material-previewer__fallback p {
  margin: 0;
}

.material-previewer__eyebrow {
  color: rgba(255, 255, 255, 0.72);
  font-size: 0.875rem;
  font-weight: 700;
}

.material-previewer__actions {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.material-previewer__actions a,
.material-previewer__actions button,
.material-previewer__body > button,
.material-previewer__fallback a {
  min-height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(255, 255, 255, 0.32);
  border-radius: var(--radius-md);
  padding: 0 var(--space-4);
  background: rgba(255, 255, 255, 0.12);
  color: white;
  font-weight: 700;
  text-decoration: none;
}

.material-previewer__actions button,
.material-previewer__body > button {
  cursor: pointer;
}

.material-previewer__body > button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.material-previewer__body {
  min-height: 0;
  justify-content: center;
}

.material-previewer__stage {
  min-width: 0;
  width: min(960px, 100%);
  height: 100%;
  display: grid;
  place-items: center;
  overflow: hidden;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.08);
}

.material-previewer__stage img,
.material-previewer__stage object {
  max-width: 100%;
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.material-previewer__fallback {
  display: grid;
  gap: var(--space-3);
  justify-items: center;
  text-align: center;
}

@media (max-width: 720px) {
  .material-previewer {
    padding: var(--space-3);
  }

  .material-previewer__bar,
  .material-previewer__body {
    align-items: stretch;
  }

  .material-previewer__bar {
    flex-direction: column;
  }

  .material-previewer__body {
    display: grid;
    grid-template-columns: 1fr 1fr;
    grid-template-rows: minmax(0, 1fr) auto;
    min-height: 0;
  }

  .material-previewer__stage {
    grid-column: 1 / -1;
    grid-row: 1;
  }

  .material-previewer__body > button {
    grid-row: 2;
  }
}
</style>
