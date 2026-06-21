<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { AcademicCapIcon, ArrowRightIcon, ChartBarSquareIcon, LockClosedIcon } from '@heroicons/vue/24/outline'

import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const username = ref('student')
const password = ref('Student@123')
const busy = ref(false)
const error = ref('')

const redirect = computed(() => {
  const value = route.query.redirect
  return typeof value === 'string' && value.startsWith('/') ? value : '/'
})

const presets = [
  { label: '学生', username: 'student', password: 'Student@123' },
  { label: '教师', username: 'teacher', password: 'Teacher@123' },
  { label: '管理员', username: 'admin', password: 'Admin@123' },
]

function applyPreset(preset: (typeof presets)[number]) {
  username.value = preset.username
  password.value = preset.password
}

async function submit() {
  error.value = ''
  busy.value = true
  try {
    await auth.login(username.value, password.value)
    await router.push(redirect.value)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '登录失败'
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <main class="grid min-h-screen bg-slate-950 lg:grid-cols-[minmax(0,0.92fr)_minmax(0,1.08fr)]">
    <section class="flex min-h-screen items-center justify-center bg-white px-6 py-10 dark:bg-slate-950">
      <div class="w-full max-w-sm">
        <div class="flex items-center gap-3">
          <span class="grid size-11 place-items-center rounded-md bg-[rgb(var(--color-brand))] text-base font-bold text-white shadow-sm">
            EV
          </span>
          <div>
            <p class="text-lg font-semibold text-slate-950 dark:text-white">EduVoyage</p>
            <p class="text-sm text-slate-500 dark:text-slate-400">高校在线学习平台</p>
          </div>
        </div>

        <div class="mt-10">
          <h1 class="text-2xl font-semibold text-slate-950 dark:text-white">登录工作台</h1>
          <p class="mt-2 text-sm text-slate-500 dark:text-slate-400">课程、图谱、作业、网盘和学情数据集中处理。</p>
        </div>

        <form class="mt-8 space-y-5" @submit.prevent="submit">
          <div>
            <label for="username" class="block text-sm font-medium text-slate-700 dark:text-slate-200">账号</label>
            <input
              id="username"
              v-model.trim="username"
              autocomplete="username"
              class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 shadow-sm placeholder:text-slate-400 dark:border-white/10 dark:bg-white/5 dark:text-white"
              required
            />
          </div>
          <div>
            <label for="password" class="block text-sm font-medium text-slate-700 dark:text-slate-200">密码</label>
            <input
              id="password"
              v-model="password"
              type="password"
              autocomplete="current-password"
              class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 shadow-sm placeholder:text-slate-400 dark:border-white/10 dark:bg-white/5 dark:text-white"
              required
            />
          </div>

          <div class="grid grid-cols-3 gap-2">
            <button
              v-for="preset in presets"
              :key="preset.username"
              type="button"
              class="focus-ring rounded-md border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 dark:border-white/10 dark:bg-white/5 dark:text-slate-200 dark:hover:bg-white/10"
              @click="applyPreset(preset)"
            >
              {{ preset.label }}
            </button>
          </div>

          <p v-if="error" class="rounded-md bg-rose-50 px-3 py-2 text-sm text-rose-700 dark:bg-rose-400/10 dark:text-rose-200">
            {{ error }}
          </p>

          <button
            type="submit"
            class="btn-primary focus-ring inline-flex h-10 w-full items-center justify-center gap-2 px-4 text-sm"
            :disabled="busy"
          >
            <span>{{ busy ? '登录中' : '进入 EduVoyage' }}</span>
            <ArrowRightIcon class="size-4" aria-hidden="true" />
          </button>
        </form>
      </div>
    </section>

    <section class="hidden min-h-screen bg-slate-900 p-8 lg:block">
      <div class="flex h-full items-center justify-center">
        <div class="w-full max-w-2xl rounded-md border border-white/10 bg-white/[0.04] p-6 shadow-2xl">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-slate-300">知识图谱核心</p>
              <h2 class="mt-1 text-xl font-semibold text-white">课程学习画像</h2>
            </div>
            <span class="rounded-md bg-emerald-400/10 px-2.5 py-1 text-xs font-medium text-emerald-200">实时同步</span>
          </div>
          <div class="mt-8 grid grid-cols-[0.95fr_1.05fr] gap-6">
            <div class="space-y-3">
              <div class="rounded-md bg-white/8 p-4">
                <div class="flex items-center gap-3">
                  <AcademicCapIcon class="size-6 text-sky-300" aria-hidden="true" />
                  <div>
                    <p class="text-sm font-medium text-white">数据结构</p>
                    <p class="text-xs text-slate-400">掌握度 86%</p>
                  </div>
                </div>
                <div class="mt-4 h-2 rounded-full bg-white/10">
                  <div class="h-2 w-[86%] rounded-full bg-sky-300" />
                </div>
              </div>
              <div class="rounded-md bg-white/8 p-4">
                <div class="flex items-center gap-3">
                  <ChartBarSquareIcon class="size-6 text-emerald-300" aria-hidden="true" />
                  <div>
                    <p class="text-sm font-medium text-white">算法分析</p>
                    <p class="text-xs text-slate-400">作业均分 91.5</p>
                  </div>
                </div>
                <div class="mt-4 grid grid-cols-8 items-end gap-1">
                  <span class="h-8 rounded-sm bg-emerald-300/50" />
                  <span class="h-11 rounded-sm bg-emerald-300/70" />
                  <span class="h-6 rounded-sm bg-emerald-300/40" />
                  <span class="h-14 rounded-sm bg-emerald-300" />
                  <span class="h-10 rounded-sm bg-emerald-300/70" />
                  <span class="h-16 rounded-sm bg-emerald-300" />
                  <span class="h-12 rounded-sm bg-emerald-300/80" />
                  <span class="h-9 rounded-sm bg-emerald-300/60" />
                </div>
              </div>
            </div>
            <div class="relative min-h-80 rounded-md bg-slate-950/70 p-5">
              <div class="absolute left-[18%] top-[20%] size-20 rounded-full border border-sky-300/50 bg-sky-300/10" />
              <div class="absolute right-[16%] top-[16%] size-16 rounded-full border border-emerald-300/50 bg-emerald-300/10" />
              <div class="absolute bottom-[22%] left-[24%] size-16 rounded-full border border-amber-300/50 bg-amber-300/10" />
              <div class="absolute bottom-[18%] right-[22%] size-24 rounded-full border border-indigo-300/50 bg-indigo-300/10" />
              <div class="absolute left-[33%] top-[38%] h-px w-36 rotate-12 bg-white/20" />
              <div class="absolute bottom-[38%] left-[33%] h-px w-32 -rotate-12 bg-white/20" />
              <div class="absolute bottom-[37%] right-[31%] h-px w-28 rotate-45 bg-white/20" />
              <div class="relative z-10 flex h-full items-center justify-center">
                <div class="rounded-md border border-white/10 bg-white/10 px-4 py-3 text-center shadow-xl">
                  <LockClosedIcon class="mx-auto size-6 text-white" aria-hidden="true" />
                  <p class="mt-2 text-sm font-semibold text-white">权限与学习路径联动</p>
                  <p class="mt-1 text-xs text-slate-300">角色菜单、课程资源、分析视图统一收束</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  </main>
</template>
