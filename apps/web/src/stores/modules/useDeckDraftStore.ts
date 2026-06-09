import { defineStore } from 'pinia'
import { computed, reactive, ref } from 'vue'

import {
  createDeckDraft,
  generateDeckOutline as requestGenerateDeckOutline,
  getDeckDraft,
  type DeckDraftResponse,
  type DeckOutlineResponse,
  type SlideStickyNoteResponse,
} from '@/api/modules/deck'

export const useDeckDraftStore = defineStore(
  'deckDraft',
  () => {
    const deckId = ref('')
    const status = ref('LOCAL_PREVIEW')
    const loadingStage = ref<'create' | 'outline' | ''>('')
    const errorMessage = ref('')
    const initialPrompt = ref('我想做一套关于 AI PPT Agent 项目可行性的内部立项汇报，目标是说服团队先投入一页 MVP。')
    const stickyNotes = ref<SlideStickyNoteResponse[]>([])
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

    return {
      deckId,
      errorMessage,
      initialPrompt,
      isLoading,
      loadingStage,
      outline,
      status,
      stickyNotes,
      createDraft,
      generateDeckOutline,
      loadDraft,
    }
  },
  {
    persist: {
      key: 'slideforge:deck-draft',
      paths: ['deckId', 'status', 'initialPrompt', 'outline', 'stickyNotes'],
    },
  },
)
