import type { CreateAxiosDefaults } from 'axios'

// Axios 基础配置。baseURL 由 Vite 环境变量注入，区分开发和生产接口地址。
const axiosConfig: CreateAxiosDefaults = {
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000,
  withCredentials: false,
}

export default axiosConfig
