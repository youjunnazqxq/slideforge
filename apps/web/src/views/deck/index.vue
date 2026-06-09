<template>
  <section class="deck-page">
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
      </div>

      <el-alert
        v-if="deckStore.errorMessage"
        :closable="false"
        :title="deckStore.errorMessage"
        type="error"
      />

      <section class="deck-meta">
        <span>{{ deckStore.status }}</span>
        <span v-if="deckStore.deckId">Deck {{ deckStore.deckId.slice(0, 8) }}</span>
      </section>
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
          <article v-for="note in visibleStickyNotes" :key="note.slideId" class="sticky-note">
            <div class="sticky-note__top">
              <span>{{ note.order }}</span>
              <el-tag size="small">{{ note.tags?.[0] || 'slide' }}</el-tag>
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
    </main>
  </section>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

import { createOnePageDraftFromDeckSlide } from '@/api/modules/deck'
import { useDeckDraftStore, useOnePageDraftStore } from '@/stores'

const deckStore = useDeckDraftStore()
const onePageStore = useOnePageDraftStore()
const router = useRouter()
const creatingSlideId = ref('')

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
</script>

<style scoped lang="scss">
.deck-page {
  display: grid;
  min-height: calc(100vh - 128px);
  grid-template-columns: 340px minmax(0, 1fr);
  gap: 14px;
}

.deck-sidebar,
.deck-main {
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

.deck-actions,
.sticky-note__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.deck-meta {
  display: grid;
  gap: 6px;
  margin-top: auto;
  color: #6b7280;
  font-size: 12px;
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

.sticky-note {
  display: grid;
  min-height: 220px;
  gap: 10px;
  padding: 14px;
  background: #fffbeb;
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
  .sticky-grid {
    grid-template-columns: 1fr;
  }
}
</style>
