import request from '@/api'
import { servicePrefix } from '@/api/config/servicePrefix'

export interface CreateOnePageDraftRequest {
  initialPrompt: string
}

export interface CreateOnePageDraftResponse {
  draftId: string
  status: string
}

export interface ConsultRequest {
  message: string
}

export interface ConsultResponse {
  message: string
  readyForBrief: boolean
}

export interface GenerateResearchRequest {
  mode: 'model-only' | 'search-assisted'
}

export interface RequirementBriefResponse {
  topic: string
  audience: string
  scenario: string
  goal: string
  coreConclusion: string
  tone: string
  mustInclude: string[]
  avoid: string[]
  language: string
  canvasRatio: string
}

export interface ResearchPackResponse {
  mode: string
  summary: string
  keyPoints: string[]
  evidence: Array<{
    claim: string
    support: string
    sourceIds: string[]
  }>
  sources: Array<{
    id: string
    title: string
    url: string
    publisher: string
    publishedAt: string
    snippet: string
  }>
  limitations: string[]
}

export interface PagePlanResponse {
  slideTitle: string
  coreMessage: string
  audienceTakeaway: string
  contentBlocks: Array<{
    id: string
    role: string
    type: string
    title: string
    content: string
  }>
  layoutIntent: string
  visualStyle: string
}

export interface VisualSpecResponse {
  canvas: {
    width: number
    height: number
    viewBox: string
  }
  theme: {
    background: string
    primary: string
    text: string
    muted: string
    card: string
    border: string
  }
  cards: Array<{
    id: string
    blockId: string
    x: number
    y: number
    w: number
    h: number
    priority: string
  }>
}

export interface ValidationReportResponse {
  valid: boolean
  warnings: string[]
}

export interface SvgGenerateResponse {
  svgContent: string
  validationReport: ValidationReportResponse
}

export interface OnePageDraftResponse {
  draftId: string
  status: string
  initialPrompt: string
  requirementBrief?: RequirementBriefResponse
  researchPack?: ResearchPackResponse
  pagePlan?: PagePlanResponse
  visualSpec?: VisualSpecResponse
  svgContent?: string
  validationReport?: ValidationReportResponse
}

export function createOnePageDraft(data: CreateOnePageDraftRequest) {
  return request.post<CreateOnePageDraftResponse>(`${servicePrefix.onePage}/drafts`, data)
}

export function getOnePageDraft(draftId: string) {
  return request.get<OnePageDraftResponse>(`${servicePrefix.onePage}/drafts/${draftId}`, undefined, {
    cancelRepeat: true,
  })
}

export function consultOnePageDraft(draftId: string, data: ConsultRequest) {
  return request.post<ConsultResponse>(`${servicePrefix.onePage}/drafts/${draftId}/consult`, data)
}

export function generateBrief(draftId: string) {
  return request.post<RequirementBriefResponse>(`${servicePrefix.onePage}/drafts/${draftId}/brief`)
}

export function updateBrief(draftId: string, data: RequirementBriefResponse) {
  return request.put<RequirementBriefResponse>(`${servicePrefix.onePage}/drafts/${draftId}/brief`, data)
}

export function generateResearch(draftId: string, data?: GenerateResearchRequest) {
  return request.post<ResearchPackResponse>(`${servicePrefix.onePage}/drafts/${draftId}/research`, data)
}

export function generatePagePlan(draftId: string) {
  return request.post<PagePlanResponse>(`${servicePrefix.onePage}/drafts/${draftId}/page-plan`)
}

export function updatePagePlan(draftId: string, data: PagePlanResponse) {
  return request.put<PagePlanResponse>(`${servicePrefix.onePage}/drafts/${draftId}/page-plan`, data)
}

export function generateVisualSpec(draftId: string) {
  return request.post<VisualSpecResponse>(`${servicePrefix.onePage}/drafts/${draftId}/visual-spec`)
}

export function updateVisualSpec(draftId: string, data: VisualSpecResponse) {
  return request.put<VisualSpecResponse>(`${servicePrefix.onePage}/drafts/${draftId}/visual-spec`, data)
}

export function generateSvg(draftId: string) {
  return request.post<SvgGenerateResponse>(`${servicePrefix.onePage}/drafts/${draftId}/svg`)
}

export function exportOnePagePptx(draftId: string) {
  return request.download(`${servicePrefix.onePage}/drafts/${draftId}/export/pptx`)
}

export function exportOnePageDraftsPptx(draftIds: string[]) {
  return request.download(`${servicePrefix.onePage}/drafts/export/pptx`, { draftIds })
}
