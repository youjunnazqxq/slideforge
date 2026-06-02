// HTTP 状态码对应的兜底提示。业务错误优先使用后端返回的 message。
const statusMessageMap: Record<number, string> = {
  400: '请求参数错误',
  401: '登录状态失效',
  403: '没有权限访问',
  404: '接口不存在',
  408: '请求超时',
  500: '服务器错误',
  502: '网关错误',
  503: '服务不可用',
  504: '网关超时',
}

export function checkStatus(status?: number) {
  if (!status) {
    return '网络异常，请稍后重试'
  }

  return statusMessageMap[status] ?? `请求错误：${status}`
}
