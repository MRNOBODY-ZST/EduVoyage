<script setup lang="ts">
import { computed, reactive, ref, watchEffect } from 'vue'
import { CheckIcon } from '@heroicons/vue/20/solid'
import { BellAlertIcon, LanguageIcon, ShieldCheckIcon, UserCircleIcon } from '@heroicons/vue/24/outline'

import ThemeSwitcher from '@/components/nav/ThemeSwitcher.vue'
import ErrorState from '@/components/state/ErrorState.vue'
import { localeOptions, useI18n } from '@/i18n'
import { updateProfile } from '@/lib/services'
import { brandThemes, useSettingsStore } from '@/stores/settings'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const settings = useSettingsStore()
const { t } = useI18n()
const saving = ref(false)
const message = ref('')
const error = ref('')
const notifications = reactive({
  course: true,
  homework: true,
  share: false,
})

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

const localeModel = computed({
  get: () => settings.locale,
  set: (locale: 'zh-CN' | 'en-US') => settings.setLocale(locale),
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
    message.value = t('settings.saved')
  } catch (e) {
    error.value = e instanceof Error ? e.message : t('settings.saveFailed')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <form class="mx-auto max-w-5xl" @submit.prevent="saveProfile">
    <div class="mb-8 rounded-md border border-slate-200 bg-white p-6 shadow-sm dark:border-white/10 dark:bg-slate-900">
      <h2 class="text-lg font-semibold text-slate-950 dark:text-white">{{ t('settings.title') }}</h2>
      <p class="mt-2 text-sm/6 text-slate-500 dark:text-slate-400">{{ t('settings.subtitle') }}</p>
      <ErrorState v-if="error" class="mt-4" :message="error" @retry="saveProfile" />
      <p v-if="message" class="mt-4 rounded-md bg-emerald-50 px-3 py-2 text-sm text-emerald-700 dark:bg-emerald-400/10 dark:text-emerald-200">
        {{ message }}
      </p>
    </div>

    <div class="space-y-12">
      <section>
        <div class="flex items-start gap-3">
          <UserCircleIcon class="mt-0.5 size-5 text-slate-400" aria-hidden="true" />
          <div>
            <h2 class="text-base/7 font-semibold text-slate-950 dark:text-white">{{ t('settings.profile') }}</h2>
            <p class="mt-1 max-w-2xl text-sm/6 text-slate-500 dark:text-slate-400">{{ t('settings.profileDesc') }}</p>
          </div>
        </div>

        <div class="mt-8 space-y-8 border-b border-slate-200 pb-10 sm:space-y-0 sm:divide-y sm:divide-slate-200 sm:border-t sm:pb-0 dark:border-white/10 dark:sm:divide-white/10">
          <div class="sm:grid sm:grid-cols-3 sm:items-start sm:gap-4 sm:py-5">
            <label for="realName" class="block text-sm/6 font-medium text-slate-900 sm:pt-1.5 dark:text-white">{{ t('settings.realName') }}</label>
            <div class="mt-2 sm:col-span-2 sm:mt-0">
              <input id="realName" v-model.trim="profile.realName" class="focus-ring block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 shadow-sm dark:border-white/10 dark:bg-white/5 dark:text-white" />
            </div>
          </div>

          <div class="sm:grid sm:grid-cols-3 sm:items-start sm:gap-4 sm:py-5">
            <label for="gender" class="block text-sm/6 font-medium text-slate-900 sm:pt-1.5 dark:text-white">{{ t('settings.gender') }}</label>
            <div class="mt-2 sm:col-span-2 sm:mt-0">
              <select id="gender" v-model.number="profile.gender" class="focus-ring block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 shadow-sm sm:max-w-xs dark:border-white/10 dark:bg-slate-900 dark:text-white">
                <option :value="0">{{ t('settings.genderUnknown') }}</option>
                <option :value="1">{{ t('settings.genderMale') }}</option>
                <option :value="2">{{ t('settings.genderFemale') }}</option>
              </select>
            </div>
          </div>

          <div class="sm:grid sm:grid-cols-3 sm:items-start sm:gap-4 sm:py-5">
            <label for="email" class="block text-sm/6 font-medium text-slate-900 sm:pt-1.5 dark:text-white">{{ t('settings.email') }}</label>
            <div class="mt-2 sm:col-span-2 sm:mt-0">
              <input id="email" v-model.trim="profile.email" type="email" class="focus-ring block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 shadow-sm sm:max-w-md dark:border-white/10 dark:bg-white/5 dark:text-white" />
            </div>
          </div>

          <div class="sm:grid sm:grid-cols-3 sm:items-start sm:gap-4 sm:py-5">
            <label for="phone" class="block text-sm/6 font-medium text-slate-900 sm:pt-1.5 dark:text-white">{{ t('settings.phone') }}</label>
            <div class="mt-2 sm:col-span-2 sm:mt-0">
              <input id="phone" v-model.trim="profile.phone" class="focus-ring block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 shadow-sm sm:max-w-md dark:border-white/10 dark:bg-white/5 dark:text-white" />
            </div>
          </div>

          <div class="sm:grid sm:grid-cols-3 sm:items-start sm:gap-4 sm:py-5">
            <label for="avatarUrl" class="block text-sm/6 font-medium text-slate-900 sm:pt-1.5 dark:text-white">{{ t('settings.avatarUrl') }}</label>
            <div class="mt-2 sm:col-span-2 sm:mt-0">
              <input id="avatarUrl" v-model.trim="profile.avatarUrl" class="focus-ring block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 shadow-sm dark:border-white/10 dark:bg-white/5 dark:text-white" />
            </div>
          </div>
        </div>
      </section>

      <section>
        <div class="flex items-start gap-3">
          <LanguageIcon class="mt-0.5 size-5 text-slate-400" aria-hidden="true" />
          <div>
            <h2 class="text-base/7 font-semibold text-slate-950 dark:text-white">{{ t('settings.appearance') }}</h2>
            <p class="mt-1 max-w-2xl text-sm/6 text-slate-500 dark:text-slate-400">{{ t('settings.appearanceDesc') }}</p>
          </div>
        </div>

        <div class="mt-8 space-y-8 border-b border-slate-200 pb-10 sm:space-y-0 sm:divide-y sm:divide-slate-200 sm:border-t sm:pb-0 dark:border-white/10 dark:sm:divide-white/10">
          <div class="sm:grid sm:grid-cols-3 sm:items-center sm:gap-4 sm:py-5">
            <div class="text-sm/6 font-medium text-slate-900 dark:text-white">{{ t('settings.themeMode') }}</div>
            <div class="mt-2 sm:col-span-2 sm:mt-0">
              <ThemeSwitcher />
            </div>
          </div>

          <div class="sm:grid sm:grid-cols-3 sm:items-start sm:gap-4 sm:py-5">
            <div class="text-sm/6 font-medium text-slate-900 dark:text-white">{{ t('settings.brandColor') }}</div>
            <div class="mt-2 grid grid-cols-2 gap-3 sm:col-span-2 sm:mt-0 sm:grid-cols-3">
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

          <div class="sm:grid sm:grid-cols-3 sm:items-start sm:gap-4 sm:py-5">
            <label for="locale" class="block text-sm/6 font-medium text-slate-900 sm:pt-1.5 dark:text-white">{{ t('settings.language') }}</label>
            <div class="mt-2 sm:col-span-2 sm:mt-0">
              <select id="locale" v-model="localeModel" class="focus-ring block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 shadow-sm sm:max-w-xs dark:border-white/10 dark:bg-slate-900 dark:text-white">
                <option v-for="locale in localeOptions" :key="locale.value" :value="locale.value">{{ locale.nativeLabel }}</option>
              </select>
            </div>
          </div>
        </div>
      </section>

      <section>
        <div class="flex items-start gap-3">
          <BellAlertIcon class="mt-0.5 size-5 text-slate-400" aria-hidden="true" />
          <div>
            <h2 class="text-base/7 font-semibold text-slate-950 dark:text-white">{{ t('settings.notifications') }}</h2>
            <p class="mt-1 max-w-2xl text-sm/6 text-slate-500 dark:text-slate-400">{{ t('settings.notificationsDesc') }}</p>
          </div>
        </div>

        <div class="mt-8 divide-y divide-slate-200 border-y border-slate-200 dark:divide-white/10 dark:border-white/10">
          <label class="flex items-start justify-between gap-4 py-5">
            <span>
              <span class="block text-sm font-medium text-slate-900 dark:text-white">{{ t('settings.courseNotice') }}</span>
              <span class="mt-1 block text-sm/6 text-slate-500 dark:text-slate-400">{{ t('settings.courseNoticeDesc') }}</span>
            </span>
            <span class="relative mt-1 inline-flex shrink-0">
              <input v-model="notifications.course" type="checkbox" class="peer sr-only" />
              <span class="h-6 w-11 rounded-full bg-slate-200 transition peer-checked:bg-[rgb(var(--color-brand))] dark:bg-white/10" />
              <span class="absolute left-0.5 top-0.5 size-5 rounded-full bg-white shadow-sm transition peer-checked:translate-x-5" />
            </span>
          </label>
          <label class="flex items-start justify-between gap-4 py-5">
            <span>
              <span class="block text-sm font-medium text-slate-900 dark:text-white">{{ t('settings.homeworkNotice') }}</span>
              <span class="mt-1 block text-sm/6 text-slate-500 dark:text-slate-400">{{ t('settings.homeworkNoticeDesc') }}</span>
            </span>
            <span class="relative mt-1 inline-flex shrink-0">
              <input v-model="notifications.homework" type="checkbox" class="peer sr-only" />
              <span class="h-6 w-11 rounded-full bg-slate-200 transition peer-checked:bg-[rgb(var(--color-brand))] dark:bg-white/10" />
              <span class="absolute left-0.5 top-0.5 size-5 rounded-full bg-white shadow-sm transition peer-checked:translate-x-5" />
            </span>
          </label>
          <label class="flex items-start justify-between gap-4 py-5">
            <span>
              <span class="block text-sm font-medium text-slate-900 dark:text-white">{{ t('settings.shareNotice') }}</span>
              <span class="mt-1 block text-sm/6 text-slate-500 dark:text-slate-400">{{ t('settings.shareNoticeDesc') }}</span>
            </span>
            <span class="relative mt-1 inline-flex shrink-0">
              <input v-model="notifications.share" type="checkbox" class="peer sr-only" />
              <span class="h-6 w-11 rounded-full bg-slate-200 transition peer-checked:bg-[rgb(var(--color-brand))] dark:bg-white/10" />
              <span class="absolute left-0.5 top-0.5 size-5 rounded-full bg-white shadow-sm transition peer-checked:translate-x-5" />
            </span>
          </label>
        </div>
      </section>

      <section>
        <div class="flex items-start gap-3">
          <ShieldCheckIcon class="mt-0.5 size-5 text-slate-400" aria-hidden="true" />
          <div>
            <h2 class="text-base/7 font-semibold text-slate-950 dark:text-white">{{ t('settings.security') }}</h2>
            <p class="mt-1 max-w-2xl text-sm/6 text-slate-500 dark:text-slate-400">{{ t('settings.securityDesc') }}</p>
          </div>
        </div>
        <dl class="mt-8 divide-y divide-slate-200 border-y border-slate-200 text-sm/6 dark:divide-white/10 dark:border-white/10">
          <div class="flex justify-between gap-x-6 py-5">
            <dt class="font-medium text-slate-900 dark:text-white">{{ t('settings.authenticated') }}</dt>
            <dd class="text-slate-500 dark:text-slate-300">{{ auth.displayName }}</dd>
          </div>
          <div class="flex justify-between gap-x-6 py-5">
            <dt class="font-medium text-slate-900 dark:text-white">{{ t('settings.permissionGuard') }}</dt>
            <dd class="text-slate-500 dark:text-slate-300">{{ auth.permissions.length }}</dd>
          </div>
          <div class="flex justify-between gap-x-6 py-5">
            <dt class="font-medium text-slate-900 dark:text-white">{{ t('settings.sessionManaged') }}</dt>
            <dd class="text-slate-500 dark:text-slate-300">JWT</dd>
          </div>
        </dl>
      </section>
    </div>

    <div class="mt-8 flex items-center justify-end">
      <button type="submit" class="btn-primary focus-ring inline-flex h-10 items-center px-4 text-sm" :disabled="saving">
        {{ saving ? t('settings.saving') : t('settings.save') }}
      </button>
    </div>
  </form>
</template>
