<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { PlayCircleIcon, ClipboardDocumentCheckIcon } from '@heroicons/vue/24/outline'

import EmptyState from '@/components/state/EmptyState.vue'
import LoadingState from '@/components/state/LoadingState.vue'
import { formatDateTime } from '@/lib/format'
import { fetchMySubmissions, startSubmission, submitSubmission } from '@/lib/services'
import { useAuthStore } from '@/stores/auth'
import type { ExamPaper, HomeworkResponse, SubmissionResult } from '@/types/api'

const props = defineProps<{
  homeworks: HomeworkResponse[]
}>()

const auth = useAuthStore()
const loading = ref(false)
const error = ref('')
const activeHomeworkId = ref<number | null>(null)
const exam = ref<ExamPaper | null>(null)
const result = ref<SubmissionResult | null>(null)
const attempts = ref<SubmissionResult[]>([])
const answers = reactive<Record<number, string>>({})

const canSubmit = computed(() => auth.hasPermission('homework:submit'))

function typeLabel(type: number) {
  if (type === 1) return '单选'
  if (type === 2) return '多选'
  if (type === 3) return '判断'
  if (type === 4) return '填空'
  return '简答'
}

function statusLabel(status: number) {
  if (status === 2) return '已批改'
  if (status === 1) return '已提交'
  return '进行中'
}

function scoreText(score: number | undefined) {
  return Number(score ?? 0).toFixed(1)
}

function selectedSet(questionId: number) {
  return new Set((answers[questionId] || '').split(',').filter(Boolean))
}

function toggleMulti(questionId: number, optionKey: string) {
  const next = selectedSet(questionId)
  if (next.has(optionKey)) {
    next.delete(optionKey)
  } else {
    next.add(optionKey)
  }
  answers[questionId] = [...next].sort().join(',')
}

async function loadAttempts(homeworkId: number) {
  if (!canSubmit.value) {
    return
  }
  attempts.value = await fetchMySubmissions(homeworkId).catch(() => [])
}

