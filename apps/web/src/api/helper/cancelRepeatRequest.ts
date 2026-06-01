import type { InternalAxiosRequestConfig } from 'axios'

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

export function clearPendingRequests() {
  pendingMap.forEach((controller) => {
    controller.abort()
  })
  pendingMap.clear()
}
