<template>
  <section class="deck-page" :class="{ 'deck-page--right-collapsed': rightPanelCollapsed }">
    <aside class="deck-sidebar">
      <header>
        <p>Full Deck</p>
        <h2>结构大纲</h2>
      </header>

      <el-input
        v-model="deckStore.initialPrompt"
        :rows="8"
        placeholder="输入整套 PPT 的主题、受众、场景和目标"
        resize="none"
        type="textarea"
      />

      <div class="deck-actions">
        <el-button :loading="deckStore.loadingStage === 'create'" plain @click="runAction(deckStore.createDraft)">
          创建草稿
        </el-button>
        <el-button :loading="deckStore.loadingStage === 'outline'" plain @click="runAction(deckStore.consultDeck)">
          Consult
        </el-button>
        <el-segmented
          v-model="deckStore.researchMode"
          :options="[
            { label: 'Model', value: 'model-only' },
            { label: 'Search', value: 'search-assisted' },
          ]"
        />
        <el-button :loading="deckStore.loadingStage === 'outline'" plain @click="runAction(deckStore.generateDeckResearch)">
          Research
        </el-button>
        <el-button
          :loading="fullPipelineRunning"
          type="success"
          @click="runAction(runFullAgentFlow)"
        >
          Run Agent Flow
        </el-button>
        <el-button
          :loading="deckStore.loadingStage === 'outline'"
          type="primary"
          @click="runAction(deckStore.generateDeckOutline)"
        >
          生成大纲
        </el-button>
        <el-button :loading="deckStore.loadingStage === 'outline'" plain @click="runAction(deckStore.addStickyNote)">
          添加页面
        </el-button>
        <el-button :loading="deckStore.loadingStage === 'outline'" plain @click="runAction(deckStore.saveStickyNotes)">
          保存编排
        </el-button>
        <el-button :loading="deckStore.loadingStage === 'outline'" type="primary" @click="runAction(batchCreateDrafts)">
          批量生成单页草稿
        </el-button>
        <el-button :loading="deckStore.loadingStage === 'outline'" type="primary" @click="runAction(batchCreatePlans)">
          Batch Page Plans
        </el-button>
        <el-button :loading="deckStore.loadingStage === 'outline'" type="primary" @click="runAction(batchCreateVisuals)">
          Batch Bento Specs
        </el-button>
        <el-button :loading="deckStore.loadingStage === 'outline'" type="primary" @click="runAction(batchGenerateSvgs)">
          批量生成 SVG
        </el-button>
        <el-button :loading="deckStore.loadingStage === 'outline'" plain @click="runAction(downloadDeckPptx)">
          导出整套 PPTX
        </el-button>
      </div>

      <el-alert
        v-if="deckStore.errorMessage"
        :closable="false"
        :title="deckStore.errorMessage"
        type="error"
      />

      <el-alert
        v-if="!aiSettingsStore.isConfigured"
        :closable="false"
        title="AI provider is not configured"
        type="warning"
      >
        <template #default>
          <el-button size="small" type="warning" @click="router.push('/app/settings')">Open Settings</el-button>
        </template>
      </el-alert>

    </aside>

    <main class="deck-main">
      <header class="outline-header">
        <div>
          <p>Deck Outline</p>
          <h1>{{ deckStore.outline.title }}</h1>
        </div>
        <el-tag>{{ visibleStickyNotes.length }} pages</el-tag>
      </header>

      <section class="thesis-band">
        <p>核心论点</p>
        <h2>{{ deckStore.outline.coreThesis }}</h2>
        <span>{{ deckStore.outline.audience }} / {{ deckStore.outline.scenario }}</span>
      </section>

      <section v-if="deckStore.researchPack.summary" class="research-pack">
        <header>
          <div>
            <p>Research Pack</p>
            <h2>{{ deckStore.researchPack.summary }}</h2>
          </div>
          <el-tag>{{ deckStore.researchPack.mode }}</el-tag>
        </header>
        <div class="research-pack__points">
          <article v-for="point in deckStore.researchPack.keyPoints" :key="point">{{ point }}</article>
        </div>
        <div v-if="deckStore.researchPack.evidence.length" class="research-pack__evidence">
          <article v-for="item in deckStore.researchPack.evidence" :key="item.claim">
            <strong>{{ item.claim }}</strong>
            <p>{{ item.support }}</p>
            <div v-if="item.sourceIds?.length">
              <el-tag v-for="sourceId in item.sourceIds" :key="sourceId" size="small">
                {{ deckSourceLabel(sourceId) }}
              </el-tag>
            </div>
          </article>
        </div>
        <div v-if="deckStore.researchPack.sources.length" class="research-pack__sources">
          <a
            v-for="source in deckStore.researchPack.sources"
            :key="source.id || source.url"
            :href="source.url"
            target="_blank"
          >
            {{ source.title || source.url }}
          </a>
        </div>
      </section>

      <section class="outline-grid">
        <article v-for="section in deckStore.outline.structure" :key="section.id">
          <p>{{ section.id }}</p>
          <h3>{{ section.title }}</h3>
          <span>{{ section.purpose }}</span>
        </article>
      </section>

      <section class="sticky-board">
        <header>
          <p>Sticky Notes</p>
          <h2>便利贴式页面编排</h2>
        </header>

        <div class="sticky-grid">
          <article
            v-for="note in visibleStickyNotes"
            :key="note.slideId"
            class="sticky-note"
            :class="{ 'is-dragging': draggingSlideId === note.slideId }"
            draggable="true"
            @dragstart="draggingSlideId = note.slideId"
            @dragend="draggingSlideId = ''"
            @dragover.prevent
            @drop.prevent="runAction(() => dropStickyNote(note.slideId))"
          >
            <div class="sticky-note__top">
              <span>{{ note.order }}</span>
              <el-select
                :model-value="note.tags?.[0] || 'content'"
                size="small"
                @update:model-value="(value: string) => setPrimaryTag(note, value)"
              >
                <el-option v-for="option in pageTypeOptions" :key="option" :label="option" :value="option" />
              </el-select>
            </div>

            <el-input v-model="note.sectionTitle" size="small" placeholder="章节" />
            <el-input v-model="note.title" size="small" placeholder="页面标题" />
            <el-input
              v-model="note.message"
              :rows="3"
              placeholder="这一页的核心信息"
              resize="none"
              type="textarea"
            />

            <div class="sticky-note__actions">
              <el-button size="small" plain @click="runAction(() => deckStore.moveStickyNote(note.slideId, -1))">
                上移
              </el-button>
              <el-button size="small" plain @click="runAction(() => deckStore.moveStickyNote(note.slideId, 1))">
                下移
              </el-button>
              <el-button size="small" plain @click="runAction(deckStore.saveStickyNotes)">
                保存
              </el-button>
              <el-button
                :loading="creatingSlideId === note.slideId"
                size="small"
                type="primary"
                @click="runAction(() => createOnePage(note.slideId))"
              >
                生成单页
              </el-button>
              <el-button size="small" text type="danger" @click="runAction(() => deckStore.deleteStickyNote(note.slideId))">
                删除
              </el-button>
            </div>
          </article>
        </div>
      </section>

      <section v-if="deckStore.generatedDrafts.length" class="generated-drafts">
        <header>
          <p>Generated Drafts</p>
          <h2>逐页生成结果</h2>
          <el-button
            v-if="failedCount"
            :loading="deckStore.loadingStage === 'outline'"
            size="small"
            type="primary"
            plain
            @click="runAction(retryFailedSvgs)"
          >
            Retry Failed
          </el-button>
          <el-button
            :loading="deckStore.loadingStage === 'outline'"
            size="small"
            plain
            @click="runAction(deckStore.loadSlidePreviews)"
          >
            Load Previews
          </el-button>
        </header>

        <div class="generated-drafts__list">
          <article v-for="(draft, index) in deckStore.generatedDrafts" :key="draft.draftId">
            <div>
              <strong>Page {{ draft.order || index + 1 }} · {{ draft.title || '未命名页面' }}</strong>
              <span>{{ draft.status }}</span>
              <small v-if="validationWarningCount(draft.slideId)">
                {{ validationWarningCount(draft.slideId) }} validation warnings
              </small>
              <ul v-if="validationWarningCount(draft.slideId)" class="generated-drafts__warnings">
                <li v-for="warning in validationWarnings(draft.slideId)" :key="warning">
                  {{ warning }}
                </li>
              </ul>
              <small v-if="draft.errorMessage">{{ draft.errorMessage }}</small>
            </div>
            <div
              v-if="deckStore.slidePreviews[draft.slideId]"
              class="generated-drafts__preview"
              v-html="deckStore.slidePreviews[draft.slideId]"
            />
            <code>{{ draft.draftId.slice(0, 8) }}</code>
            <div class="generated-drafts__actions">
              <el-button
                v-if="draft.status === 'FAILED'"
                size="small"
                type="primary"
                plain
                @click="runAction(() => retrySlideSvg(draft.slideId))"
              >
                重试 SVG
              </el-button>
              <el-button
                :disabled="!draft.draftId"
                size="small"
                plain
                @click="runAction(() => openGeneratedDraft(draft.draftId))"
              >
                打开编辑
              </el-button>
            </div>
          </article>
        </div>
      </section>
    </main>

    <aside class="deck-agent-panel">
      <button class="deck-agent-panel__toggle" type="button" @click="rightPanelCollapsed = !rightPanelCollapsed">
        <el-icon>
          <ArrowLeft v-if="rightPanelCollapsed" />
          <ArrowRight v-else />
        </el-icon>
      </button>

      <template v-if="!rightPanelCollapsed">
        <section v-if="deckStore.assistantMessage" class="deck-consult">
          <p>AI Consultant</p>
          <span>{{ deckStore.assistantMessage }}</span>
        </section>

        <section class="deck-meta">
          <span>{{ deckStore.status }}</span>
          <span v-if="deckStore.deckId">Deck {{ deckStore.deckId.slice(0, 8) }}</span>
          <span v-if="fullPipelineStep">Flow {{ fullPipelineStep }}</span>
          <span v-if="deckStore.generatedDrafts.length">已生成 {{ deckStore.generatedDrafts.length }} 个单页草稿</span>
          <span v-if="svgReadyCount">SVG ready {{ svgReadyCount }} / {{ deckStore.generatedDrafts.length }}</span>
          <span v-if="failedCount">Failed {{ failedCount }}</span>
        </section>

        <section class="agent-timeline">
          <header>
            <p>Agent Flow</p>
            <span>{{ agentFlowDoneCount }} / {{ agentFlowTimeline.length }}</span>
          </header>
          <article
            v-for="step in agentFlowTimeline"
            :key="step.key"
            :class="[`is-${step.status}`]"
          >
            <i />
            <div>
              <strong>{{ step.label }}</strong>
              <span>{{ step.description }}</span>
            </div>
            <el-button
              :disabled="fullPipelineRunning"
              size="small"
              text
              @click="runAction(() => rerunAgentStep(step.key))"
            >
              Rerun
            </el-button>
          </article>
        </section>

        <section v-if="deckStore.generatedDrafts.length" class="deck-progress">
          <article>
            <strong>{{ pagePlanReadyCount }}</strong>
            <span>Page Plans</span>
          </article>
          <article>
            <strong>{{ visualSpecReadyCount }}</strong>
            <span>Bento Specs</span>
          </article>
          <article>
            <strong>{{ svgReadyCount }}</strong>
            <span>SVG Ready</span>
          </article>
          <article :class="{ 'is-danger': failedCount }">
            <strong>{{ failedCount }}</strong>
            <span>Failed</span>
          </article>
        </section>

        <section v-if="exportPreviewRows.length" class="export-preview">
          <header>
            <p>Export Preview</p>
            <span>{{ exportReadyCount }} / {{ exportPreviewRows.length }}</span>
          </header>
          <article
            v-for="row in exportPreviewRows"
            :key="row.slideId"
            :class="{ 'is-ready': row.ready, 'is-blocked': !row.ready }"
          >
            <strong>{{ row.order }}. {{ row.title }}</strong>
            <span>{{ row.status }}</span>
          </article>
        </section>

        <section v-if="deckStore.workflowRuns.length" class="deck-trace">
          <header>
            <p>Prompt Trace</p>
            <el-button size="small" text @click="runAction(deckStore.loadWorkflowRuns)">Refresh</el-button>
          </header>
          <article v-for="run in deckStore.workflowRuns.slice(0, 6)" :key="run.id">
            <button type="button" @click="toggleTrace(run.id)">
              <strong>{{ run.stage }}</strong>
              <span>{{ run.promptKey || 'manual' }}</span>
            </button>
            <em>{{ run.status }} / {{ run.durationMs || 0 }}ms</em>
            <div v-if="activeTraceId === run.id" class="deck-trace__detail">
              <label>Prompt</label>
              <pre>{{ run.inputPreview || 'No prompt preview' }}</pre>
              <label>Output</label>
              <pre>{{ run.outputPreview || run.errorMessage || 'No output preview' }}</pre>
            </div>
          </article>
        </section>
      </template>
    </aside>
  </section>
