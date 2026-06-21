<script setup lang="ts">
import { reactive, watchEffect, ref } from 'vue'
import { CheckIcon } from '@heroicons/vue/20/solid'

import ThemeSwitcher from '@/components/nav/ThemeSwitcher.vue'
import ErrorState from '@/components/state/ErrorState.vue'
import { updateProfile } from '@/lib/services'
import { brandThemes, useSettingsStore } from '@/stores/settings'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const settings = useSettingsStore()
const saving = ref(false)
const message = ref('')
const error = ref('')

const profile = reactive({
  realName: '',
  email: '',
  phone: '',
  avatarUrl: '',
  gender: 0,
})

watchEffect(() => {
  profile.realName = auth.user?.realName || ''
  profile.email = auth.user?.email || ''
  profile.phone = auth.user?.phone || ''
  profile.avatarUrl = auth.user?.avatarUrl || ''
  profile.gender = auth.user?.gender || 0
})

async function saveProfile() {
  saving.value = true
  error.value = ''
  message.value = ''
  try {
    const updated = await updateProfile({
      realName: profile.realName || undefined,
      email: profile.email || undefined,
      phone: profile.phone || undefined,
      avatarUrl: profile.avatarUrl || undefined,
      gender: profile.gender,
    })
    auth.user = { ...auth.user, ...updated }
    message.value = '资料已保存'
  } catch (e) {
    error.value = e instanceof Error ? e.message : '资料保存失败'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="grid gap-6 xl:grid-cols-[minmax(0,0.9fr)_minmax(0,1.1fr)]">
    <section class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900">
      <h2 class="text-base font-semibold text-slate-950 dark:text-white">界面偏好</h2>
      <div class="mt-5 space-y-5">
        <div>
          <p class="text-sm font-medium text-slate-700 dark:text-slate-200">主题模式</p>
          <div class="mt-2">
            <ThemeSwitcher />
          </div>
        </div>
        <div>
          <p class="text-sm font-medium text-slate-700 dark:text-slate-200">品牌色</p>
          <div class="mt-3 grid grid-cols-2 gap-3 sm:grid-cols-3">
            <button
              v-for="theme in brandThemes"
              :key="theme.key"
              type="button"
              class="focus-ring flex items-center justify-between rounded-md border border-slate-200 bg-white px-3 py-3 text-sm font-medium text-slate-700 hover:bg-slate-50 dark:border-white/10 dark:bg-white/5 dark:text-slate-200 dark:hover:bg-white/10"
              @click="settings.setBrand(theme.key)"
            >
              <span class="flex items-center gap-2">
                <span class="size-4 rounded-full" :style="{ backgroundColor: `rgb(${theme.light})` }" />
                {{ theme.label }}
              </span>
              <CheckIcon v-if="settings.brand === theme.key" class="size-4 text-[rgb(var(--color-brand))]" aria-hidden="true" />
            </button>
          </div>
        </div>
      </div>
    </section>

    <section class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900">
      <h2 class="text-base font-semibold text-slate-950 dark:text-white">个人资料</h2>
      <ErrorState v-if="error" class="mt-4" :message="error" @retry="saveProfile" />
      <p v-if="message" class="mt-4 rounded-md bg-emerald-50 px-3 py-2 text-sm text-emerald-700 dark:bg-emerald-400/10 dark:text-emerald-200">
        {{ message }}
      </p>
      <form class="mt-5 grid gap-4 sm:grid-cols-2" @submit.prevent="saveProfile">
        <label>
          <span class="text-sm font-medium text-slate-700 dark:text-slate-200">真实姓名</span>
          <input
            v-model.trim="profile.realName"
            class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white"
          />
        </label>
        <label>
          <span class="text-sm font-medium text-slate-700 dark:text-slate-200">性别</span>
          <select
            v-model.number="profile.gender"
            class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-slate-900 dark:text-white"
          >
            <option :value="0">未知</option>
            <option :value="1">男</option>
            <option :value="2">女</option>
          </select>
        </label>
        <label>
          <span class="text-sm font-medium text-slate-700 dark:text-slate-200">邮箱</span>
          <input
            v-model.trim="profile.email"
            type="email"
            class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white"
          />
        </label>
        <label>
          <span class="text-sm font-medium text-slate-700 dark:text-slate-200">手机号</span>
          <input
            v-model.trim="profile.phone"
            class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white"
          />
        </label>
        <label class="sm:col-span-2">
          <span class="text-sm font-medium text-slate-700 dark:text-slate-200">头像 URL</span>
          <input
            v-model.trim="profile.avatarUrl"
            class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white"
          />
        </label>
        <div class="sm:col-span-2">
          <button type="submit" class="btn-primary focus-ring inline-flex h-10 items-center px-4 text-sm" :disabled="saving">
            {{ saving ? '保存中' : '保存资料' }}
          </button>
        </div>
      </form>
    </section>
  </div>
</template>
