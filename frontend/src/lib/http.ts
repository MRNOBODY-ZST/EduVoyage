import axios, { AxiosError, type AxiosRequestConfig } from 'axios'

import type { Result } from '@/types/api'

const ACCESS_TOKEN_KEY = 'eduvoyage.accessToken'
const REFRESH_TOKEN_KEY = 'eduvoyage.refreshToken'

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 20000,
})

let refreshing: Promise<string | null> | null = null

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(ACCESS_TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<Result<unknown>>) => {
    const status = error.response?.status
    const original = error.config as (AxiosRequestConfig & { _retried?: boolean }) | undefined
    if (status !== 401 || !original || original._retried || original.url?.includes('/api/auth/refresh')) {
      return Promise.reject(toAppError(error))
    }
    original._retried = true
    refreshing ??= refreshAccessToken().finally(() => {
      refreshing = null
    })
    const token = await refreshing
    if (!token) {
      return Promise.reject(toAppError(error))
    }
    original.headers = { ...original.headers, Authorization: `Bearer ${token}` }
    return api(original)
  },
)

export async function getData<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  const response = await api.get<Result<T>>(url, config)
  return unwrap(response.data)
}

export async function postData<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  const response = await api.post<Result<T>>(url, data, config)
  return unwrap(response.data)
}

export async function putData<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  const response = await api.put<Result<T>>(url, data, config)
  return unwrap(response.data)
}

export async function deleteData<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  const response = await api.delete<Result<T>>(url, config)
  return unwrap(response.data)
}

export function saveTokens(accessToken: string, refreshToken: string) {
  localStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
  localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
}

export function clearTokens() {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
}

export function hasAccessToken() {
  return Boolean(localStorage.getItem(ACCESS_TOKEN_KEY))
}

function unwrap<T>(payload: Result<T>): T {
  if (payload.code !== 0) {
    throw new Error(payload.message || '请求失败')
  }
  return payload.data
}

async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY)
  if (!refreshToken) {
    return null
  }
  try {
    const response = await axios.post<Result<{ accessToken: string; refreshToken: string }>>('/api/auth/refresh', {
      refreshToken,
    })
    const tokens = unwrap(response.data)
    saveTokens(tokens.accessToken, tokens.refreshToken)
    return tokens.accessToken
  } catch {
    clearTokens()
    return null
  }
}

function toAppError(error: AxiosError<Result<unknown>>) {
  const message = error.response?.data?.message || error.message || '网络异常'
  return new Error(message)
}
