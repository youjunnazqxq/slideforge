import request from '@/api'
import { servicePrefix } from '@/api/config/servicePrefix'

export interface CreateDeckDraftRequest {
  initialPrompt: string
}

export interface CreateDeckDraftResponse {
  deckId: string
  status: string
}

export interface DeckOutlineResponse {
  title: string
  audience: string
  scenario: string
  coreThesis: string
  structure: Array<{
    id: string
    title: string
    purpose: string
  }>
  slides: Array<{
    id: string
    type: string
    sectionId: string
    title: string
    message: string
    purpose: string
  }>
}

export interface SlideStickyNoteResponse {
  slideId: string
  order: number
  sectionTitle: string
  title: string
  message: string
  status: string
  tags: string[]
}

export interface DeckDraftResponse {
  deckId: string
  status: string
  initialPrompt: string
  outline?: DeckOutlineResponse
  stickyNotes: SlideStickyNoteResponse[]
}

export function createDeckDraft(data: CreateDeckDraftRequest) {
  return request.post<CreateDeckDraftResponse>(servicePrefix.decks, data)
}

export function getDeckDraft(deckId: string) {
  return request.get<DeckDraftResponse>(`${servicePrefix.decks}/${deckId}`, undefined, {
    cancelRepeat: true,
  })
}

export function generateDeckOutline(deckId: string) {
  return request.post<DeckOutlineResponse>(`${servicePrefix.decks}/${deckId}/outline`)
}
