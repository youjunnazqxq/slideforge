// 后端模块前缀统一维护，业务 API 文件不要散落硬编码模块路径。
export const servicePrefix = {
  auth: '/login',
  settings: '/api/settings',
  user: '/user',
  users: '/api/users',
  onePage: '/api/one-page',
  project: '/project',
  ppt: '/ppt',
} as const
