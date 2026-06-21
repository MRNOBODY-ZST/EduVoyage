<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { AcademicCapIcon, ArrowPathIcon, MagnifyingGlassIcon, PlusCircleIcon } from '@heroicons/vue/24/outline'

import EmptyState from '@/components/state/EmptyState.vue'
import ErrorState from '@/components/state/ErrorState.vue'
import LoadingState from '@/components/state/LoadingState.vue'
import { courseStatusLabel, formatDateTime, formatNumber } from '@/lib/format'
import { enrollCourse, fetchCourses } from '@/lib/services'
import { useAuthStore } from '@/stores/auth'
import type { CourseResponse } from '@/types/api'

const auth = useAuthStore()
const loading = ref(true)
const error = ref('')
const keyword = ref('')
const courses = ref<CourseResponse[]>([])
const total = ref(0)
const enrolling = ref<number | null>(null)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const page = await fetchCourses({ keyword: keyword.value || undefined, pageNo: 1, pageSize: 24 })
    courses.value = page.records
    total.value = page.total
  } catch (e) {
    error.value = e instanceof Error ? e.message : '课程加载失败'
  } finally {
    loading.value = false
  }
}

async function enroll(course: CourseResponse) {
  enrolling.value = course.id
  try {
    await enrollCourse(course.id)
    course.enrolled = true
  } catch (e) {
    error.value = e instanceof Error ? e.message : '选课失败'
  } finally {
    enrolling.value = null
  }
}

onMounted(load)
</script>

<template>
  <div class="space-y-6">
    <section class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <h2 class="text-base font-semibold text-slate-950 dark:text-white">课程中心</h2>
          <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">当前匹配 {{ formatNumber(total) }} 门课程</p>
        </div>
        <div class="flex flex-col gap-3 sm:flex-row">
          <label class="relative">
            <MagnifyingGlassIcon class="pointer-events-none absolute left-3 top-2.5 size-5 text-slate-400" aria-hidden="true" />
            <input
              v-model.trim="keyword"
              class="focus-ring h-10 rounded-md border border-slate-300 bg-white pl-10 pr-3 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white"
              placeholder="课程名称"
              @keyup.enter="load"
            />
          </label>
          <button
            type="button"
            class="focus-ring inline-flex h-10 items-center justify-center gap-2 rounded-md border border-slate-200 bg-white px-3 text-sm font-semibold text-slate-700 hover:bg-slate-50 dark:border-white/10 dark:bg-white/5 dark:text-slate-200 dark:hover:bg-white/10"
            @click="load"
          >
            <ArrowPathIcon class="size-4" aria-hidden="true" />
            查询
          </button>
        </div>
      </div>
    </section>

    <LoadingState v-if="loading" />
    <ErrorState v-else-if="error" :message="error" @retry="load" />
    <EmptyState v-else-if="courses.length === 0" title="暂无课程" description="换一个关键词再试。" />
    <section v-else class="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
      <article
        v-for="course in courses"
        :key="course.id"
        class="overflow-hidden rounded-md border border-slate-200 bg-white shadow-sm dark:border-white/10 dark:bg-slate-900"
      >
        <div class="aspect-[16/7] bg-slate-100 dark:bg-white/5">
          <img v-if="course.coverUrl" :src="course.coverUrl" :alt="course.title" class="h-full w-full object-cover" />
          <div v-else class="flex h-full items-center justify-center bg-[rgb(var(--color-brand-soft))] text-[rgb(var(--color-brand-strong))] dark:text-white">
            <AcademicCapIcon class="size-12" aria-hidden="true" />
          </div>
        </div>
        <div class="p-5">
          <div class="flex items-start justify-between gap-3">
            <div class="min-w-0">
              <h3 class="truncate text-base font-semibold text-slate-950 dark:text-white">{{ course.title }}</h3>
              <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">教师 {{ course.teacherId }} · {{ course.credit ?? 0 }} 学分</p>
            </div>
            <span class="shrink-0 rounded-md bg-slate-100 px-2 py-1 text-xs font-medium text-slate-700 dark:bg-white/10 dark:text-slate-200">
              {{ courseStatusLabel(course.status) }}
            </span>
          </div>
          <p class="mt-4 line-clamp-2 min-h-10 text-sm leading-5 text-slate-600 dark:text-slate-300">
            {{ course.intro || '暂无课程简介' }}
          </p>
          <div class="mt-5 flex items-center justify-between gap-3">
            <span class="text-xs text-slate-500 dark:text-slate-400">{{ formatDateTime(course.createdAt) }}</span>
            <button
              v-if="auth.hasPermission('course:enroll')"
              type="button"
              class="btn-primary focus-ring inline-flex h-9 items-center gap-2 px-3 text-sm disabled:cursor-not-allowed disabled:opacity-60"
              :disabled="course.enrolled || enrolling === course.id"
              @click="enroll(course)"
            >
              <PlusCircleIcon class="size-4" aria-hidden="true" />
              {{ course.enrolled ? '已选课' : enrolling === course.id ? '处理中' : '选课' }}
            </button>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>
