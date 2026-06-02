import type { InternalAxiosRequestConfig } from 'axios'

// 保存正在进行中的请求。key 相同表示 method、url、params、data 都一致。
const pendingMap = new Map<string, AbortController>()

function stableStringify(value: unknown) {
  if (!value) {
    return ''
  }

  try {
    return JSON.stringify(value)
  } catch {
    return String(value)
  }
}

export function getRequestKey(config: InternalAxiosRequestConfig) {
  const { data, method, params, url } = config

  return [method, url, stableStringify(params), stableStringify(data)].join('&')
}

// 请求发出前调用：如果存在相同请求，取消旧请求并记录最新请求。
export function addPendingRequest(config: InternalAxiosRequestConfig) {
  if (!config.cancelRepeat) {
    return
  }

  removePendingRequest(config)

  const requestKey = getRequestKey(config)
  const controller = new AbortController()

  config.requestKey = requestKey
  config.signal = controller.signal
  pendingMap.set(requestKey, controller)
}

// 请求结束后调用：清理 pending 记录，避免 Map 一直增长。
export function removePendingRequest(config?: InternalAxiosRequestConfig) {
  if (!config?.cancelRepeat) {
    return
  }

  const requestKey = config.requestKey ?? getRequestKey(config)
  const controller = pendingMap.get(requestKey)

  if (controller) {
    controller.abort()
    pendingMap.delete(requestKey)
  }
}

// 预留给退出登录、路由切换等场景，一次性取消所有未完成请求。
export function clearPendingRequests() {
  pendingMap.forEach((controller) => {
    controller.abort()
  })
  pendingMap.clear()
}
