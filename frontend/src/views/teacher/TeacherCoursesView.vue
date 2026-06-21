<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ArrowPathIcon, PlusIcon, RocketLaunchIcon } from '@heroicons/vue/24/outline'

import EmptyState from '@/components/state/EmptyState.vue'
import ErrorState from '@/components/state/ErrorState.vue'
import LoadingState from '@/components/state/LoadingState.vue'
import { courseStatusLabel, formatDateTime, formatNumber } from '@/lib/format'
import { createCourse, fetchCourses, publishCourse } from '@/lib/services'
import { useAuthStore } from '@/stores/auth'
import type { CourseResponse } from '@/types/api'

const auth = useAuthStore()
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const keyword = ref('')
const creating = ref(false)
const courses = ref<CourseResponse[]>([])
const total = ref(0)

const form = reactive({
  title: '',
  intro: '',
  credit: 3,
  visibility: 1,
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const page = await fetchCourses({
      keyword: keyword.value || undefined,
      teacherId: auth.user?.id,
      pageNo: 1,
      pageSize: 20,
    })
    courses.value = page.records
    total.value = page.total
  } catch (e) {
    error.value = e instanceof Error ? e.message : '课程加载失败'
  } finally {
    loading.value = false
  }
}

async function submit() {
  saving.value = true
  try {
    await createCourse({
      title: form.title,
      intro: form.intro || undefined,
      credit: form.credit,
      visibility: form.visibility,
    })
    form.title = ''
    form.intro = ''
    form.credit = 3
    form.visibility = 1
    creating.value = false
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '课程创建失败'
  } finally {
    saving.value = false
  }
}

async function publish(id: number) {
  await publishCourse(id)
  await load()
}

onMounted(load)
</script>

<template>
  <div class="space-y-6">
    <section class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <h2 class="text-base font-semibold text-slate-950 dark:text-white">课程管理</h2>
          <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">共 {{ formatNumber(total) }} 门课程</p>
        </div>
        <div class="flex flex-col gap-3 sm:flex-row">
          <input
            v-model.trim="keyword"
            class="focus-ring h-10 rounded-md border border-slate-300 bg-white px-3 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white"
            placeholder="搜索课程"
            @keyup.enter="load"
          />
          <button
            type="button"
            class="focus-ring inline-flex h-10 items-center justify-center gap-2 rounded-md border border-slate-200 bg-white px-3 text-sm font-semibold text-slate-700 hover:bg-slate-50 dark:border-white/10 dark:bg-white/5 dark:text-slate-200 dark:hover:bg-white/10"
            @click="load"
          >
            <ArrowPathIcon class="size-4" aria-hidden="true" />
            刷新
          </button>
          <button
            v-if="auth.hasPermission('course:create')"
            type="button"
            class="btn-primary focus-ring inline-flex h-10 items-center justify-center gap-2 px-3 text-sm"
            @click="creating = !creating"
          >
            <PlusIcon class="size-4" aria-hidden="true" />
            新建课程
          </button>
        </div>
      </div>

      <form v-if="creating" class="mt-5 grid gap-4 border-t border-slate-200 pt-5 md:grid-cols-4 dark:border-white/10" @submit.prevent="submit">
        <label class="md:col-span-2">
          <span class="text-sm font-medium text-slate-700 dark:text-slate-200">课程标题</span>
          <input
            v-model.trim="form.title"
            class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white"
            required
          />
        </label>
        <label>
          <span class="text-sm font-medium text-slate-700 dark:text-slate-200">学分</span>
          <input
            v-model.number="form.credit"
            type="number"
            min="0"
            step="0.5"
            class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white"
          />
        </label>
        <label>
          <span class="text-sm font-medium text-slate-700 dark:text-slate-200">可见性</span>
          <select
            v-model.number="form.visibility"
            class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-slate-900 dark:text-white"
          >
            <option :value="1">公开</option>
            <option :value="0">私有</option>
          </select>
        </label>
        <label class="md:col-span-4">
          <span class="text-sm font-medium text-slate-700 dark:text-slate-200">简介</span>
          <textarea
            v-model.trim="form.intro"
            rows="3"
            class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white"
          />
        </label>
        <div class="md:col-span-4">
          <button type="submit" class="btn-primary focus-ring inline-flex h-10 items-center px-4 text-sm" :disabled="saving">
            {{ saving ? '保存中' : '保存课程' }}
          </button>
        </div>
      </form>
    </section>

    <LoadingState v-if="loading" />
    <ErrorState v-else-if="error" :message="error" @retry="load" />
    <EmptyState v-else-if="courses.length === 0" title="暂无课程" description="创建课程后会显示在工作台。" />
    <section v-else class="overflow-hidden rounded-md border border-slate-200 bg-white shadow-sm dark:border-white/10 dark:bg-slate-900">
      <div class="overflow-x-auto">
        <table class="min-w-full divide-y divide-slate-200 dark:divide-white/10">
          <thead class="bg-slate-50 dark:bg-white/5">
            <tr>
              <th class="px-4 py-3 text-left text-sm font-semibold text-slate-900 dark:text-white">课程</th>
              <th class="px-4 py-3 text-left text-sm font-semibold text-slate-900 dark:text-white">学分</th>
              <th class="px-4 py-3 text-left text-sm font-semibold text-slate-900 dark:text-white">状态</th>
              <th class="px-4 py-3 text-left text-sm font-semibold text-slate-900 dark:text-white">创建时间</th>
              <th class="px-4 py-3 text-right text-sm font-semibold text-slate-900 dark:text-white">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100 dark:divide-white/10">
            <tr v-for="course in courses" :key="course.id">
              <td class="px-4 py-4">
                <RouterLink :to="`/courses/${course.id}`" class="max-w-md truncate text-sm font-medium text-slate-950 hover:text-[rgb(var(--color-brand))] dark:text-white">
                  {{ course.title }}
                </RouterLink>
                <p class="mt-1 max-w-md truncate text-xs text-slate-500 dark:text-slate-400">{{ course.intro || '暂无简介' }}</p>
              </td>
              <td class="px-4 py-4 text-sm text-slate-600 dark:text-slate-300">{{ course.credit ?? 0 }}</td>
              <td class="px-4 py-4">
                <span class="rounded-md bg-slate-100 px-2 py-1 text-xs font-medium text-slate-700 dark:bg-white/10 dark:text-slate-200">
                  {{ courseStatusLabel(course.status) }}
                </span>
              </td>
              <td class="px-4 py-4 text-sm text-slate-500 dark:text-slate-400">{{ formatDateTime(course.createdAt) }}</td>
              <td class="px-4 py-4 text-right">
                <button
                  v-if="course.status === 0 && auth.hasPermission('course:update')"
                  type="button"
                  class="inline-flex items-center gap-1 text-sm font-semibold text-[rgb(var(--color-brand))] hover:underline"
                  @click="publish(course.id)"
                >
                  <RocketLaunchIcon class="size-4" aria-hidden="true" />
                  发布
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>
