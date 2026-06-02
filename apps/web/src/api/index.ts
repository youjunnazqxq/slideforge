import axios from 'axios'
import type {
  AxiosError,
  AxiosInstance,
  AxiosResponse,
  CreateAxiosDefaults,
  InternalAxiosRequestConfig,
} from 'axios'

import { useStore } from '@/stores'

import axiosConfig from './config'
import { addPendingRequest, removePendingRequest } from './helper/cancelRepeatRequest'
import { checkStatus } from './helper/checkStatus'
import type { ApiError, ApiResponse, RequestConfig, ResultData } from './interface'

interface AuthResponsePayload {
  username?: string
  token?: string
}

type RequestParams = object
type RequestBody = object | string

// 项目统一请求类：页面和业务模块只使用该类暴露的方法，不直接调用 axios。
class HttpRequest {
  private readonly service: AxiosInstance

  constructor(config: CreateAxiosDefaults) {
    this.service = axios.create(config)
    this.setInterceptors()
  }

  /**
   * @description 通用请求方法封装
   */
  get<T>(url: string, params?: RequestParams, _object: RequestConfig = {}): Promise<ResultData<T>> {
    return this.service.get(url, { params, ..._object })
  }

  post<T>(url: string, params?: RequestBody, _object: RequestConfig = {}): Promise<ResultData<T>> {
    return this.service.post(url, params, _object)
  }

  put<T>(url: string, params?: RequestParams, _object: RequestConfig = {}): Promise<ResultData<T>> {
    return this.service.put(url, params, _object)
  }

  patch<T>(url: string, params?: RequestBody, _object: RequestConfig = {}): Promise<ResultData<T>> {
    return this.service.patch(url, params, _object)
  }

  delete<T>(
    url: string,
    params?: RequestParams,
    _object: RequestConfig = {},
  ): Promise<ResultData<T>> {
    return this.service.delete(url, { params, ..._object })
  }

  download(url: string, params?: RequestParams, _object: RequestConfig = {}): Promise<BlobPart> {
    return this.service.post(url, params, { ..._object, responseType: 'blob' })
  }

  // 注册请求和响应拦截器，统一处理 token、重复请求、响应校验和错误格式化。
  private setInterceptors() {
    this.service.interceptors.request.use(
      (config) => this.handleRequest(config),
      (error: AxiosError) => Promise.reject(error),
    )

    this.service.interceptors.response.use(
      (response) => this.handleResponse(response) as unknown as AxiosResponse,
      (error: AxiosError<ApiResponse>) => this.handleResponseError(error),
    )
  }

  // 请求发出前：注入 token、补默认 Content-Type，并根据配置处理重复请求。
  private handleRequest(config: InternalAxiosRequestConfig) {
    const userStore = useStore()

    if (!config.skipAuth && userStore.token) {
      config.headers.set('Authorization', `Bearer ${userStore.token}`)
    }

    if (!config.headers.has('Content-Type')) {
      config.headers.set('Content-Type', 'application/json')
    }

    addPendingRequest(config)

    return config
  }

  // 请求成功后：保存后端返回的 token，清理 pending 请求，并返回后端统一响应结构。
  private handleResponse<T = unknown>(response: AxiosResponse<ApiResponse<T>>) {
    removePendingRequest(response.config)

    if (response.config.responseType === 'blob') {
      return response.data
    }

    const responseData = response.data

    if (!this.isApiResponse(responseData)) {
      return responseData
    }

    this.persistAuthFromResponse(responseData.data)

    if (response.config.returnRawResponse) {
      return response
    }

    if (responseData.code === 0 || responseData.code === 200) {
      return responseData
    }

    return Promise.reject<ApiError>({
      code: responseData.code,
      message: responseData.message || '请求失败',
      status: response.status,
      url: response.config.url,
      raw: responseData,
    })
  }

  // 请求失败后：把 AxiosError 转成统一 ApiError，业务层只处理一种错误形态。
  private handleResponseError(error: AxiosError<ApiResponse>) {
    removePendingRequest(error.config)

    if (error.response?.status === 401) {
      useStore().clearUser()
    }

    const apiError: ApiError = {
      code: error.response?.data?.code ?? error.code,
      message: error.response?.data?.message ?? checkStatus(error.response?.status),
      status: error.response?.status,
      url: error.config?.url,
      raw: error,
    }

    return Promise.reject(apiError)
  }

  // 兼容暂未套用统一后端结构的接口，避免强行读取 code/data 造成异常。
  private isApiResponse<T>(data: unknown): data is ApiResponse<T> {
    return Boolean(data && typeof data === 'object' && 'code' in data && 'data' in data)
  }

  // 后端登录类接口返回 token 时，统一写入 Pinia；Pinia 持久化插件会同步到 localStorage。
  private persistAuthFromResponse(data: unknown) {
    if (!this.isAuthResponsePayload(data)) {
      return
    }

    const userStore = useStore()

    if (data.username) {
      userStore.setUsername(data.username)
    }

    if (data.token) {
      userStore.setToken(data.token)
    }
  }

  private isAuthResponsePayload(data: unknown): data is AuthResponsePayload {
    return Boolean(data && typeof data === 'object' && 'token' in data)
  }
}

// 默认请求实例。后续 modules 中统一导入这个实例调用接口。
const request = new HttpRequest(axiosConfig)

export { HttpRequest }
export default request
