// 后端统一响应结构。T 是具体接口的业务数据类型。
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

// 请求层抛给业务层的统一错误结构。
export interface ApiError {
  code?: number | string
  message: string
  status?: number
  url?: string
  raw?: unknown
}