</template>

<script setup lang="ts">
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { createOnePageDraftFromDeckSlide } from '@/api/modules/deck'
import { useAiSettingsStore, useDeckDraftStore, useOnePageDraftStore } from '@/stores'

const deckStore = useDeckDraftStore()
const aiSettingsStore = useAiSettingsStore()
const onePageStore = useOnePageDraftStore()
const router = useRouter()
const creatingSlideId = ref('')
const draggingSlideId = ref('')
const fullPipelineRunning = ref(false)
const fullPipelineStep = ref('')
const activeTraceId = ref('')
const rightPanelCollapsed = ref(false)
const pageTypeOptions = ['cover', 'agenda', 'section', 'content', 'summary']

const svgReadyCount = computed(() => deckStore.generatedDrafts.filter((draft) => draft.status === 'SVG_READY').length)
const failedCount = computed(() => deckStore.generatedDrafts.filter((draft) => draft.status === 'FAILED').length)
const pagePlanReadyCount = computed(
  () =>
    deckStore.generatedDrafts.filter((draft) =>
      ['PAGE_PLAN_READY', 'VISUAL_SPEC_READY', 'SVG_READY'].includes(draft.status),
    ).length,
)
const visualSpecReadyCount = computed(
  () => deckStore.generatedDrafts.filter((draft) => ['VISUAL_SPEC_READY', 'SVG_READY'].includes(draft.status)).length,
)
const agentFlowTimeline = computed(() => [
  {
    key: 'consult',
    label: 'Consult',
    description: deckStore.assistantMessage ? '需求澄清已完成' : '等待顾问追问',
    status: deckStore.assistantMessage ? 'done' : currentStepStatus('consult'),
  },
  {
    key: 'research',
    label: 'Research',
    description: deckStore.researchPack.summary ? deckStore.researchPack.mode : '等待资料整理',
    status: deckStore.researchPack.summary ? 'done' : currentStepStatus('research'),
  },
  {
    key: 'outline',
    label: 'Outline',
    description: deckStore.stickyNotes.length ? `${deckStore.stickyNotes.length} pages planned` : '等待结构大纲',
    status: deckStore.stickyNotes.length ? 'done' : currentStepStatus('outline'),
  },
  {
    key: 'page-plans',
    label: 'Page Plans',
    description: `${pagePlanReadyCount.value} / ${deckStore.generatedDrafts.length || visibleStickyNotes.value.length}`,
    status: pagePlanReadyCount.value ? 'done' : currentStepStatus('page plans'),
  },
  {
    key: 'bento-specs',
    label: 'Bento Specs',
    description: `${visualSpecReadyCount.value} / ${deckStore.generatedDrafts.length || visibleStickyNotes.value.length}`,
    status: visualSpecReadyCount.value ? 'done' : currentStepStatus('bento specs'),
  },
  {
    key: 'svg',
    label: 'SVG Pages',
    description: `${svgReadyCount.value} ready, ${failedCount.value} failed`,
    status: svgReadyCount.value && !failedCount.value ? 'done' : currentStepStatus('server agent flow'),
  },
])
const agentFlowDoneCount = computed(() => agentFlowTimeline.value.filter((step) => step.status === 'done').length)
const exportPreviewRows = computed(() =>
  visibleStickyNotes.value.map((note, index) => {
    const draft = deckStore.generatedDrafts.find((item) => item.slideId === note.slideId)

    return {
      slideId: note.slideId,
      order: note.order || index + 1,
      title: draft?.title || note.title || 'Untitled slide',
      status: draft?.status || 'NOT_GENERATED',
      ready: draft?.status === 'SVG_READY',
    }
  }),
)
const exportReadyCount = computed(() => exportPreviewRows.value.filter((row) => row.ready).length)

