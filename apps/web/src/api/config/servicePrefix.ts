// 后端模块前缀统一维护，业务 API 文件不要散落硬编码模块路径。
export const servicePrefix = {
  auth: '/login',
  user: '/user',
  project: '/project',
  ppt: '/ppt',
} as const
