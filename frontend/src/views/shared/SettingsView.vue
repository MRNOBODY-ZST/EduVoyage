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
const activeSection = ref<'profile' | 'appearance' | 'notifications' | 'security'>('profile')
const notifications = reactive({ course: true, homework: true, share: false })

const profile = reactive({ realName: '', email: '', phone: '', avatarUrl: '', gender: 0 })

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

const sidebarNav = computed(() => [
  { key: 'profile' as const, label: t('settings.profile'), icon: UserCircleIcon },
  { key: 'appearance' as const, label: t('settings.appearance'), icon: LanguageIcon },
  { key: 'notifications' as const, label: t('settings.notifications'), icon: BellAlertIcon },
  { key: 'security' as const, label: t('settings.security'), icon: ShieldCheckIcon },
])

async function saveProfile() {
  saving.value = true; error.value = ''; message.value = ''
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

const inputClass = 'focus-ring block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 shadow-xs placeholder:text-slate-400 dark:border-white/10 dark:bg-white/5 dark:text-white'
const selectClass = 'focus-ring block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 shadow-xs dark:border-white/10 dark:bg-slate-900 dark:text-white'
const fieldRowClass = 'sm:grid sm:grid-cols-3 sm:items-start sm:gap-4 sm:py-5'
const labelClass = 'block text-sm/6 font-medium text-slate-900 sm:pt-1.5 dark:text-white'
</script>

<template>
  <div class="mx-auto max-w-5xl">
    <!-- Header -->
    <div class="mb-8">
      <h2 class="text-lg font-semibold text-slate-950 dark:text-white">{{ t('settings.title') }}</h2>
      <p class="mt-1 text-sm/6 text-slate-500 dark:text-slate-400">{{ t('settings.subtitle') }}</p>
    </div>

    <div class="grid gap-8 lg:grid-cols-[220px_minmax(0,1fr)]">
      <!-- Sidebar nav -->
      <nav class="flex flex-row flex-wrap gap-1 lg:flex-col lg:flex-nowrap">
        <button
          v-for="item in sidebarNav"
          :key="item.key"
          type="button"
          :class="[
            activeSection === item.key
              ? 'bg-[rgb(var(--color-brand-soft))] text-[rgb(var(--color-brand))] font-semibold'
              : 'text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-white/5',
            'flex items-center gap-2.5 rounded-lg px-3 py-2.5 text-sm transition',
          ]"
          @click="activeSection = item.key"
        >
          <component :is="item.icon" class="size-4 shrink-0" aria-hidden="true" />
          {{ item.label }}
        </button>
      </nav>

      <!-- Content -->
      <div class="min-w-0 rounded-xl border border-slate-200 bg-white p-6 shadow-xs dark:border-white/10 dark:bg-slate-900">
        <!-- Alerts -->
        <ErrorState v-if="error" class="mb-5" :message="error" @retry="saveProfile" />
        <p v-if="message" class="mb-5 rounded-lg bg-emerald-50 px-3 py-2 text-sm text-emerald-700 dark:bg-emerald-400/10 dark:text-emerald-200">{{ message }}</p>

        <!-- Profile section -->
        <form v-if="activeSection === 'profile'" @submit.prevent="saveProfile">
          <div>
            <h3 class="text-base/7 font-semibold text-slate-950 dark:text-white">{{ t('settings.profile') }}</h3>
            <p class="mt-1 text-sm/6 text-slate-500 dark:text-slate-400">{{ t('settings.profileDesc') }}</p>
          </div>
          <div class="mt-6 space-y-0 divide-y divide-slate-200 border-y border-slate-200 dark:divide-white/10 dark:border-white/10">
            <div :class="fieldRowClass">
              <label for="realName" :class="labelClass">{{ t('settings.realName') }}</label>
              <div class="mt-2 sm:col-span-2 sm:mt-0">
                <input id="realName" v-model.trim="profile.realName" :class="inputClass" />
              </div>
            </div>
            <div :class="fieldRowClass">
              <label for="gender" :class="labelClass">{{ t('settings.gender') }}</label>
              <div class="mt-2 sm:col-span-2 sm:mt-0">
                <select id="gender" v-model.number="profile.gender" :class="[selectClass, 'sm:max-w-xs']">
                  <option :value="0">{{ t('settings.genderUnknown') }}</option>
                  <option :value="1">{{ t('settings.genderMale') }}</option>
                  <option :value="2">{{ t('settings.genderFemale') }}</option>
                </select>
              </div>
            </div>
            <div :class="fieldRowClass">
              <label for="email" :class="labelClass">{{ t('settings.email') }}</label>
              <div class="mt-2 sm:col-span-2 sm:mt-0">
                <input id="email" v-model.trim="profile.email" type="email" :class="[inputClass, 'sm:max-w-md']" />
              </div>
            </div>
            <div :class="fieldRowClass">
              <label for="phone" :class="labelClass">{{ t('settings.phone') }}</label>
              <div class="mt-2 sm:col-span-2 sm:mt-0">
                <input id="phone" v-model.trim="profile.phone" :class="[inputClass, 'sm:max-w-md']" />
              </div>
            </div>
            <div :class="fieldRowClass">
              <label for="avatarUrl" :class="labelClass">{{ t('settings.avatarUrl') }}</label>
              <div class="mt-2 sm:col-span-2 sm:mt-0">
                <div class="flex items-center gap-4">
                  <img v-if="profile.avatarUrl" :src="profile.avatarUrl" class="size-12 rounded-full object-cover" alt="avatar" />
                  <span v-else class="grid size-12 place-items-center rounded-full bg-[rgb(var(--color-brand-soft))] text-lg font-semibold text-[rgb(var(--color-brand))]">
                    {{ (auth.displayName || 'E').slice(0, 1).toUpperCase() }}
                  </span>
                  <input id="avatarUrl" v-model.trim="profile.avatarUrl" :class="[inputClass, 'flex-1']" placeholder="https://…" />
                </div>
              </div>
            </div>
          </div>
          <div class="mt-6 flex justify-end">
            <button type="submit" class="btn-primary focus-ring inline-flex h-10 items-center px-5 text-sm" :disabled="saving">
              {{ saving ? t('settings.saving') : t('settings.save') }}
            </button>
          </div>
        </form>

        <!-- Appearance section -->
        <div v-else-if="activeSection === 'appearance'">
          <h3 class="text-base/7 font-semibold text-slate-950 dark:text-white">{{ t('settings.appearance') }}</h3>
          <p class="mt-1 text-sm/6 text-slate-500 dark:text-slate-400">{{ t('settings.appearanceDesc') }}</p>
          <div class="mt-6 space-y-0 divide-y divide-slate-200 border-y border-slate-200 dark:divide-white/10 dark:border-white/10">
            <div :class="fieldRowClass">
              <div :class="labelClass">{{ t('settings.themeMode') }}</div>
              <div class="mt-2 sm:col-span-2 sm:mt-0"><ThemeSwitcher /></div>
            </div>
            <div :class="fieldRowClass">
              <div :class="labelClass">{{ t('settings.brandColor') }}</div>
              <div class="mt-2 grid grid-cols-2 gap-3 sm:col-span-2 sm:mt-0 sm:grid-cols-3">
                <button
                  v-for="theme in brandThemes"
                  :key="theme.key"
                  type="button"
                  :class="[
                    settings.brand === theme.key
                      ? 'ring-2 ring-[rgb(var(--color-brand))] ring-offset-2 dark:ring-offset-slate-900'
                      : 'hover:bg-slate-50 dark:hover:bg-white/10',
                    'focus-ring flex items-center justify-between rounded-lg border border-slate-200 bg-white px-3 py-3 text-sm font-medium text-slate-700 dark:border-white/10 dark:bg-white/5 dark:text-slate-200',
                  ]"
                  @click="settings.setBrand(theme.key)"
                >
                  <span class="flex items-center gap-2">
                    <span class="size-4 rounded-full shadow-xs" :style="{ backgroundColor: `rgb(${theme.light})` }" />
                    {{ theme.label }}
                  </span>
                  <CheckIcon v-if="settings.brand === theme.key" class="size-4 text-[rgb(var(--color-brand))]" aria-hidden="true" />
                </button>
              </div>
            </div>
            <div :class="fieldRowClass">
              <label for="locale" :class="labelClass">{{ t('settings.language') }}</label>
              <div class="mt-2 sm:col-span-2 sm:mt-0">
                <select id="locale" v-model="localeModel" :class="[selectClass, 'sm:max-w-xs']">
                  <option v-for="locale in localeOptions" :key="locale.value" :value="locale.value">{{ locale.nativeLabel }}</option>
                </select>
              </div>
            </div>
          </div>
        </div>

        <!-- Notifications section -->
        <div v-else-if="activeSection === 'notifications'">
          <h3 class="text-base/7 font-semibold text-slate-950 dark:text-white">{{ t('settings.notifications') }}</h3>
          <p class="mt-1 text-sm/6 text-slate-500 dark:text-slate-400">{{ t('settings.notificationsDesc') }}</p>
          <div class="mt-6 divide-y divide-slate-200 border-y border-slate-200 dark:divide-white/10 dark:border-white/10">
            <label v-for="(item, key) in [
              { model: 'course', title: t('settings.courseNotice'), desc: t('settings.courseNoticeDesc') },
              { model: 'homework', title: t('settings.homeworkNotice'), desc: t('settings.homeworkNoticeDesc') },
              { model: 'share', title: t('settings.shareNotice'), desc: t('settings.shareNoticeDesc') },
            ]" :key="item.model" class="flex items-start justify-between gap-6 py-5">
              <span>
                <span class="block text-sm font-medium text-slate-900 dark:text-white">{{ item.title }}</span>
                <span class="mt-1 block text-sm/6 text-slate-500 dark:text-slate-400">{{ item.desc }}</span>
              </span>
              <span class="relative mt-1 inline-flex shrink-0">
                <input v-model="(notifications as any)[item.model]" type="checkbox" class="peer sr-only" />
                <span class="h-6 w-11 rounded-full bg-slate-200 transition peer-checked:bg-[rgb(var(--color-brand))] dark:bg-white/10" />
                <span class="absolute left-0.5 top-0.5 size-5 rounded-full bg-white shadow-xs transition peer-checked:translate-x-5" />
              </span>
            </label>
          </div>
        </div>

        <!-- Security section -->
        <div v-else-if="activeSection === 'security'">
          <h3 class="text-base/7 font-semibold text-slate-950 dark:text-white">{{ t('settings.security') }}</h3>
          <p class="mt-1 text-sm/6 text-slate-500 dark:text-slate-400">{{ t('settings.securityDesc') }}</p>
          <dl class="mt-6 divide-y divide-slate-200 border-y border-slate-200 text-sm/6 dark:divide-white/10 dark:border-white/10">
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
        </div>
      </div>
    </div>
  </div>
</template>
