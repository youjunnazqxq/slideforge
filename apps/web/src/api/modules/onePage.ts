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

export function createOnePageDraft(data: CreateOnePageDraftRequest) {
  return request.post<CreateOnePageDraftResponse>(`${servicePrefix.onePage}/drafts`, data)
}

export function consultOnePageDraft(draftId: string, data: ConsultRequest) {
  return request.post<ConsultResponse>(`${servicePrefix.onePage}/drafts/${draftId}/consult`, data)
}

export function generateBrief(draftId: string) {
  return request.post(`${servicePrefix.onePage}/drafts/${draftId}/brief`)
}

export function generateResearch(draftId: string) {
  return request.post(`${servicePrefix.onePage}/drafts/${draftId}/research`)
}

export function generatePagePlan(draftId: string) {
  return request.post(`${servicePrefix.onePage}/drafts/${draftId}/page-plan`)
}

export function generateSvg(draftId: string) {
  return request.post(`${servicePrefix.onePage}/drafts/${draftId}/svg`)
}
