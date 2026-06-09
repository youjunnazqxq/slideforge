import request from '@/api'
import { servicePrefix } from '@/api/config/servicePrefix'

export interface CreateDeckDraftRequest {
  initialPrompt: string
}

export interface CreateDeckDraftResponse {
  deckId: string
  status: string
}

export interface CreateOnePageDraftFromDeckResponse {
  draftId: string
  status: string
}

export interface DeckSlideDraftResponse {
  slideId: string
  order: number
  title: string
  draftId: string
  status: string
  errorMessage?: string
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
  generatedDrafts: DeckSlideDraftResponse[]
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

export function createOnePageDraftFromDeckSlide(deckId: string, slideId: string) {
  return request.post<CreateOnePageDraftFromDeckResponse>(
    `${servicePrefix.decks}/${deckId}/slides/${slideId}/one-page-draft`,
  )
}

export function createSvgDraftFromDeckSlide(deckId: string, slideId: string) {
  return request.post<DeckSlideDraftResponse>(`${servicePrefix.decks}/${deckId}/slides/${slideId}/svg-draft`)
}

export function createOnePageDraftsFromDeck(deckId: string) {
  return request.post<DeckSlideDraftResponse[]>(`${servicePrefix.decks}/${deckId}/slides/one-page-drafts`)
}

export function createSvgDraftsFromDeck(deckId: string) {
  return request.post<DeckSlideDraftResponse[]>(`${servicePrefix.decks}/${deckId}/slides/svg-drafts`)
}

export function saveDeckStickyNotes(deckId: string, data: SlideStickyNoteResponse[]) {
  return request.put<SlideStickyNoteResponse[]>(`${servicePrefix.decks}/${deckId}/sticky-notes`, data)
}

export function addDeckStickyNote(deckId: string, data: Partial<SlideStickyNoteResponse>) {
  return request.post<SlideStickyNoteResponse>(`${servicePrefix.decks}/${deckId}/sticky-notes`, data)
}

export function deleteDeckStickyNote(deckId: string, slideId: string) {
  return request.delete<SlideStickyNoteResponse[]>(`${servicePrefix.decks}/${deckId}/sticky-notes/${slideId}`)
}
