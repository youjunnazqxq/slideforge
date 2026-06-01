export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

export interface ApiError {
  code?: number | string
  message: string
  status?: number
  url?: string
  raw?: unknown
}