onMounted(() => {
  aiSettingsStore.loadSettings().catch(() => undefined)
})

const visibleStickyNotes = computed(() => {
  if (deckStore.stickyNotes.length) {
    return deckStore.stickyNotes
  }

  return deckStore.outline.slides.map((slide, index) => ({
    slideId: slide.id,
    order: index + 1,
    sectionTitle: deckStore.outline.structure.find((section) => section.id === slide.sectionId)?.title || '',
    title: slide.title,
    message: slide.message,
    status: 'planned',
    tags: [slide.type],
  }))
})

async function runAction(action: () => Promise<unknown>) {
  try {
    await action()
  } catch {
    ElMessage.error(deckStore.errorMessage || '操作失败')
  }
}

async function createOnePage(slideId: string) {
  if (!deckStore.deckId) {
    await deckStore.createDraft()
  }

  creatingSlideId.value = slideId

  try {
    const response = await createOnePageDraftFromDeckSlide(deckStore.deckId, slideId)
    await onePageStore.loadDraft(response.data.draftId)
    await router.push('/app/one-page')
  } finally {
    creatingSlideId.value = ''
  }
}

function setPrimaryTag(note: { tags?: string[] }, value: string) {
  note.tags = [value]
}

function validationWarningCount(slideId: string) {
  return deckStore.slideValidationWarnings[slideId]?.length ?? 0
}

