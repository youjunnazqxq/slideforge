import { defineStore } from 'pinia'
import { computed, reactive, ref } from 'vue'

export type WorkflowStage = 'consult' | 'brief' | 'research' | 'pagePlan' | 'svg'
export type WorkflowStatus = 'pending' | 'running' | 'done' | 'failed'

export interface WorkflowStepState {
  key: WorkflowStage
  label: string
  status: WorkflowStatus
}

export interface RequirementBrief {
  topic: string
  audience: string
  scenario: string
  goal: string
  coreConclusion: string
  tone: string
  mustInclude: string
  avoid: string
}

export interface ResearchPack {
  summary: string
  keyPoints: string[]
  sources: string[]
}

export interface PagePlanBlock {
  id: string
  role: string
  title: string
  content: string
}

export interface PagePlan {
  slideTitle: string
  coreMessage: string
  audienceTakeaway: string
  layoutIntent: string
  visualStyle: string
  contentBlocks: PagePlanBlock[]
}

const defaultSvg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1280 720" width="1280" height="720">
  <rect width="1280" height="720" fill="#F7F8FA"/>
  <rect x="56" y="56" width="1168" height="608" rx="24" fill="#FFFFFF" stroke="#E5E7EB"/>
  <text x="92" y="118" fill="#2563EB" font-size="24" font-family="Arial, sans-serif" font-weight="700">SlideForge 一页 MVP</text>
  <text x="92" y="172" fill="#111827" font-size="44" font-family="Arial, sans-serif" font-weight="700">先验证一页闭环</text>
  <text x="92" y="226" fill="#4B5563" font-size="24" font-family="Arial, sans-serif">需求 brief、页面策划稿和 Bento Grid SVG 是当前核心链路。</text>
  <rect x="92" y="292" width="520" height="280" rx="18" fill="#EFF6FF"/>
  <text x="128" y="354" fill="#1D4ED8" font-size="28" font-family="Arial, sans-serif" font-weight="700">核心判断</text>
  <text x="128" y="414" fill="#111827" font-size="24" font-family="Arial, sans-serif">技术路线可行，第一阶段应聚焦</text>
  <text x="128" y="456" fill="#111827" font-size="24" font-family="Arial, sans-serif">一页生成质量和用户可控性。</text>
  <rect x="652" y="292" width="240" height="130" rx="18" fill="#ECFDF5"/>
  <text x="684" y="345" fill="#047857" font-size="22" font-family="Arial, sans-serif" font-weight="700">BYOK</text>
  <text x="684" y="384" fill="#111827" font-size="18" font-family="Arial, sans-serif">用户接入自己的 API</text>
  <rect x="928" y="292" width="240" height="130" rx="18" fill="#F5F3FF"/>
  <text x="960" y="345" fill="#7C3AED" font-size="22" font-family="Arial, sans-serif" font-weight="700">Bento Grid</text>
  <text x="960" y="384" fill="#111827" font-size="18" font-family="Arial, sans-serif">内容驱动布局</text>
  <rect x="652" y="442" width="516" height="130" rx="18" fill="#FFF7ED"/>
  <text x="684" y="495" fill="#C2410C" font-size="22" font-family="Arial, sans-serif" font-weight="700">下一步</text>
  <text x="684" y="534" fill="#111827" font-size="18" font-family="Arial, sans-serif">稳定一页闭环后，再扩展多页便利贴和 PPTX 导出。</text>
</svg>`

export const useOnePageDraftStore = defineStore(
  'onePageDraft',
  () => {
    const currentStage = ref<WorkflowStage>('consult')
    const rightPanelCollapsed = ref(false)
    const userPrompt = ref('我想做一页关于 AI PPT Agent 项目可行性的汇报页，给团队内部立项讨论用。')
    const assistantMessage = ref('我会先确认受众、场景和核心结论，再生成结构化 brief。')
    const svgContent = ref(defaultSvg)

    const steps = reactive<WorkflowStepState[]>([
      { key: 'consult', label: '需求调研', status: 'done' },
      { key: 'brief', label: '生成 brief', status: 'done' },
      { key: 'research', label: '资料整理', status: 'done' },
      { key: 'pagePlan', label: '页面策划', status: 'done' },
      { key: 'svg', label: 'SVG 生成', status: 'done' },
    ])

    const brief = reactive<RequirementBrief>({
      topic: 'AI PPT Agent 项目可行性',
      audience: '团队内部成员',
      scenario: '项目立项讨论',
      goal: '帮助团队判断是否值得投入 MVP 开发',
      coreConclusion: '项目技术上可行，但第一阶段应聚焦一页闭环，先验证工作流质量。',
      tone: '专业、务实、信息密度适中',
      mustInclude: '技术可行性、产品价值、主要风险、下一步建议',
      avoid: '夸大商业价值、承诺完全自动生成高质量 PPT',
    })

    const researchPack = reactive<ResearchPack>({
      summary:
        '工作流型 AI PPT 的关键价值在于把需求澄清、内容策划和视觉生成拆成可编辑阶段，而不是直接套模板。',
      keyPoints: [
        '一页 MVP 可以先验证需求对话、brief、策划稿和 SVG 生成质量。',
        'Bento Grid 适合承载结论、风险、能力和下一步等多块信息。',
        '主要风险集中在模型输出稳定性、SVG 重叠和资料可靠性。',
      ],
      sources: ['当前为 model-only 资料整理，尚未接入外部来源。'],
    })

    const pagePlan = reactive<PagePlan>({
      slideTitle: 'AI PPT Agent：先验证一页闭环的可行性',
      coreMessage: '项目技术可行，但第一阶段应聚焦一页流程，先验证工作流质量。',
      audienceTakeaway: '团队应先投入 MVP，而不是一开始做完整 PPT SaaS。',
      layoutIntent: '使用 Bento Grid：左侧大卡片放核心判断，右侧三个小卡片放 BYOK、Bento Grid 和下一步。',
      visualStyle: '专业、克制、现代，避免花哨装饰，强调层级和留白。',
      contentBlocks: [
        {
          id: 'primary',
          role: 'primary',
          title: '核心判断',
          content: '一页闭环可行性高，适合作为第一阶段目标。',
        },
        {
          id: 'byok',
          role: 'supporting',
          title: 'BYOK 接入',
          content: '用户使用自己的 OpenAI-compatible API，由后端代理调用。',
        },
        {
          id: 'risk',
          role: 'risk',
          title: '主要风险',
          content: '生成质量不稳定、SVG 易重叠、资料来源不充分。',
        },
      ],
    })

    const activeStep = computed(() => steps.find((step) => step.key === currentStage.value))

    function setStage(stage: WorkflowStage) {
      currentStage.value = stage
    }

    function toggleRightPanel() {
      rightPanelCollapsed.value = !rightPanelCollapsed.value
    }

    function regenerateSvg() {
      svgContent.value = defaultSvg.replace(
        '先验证一页闭环',
        pagePlan.slideTitle.slice(0, 18) || '先验证一页闭环',
      )
      setStage('svg')
    }

    return {
      activeStep,
      assistantMessage,
      brief,
      currentStage,
      pagePlan,
      researchPack,
      rightPanelCollapsed,
      steps,
      svgContent,
      userPrompt,
      regenerateSvg,
      setStage,
      toggleRightPanel,
    }
  },
  {
    persist: {
      key: 'slideforge:one-page-draft',
      paths: [
        'currentStage',
        'rightPanelCollapsed',
        'userPrompt',
        'assistantMessage',
        'brief',
        'researchPack',
        'pagePlan',
        'svgContent',
      ],
    },
  },
)
