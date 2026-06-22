<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ChatBubbleLeftRightIcon, HandThumbUpIcon, PaperAirplaneIcon } from '@heroicons/vue/24/outline'

import EmptyState from '@/components/state/EmptyState.vue'
import ErrorState from '@/components/state/ErrorState.vue'
import LoadingState from '@/components/state/LoadingState.vue'
import { formatDateTime, formatNumber } from '@/lib/format'
import { createDiscussion, fetchCourses, fetchDiscussions, toggleDiscussionLike } from '@/lib/services'
import { useAuthStore } from '@/stores/auth'
import type { CourseResponse, DiscussionResponse } from '@/types/api'

const auth = useAuthStore()
const loadingCourses = ref(true)
const loadingDiscussions = ref(false)
const error = ref('')
const courses = ref<CourseResponse[]>([])
const selectedCourseId = ref<number | null>(null)
const discussions = ref<DiscussionResponse[]>([])
const total = ref(0)

const form = reactive({
  title: '',
  content: '',
})

async function loadCourses() {
  loadingCourses.value = true
  error.value = ''
  try {
    const page = await fetchCourses({ pageNo: 1, pageSize: 50 })
    courses.value = page.records
    selectedCourseId.value = selectedCourseId.value || page.records[0]?.id || null
  } catch (e) {
    error.value = e instanceof Error ? e.message : '课程加载失败'
  } finally {
    loadingCourses.value = false
  }
}

async function loadDiscussions(courseId: number | null) {
  if (!courseId) {
    discussions.value = []
    total.value = 0
    return
  }
  loadingDiscussions.value = true
  error.value = ''
  try {
    const page = await fetchDiscussions(courseId, { pageNo: 1, pageSize: 20 })
    discussions.value = page.records
    total.value = page.total
  } catch (e) {
    error.value = e instanceof Error ? e.message : '讨论加载失败'
  } finally {
    loadingDiscussions.value = false
  }
}

async function submit() {
  if (!selectedCourseId.value || !form.title.trim() || !form.content.trim()) {
    return
  }
  try {
    await createDiscussion(selectedCourseId.value, {
      title: form.title.trim(),
      content: form.content.trim(),
    })
    form.title = ''
    form.content = ''
    await loadDiscussions(selectedCourseId.value)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '发帖失败'
  }
}

async function like(item: DiscussionResponse) {
  const updated = await toggleDiscussionLike(item.id)
  const index = discussions.value.findIndex((row) => row.id === item.id)
  if (index >= 0) {
    discussions.value[index] = updated
  }
}

watch(selectedCourseId, loadDiscussions)
onMounted(loadCourses)
</script>

<template>
  <LoadingState v-if="loadingCourses" />
  <ErrorState v-else-if="error && !discussions.length" :message="error" @retry="loadCourses" />
  <EmptyState v-else-if="courses.length === 0" title="暂无课程" description="课程中心有数据后可进入讨论区。" :icon="ChatBubbleLeftRightIcon" />
  <div v-else class="space-y-6">
    <section class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <label class="block max-w-sm">
          <span class="text-sm font-medium text-slate-700 dark:text-slate-200">课程</span>
          <select
            v-model="selectedCourseId"
            class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-slate-900 dark:text-white"
          >
            <option v-for="course in courses" :key="course.id" :value="course.id">{{ course.title }}</option>
          </select>
        </label>
        <p class="text-sm text-slate-500 dark:text-slate-400">共 {{ formatNumber(total) }} 条讨论</p>
      </div>
    </section>

    <form
      v-if="auth.hasPermission('discussion:write')"
      class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900"
      @submit.prevent="submit"
    >
      <h2 class="text-base font-semibold text-slate-950 dark:text-white">发布讨论</h2>
      <div class="mt-4 grid gap-4">
        <input
          v-model.trim="form.title"
          class="focus-ring rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white"
          maxlength="200"
          placeholder="标题"
        />
        <textarea
          v-model.trim="form.content"
          rows="4"
          class="focus-ring rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white"
          maxlength="8000"
          placeholder="内容"
        />
      </div>
      <div class="mt-4">
        <button type="submit" class="btn-primary focus-ring inline-flex h-10 items-center gap-2 px-4 text-sm">
          <PaperAirplaneIcon class="size-4" aria-hidden="true" />
          发布
        </button>
      </div>
    </form>

    <ErrorState v-if="error" :message="error" @retry="loadDiscussions(selectedCourseId)" />
    <LoadingState v-else-if="loadingDiscussions" />
    <EmptyState v-else-if="discussions.length === 0" title="暂无讨论" description="成为第一个发起课程讨论的人。" :icon="ChatBubbleLeftRightIcon" />
    <section v-else class="space-y-4">
      <article
        v-for="item in discussions"
        :key="item.id"
        class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900"
      >
        <div class="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
          <div class="min-w-0">
            <h3 class="text-base font-semibold text-slate-950 dark:text-white">{{ item.title }}</h3>
            <p class="mt-1 text-xs text-slate-500 dark:text-slate-400">作者 {{ item.authorId }} · {{ formatDateTime(item.ts) }}</p>
          </div>
          <button
            type="button"
            class="focus-ring inline-flex h-9 items-center gap-2 rounded-md border border-slate-200 px-3 text-sm font-medium text-slate-700 hover:bg-slate-50 dark:border-white/10 dark:text-slate-200 dark:hover:bg-white/10"
            @click="like(item)"
          >
            <HandThumbUpIcon class="size-4" :class="item.liked ? 'text-[rgb(var(--color-brand))]' : 'text-slate-400'" aria-hidden="true" />
            {{ item.likeCount }}
          </button>
        </div>
        <p class="mt-4 whitespace-pre-wrap text-sm leading-6 text-slate-700 dark:text-slate-300">{{ item.content }}</p>
        <p class="mt-4 text-xs text-slate-500 dark:text-slate-400">{{ item.replyCount }} 条回复</p>
      </article>
    </section>
  </div>
</template>