function validationWarnings(slideId: string) {
  return deckStore.slideValidationWarnings[slideId] ?? []
}

function deckSourceLabel(sourceId: string) {
  const source = deckStore.researchPack.sources.find((item) => item.id === sourceId)

  if (!source) {
    return sourceId
  }

  return source.title || source.publisher || source.id || sourceId
}

function currentStepStatus(step: string) {
  return fullPipelineRunning.value && fullPipelineStep.value === step ? 'active' : 'pending'
}

async function rerunAgentStep(stepKey: string) {
  const actions: Record<string, () => Promise<unknown>> = {
    consult: deckStore.consultDeck,
    research: deckStore.generateDeckResearch,
    outline: deckStore.generateDeckOutline,
    'page-plans': deckStore.createAllPagePlanDrafts,
    'bento-specs': deckStore.createAllVisualSpecDrafts,
    svg: deckStore.generateAllSlideSvgs,
  }
  const action = actions[stepKey]

  if (!action) {
    return
  }

  fullPipelineStep.value = stepKey
  await action()
  fullPipelineStep.value = ''
  ElMessage.success(`${stepKey} rerun complete`)
}

function toggleTrace(runId: string) {
  activeTraceId.value = activeTraceId.value === runId ? '' : runId
}

async function batchCreateDrafts() {
  await deckStore.createAllOnePageDrafts()
  ElMessage.success(`已生成 ${deckStore.generatedDrafts.length} 个单页草稿`)
}

