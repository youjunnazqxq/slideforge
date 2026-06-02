import type { AxiosRequestConfig } from 'axios'

// 业务层请求配置，在 Axios 原始配置上扩展项目内控制项。
export interface RequestConfig<D = unknown> extends AxiosRequestConfig<D> {
  // 登录、注册等接口不需要携带 token。
  skipAuth?: boolean
  // 开启后会取消同 method/url/params/data 的旧请求。
  cancelRepeat?: boolean
  // 默认返回 response.data.data；开启后返回完整后端响应结构。
  returnRawResponse?: boolean
  // helper 内部生成和复用的请求唯一标识，业务侧通常不需要手动传。
  requestKey?: string
}

// 扩展 Axios 内置类型，让拦截器中的 config 能识别上面的业务字段。
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
