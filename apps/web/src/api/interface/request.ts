import type { AxiosRequestConfig } from 'axios'

export interface RequestConfig<D = unknown> extends AxiosRequestConfig<D> {
  skipAuth?: boolean
  cancelRepeat?: boolean
  returnRawResponse?: boolean
  requestKey?: string
}

declare module 'axios' {
  export interface AxiosRequestConfig<D = any> {
    skipAuth?: boolean
    cancelRepeat?: boolean
    returnRawResponse?: boolean
    requestKey?: string
  }

  export interface InternalAxiosRequestConfig<D = any> {
    skipAuth?: boolean
    cancelRepeat?: boolean
    returnRawResponse?: boolean
    requestKey?: string
  }
}