async function batchCreatePlans() {
  await deckStore.createAllPagePlanDrafts()
  const readyCount = deckStore.generatedDrafts.filter((draft) => draft.status === 'PAGE_PLAN_READY').length
  ElMessage.success(`Page plans ready ${readyCount} / ${deckStore.generatedDrafts.length}`)
}

async function batchCreateVisuals() {
  await deckStore.createAllVisualSpecDrafts()
  const readyCount = deckStore.generatedDrafts.filter((draft) => draft.status === 'VISUAL_SPEC_READY').length
  ElMessage.success(`Bento specs ready ${readyCount} / ${deckStore.generatedDrafts.length}`)
}

async function dropStickyNote(targetSlideId: string) {
  if (!draggingSlideId.value) {
    return
  }

  await deckStore.reorderStickyNotes(draggingSlideId.value, targetSlideId)
  draggingSlideId.value = ''
}

async function batchGenerateSvgs() {
  await deckStore.generateAllSlideSvgs()
  ElMessage.success(`已生成 ${svgReadyCount.value} 个 SVG 页面`)
}

async function openGeneratedDraft(draftId: string) {
  if (!draftId) {
    return
  }

  await onePageStore.loadDraft(draftId)
  await router.push('/app/one-page')
}

async function retrySlideSvg(slideId: string) {
  await deckStore.retrySlideSvg(slideId)
  const draft = deckStore.generatedDrafts.find((item) => item.slideId === slideId)

  if (draft?.status === 'SVG_READY') {
    ElMessage.success('本页 SVG 已重新生成')
  }
}

