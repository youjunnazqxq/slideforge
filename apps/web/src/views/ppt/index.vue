<template>
  <section
    class="one-page-workspace"
    :class="{ 'one-page-workspace--collapsed': draftStore.rightPanelCollapsed }"
  >
    <aside class="slide-nav">
      <div class="mode-tabs">
        <button class="is-active" type="button">搜索</button>
        <button type="button">初稿</button>
        <button type="button">设计稿</button>
      </div>

      <div class="slide-nav__title">
        <span>幻灯片</span>
        <strong>共 1 页</strong>
      </div>

      <button class="slide-thumb is-active" type="button" @click="draftStore.setStage('svg')">
        <span class="slide-thumb__index">1</span>
        <span class="slide-thumb__preview">
          <span />
          <span />
          <span />
        </span>
        <strong>一页 MVP</strong>
      </button>

      <div class="slide-nav__footer">
        <el-button :icon="ArrowUp" circle text />
        <span>1 / 1</span>
        <el-button :icon="ArrowDown" circle text />
      </div>
    </aside>

    <main class="work-area">
      <header class="work-header">
        <div>
          <p>One Page Workflow</p>
          <h2>{{ draftStore.pagePlan.slideTitle }}</h2>
        </div>
        <div class="work-header__actions">
          <el-button :loading="draftStore.loadingStage === 'create'" plain @click="runAction(draftStore.createDraft)">
            创建草稿
          </el-button>
          <el-button
            :icon="DocumentChecked"
            :loading="draftStore.loadingStage === 'brief'"
            plain
            @click="runAction(draftStore.generateBrief)"
          >
            生成 Brief
          </el-button>
          <el-button
            :icon="Picture"
            :loading="draftStore.loadingStage === 'svg'"
            type="primary"
            @click="runAction(draftStore.regenerateSvg)"
          >
            生成 SVG
          </el-button>
          <el-button
            :icon="Download"
            :loading="draftStore.loadingStage === 'svg'"
            plain
            @click="runAction(downloadPptx)"
          >
            导出 PPTX
          </el-button>
        </div>
      </header>

      <el-alert
        v-if="draftStore.errorMessage"
        class="workspace-alert"
        :closable="false"
        :title="draftStore.errorMessage"
        type="error"
      />

      <el-alert
        v-if="!aiSettingsStore.isConfigured"
        class="workspace-alert"
        :closable="false"
        title="AI provider is not configured"
        type="warning"
      >
        <template #default>
          <el-button size="small" type="warning" @click="router.push('/app/settings')">Open Settings</el-button>
        </template>
      </el-alert>

      <section class="stage-tabs">
        <button
          v-for="step in draftStore.steps"
          :key="step.key"
          :class="{ 'is-active': draftStore.currentStage === step.key }"
          type="button"
          @click="draftStore.setStage(step.key)"
        >
          <el-icon>
            <CircleCheck v-if="step.status === 'done'" />
            <Clock v-else />
          </el-icon>
          {{ step.label }}
        </button>
      </section>

      <section class="workspace-card">
        <template v-if="draftStore.currentStage === 'consult'">
          <div class="consult-view">
            <div class="message message--user">
              <strong>用户需求</strong>
              <el-input v-model="draftStore.userPrompt" resize="none" :rows="5" type="textarea" />
              <div class="stage-actions">
                <el-button
                  :loading="draftStore.loadingStage === 'consult'"
                  plain
                  type="primary"
                  @click="runAction(draftStore.consult)"
                >
                  发送给 AI 顾问
                </el-button>
                <el-button
                  :loading="draftStore.loadingStage === 'brief'"
                  @click="runAction(draftStore.generateBrief)"
                >
                  生成 brief
                </el-button>
              </div>
            </div>
            <div class="message message--assistant">
              <strong>AI 顾问</strong>
              <p>{{ draftStore.assistantMessage }}</p>
              <ul>
                <li>目标观众：团队内部成员</li>
                <li>使用场景：项目立项讨论</li>
                <li>核心目标：判断是否投入一页 MVP</li>
              </ul>
            </div>
          </div>
        </template>

        <template v-else-if="draftStore.currentStage === 'brief'">
          <div class="form-grid">
            <label v-for="field in briefFields" :key="field.key">
              <span>{{ field.label }}</span>
              <el-input v-model="draftStore.brief[field.key]" :rows="field.rows" type="textarea" />
            </label>
          </div>
          <div class="stage-actions">
            <el-button
              :loading="draftStore.loadingStage === 'brief'"
              plain
              @click="runAction(draftStore.saveBrief)"
            >
              保存 brief
            </el-button>
            <el-button
              :loading="draftStore.loadingStage === 'research'"
              type="primary"
              @click="runAction(draftStore.generateResearch)"
            >
              生成资料包
            </el-button>
          </div>
        </template>

        <template v-else-if="draftStore.currentStage === 'research'">
          <div class="research-view">
            <div>
              <p class="section-kicker">资料摘要</p>
              <h3>{{ draftStore.researchPack.summary }}</h3>
              <el-segmented
                v-model="draftStore.researchMode"
                :options="[
                  { label: 'Model Only', value: 'model-only' },
                  { label: 'Search Assisted', value: 'search-assisted' },
                ]"
              />
            </div>
            <div class="research-list">
              <article v-for="point in draftStore.researchPack.keyPoints" :key="point">
                <el-icon><Check /></el-icon>
                <span>{{ point }}</span>
              </article>
            </div>
            <div v-if="draftStore.researchPack.sources.length" class="research-sources">
              <article v-for="source in draftStore.researchPack.sources" :key="source.id || source.url">
                <div>
                  <strong>{{ source.title || source.url }}</strong>
                  <span>{{ source.publisher || 'Source' }} {{ source.publishedAt ? `· ${source.publishedAt}` : '' }}</span>
                </div>
                <p>{{ source.snippet }}</p>
                <a :href="source.url" rel="noreferrer" target="_blank">打开来源</a>
              </article>
            </div>
            <div v-if="draftStore.researchPack.limitations.length" class="research-limitations">
              <strong>Limitations</strong>
              <span v-for="item in draftStore.researchPack.limitations" :key="item">{{ item }}</span>
            </div>
            <el-alert
              :closable="false"
              show-icon
              title="当前为 model-only 资料整理，后续可接入联网检索并保留 sources。"
              type="info"
            />
            <div class="stage-actions">
              <el-button
                :loading="draftStore.loadingStage === 'pagePlan'"
                type="primary"
                @click="runAction(draftStore.generatePagePlan)"
              >
                生成页面策划稿
              </el-button>
            </div>
          </div>
        </template>

        <template v-else-if="draftStore.currentStage === 'pagePlan'">
          <div class="page-plan-view">
            <label>
              <span>页面标题</span>
              <el-input v-model="draftStore.pagePlan.slideTitle" />
            </label>
            <label>
              <span>核心信息</span>
              <el-input v-model="draftStore.pagePlan.coreMessage" :rows="3" type="textarea" />
            </label>
            <label>
              <span>布局意图</span>
              <el-input v-model="draftStore.pagePlan.layoutIntent" :rows="3" type="textarea" />
            </label>
            <div class="plan-blocks">
              <article v-for="block in draftStore.pagePlan.contentBlocks" :key="block.id">
                <strong>{{ block.title }}</strong>
                <span>{{ block.role }}</span>
                <p>{{ block.content }}</p>
              </article>
            </div>
            <div class="stage-actions">
              <el-button
                :loading="draftStore.loadingStage === 'pagePlan'"
                plain
                @click="runAction(draftStore.savePagePlan)"
              >
                保存策划稿
              </el-button>
              <el-button
                :loading="draftStore.loadingStage === 'visualSpec'"
                type="primary"
                @click="runAction(draftStore.generateVisualSpec)"
              >
                生成视觉设计
              </el-button>
            </div>
          </div>
        </template>

        <template v-else-if="draftStore.currentStage === 'visualSpec'">
          <div class="visual-spec-view">
            <div>
              <p class="section-kicker">Bento Grid Visual Spec</p>
              <h3>{{ draftStore.visualSpec.canvas.width }} x {{ draftStore.visualSpec.canvas.height }}</h3>
              <div class="theme-swatches">
                <label v-for="(color, name) in draftStore.visualSpec.theme" :key="name">
                  <span>{{ name }}</span>
                  <input v-model="draftStore.visualSpec.theme[name]" type="color" />
                  <code>{{ color }}</code>
                </label>
              </div>
            </div>

            <div class="visual-canvas">
              <article
                v-for="card in draftStore.visualSpec.cards"
                :key="card.id"
                :class="{ 'is-primary': card.priority === 'primary' }"
                :style="cardStyle(card)"
              >
                <strong>{{ card.id }}</strong>
                <span>{{ card.blockId }}</span>
              </article>
            </div>

            <div class="visual-card-list">
              <article v-for="card in draftStore.visualSpec.cards" :key="card.id">
                <strong>{{ card.id }}</strong>
                <span>{{ card.blockId }} / {{ card.priority }}</span>
                <div class="visual-card-controls">
                  <label>
                    <span>X</span>
                    <el-input-number v-model="card.x" :min="0" :max="1200" size="small" controls-position="right" />
                  </label>
                  <label>
                    <span>Y</span>
                    <el-input-number v-model="card.y" :min="0" :max="660" size="small" controls-position="right" />
                  </label>
                  <label>
                    <span>W</span>
                    <el-input-number v-model="card.w" :min="80" :max="1280" size="small" controls-position="right" />
                  </label>
                  <label>
                    <span>H</span>
                    <el-input-number v-model="card.h" :min="60" :max="720" size="small" controls-position="right" />
                  </label>
                </div>
              </article>
            </div>

            <div class="stage-actions">
              <el-button
                :loading="draftStore.loadingStage === 'visualSpec'"
                plain
                @click="runAction(draftStore.saveVisualSpec)"
              >
                保存视觉规格
              </el-button>
              <el-button
                :loading="draftStore.loadingStage === 'svg'"
                type="primary"
                @click="runAction(draftStore.regenerateSvg)"
              >
                生成 Bento Grid SVG
              </el-button>
            </div>
          </div>
        </template>

        <template v-else>
          <div class="svg-preview">
            <div class="svg-preview__frame" v-html="draftStore.svgContent" />
            <div class="svg-preview__meta">
              <span v-if="draftStore.draftId">Draft {{ draftStore.draftId.slice(0, 8) }}</span>
              <span>viewBox 0 0 1280 720</span>
              <span>Bento Grid</span>
              <span>{{ draftStore.validationWarnings.length ? '有校验提醒' : '校验通过' }}</span>
            </div>
            <section class="svg-quality">
              <header>
                <div>
                  <p>SVG Quality</p>
                  <h3>{{ draftStore.validationWarnings.length ? '需要复核' : '校验通过' }}</h3>
                </div>
                <el-tag :type="draftStore.validationWarnings.length ? 'warning' : 'success'">
                  {{ draftStore.validationWarnings.length }} warnings
                </el-tag>
              </header>
              <ul v-if="draftStore.validationWarnings.length">
                <li v-for="warning in draftStore.validationWarnings" :key="warning">{{ warning }}</li>
              </ul>
              <span v-else>安全、画布和基础布局检查未发现问题。</span>
            </section>
            <section class="svg-source-editor">
              <header>
                <div>
                  <p>SVG Source</p>
                  <h3>Editable final SVG</h3>
                </div>
                <el-button
                  :loading="draftStore.loadingStage === 'svg'"
                  type="primary"
                  @click="runAction(draftStore.saveSvgContent)"
                >
                  Save SVG
                </el-button>
              </header>
              <el-input
                v-model="draftStore.svgContent"
                class="svg-source-editor__input"
                resize="vertical"
                :rows="12"
                spellcheck="false"
                type="textarea"
              />
            </section>
          </div>
        </template>
      </section>
    </main>

    <aside class="assistant-panel">
      <button class="collapse-button" type="button" @click="draftStore.toggleRightPanel">
        <el-icon>
          <ArrowRight v-if="!draftStore.rightPanelCollapsed" />
          <ArrowLeft v-else />
        </el-icon>
      </button>

      <template v-if="!draftStore.rightPanelCollapsed">
        <header class="assistant-panel__header">
          <div>
            <p>AI Assistant</p>
          <h3>Dify 产品介绍</h3>
          </div>
          <el-tag :type="draftStore.isLoading ? 'warning' : 'success'">
            {{ draftStore.isLoading ? '处理中' : draftStore.status }}
          </el-tag>
        </header>

        <div class="assistant-steps">
          <button
            v-for="step in draftStore.steps"
            :key="step.key"
            :class="{ 'is-active': draftStore.currentStage === step.key }"
            type="button"
            @click="draftStore.setStage(step.key)"
          >
            <el-icon><CircleCheck /></el-icon>
            <span>{{ step.label }}</span>
            <strong>{{ step.status }}</strong>
          </button>
        </div>

        <section v-if="draftStore.workflowRuns.length" class="prompt-trace">
          <header>
            <p>Prompt Trace</p>
            <el-button size="small" text @click="runAction(draftStore.loadWorkflowRuns)">刷新</el-button>
          </header>
          <article v-for="run in draftStore.workflowRuns.slice(0, 5)" :key="run.id">
            <div>
              <strong>{{ run.stage }}</strong>
              <span>{{ run.promptKey || 'manual' }}</span>
            </div>
            <em>{{ run.status }} · {{ run.durationMs || 0 }}ms</em>
          </article>
        </section>

        <section class="assistant-result">
          <p>P4</p>
          <h4>{{ draftStore.pagePlan.slideTitle }}</h4>
          <span>{{ draftStore.pagePlan.coreMessage }}</span>
        </section>

        <div class="assistant-input">
          <el-input
            v-model="draftStore.userPrompt"
            placeholder="请输入你的编辑需求..."
            resize="none"
            :rows="4"
            type="textarea"
          />
          <div>
            <el-button
              :icon="Refresh"
              :loading="draftStore.loadingStage === 'svg'"
              plain
              @click="runAction(draftStore.regenerateSvg)"
            >
              重新生成
            </el-button>
            <el-button :icon="Download" type="primary" @click="downloadSvg">下载 SVG</el-button>
          </div>
        </div>
      </template>

      <template v-else>
        <div class="collapsed-rail">
          <el-icon><MagicStick /></el-icon>
          <span>AI</span>
        </div>
      </template>
    </aside>
  </section>
