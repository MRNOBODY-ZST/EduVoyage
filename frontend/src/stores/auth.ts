import { defineStore } from 'pinia'

import { clearTokens, getData, hasAccessToken, postData, saveTokens } from '@/lib/http'
import type { MeResponse, TokenResponse, UserProfile } from '@/types/api'

interface AuthState {
  user: UserProfile | null
  roles: string[]
  permissions: string[]
  bootstrapped: boolean
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    user: null,
    roles: [],
    permissions: [],
    bootstrapped: false,
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.user),
    displayName: (state) => state.user?.realName || state.user?.username || '未登录',
    primaryRole: (state) => state.roles[0] || state.user?.roles?.[0] || 'STUDENT',
  },
  actions: {
    async login(username: string, password: string) {
      const tokens = await postData<TokenResponse>('/api/auth/login', { username, password })
      saveTokens(tokens.accessToken, tokens.refreshToken)
      await this.fetchMe(tokens.user)
    },
    async fetchMe(fallbackUser?: UserProfile) {
      if (!hasAccessToken()) {
        this.bootstrapped = true
        return
      }
      try {
        const me = await getData<MeResponse>('/api/auth/me')
        this.applyMe(me)
      } catch {
        if (fallbackUser) {
          this.user = fallbackUser
          this.roles = fallbackUser.roles || []
          this.permissions = []
        } else {
          this.logout()
        }
      } finally {
        this.bootstrapped = true
      }
    },
    applyMe(me: MeResponse) {
      this.user = me.profile
      this.roles = me.roles
      this.permissions = me.permissions
    },
    hasPermission(permission: string) {
      return this.permissions.includes(permission)
    },
    hasRole(role: string) {
      return this.roles.includes(role) || Boolean(this.user?.roles?.includes(role))
    },
    logout() {
      clearTokens()
      this.user = null
      this.roles = []
      this.permissions = []
      this.bootstrapped = true
    },
  },
})