async function retryFailedSvgs() {
  await deckStore.retryFailedSlideSvgs()
  ElMessage.success(`Retry complete: ${svgReadyCount.value} SVG ready`)
}

async function runFullAgentFlow() {
  fullPipelineRunning.value = true
  fullPipelineStep.value = 'server agent flow'

  try {
    await deckStore.runDeckAgentFlow()
    fullPipelineStep.value = 'complete'
    ElMessage.success(`Agent flow complete: ${svgReadyCount.value} SVG ready`)
  } finally {
    fullPipelineRunning.value = false
  }
}

async function downloadDeckPptx() {
  const blobPart = await deckStore.exportDeckPptx()
  const blob = new Blob([blobPart], {
    type: 'application/vnd.openxmlformats-officedocument.presentationml.presentation',
  })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')

  link.href = url
  link.download = `slideforge-deck-${deckStore.deckId.slice(0, 8) || 'draft'}.pptx`
  link.click()
  URL.revokeObjectURL(url)
}
</script>

<style scoped lang="scss">
.deck-page {
  display: grid;
  min-height: calc(100vh - 128px);
  grid-template-columns: 320px minmax(0, 1fr) 320px;
  gap: 14px;
}

.deck-page--right-collapsed {
  grid-template-columns: 320px minmax(0, 1fr) 48px;
}

.deck-sidebar,
.deck-main,
.deck-agent-panel {
  min-height: 0;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
}

.deck-sidebar {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px;

  header p,
  header h2 {
    margin: 0;
  }

  header p {
    color: #2563eb;
    font-size: 12px;
    font-weight: 800;
  }

  header h2 {
    margin-top: 6px;
    font-size: 22px;
  }
}

.deck-agent-panel {
  display: grid;
  align-content: start;
  gap: 14px;
  padding: 14px;
}

.deck-agent-panel__toggle {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  justify-self: end;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
  color: #374151;
  cursor: pointer;

  &:hover {
    border-color: #bfdbfe;
    color: #1d4ed8;
  }
}

.deck-actions,
.sticky-note__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.deck-meta {
  display: grid;
  gap: 6px;
  color: #6b7280;
  font-size: 12px;
}

.deck-consult {
  display: grid;
  gap: 6px;
  padding: 12px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #eff6ff;

  p,
  span {
    margin: 0;
  }

  p {
    color: #1d4ed8;
    font-size: 12px;
    font-weight: 800;
  }

  span {
    color: #374151;
    font-size: 13px;
    line-height: 1.6;
  }
}

.deck-progress {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;

  article {
    display: grid;
    gap: 3px;
    padding: 10px;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    background: #ffffff;
  }

  strong {
    color: #111827;
    font-size: 18px;
  }

  span {
    color: #6b7280;
    font-size: 11px;
  }

  .is-danger {
    border-color: #fecaca;
    background: #fef2f2;

    strong {
      color: #b91c1c;
    }
  }
}