</template>

<script setup lang="ts">
import {
  ArrowDown,
  ArrowLeft,
  ArrowRight,
  ArrowUp,
  Check,
  CircleCheck,
  Clock,
  DocumentChecked,
  Download,
  MagicStick,
  Picture,
  Refresh,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { onMounted, type CSSProperties } from 'vue'
import { useRouter } from 'vue-router'

import { useAiSettingsStore, useOnePageDraftStore } from '@/stores'
import type { RequirementBrief, VisualSpec } from '@/stores'

const draftStore = useOnePageDraftStore()
const aiSettingsStore = useAiSettingsStore()
const router = useRouter()

const briefFields: Array<{
  key: keyof RequirementBrief
  label: string
  rows: number
}> = [
  { key: 'topic', label: '主题', rows: 2 },
  { key: 'audience', label: '目标观众', rows: 2 },
  { key: 'scenario', label: '使用场景', rows: 2 },
  { key: 'goal', label: '目标', rows: 2 },
  { key: 'coreConclusion', label: '核心结论', rows: 3 },
  { key: 'mustInclude', label: '必须包含', rows: 3 },
  { key: 'avoid', label: '避免表达', rows: 3 },
  { key: 'tone', label: '语气风格', rows: 2 },
]

onMounted(() => {
  aiSettingsStore.loadSettings().catch(() => undefined)
})

function downloadSvg() {
  const blob = new Blob([draftStore.svgContent], {
    type: 'image/svg+xml;charset=utf-8',
  })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = 'slideforge-one-page.svg'
  link.click()
  URL.revokeObjectURL(url)
}

async function downloadPptx() {
  const blobPart = await draftStore.exportPptx()
  const blob = new Blob([blobPart], {
    type: 'application/vnd.openxmlformats-officedocument.presentationml.presentation',
  })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')

  link.href = url
  link.download = `slideforge-one-page-${draftStore.draftId.slice(0, 8) || 'draft'}.pptx`
  link.click()
  URL.revokeObjectURL(url)
}

async function runAction(action: () => Promise<unknown>) {
  try {
    await action()
  } catch {
    ElMessage.error(draftStore.errorMessage || '操作失败')
  }
}

function cardStyle(card: VisualSpec['cards'][number]): CSSProperties {
  return {
    left: `${(card.x / 1280) * 100}%`,
    top: `${(card.y / 720) * 100}%`,
    width: `${(card.w / 1280) * 100}%`,
    height: `${(card.h / 720) * 100}%`,
  }
}
</script>

<style scoped lang="scss">
.one-page-workspace {
  display: grid;
  height: calc(100vh - 128px);
  min-height: 680px;
  grid-template-columns: 156px minmax(640px, 1fr) 360px;
  gap: 12px;
}

.one-page-workspace--collapsed {
  grid-template-columns: 156px minmax(640px, 1fr) 56px;
}

.slide-nav,
.work-area,
.assistant-panel {
  min-height: 0;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
}

.slide-nav {
  display: flex;
  flex-direction: column;
  padding: 12px;
}

.mode-tabs {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 4px;
  padding: 4px;
  border-radius: 8px;
  background: #f3f4f6;

  button {
    height: 28px;
    border: 0;
    border-radius: 6px;
    background: transparent;
    color: #6b7280;
    cursor: pointer;
    font-size: 12px;
  }

  .is-active {
    background: #ffffff;
    color: #111827;
    font-weight: 700;
    box-shadow: 0 1px 3px rgba(15, 23, 42, 0.08);
  }
}

.slide-nav__title,
.slide-nav__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.slide-nav__title {
  margin: 18px 0 10px;
  color: #6b7280;
  font-size: 12px;

  strong {
    color: #9ca3af;
    font-weight: 500;
  }
}

.slide-thumb {
  position: relative;
  display: grid;
  gap: 8px;
  padding: 8px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
  color: #111827;
  cursor: pointer;
  text-align: left;

  &.is-active {
    border-color: #2563eb;
    box-shadow: 0 0 0 1px #2563eb inset;
  }

  strong {
    font-size: 12px;
  }
}

.slide-thumb__index {
  position: absolute;
  top: 6px;
  left: 6px;
  display: grid;
  width: 20px;
  height: 20px;
  place-items: center;
  border-radius: 6px;
  background: #2563eb;
  color: #ffffff;
  font-size: 12px;
  font-weight: 700;
}

.slide-thumb__preview {
  display: grid;
  height: 72px;
  place-items: center;
  border-radius: 6px;
  background: #f9fafb;

  span {
    width: 62%;
    height: 8px;
    border-radius: 999px;
    background: #e5e7eb;
  }
}

.slide-nav__footer {
  margin-top: auto;
  color: #6b7280;
  font-size: 12px;
}

.work-area {
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  overflow: hidden;
}

.work-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 22px;
  border-bottom: 1px solid #eef0f3;

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
    margin-top: 5px;
    font-size: 19px;
  }
}

