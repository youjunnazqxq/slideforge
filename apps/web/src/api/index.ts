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
import type { ApiError, ApiResponse, RequestConfig } from './interface'

// 项目统一请求类：页面和业务模块只使用该类暴露的方法，不直接调用 axios。
class HttpRequest {
  private readonly instance: AxiosInstance

  constructor(config: CreateAxiosDefaults) {
    this.instance = axios.create(config)
    this.setInterceptors()
  }

  // 所有快捷方法最终都会走 request，保证拦截器、错误处理和泛型返回一致。
  request<T = unknown>(config: RequestConfig): Promise<T> {
    return this.instance.request<unknown, T>(config)
  }

  get<T = unknown>(url: string, config?: RequestConfig): Promise<T> {
    return this.request<T>({ ...config, method: 'GET', url })
  }

  post<T = unknown, D = unknown>(url: string, data?: D, config?: RequestConfig<D>): Promise<T> {
    return this.request<T>({ ...config, data, method: 'POST', url })
  }

  put<T = unknown, D = unknown>(url: string, data?: D, config?: RequestConfig<D>): Promise<T> {
    return this.request<T>({ ...config, data, method: 'PUT', url })
  }

  patch<T = unknown, D = unknown>(url: string, data?: D, config?: RequestConfig<D>): Promise<T> {
    return this.request<T>({ ...config, data, method: 'PATCH', url })
  }

  delete<T = unknown>(url: string, config?: RequestConfig): Promise<T> {
    return this.request<T>({ ...config, method: 'DELETE', url })
  }

  // 注册请求和响应拦截器，统一处理 token、重复请求、响应解包和错误格式化。
  private setInterceptors() {
    this.instance.interceptors.request.use(
      (config) => this.handleRequest(config),
      (error: AxiosError) => Promise.reject(error),
    )

    this.instance.interceptors.response.use(
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

  // 请求成功后：清理 pending 请求，默认把后端通用响应解包成 data 返回。
  private handleResponse<T = unknown>(response: AxiosResponse<ApiResponse<T>>) {
    removePendingRequest(response.config)

    if (response.config.returnRawResponse) {
      return response
    }

    const responseData = response.data

    if (!this.isApiResponse(responseData)) {
      return responseData
    }

    if (responseData.code === 0 || responseData.code === 200) {
      return responseData.data
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
}

// 默认请求实例。后续 modules 中统一导入这个实例调用接口。
const request = new HttpRequest(axiosConfig)

export { HttpRequest }
export default request