.agent-timeline {
  display: grid;
  gap: 8px;

  header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
  }

  header p,
  header span {
    margin: 0;
    color: #2563eb;
    font-size: 12px;
    font-weight: 800;
  }

  article {
    display: grid;
    grid-template-columns: 14px minmax(0, 1fr) auto;
    align-items: start;
    gap: 8px;
    padding: 9px;
    border: 1px solid #edf2f7;
    border-radius: 8px;
    background: #ffffff;
  }

  i {
    width: 10px;
    height: 10px;
    margin-top: 4px;
    border-radius: 999px;
    background: #d1d5db;
  }

  strong,
  span {
    display: block;
  }

  strong {
    color: #111827;
    font-size: 12px;
  }

  span {
    margin-top: 2px;
    color: #6b7280;
    font-size: 11px;
    line-height: 1.45;
  }

  .is-done i {
    background: #16a34a;
  }

  .is-active {
    border-color: #bfdbfe;
    background: #eff6ff;

    i {
      background: #2563eb;
    }
  }
}

.export-preview {
  display: grid;
  gap: 8px;

  header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
  }

  header p,
  header span {
    margin: 0;
    color: #2563eb;
    font-size: 12px;
    font-weight: 800;
  }

  article {
    display: grid;
    gap: 3px;
    padding: 9px;
    border: 1px solid #fecaca;
    border-radius: 8px;
    background: #fff7ed;
  }

  strong {
    overflow: hidden;
    color: #111827;
    font-size: 12px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  span {
    color: #9a3412;
    font-size: 11px;
  }

  .is-ready {
    border-color: #bbf7d0;
    background: #f0fdf4;

    span {
      color: #166534;
    }
  }
}

.deck-trace {
  display: grid;
  gap: 8px;
  max-height: 220px;
  overflow: auto;

  header,
  article {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    align-items: center;
    gap: 8px;
  }

  header p,
  strong,
  span,
  em {
    margin: 0;
  }

  header p {
    color: #2563eb;
    font-size: 12px;
    font-weight: 800;
  }

  article {
    padding: 10px;
    border: 1px solid #edf2f7;
    border-radius: 8px;
    background: #ffffff;
  }

  button {
    display: grid;
    min-width: 0;
    padding: 0;
    border: 0;
    background: transparent;
    text-align: left;
    cursor: pointer;
  }

  strong,
  span {
    display: block;
  }

  strong {
    color: #111827;
    font-size: 13px;
  }

  span,
  em {
    color: #6b7280;
    font-size: 11px;
    font-style: normal;
  }

  em {
    grid-column: 1 / -1;
  }
}

.deck-trace__detail {
  display: grid;
  grid-column: 1 / -1;
  gap: 6px;
  min-width: 0;

  label {
    color: #374151;
    font-size: 11px;
    font-weight: 800;
  }

  pre {
    max-height: 120px;
    overflow: auto;
    margin: 0;
    padding: 8px;
    border-radius: 6px;
    background: #f9fafb;
    color: #111827;
    font-size: 11px;
    line-height: 1.5;
    white-space: pre-wrap;
    word-break: break-word;
  }
}

.deck-main {
  display: grid;
  align-content: start;
  gap: 18px;
  overflow: auto;
  padding: 22px;
}

.outline-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;

  p,
  h1 {
    margin: 0;
  }

  p {
    color: #2563eb;
    font-size: 12px;
    font-weight: 800;
  }

  h1 {
    margin-top: 6px;
    font-size: 26px;
  }
}

.thesis-band {
  padding: 20px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #eff6ff;

  p,
  h2,
  span {
    margin: 0;
  }

  p {
    color: #1d4ed8;
    font-size: 12px;
    font-weight: 800;
  }

  h2 {
    margin: 8px 0;
    color: #111827;
    font-size: 22px;
    line-height: 1.35;
  }

  span {
    color: #475569;
  }
}

.research-pack {
  display: grid;
  gap: 12px;
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;

  header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;
  }

  p,
  h2 {
    margin: 0;
  }

  p {
    color: #2563eb;
    font-size: 12px;
    font-weight: 800;
  }

  h2 {
    margin-top: 6px;
    color: #111827;
    font-size: 18px;
    line-height: 1.45;
  }
}

