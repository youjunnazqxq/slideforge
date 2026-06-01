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

class HttpRequest {
  private readonly instance: AxiosInstance

  constructor(config: CreateAxiosDefaults) {
    this.instance = axios.create(config)
    this.setInterceptors()
  }

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

  private isApiResponse<T>(data: unknown): data is ApiResponse<T> {
    return Boolean(data && typeof data === 'object' && 'code' in data && 'data' in data)
  }
}

const request = new HttpRequest(axiosConfig)

export { HttpRequest }
export default request