.work-header__actions,
.assistant-input div,
.stage-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.workspace-alert {
  margin: 12px 22px 0;
}

.stage-tabs {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding: 12px 22px;
  border-bottom: 1px solid #eef0f3;

  button {
    display: flex;
    align-items: center;
    gap: 6px;
    height: 34px;
    flex: 0 0 auto;
    border: 1px solid #e5e7eb;
    border-radius: 999px;
    background: #ffffff;
    color: #4b5563;
    cursor: pointer;
    padding: 0 12px;
  }

  .is-active {
    border-color: #bfdbfe;
    background: #eff6ff;
    color: #1d4ed8;
  }
}

.workspace-card {
  min-height: 0;
  overflow: auto;
  padding: 22px;
}

.consult-view,
.research-view,
.page-plan-view,
.visual-spec-view {
  display: grid;
  gap: 16px;
}

.message {
  padding: 18px;
  border-radius: 8px;

  strong {
    display: block;
    margin-bottom: 10px;
  }

  p {
    margin: 0 0 10px;
    color: #374151;
    line-height: 1.7;
  }

  ul {
    margin: 0;
    padding-left: 18px;
    color: #4b5563;
    line-height: 1.8;
  }
}

.stage-actions {
  margin-top: 14px;
}

.message--user {
  background: #f9fafb;
}

