import { defineStore } from 'pinia'
import { computed, reactive, ref } from 'vue'

import {
  addDeckStickyNote,
  createDeckDraft,
  createOnePageDraftsFromDeck,
  createPagePlanDraftsFromDeck,
  createSvgDraftFromDeckSlide,
  createSvgDraftsFromDeck,
  createVisualSpecDraftsFromDeck,
  deleteDeckStickyNote,
  generateDeckResearch as requestGenerateDeckResearch,
  generateDeckOutline as requestGenerateDeckOutline,
  getDeckDraft,
  saveDeckStickyNotes,
  type DeckDraftResponse,
  type DeckOutlineResponse,
  type DeckResearchPackResponse,
  type DeckSlideDraftResponse,
  type SlideStickyNoteResponse,
} from '@/api/modules/deck'
import { exportOnePageDraftsPptx } from '@/api/modules/onePage'
import { getWorkflowRuns, type WorkflowRunResponse } from '@/api/modules/workflow'

export const useDeckDraftStore = defineStore(
  'deckDraft',
  () => {
    const deckId = ref('')
    const status = ref('LOCAL_PREVIEW')
    const loadingStage = ref<'create' | 'outline' | ''>('')
    const errorMessage = ref('')
    const initialPrompt = ref('我想做一套关于 AI PPT Agent 项目可行性的内部立项汇报，目标是说服团队先投入一页 MVP。')
    const researchMode = ref<'model-only' | 'search-assisted'>('model-only')
    const stickyNotes = ref<SlideStickyNoteResponse[]>([])
    const generatedDrafts = ref<DeckSlideDraftResponse[]>([])
    const workflowRuns = ref<WorkflowRunResponse[]>([])
    const researchPack = ref<DeckResearchPackResponse>({
      mode: 'model-only',
      summary: '',
      keyPoints: [],
      evidence: [],
      sources: [],
      limitations: [],
    })
    const outline = reactive<DeckOutlineResponse>({
      title: 'AI PPT Agent 项目可行性汇报',
      audience: '团队内部成员',
      scenario: '项目立项讨论',
      coreThesis: '先用一页 MVP 验证工作流质量，再扩展完整 PPT SaaS。',
      structure: [
        { id: 'section-1', title: '为什么现在做', purpose: '说明机会和痛点' },
        { id: 'section-2', title: '先做什么', purpose: '收敛到一页 MVP' },
      ],
      slides: [
        {
          id: 'slide-001',
          type: 'cover',
          sectionId: 'section-1',
          title: 'AI PPT Agent 项目可行性汇报',
          message: '从需求调研到 Bento Grid SVG 的可控工作流。',
          purpose: '建立主题和讨论范围',
        },
        {
          id: 'slide-002',
          type: 'content',
          sectionId: 'section-2',
          title: '先验证一页闭环',
          message: '一页 MVP 更适合验证需求、策划稿、SVG 质量和 BYOK 接入。',
          purpose: '给出第一阶段建议',
        },
      ],
    })

    const isLoading = computed(() => Boolean(loadingStage.value))

    async function ensureDeck() {
      if (deckId.value) {
        return deckId.value
      }

      return createDraft()
    }

    async function createDraft() {
      return runWithLoading('create', async () => {
        const response = await createDeckDraft({ initialPrompt: initialPrompt.value })
        deckId.value = response.data.deckId
        status.value = response.data.status
        return deckId.value
      })
    }

    async function loadDraft(nextDeckId = deckId.value) {
      if (!nextDeckId) {
        return
      }

      await runWithLoading('create', async () => {
        const response = await getDeckDraft(nextDeckId)
        applyDraft(response.data)
        await loadWorkflowRuns(response.data.deckId)
      })
    }

    async function generateDeckOutline() {
      const id = await ensureDeck()

      await runWithLoading('outline', async () => {
        const response = await requestGenerateDeckOutline(id)
        applyOutline(response.data)
        await loadDraft(id)
      })
    }

    async function generateDeckResearch() {
      const id = await ensureDeck()

      await runWithLoading('outline', async () => {
        const response = await requestGenerateDeckResearch(id, { mode: researchMode.value })
        researchPack.value = response.data
        status.value = 'RESEARCH_READY'
        await loadWorkflowRuns(id)
      })
    }

    async function loadWorkflowRuns(nextDeckId = deckId.value) {
      if (!nextDeckId) {
        workflowRuns.value = []
        return
      }

      const response = await getWorkflowRuns(nextDeckId)
      workflowRuns.value = response.data
    }

    async function saveStickyNotes() {
      const id = await ensureDeck()

      await runWithLoading('outline', async () => {
        const response = await saveDeckStickyNotes(id, stickyNotes.value)
        stickyNotes.value = response.data
      })
    }

    async function addStickyNote() {
      const id = await ensureDeck()

      await runWithLoading('outline', async () => {
        const response = await addDeckStickyNote(id, {
          sectionTitle: '新增章节',
          title: '新页面标题',
          message: '补充这一页要表达的核心信息。',
          status: 'planned',
          tags: ['content'],
        })
        stickyNotes.value = [...stickyNotes.value, response.data]
        await saveStickyNotes()
      })
    }

    async function deleteStickyNote(slideId: string) {
      const id = await ensureDeck()

      await runWithLoading('outline', async () => {
        const response = await deleteDeckStickyNote(id, slideId)
        stickyNotes.value = response.data
      })
    }

    async function moveStickyNote(slideId: string, direction: -1 | 1) {
      const currentIndex = stickyNotes.value.findIndex((note) => note.slideId === slideId)
      const nextIndex = currentIndex + direction

      if (currentIndex < 0 || nextIndex < 0 || nextIndex >= stickyNotes.value.length) {
        return
      }

      const notes = [...stickyNotes.value]
      const item = notes[currentIndex]

      if (!item) {
        return
      }

      notes.splice(currentIndex, 1)
      notes.splice(nextIndex, 0, item)
      stickyNotes.value = notes.map((note, index) => ({ ...note, order: index + 1 }))
      await saveStickyNotes()
    }

    async function reorderStickyNotes(sourceSlideId: string, targetSlideId: string) {
      if (sourceSlideId === targetSlideId) {
        return
      }

      const sourceIndex = stickyNotes.value.findIndex((note) => note.slideId === sourceSlideId)
      const targetIndex = stickyNotes.value.findIndex((note) => note.slideId === targetSlideId)

      if (sourceIndex < 0 || targetIndex < 0) {
        return
      }

      const notes = [...stickyNotes.value]
      const item = notes[sourceIndex]

      if (!item) {
        return
      }

      notes.splice(sourceIndex, 1)
      notes.splice(targetIndex, 0, item)
      stickyNotes.value = notes.map((note, index) => ({ ...note, order: index + 1 }))
      await saveStickyNotes()
    }

    async function createAllOnePageDrafts() {
      const id = await ensureDeck()

      await runWithLoading('outline', async () => {
        await saveStickyNotes()
        const response = await createOnePageDraftsFromDeck(id)
        generatedDrafts.value = response.data
      })
    }

    async function createAllPagePlanDrafts() {
      const id = await ensureDeck()

      await runWithLoading('outline', async () => {
        await saveStickyNotes()
        const response = await createPagePlanDraftsFromDeck(id)
        generatedDrafts.value = response.data
      })
    }

    async function createAllVisualSpecDrafts() {
      const id = await ensureDeck()

      await runWithLoading('outline', async () => {
        await saveStickyNotes()
        const response = await createVisualSpecDraftsFromDeck(id)
        generatedDrafts.value = response.data
      })
    }

    async function generateAllSlideSvgs() {
      const id = await ensureDeck()

      await runWithLoading('outline', async () => {
        await saveStickyNotes()
        const response = await createSvgDraftsFromDeck(id)
        generatedDrafts.value = response.data
      })
    }

    async function retrySlideSvg(slideId: string) {
      const id = await ensureDeck()

      await runWithLoading('outline', async () => {
        const response = await createSvgDraftFromDeckSlide(id, slideId)
        generatedDrafts.value = upsertGeneratedDraft(response.data)
      })
    }

    async function exportDeckPptx() {
      if (!generatedDrafts.value.length || generatedDrafts.value.some((draft) => draft.status !== 'SVG_READY')) {
        await generateAllSlideSvgs()
      }

      if (generatedDrafts.value.some((draft) => draft.status !== 'SVG_READY')) {
        errorMessage.value = '仍有页面 SVG 生成失败，请先重试失败页面后再导出 PPTX。'
        throw new Error(errorMessage.value)
      }

      const draftIds = generatedDrafts.value.map((draft) => draft.draftId)
      return runWithLoading('outline', async () => exportOnePageDraftsPptx(draftIds))
    }

    async function runWithLoading<T>(stage: 'create' | 'outline', task: () => Promise<T>) {
      loadingStage.value = stage
      errorMessage.value = ''

      try {
        return await task()
      } catch (error) {
        errorMessage.value = error instanceof Error ? error.message : '请求失败，请稍后重试'
        throw error
      } finally {
        loadingStage.value = ''
      }
    }

    function applyDraft(draft: DeckDraftResponse) {
      deckId.value = draft.deckId
      status.value = draft.status
      initialPrompt.value = draft.initialPrompt || initialPrompt.value
      generatedDrafts.value = draft.generatedDrafts ?? []
      researchPack.value = draft.researchPack ?? researchPack.value

      if (draft.outline) {
        applyOutline(draft.outline)
      }

      stickyNotes.value = draft.stickyNotes ?? []
    }

    function applyOutline(nextOutline: DeckOutlineResponse) {
      outline.title = nextOutline.title
      outline.audience = nextOutline.audience
      outline.scenario = nextOutline.scenario
      outline.coreThesis = nextOutline.coreThesis
      outline.structure = nextOutline.structure ?? []
      outline.slides = nextOutline.slides ?? []
    }

    function upsertGeneratedDraft(nextDraft: DeckSlideDraftResponse) {
      const nextDrafts = generatedDrafts.value.map((draft) =>
        draft.slideId === nextDraft.slideId ? nextDraft : draft,
      )

      if (!nextDrafts.some((draft) => draft.slideId === nextDraft.slideId)) {
        nextDrafts.push(nextDraft)
      }

      return nextDrafts.sort((first, second) => first.order - second.order)
    }

    return {
      deckId,
      errorMessage,
      generatedDrafts,
      initialPrompt,
      isLoading,
      loadingStage,
      outline,
      researchMode,
      researchPack,
      status,
      stickyNotes,
      workflowRuns,
      addStickyNote,
      createDraft,
      createAllOnePageDrafts,
      createAllPagePlanDrafts,
      createAllVisualSpecDrafts,
      deleteStickyNote,
      exportDeckPptx,
      generateAllSlideSvgs,
      generateDeckOutline,
      generateDeckResearch,
      loadDraft,
      loadWorkflowRuns,
      moveStickyNote,
      reorderStickyNotes,
      retrySlideSvg,
      saveStickyNotes,
    }
  },
  {
    persist: {
      key: 'slideforge:deck-draft',
      paths: ['deckId', 'status', 'initialPrompt', 'researchMode', 'researchPack', 'outline', 'stickyNotes', 'generatedDrafts'],
    },
  },
)