async function start(homework: HomeworkResponse) {
  loading.value = true
  error.value = ''
  result.value = null
  activeHomeworkId.value = homework.id
  Object.keys(answers).forEach((key) => delete answers[Number(key)])
  try {
    exam.value = await startSubmission(homework.id)
    exam.value.questions.forEach((question) => {
      answers[question.id] = ''
    })
    await loadAttempts(homework.id)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '开始作答失败'
    exam.value = null
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!exam.value) {
    return
  }
  loading.value = true
  error.value = ''
  try {
    result.value = await submitSubmission(exam.value.submissionId, {
      answers: exam.value.questions.map((question) => ({
        questionId: question.id,
        answer: answers[question.id] || '',
      })),
      switchCount: 0,
    })
    await loadAttempts(exam.value.homeworkId)
    exam.value = null
  } catch (e) {
    error.value = e instanceof Error ? e.message : '提交失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="space-y-6">
    <EmptyState v-if="homeworks.length === 0" title="暂无作业" description="发布作业后会显示在这里。" />

    <div v-else class="overflow-hidden rounded-md border border-slate-200 bg-white shadow-sm dark:border-white/10 dark:bg-slate-900">
      <div class="overflow-x-auto">
        <table class="min-w-full divide-y divide-slate-200 dark:divide-white/10">
          <thead class="bg-slate-50 dark:bg-white/5">
            <tr>
              <th class="px-5 py-3 text-left text-sm font-semibold text-slate-900 dark:text-white">标题</th>
              <th class="px-5 py-3 text-left text-sm font-semibold text-slate-900 dark:text-white">总分</th>
              <th class="px-5 py-3 text-left text-sm font-semibold text-slate-900 dark:text-white">题量</th>
              <th class="px-5 py-3 text-left text-sm font-semibold text-slate-900 dark:text-white">截止</th>
              <th class="px-5 py-3 text-right text-sm font-semibold text-slate-900 dark:text-white">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100 dark:divide-white/10">
            <tr v-for="item in homeworks" :key="item.id">
              <td class="px-5 py-4">
                <p class="text-sm font-medium text-slate-950 dark:text-white">{{ item.title }}</p>
                <p class="mt-1 text-xs text-slate-500 dark:text-slate-400">状态 {{ item.status }} · 最多 {{ item.maxAttempts || 1 }} 次</p>
              </td>
              <td class="px-5 py-4 text-sm text-slate-500 dark:text-slate-400">{{ scoreText(item.totalScore) }}</td>
              <td class="px-5 py-4 text-sm text-slate-500 dark:text-slate-400">{{ item.questionCount }}</td>
              <td class="px-5 py-4 text-sm text-slate-500 dark:text-slate-400">{{ formatDateTime(item.deadline) }}</td>
              <td class="px-5 py-4 text-right">
                <button
                  v-if="canSubmit"
                  type="button"
                  class="btn-primary focus-ring inline-flex h-9 items-center gap-2 px-3 text-sm"
                  :disabled="loading || item.status !== 1"
                  @click="start(item)"
                >
                  <PlayCircleIcon class="size-4" aria-hidden="true" />
                  作答
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <p v-if="error" class="rounded-md bg-rose-50 px-3 py-2 text-sm text-rose-700 dark:bg-rose-400/10 dark:text-rose-200">
      {{ error }}
    </p>
    <LoadingState v-if="loading" />

    <section v-if="exam" class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900">
      <div class="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h3 class="text-base font-semibold text-slate-950 dark:text-white">{{ exam.title }}</h3>
          <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
            第 {{ exam.attemptNo }} 次尝试 · 总分 {{ scoreText(exam.totalScore) }} · {{ exam.timeLimit ? `${exam.timeLimit} 分钟` : '不限时' }}
          </p>
        </div>
        <button type="button" class="btn-primary focus-ring inline-flex h-10 items-center px-4 text-sm" :disabled="loading" @click="submit">
          提交作答
        </button>
      </div>

      <ol class="mt-5 space-y-4">
        <li v-for="(question, index) in exam.questions" :key="question.id" class="rounded-md border border-slate-200 p-4 dark:border-white/10">
          <div class="flex flex-wrap items-center justify-between gap-3">
            <h4 class="text-sm font-semibold text-slate-950 dark:text-white">{{ index + 1 }}. {{ question.stem }}</h4>
            <span class="rounded-md bg-slate-100 px-2 py-1 text-xs text-slate-600 dark:bg-white/10 dark:text-slate-300">
              {{ typeLabel(question.type) }} · {{ scoreText(question.score) }} 分
            </span>
          </div>

          <div v-if="question.type === 1 || question.type === 3" class="mt-4 grid gap-2 sm:grid-cols-2">
            <label
              v-for="option in question.options"
              :key="option.optionKey"
              class="flex cursor-pointer items-start gap-2 rounded-md border border-slate-200 px-3 py-2 text-sm text-slate-700 dark:border-white/10 dark:text-slate-200"
            >
              <input v-model="answers[question.id]" :value="option.optionKey" type="radio" class="mt-1" />
              <span>{{ option.optionKey }}. {{ option.content }}</span>
            </label>
          </div>

          <div v-else-if="question.type === 2" class="mt-4 grid gap-2 sm:grid-cols-2">
            <label
              v-for="option in question.options"
              :key="option.optionKey"
              class="flex cursor-pointer items-start gap-2 rounded-md border border-slate-200 px-3 py-2 text-sm text-slate-700 dark:border-white/10 dark:text-slate-200"
            >
              <input :checked="selectedSet(question.id).has(option.optionKey)" type="checkbox" class="mt-1" @change="toggleMulti(question.id, option.optionKey)" />
              <span>{{ option.optionKey }}. {{ option.content }}</span>
            </label>
          </div>

          <textarea
            v-else
            v-model="answers[question.id]"
            rows="4"
            class="focus-ring mt-4 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white"
          />
        </li>
      </ol>
    </section>

    <section v-if="result" class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900">
      <div class="flex items-start gap-3">
        <ClipboardDocumentCheckIcon class="size-6 text-[rgb(var(--color-brand))]" aria-hidden="true" />
        <div>
          <h3 class="text-base font-semibold text-slate-950 dark:text-white">提交结果</h3>
          <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
            {{ statusLabel(result.status) }} · 得分 {{ scoreText(result.totalScore) }} · {{ formatDateTime(result.submittedAt) }}
          </p>
        </div>
      </div>
      <ul class="mt-4 divide-y divide-slate-100 dark:divide-white/10">
        <li v-for="answer in result.answers" :key="answer.questionId" class="flex items-center justify-between gap-4 py-3">
          <span class="text-sm text-slate-700 dark:text-slate-200">题目 {{ answer.questionId }}：{{ answer.answer || '未作答' }}</span>
          <span class="rounded-md bg-slate-100 px-2 py-1 text-xs text-slate-600 dark:bg-white/10 dark:text-slate-300">
            {{ scoreText(answer.score) }} 分
          </span>
        </li>
      </ul>
    </section>

    <section v-if="attempts.length" class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900">
      <h3 class="text-base font-semibold text-slate-950 dark:text-white">我的提交记录</h3>
      <ul class="mt-3 divide-y divide-slate-100 dark:divide-white/10">
        <li v-for="item in attempts" :key="item.id" class="flex items-center justify-between gap-4 py-3">
          <span class="text-sm text-slate-700 dark:text-slate-200">第 {{ item.attemptNo }} 次 · {{ statusLabel(item.status) }}</span>
          <span class="text-sm font-semibold text-slate-950 dark:text-white">{{ scoreText(item.totalScore) }}</span>
        </li>
      </ul>
    </section>
  </div>
</template>
