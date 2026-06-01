import type { CreateAxiosDefaults } from 'axios'

const axiosConfig: CreateAxiosDefaults = {
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000,
  withCredentials: false,
}

export default axiosConfig