.message--assistant {
  background: #eff6ff;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;

  label {
    display: grid;
    gap: 8px;
  }
}

.page-plan-view label {
  display: grid;
  gap: 8px;
}

.form-grid span,
.page-plan-view label span,
.section-kicker {
  color: #6b7280;
  font-size: 12px;
  font-weight: 800;
}

.research-view h3,
.visual-spec-view h3 {
  max-width: 840px;
  margin: 4px 0 0;
  color: #111827;
  font-size: 22px;
  line-height: 1.5;
}

.research-list,
.research-sources,
.plan-blocks,
.visual-card-list {
  display: grid;
  gap: 12px;
}

.research-limitations {
  display: grid;
  gap: 6px;
  padding: 12px;
  border: 1px solid #fed7aa;
  border-radius: 8px;
  background: #fff7ed;
  color: #9a3412;
  font-size: 13px;
}

.research-list article {
  display: flex;
  gap: 10px;
  padding: 14px;
  border-radius: 8px;
  background: #f9fafb;
  color: #374151;
  line-height: 1.7;

  .el-icon {
    margin-top: 4px;
    color: #059669;
  }
}

.research-sources {
  article {
    display: grid;
    gap: 8px;
    padding: 14px;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    background: #ffffff;
  }

  strong,
  span,
  p,
  a {
    margin: 0;
  }

  strong,
  span {
    display: block;
  }

  strong {
    color: #111827;
    font-size: 14px;
  }

  span {
    margin-top: 4px;
    color: #6b7280;
    font-size: 12px;
  }

  p {
    color: #4b5563;
    font-size: 13px;
    line-height: 1.6;
  }

  a {
    color: #2563eb;
    font-size: 13px;
    font-weight: 700;
    text-decoration: none;
  }
}

