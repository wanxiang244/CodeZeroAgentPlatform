const removeTrailingSlash = (value: string) => value.replace(/\/+$/, '')

const apiBaseUrl = removeTrailingSlash(import.meta.env.VITE_API_BASE_URL || 'http://localhost:8123/api')
const previewBaseUrl = removeTrailingSlash(
  import.meta.env.VITE_APP_PREVIEW_BASE_URL || apiBaseUrl
)
const deployBaseUrl = removeTrailingSlash(
  import.meta.env.VITE_APP_DEPLOY_BASE_URL || 'http://localhost'
)

export const API_BASE_URL = apiBaseUrl
export const APP_PREVIEW_BASE_URL = previewBaseUrl
export const APP_DEPLOY_BASE_URL = deployBaseUrl
