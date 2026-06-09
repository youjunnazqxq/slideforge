import request from '@/api'

export interface WorkflowRunResponse {
  id: string
  draftId: string
  stage: string
  model: string
  promptKey: string
  promptVersion: string
  status: string
  errorMessage?: string
  durationMs?: number
  createdAt: string
  inputPreview: string
  outputPreview: string
}

export function getWorkflowRuns(draftId: string) {
  return request.get<WorkflowRunResponse[]>('/api/workflow-runs', { draftId })
}