.research-pack__points,
.research-pack__evidence,
.research-pack__sources {
  display: grid;
  gap: 8px;
}

.research-pack__points article {
  padding: 10px 12px;
  border-radius: 8px;
  background: #f9fafb;
  color: #374151;
  line-height: 1.55;
}

.research-pack__evidence {
  grid-template-columns: repeat(2, minmax(0, 1fr));

  article {
    display: grid;
    gap: 8px;
    padding: 12px;
    border: 1px solid #dbeafe;
    border-radius: 8px;
    background: #eff6ff;
  }

  strong,
  p {
    margin: 0;
  }

  strong {
    color: #111827;
    font-size: 14px;
  }

  p {
    color: #4b5563;
    font-size: 13px;
    line-height: 1.55;
  }

  div {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }
}

.research-pack__sources a {
  color: #2563eb;
  font-size: 13px;
  font-weight: 700;
  text-decoration: none;
}

.outline-grid,
.sticky-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.outline-grid article,
.sticky-note {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
}

.outline-grid article {
  padding: 16px;

  p,
  h3,
  span {
    margin: 0;
  }

  p {
    color: #6b7280;
    font-size: 12px;
    font-weight: 700;
  }

  h3 {
    margin: 8px 0;
    font-size: 17px;
  }

  span {
    color: #4b5563;
    line-height: 1.6;
  }
}

.sticky-board {
  display: grid;
  gap: 12px;

  header p,
  header h2 {
    margin: 0;
  }

  header p {
    color: #2563eb;
    font-size: 12px;
    font-weight: 800;
  }

  header h2 {
    margin-top: 4px;
    font-size: 20px;
  }
}

.generated-drafts {
  display: grid;
  gap: 12px;

  header p,
  header h2 {
    margin: 0;
  }

  header p {
    color: #2563eb;
    font-size: 12px;
    font-weight: 800;
  }

  header h2 {
    margin-top: 4px;
    font-size: 20px;
  }
}

.generated-drafts__list {
  display: grid;
  gap: 10px;

  article {
    display: grid;
    grid-template-columns: minmax(0, 1fr) 160px auto auto;
    align-items: center;
    gap: 12px;
    padding: 12px;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    background: #ffffff;
  }

  strong,
  span,
  small {
    display: block;
  }

  strong {
    color: #111827;
    font-size: 14px;
  }

  span,
  code {
    color: #6b7280;
    font-size: 12px;
  }

  small {
    margin-top: 4px;
    color: #b91c1c;
    font-size: 12px;
    line-height: 1.4;
  }
}

.generated-drafts__preview {
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  aspect-ratio: 16 / 9;
  background: #f9fafb;

  :deep(svg) {
    display: block;
    width: 100%;
    height: 100%;
  }
}

.generated-drafts__warnings {
  display: grid;
  max-height: 96px;
  gap: 3px;
  overflow: auto;
  margin: 6px 0 0;
  padding: 0;
  list-style: none;

  li {
    padding-left: 8px;
    border-left: 2px solid #fca5a5;
    color: #991b1b;
    font-size: 11px;
    line-height: 1.45;
  }
}

.generated-drafts__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.sticky-note {
  display: grid;
  min-height: 220px;
  gap: 10px;
  padding: 14px;
  background: #fffbeb;
  cursor: grab;

  &.is-dragging {
    border-color: #f59e0b;
    opacity: 0.62;
    cursor: grabbing;
    box-shadow: 0 10px 24px rgba(146, 64, 14, 0.14);
  }
}

.sticky-note__top {
  display: flex;
  align-items: center;
  justify-content: space-between;

  span {
    display: grid;
    width: 28px;
    height: 28px;
    place-items: center;
    border-radius: 999px;
    background: #111827;
    color: #ffffff;
    font-size: 12px;
    font-weight: 800;
  }
}

@media (max-width: 1080px) {
  .deck-page {
    grid-template-columns: 1fr;
  }

  .outline-grid,
  .research-pack__evidence,
  .sticky-grid,
  .generated-drafts__list article {
    grid-template-columns: 1fr;
  }
}
</style>