.plan-blocks {
  grid-template-columns: repeat(3, minmax(0, 1fr));

  article {
    padding: 16px;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    background: #f9fafb;

    strong,
    span {
      display: block;
    }

    strong {
      font-size: 15px;
    }

    span {
      margin: 6px 0 10px;
      color: #2563eb;
      font-size: 12px;
      font-weight: 700;
    }

    p {
      margin: 0;
      color: #4b5563;
      line-height: 1.6;
    }
  }
}

.theme-swatches {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-top: 12px;

  label {
    display: grid;
    grid-template-columns: auto 34px;
    align-items: center;
    gap: 6px;
    padding: 8px;
    border: 1px solid #d1d5db;
    border-radius: 8px;
  }

  span,
  code {
    min-width: 0;
    overflow: hidden;
    color: #4b5563;
    font-size: 11px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  code {
    grid-column: 1 / -1;
    color: #6b7280;
  }

  input {
    width: 34px;
    height: 28px;
    border: 0;
    background: transparent;
    cursor: pointer;
    padding: 0;
  }
}

.visual-canvas {
  position: relative;
  overflow: hidden;
  width: min(100%, 920px);
  border: 1px solid #d1d5db;
  border-radius: 8px;
  aspect-ratio: 16 / 9;
  background:
    linear-gradient(rgba(17, 24, 39, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(17, 24, 39, 0.04) 1px, transparent 1px),
    #f9fafb;
  background-size: 40px 40px;

  article {
    position: absolute;
    display: grid;
    align-content: start;
    gap: 4px;
    overflow: hidden;
    padding: 12px;
    border: 1px solid #bfdbfe;
    border-radius: 8px;
    background: rgba(239, 246, 255, 0.88);
    color: #1e3a8a;
    font-size: 12px;

    &.is-primary {
      border-color: #93c5fd;
      background: rgba(219, 234, 254, 0.95);
      font-size: 14px;
    }

    strong,
    span {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}

.visual-card-list {
  grid-template-columns: repeat(2, minmax(0, 1fr));

  article {
    padding: 14px;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    background: #ffffff;

    strong,
    span {
      display: block;
      margin: 0;
    }

    span {
      margin: 6px 0;
      color: #2563eb;
      font-size: 12px;
      font-weight: 700;
    }

  }
}

.visual-card-controls {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;

  label {
    display: grid;
    gap: 4px;
  }

  span {
    color: #6b7280;
    font-size: 11px;
    font-weight: 800;
  }

  :deep(.el-input-number) {
    width: 100%;
  }
}

.svg-preview {
  display: grid;
  gap: 14px;
}

.svg-preview__frame {
  overflow: hidden;
  width: min(100%, 1040px);
  margin: 0 auto;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  aspect-ratio: 16 / 9;
  background: #f3f4f6;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.1);

  :deep(svg) {
    display: block;
    width: 100%;
    height: 100%;
  }
}

.svg-preview__meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;

  span {
    padding: 6px 10px;
    border-radius: 999px;
    background: #f3f4f6;
    color: #4b5563;
    font-size: 12px;
  }
}

.svg-quality {
  display: grid;
  gap: 10px;
  width: min(100%, 1040px);
  margin: 0 auto;
  padding: 14px;
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
  h3,
  ul,
  li,
  span {
    margin: 0;
  }

  p {
    color: #2563eb;
    font-size: 12px;
    font-weight: 800;
  }

  h3 {
    margin-top: 4px;
    color: #111827;
    font-size: 16px;
  }

  ul {
    display: grid;
    gap: 6px;
    padding-left: 18px;
    color: #92400e;
    font-size: 13px;
    line-height: 1.5;
  }

  > span {
    color: #047857;
    font-size: 13px;
  }
}

.svg-source-editor {
  display: grid;
  gap: 12px;
  width: min(100%, 1040px);
  margin: 0 auto;
  padding: 14px;
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
  h3 {
    margin: 0;
  }

  p {
    color: #2563eb;
    font-size: 12px;
    font-weight: 800;
  }

  h3 {
    margin-top: 4px;
    color: #111827;
    font-size: 16px;
  }
}

.svg-source-editor__input {
  :deep(textarea) {
    font-family: Consolas, 'Liberation Mono', monospace;
    font-size: 12px;
    line-height: 1.5;
  }
}

.assistant-panel {
  position: relative;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto auto;
  gap: 12px;
  overflow: hidden;
  padding: 16px;
}

.collapse-button {
  position: absolute;
  top: 14px;
  left: 12px;
  z-index: 2;
  display: grid;
  width: 28px;
  height: 28px;
  place-items: center;
  border: 1px solid #e5e7eb;
  border-radius: 999px;
  background: #ffffff;
  color: #4b5563;
  cursor: pointer;
}

.assistant-panel__header {
  padding-left: 28px;

  p,
  h3 {
    margin: 0;
  }

  p {
    color: #2563eb;
    font-size: 12px;
    font-weight: 800;
  }

  h3 {
    margin: 5px 0 12px;
    font-size: 17px;
  }
}

.assistant-steps {
  display: grid;
  align-content: start;
  gap: 8px;
  overflow: auto;

  button {
    display: grid;
    grid-template-columns: auto minmax(0, 1fr) auto;
    align-items: center;
    gap: 8px;
    min-height: 42px;
    border: 1px solid #edf2f7;
    border-radius: 8px;
    background: #f8fbff;
    color: #374151;
    cursor: pointer;
    padding: 0 10px;
    text-align: left;
  }

  .is-active {
    border-color: #bfdbfe;
    background: #eff6ff;
  }

  .el-icon {
    color: #2563eb;
  }

  strong {
    color: #2563eb;
    font-size: 11px;
    text-transform: uppercase;
  }
}

.prompt-trace {
  display: grid;
  gap: 8px;
  overflow: auto;
  max-height: 220px;

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
}

.assistant-result {
  padding: 14px;
  border-radius: 8px;
  background: #f9fafb;

  p,
  h4,
  span {
    margin: 0;
  }

  p {
    color: #2563eb;
    font-weight: 800;
  }

  h4 {
    margin: 6px 0;
    font-size: 14px;
  }

  span {
    color: #6b7280;
    font-size: 13px;
    line-height: 1.6;
  }
}

.assistant-input {
  display: grid;
  gap: 10px;
}

.collapsed-rail {
  display: grid;
  height: 100%;
  place-items: center;
  color: #2563eb;
  font-weight: 800;
  writing-mode: vertical-rl;
}

@media (max-width: 1180px) {
  .one-page-workspace,
  .one-page-workspace--collapsed {
    height: auto;
    grid-template-columns: 132px minmax(0, 1fr);
  }

  .assistant-panel {
    grid-column: 1 / -1;
    min-height: 420px;
  }
}

@media (max-width: 760px) {
  .one-page-workspace,
  .one-page-workspace--collapsed {
    grid-template-columns: 1fr;
  }

  .slide-nav {
    display: none;
  }

  .form-grid,
  .plan-blocks,
  .visual-card-list {
    grid-template-columns: 1fr;
  }

  .work-header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
