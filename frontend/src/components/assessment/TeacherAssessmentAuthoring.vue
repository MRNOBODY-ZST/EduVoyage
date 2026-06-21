<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ArrowPathIcon, CheckCircleIcon, PlusIcon, RocketLaunchIcon, TrashIcon, XCircleIcon } from '@heroicons/vue/24/outline'

import EmptyState from '@/components/state/EmptyState.vue'
import {
  closeHomework,
  createHomework,
  createQuestion,
  deleteHomework,
  deleteQuestion,
  fetchHomeworkSubmissions,
  fetchQuestions,
  gradeSubmission,
  publishHomework,
} from '@/lib/services'
import { formatDateTime } from '@/lib/format'
import type { AnswerResult, HomeworkResponse, KnowledgeNodeResponse, QuestionResponse, SubmissionResult } from '@/types/api'

const props = defineProps<{
  courseId: number
  nodes: KnowledgeNodeResponse[]
  homeworks: HomeworkResponse[]
}>()

const emit = defineEmits<{
  refresh: []
}>()

const loading = ref(false)
const message = ref('')
const error = ref('')
const questions = ref<QuestionResponse[]>([])
const selectedQuestionIds = ref<number[]>([])
const scoreMap = reactive<Record<number, number>>({})
const selectedHomeworkId = ref(0)
const submissions = ref<SubmissionResult[]>([])
const activeSubmissionId = ref(0)
const gradeMap = reactive<Record<number, number>>({})
const commentMap = reactive<Record<number, string>>({})

const questionForm = reactive({
  type: 1,
  stem: '',
  answer: '',
  analysis: '',
  difficulty: 3,
  nodeId: 0,
  lang: '',
  options: [
    { optionKey: 'A', content: '', correct: false, sortNo: 1 },
    { optionKey: 'B', content: '', correct: false, sortNo: 2 },
    { optionKey: 'C', content: '', correct: false, sortNo: 3 },
    { optionKey: 'D', content: '', correct: false, sortNo: 4 },
  ],
})

const homeworkForm = reactive({
  title: '',
  timeLimit: null as number | null,
  deadline: '',
  maxAttempts: 1,
  shuffle: false,
  antiSwitch: false,
})

const needsOptions = computed(() => questionForm.type <= 3)
const selectedHomework = computed(() => props.homeworks.find((item) => item.id === selectedHomeworkId.value))
const activeSubmission = computed(() => submissions.value.find((item) => item.id === activeSubmissionId.value))
const questionById = computed(() => new Map(questions.value.map((item) => [item.id, item])))
const gradableAnswers = computed(() => activeSubmission.value?.answers.filter((answer) => canGradeAnswer(answer)) ?? [])

function typeLabel(type: number) {
  if (type === 1) return '单选'
  if (type === 2) return '多选'
  if (type === 3) return '判断'
  if (type === 4) return '填空'
  return '简答'
}

function homeworkStatusLabel(status: number) {
  if (status === 1) return '已发布'
  if (status === 2) return '已关闭'
  return '草稿'
}

function submissionStatusLabel(status: number) {
  if (status === 0) return '作答中'
  if (status === 1) return '待批阅'
  if (status === 2) return '已批阅'
  return '未知'
}

function scoreText(score: number | undefined) {
  return Number(score ?? 0).toFixed(1)
}

function questionFor(questionId: number) {
  return questionById.value.get(questionId)
}

function canGradeAnswer(answer: AnswerResult) {
  if (!activeSubmission.value || activeSubmission.value.status === 2) {
    return false
  }
  const question = questionFor(answer.questionId)
  return answer.score == null || question?.type === 5
}

function resetQuestion() {
  questionForm.stem = ''
  questionForm.answer = ''
  questionForm.analysis = ''
  questionForm.difficulty = 3
  questionForm.nodeId = 0
  questionForm.lang = ''
  questionForm.options.forEach((option) => {
    option.content = ''
    option.correct = false
  })
}

function normalizeDeadline(value: string) {
  return value ? value : undefined
}

function toggleQuestion(id: number) {
  if (selectedQuestionIds.value.includes(id)) {
    selectedQuestionIds.value = selectedQuestionIds.value.filter((item) => item !== id)
    delete scoreMap[id]
  } else {
    selectedQuestionIds.value = [...selectedQuestionIds.value, id]
    scoreMap[id] = scoreMap[id] || 10
  }
}

async function loadQuestions() {
  loading.value = true
  error.value = ''
  try {
    const page = await fetchQuestions({ courseId: props.courseId, pageNo: 1, pageSize: 50 })
    questions.value = page.records
  } catch (e) {
    error.value = e instanceof Error ? e.message : '题库加载失败'
  } finally {
    loading.value = false
  }
}

function seedGradeForm(submission: SubmissionResult | undefined) {
  Object.keys(gradeMap).forEach((key) => delete gradeMap[Number(key)])
  Object.keys(commentMap).forEach((key) => delete commentMap[Number(key)])
  if (!submission) {
    return
  }
  submission.answers.forEach((answer) => {
    if (canGradeAnswer(answer)) {
      gradeMap[answer.questionId] = Number(answer.score ?? 0)
      commentMap[answer.questionId] = answer.comment || ''
    }
  })
}

function selectSubmission(submission: SubmissionResult) {
  activeSubmissionId.value = submission.id
  seedGradeForm(submission)
}

async function loadSubmissions(homeworkId = selectedHomeworkId.value) {
  if (!homeworkId) {
    submissions.value = []
    activeSubmissionId.value = 0
    seedGradeForm(undefined)
    return
  }
  selectedHomeworkId.value = homeworkId
  loading.value = true
  error.value = ''
  try {
    submissions.value = await fetchHomeworkSubmissions(homeworkId)
    const current = submissions.value.find((item) => item.id === activeSubmissionId.value)
    const next = current || submissions.value.find((item) => item.status === 1) || submissions.value[0]
    activeSubmissionId.value = next?.id || 0
    seedGradeForm(next)
  } catch (e) {
    submissions.value = []
    activeSubmissionId.value = 0
    seedGradeForm(undefined)
    error.value = e instanceof Error ? e.message : '提交记录加载失败'
  } finally {
    loading.value = false
  }
}

async function submitGrades() {
  const submission = activeSubmission.value
  if (!submission) {
    return
  }
  const grades = gradableAnswers.value.map((answer) => ({
    questionId: answer.questionId,
    score: Number(gradeMap[answer.questionId] ?? 0),
    comment: commentMap[answer.questionId]?.trim() || undefined,
  }))
  if (grades.length === 0) {
    error.value = '当前提交没有待批阅题目'
    return
  }
  loading.value = true
  error.value = ''
  message.value = ''
  try {
    await gradeSubmission(submission.id, { grades })
    message.value = '提交已批阅'
    await loadSubmissions(submission.homeworkId)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '批阅提交失败'
  } finally {
    loading.value = false
  }
}

async function submitQuestion() {
  if (!questionForm.stem.trim()) {
    return
  }
  loading.value = true
  error.value = ''
  message.value = ''
  try {
    await createQuestion({
      courseId: props.courseId,
      type: questionForm.type,
      stem: questionForm.stem.trim(),
      answer: questionForm.answer || undefined,
      analysis: questionForm.analysis || undefined,
      difficulty: questionForm.difficulty || undefined,
      nodeId: questionForm.nodeId || undefined,
      lang: questionForm.lang || undefined,
      options: needsOptions.value
        ? questionForm.options
            .filter((option) => option.optionKey && option.content)
            .map((option) => ({ ...option }))
        : undefined,
    })
    resetQuestion()
    message.value = '题目已创建'
    await loadQuestions()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '题目创建失败'
  } finally {
    loading.value = false
  }
}

async function submitHomework() {
  if (!homeworkForm.title.trim() || selectedQuestionIds.value.length === 0) {
    error.value = '请填写标题并选择题目'
    return
  }
  loading.value = true
  error.value = ''
  message.value = ''
  try {
    await createHomework(props.courseId, {
      title: homeworkForm.title.trim(),
      timeLimit: homeworkForm.timeLimit || undefined,
      deadline: normalizeDeadline(homeworkForm.deadline),
      maxAttempts: homeworkForm.maxAttempts || 1,
      shuffle: homeworkForm.shuffle,
      antiSwitch: homeworkForm.antiSwitch,
      items: selectedQuestionIds.value.map((id, index) => ({
        questionId: id,
        score: scoreMap[id] || 10,
        sortNo: index + 1,
      })),
    })
    homeworkForm.title = ''
    homeworkForm.timeLimit = null
    homeworkForm.deadline = ''
    homeworkForm.maxAttempts = 1
    homeworkForm.shuffle = false
    homeworkForm.antiSwitch = false
    selectedQuestionIds.value = []
    Object.keys(scoreMap).forEach((key) => delete scoreMap[Number(key)])
    message.value = '作业已创建'
    emit('refresh')
  } catch (e) {
    error.value = e instanceof Error ? e.message : '作业创建失败'
  } finally {
    loading.value = false
  }
}

async function publish(id: number) {
  loading.value = true
  error.value = ''
  try {
    await publishHomework(id)
    message.value = '作业已发布'
    emit('refresh')
  } catch (e) {
    error.value = e instanceof Error ? e.message : '发布失败'
  } finally {
    loading.value = false
  }
}

async function removeQuestion(question: QuestionResponse) {
  if (!window.confirm('确认删除这道题目？')) {
    return
  }
  loading.value = true
  error.value = ''
  try {
    await deleteQuestion(question.id)
    selectedQuestionIds.value = selectedQuestionIds.value.filter((id) => id !== question.id)
    delete scoreMap[question.id]
    message.value = '题目已删除'
    await loadQuestions()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '题目删除失败'
  } finally {
    loading.value = false
  }
}

async function close(id: number) {
  loading.value = true
  error.value = ''
  try {
    await closeHomework(id)
    message.value = '作业已关闭'
    emit('refresh')
  } catch (e) {
    error.value = e instanceof Error ? e.message : '关闭失败'
  } finally {
    loading.value = false
  }
}

async function removeHomework(homework: HomeworkResponse) {
  if (!window.confirm(`确认删除作业“${homework.title}”？`)) {
    return
  }
  loading.value = true
  error.value = ''
  try {
    await deleteHomework(homework.id)
    message.value = '作业已删除'
    emit('refresh')
  } catch (e) {
    error.value = e instanceof Error ? e.message : '作业删除失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadQuestions)

watch(
  () => props.homeworks,
  (items) => {
    if (selectedHomeworkId.value && !items.some((item) => item.id === selectedHomeworkId.value)) {
      selectedHomeworkId.value = 0
      submissions.value = []
      activeSubmissionId.value = 0
      seedGradeForm(undefined)
    }
  },
)
</script>

<template>
  <div class="space-y-6">
    <div class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h3 class="text-base font-semibold text-slate-950 dark:text-white">题库与作业</h3>
          <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">当前题库 {{ questions.length }} 题，课程作业 {{ homeworks.length }} 个</p>
        </div>
        <button type="button" class="text-sm font-semibold text-[rgb(var(--color-brand))] hover:underline" @click="loadQuestions">刷新题库</button>
      </div>
      <p v-if="message" class="mt-4 rounded-md bg-emerald-50 px-3 py-2 text-sm text-emerald-700 dark:bg-emerald-400/10 dark:text-emerald-200">
        {{ message }}
      </p>
      <p v-if="error" class="mt-4 rounded-md bg-rose-50 px-3 py-2 text-sm text-rose-700 dark:bg-rose-400/10 dark:text-rose-200">
        {{ error }}
      </p>
    </div>

    <div class="grid gap-6 xl:grid-cols-2">
      <form class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900" @submit.prevent="submitQuestion">
        <h4 class="text-sm font-semibold text-slate-950 dark:text-white">新增题目</h4>
        <div class="mt-4 grid gap-4 sm:grid-cols-2">
          <label>
            <span class="text-sm font-medium text-slate-700 dark:text-slate-200">题型</span>
            <select v-model.number="questionForm.type" class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-slate-900 dark:text-white">
              <option :value="1">单选</option>
              <option :value="2">多选</option>
              <option :value="3">判断</option>
              <option :value="4">填空</option>
              <option :value="5">简答</option>
            </select>
          </label>
          <label>
            <span class="text-sm font-medium text-slate-700 dark:text-slate-200">难度</span>
            <input v-model.number="questionForm.difficulty" type="number" min="1" max="5" class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white" />
          </label>
          <label class="sm:col-span-2">
            <span class="text-sm font-medium text-slate-700 dark:text-slate-200">题干</span>
            <textarea v-model.trim="questionForm.stem" rows="4" class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white" required />
          </label>
          <label>
            <span class="text-sm font-medium text-slate-700 dark:text-slate-200">知识点</span>
            <select v-model.number="questionForm.nodeId" class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-slate-900 dark:text-white">
              <option :value="0">不关联</option>
              <option v-for="node in nodes" :key="node.id" :value="node.id">{{ node.name }}</option>
            </select>
          </label>
          <label>
            <span class="text-sm font-medium text-slate-700 dark:text-slate-200">答案</span>
            <input v-model.trim="questionForm.answer" class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white" />
          </label>
          <div v-if="needsOptions" class="sm:col-span-2 grid gap-3">
            <label v-for="option in questionForm.options" :key="option.optionKey" class="grid gap-2 sm:grid-cols-[72px_minmax(0,1fr)_80px] sm:items-end">
              <span class="text-sm font-medium text-slate-700 dark:text-slate-200">{{ option.optionKey }}</span>
              <input v-model.trim="option.content" class="focus-ring rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white" />
              <label class="flex items-center gap-2 text-sm text-slate-600 dark:text-slate-300">
                <input v-model="option.correct" type="checkbox" />
                正确
              </label>
            </label>
          </div>
          <label class="sm:col-span-2">
            <span class="text-sm font-medium text-slate-700 dark:text-slate-200">解析</span>
            <textarea v-model.trim="questionForm.analysis" rows="3" class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white" />
          </label>
        </div>
        <button type="submit" class="btn-primary focus-ring mt-4 inline-flex h-10 items-center gap-2 px-4 text-sm" :disabled="loading">
          <PlusIcon class="size-4" aria-hidden="true" />
          创建题目
        </button>
      </form>

      <form class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900" @submit.prevent="submitHomework">
        <h4 class="text-sm font-semibold text-slate-950 dark:text-white">创建作业</h4>
        <div class="mt-4 grid gap-4 sm:grid-cols-2">
          <label class="sm:col-span-2">
            <span class="text-sm font-medium text-slate-700 dark:text-slate-200">标题</span>
            <input v-model.trim="homeworkForm.title" class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white" required />
          </label>
          <label>
            <span class="text-sm font-medium text-slate-700 dark:text-slate-200">限时分钟</span>
            <input v-model.number="homeworkForm.timeLimit" type="number" min="0" class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white" />
          </label>
          <label>
            <span class="text-sm font-medium text-slate-700 dark:text-slate-200">截止时间</span>
            <input v-model="homeworkForm.deadline" type="datetime-local" class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white" />
          </label>
          <label>
            <span class="text-sm font-medium text-slate-700 dark:text-slate-200">最大尝试次数</span>
            <input v-model.number="homeworkForm.maxAttempts" type="number" min="1" class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white" />
          </label>
          <div class="flex items-end gap-4">
            <label class="flex items-center gap-2 text-sm text-slate-600 dark:text-slate-300">
              <input v-model="homeworkForm.shuffle" type="checkbox" />
              乱序
            </label>
            <label class="flex items-center gap-2 text-sm text-slate-600 dark:text-slate-300">
              <input v-model="homeworkForm.antiSwitch" type="checkbox" />
              防切屏
            </label>
          </div>
        </div>

        <div class="mt-5">
          <p class="text-sm font-medium text-slate-700 dark:text-slate-200">选择题目</p>
          <EmptyState v-if="questions.length === 0" class="mt-3" title="暂无题目" description="先创建题目再组卷。" />
          <ul v-else class="mt-3 max-h-96 divide-y divide-slate-100 overflow-y-auto rounded-md border border-slate-200 dark:divide-white/10 dark:border-white/10">
            <li v-for="question in questions" :key="question.id" class="p-3">
              <div class="flex items-start gap-3">
                <input :checked="selectedQuestionIds.includes(question.id)" type="checkbox" class="mt-1" @change="toggleQuestion(question.id)" />
                <div class="min-w-0 flex-1">
                  <p class="line-clamp-2 text-sm font-medium text-slate-900 dark:text-white">{{ question.stem }}</p>
                  <p class="mt-1 text-xs text-slate-500 dark:text-slate-400">{{ typeLabel(question.type) }} · 难度 {{ question.difficulty || '-' }}</p>
                </div>
                <input
                  v-if="selectedQuestionIds.includes(question.id)"
                  v-model.number="scoreMap[question.id]"
                  type="number"
                  min="0"
                  step="0.5"
                  class="focus-ring h-8 w-20 rounded-md border border-slate-300 bg-white px-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white"
                />
                <button type="button" class="shrink-0 text-rose-600 hover:underline" :disabled="loading" @click="removeQuestion(question)">
                  <TrashIcon class="size-4" aria-hidden="true" />
                  <span class="sr-only">删除题目</span>
                </button>
              </div>
            </li>
          </ul>
        </div>

        <button type="submit" class="btn-primary focus-ring mt-4 inline-flex h-10 items-center gap-2 px-4 text-sm" :disabled="loading">
          <PlusIcon class="size-4" aria-hidden="true" />
          创建作业
        </button>
      </form>
    </div>

    <section class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900">
      <h4 class="text-sm font-semibold text-slate-950 dark:text-white">作业发布</h4>
      <EmptyState v-if="homeworks.length === 0" class="mt-3" title="暂无作业" description="创建作业后可发布给学生。" />
      <ul v-else class="mt-3 divide-y divide-slate-100 dark:divide-white/10">
        <li v-for="homework in homeworks" :key="homework.id" class="flex items-center justify-between gap-4 py-3">
          <div class="min-w-0">
            <p class="truncate text-sm font-medium text-slate-950 dark:text-white">{{ homework.title }}</p>
            <p class="mt-1 text-xs text-slate-500 dark:text-slate-400">题量 {{ homework.questionCount }} · 总分 {{ Number(homework.totalScore || 0).toFixed(1) }}</p>
          </div>
          <div class="flex shrink-0 items-center gap-2">
            <button
              v-if="homework.status === 0"
              type="button"
              class="btn-primary focus-ring inline-flex h-9 items-center gap-2 px-3 text-sm"
              :disabled="loading"
              @click="publish(homework.id)"
            >
              <RocketLaunchIcon class="size-4" aria-hidden="true" />
              发布
            </button>
            <button
              v-if="homework.status === 1"
              type="button"
              class="focus-ring inline-flex h-9 items-center gap-2 rounded-md border border-slate-200 px-3 text-sm font-semibold text-slate-700 hover:bg-slate-50 dark:border-white/10 dark:text-slate-200 dark:hover:bg-white/10"
              :disabled="loading"
              @click="close(homework.id)"
            >
              <XCircleIcon class="size-4" aria-hidden="true" />
              关闭
            </button>
            <span v-if="homework.status === 2" class="rounded-md bg-slate-100 px-2 py-1 text-xs text-slate-600 dark:bg-white/10 dark:text-slate-300">
              已关闭
            </span>
            <button
              v-if="homework.status !== 0"
              type="button"
              class="focus-ring inline-flex h-9 items-center gap-2 rounded-md border border-slate-200 px-3 text-sm font-semibold text-slate-700 hover:bg-slate-50 dark:border-white/10 dark:text-slate-200 dark:hover:bg-white/10"
              :disabled="loading"
              @click="loadSubmissions(homework.id)"
            >
              <CheckCircleIcon class="size-4" aria-hidden="true" />
              批阅
            </button>
            <button type="button" class="inline-flex items-center gap-1 text-sm font-semibold text-rose-600 hover:underline" :disabled="loading" @click="removeHomework(homework)">
              <TrashIcon class="size-4" aria-hidden="true" />
              删除
            </button>
          </div>
        </li>
      </ul>
    </section>

    <section class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900">
      <div class="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h4 class="text-sm font-semibold text-slate-950 dark:text-white">提交批阅</h4>
          <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
            {{ selectedHomework ? `${selectedHomework.title} · ${submissions.length} 份提交` : '选择已发布或已关闭作业后查看提交。' }}
          </p>
        </div>
        <div class="flex flex-wrap items-center gap-2">
          <select
            v-model.number="selectedHomeworkId"
            class="focus-ring h-9 rounded-md border border-slate-300 bg-white px-3 text-sm text-slate-950 dark:border-white/10 dark:bg-slate-900 dark:text-white"
            @change="loadSubmissions()"
          >
            <option :value="0">选择作业</option>
            <option v-for="homework in homeworks" :key="homework.id" :value="homework.id">
              {{ homework.title }} · {{ homeworkStatusLabel(homework.status) }}
            </option>
          </select>
          <button
            type="button"
            class="focus-ring inline-flex h-9 items-center gap-2 rounded-md border border-slate-200 px-3 text-sm font-semibold text-slate-700 hover:bg-slate-50 dark:border-white/10 dark:text-slate-200 dark:hover:bg-white/10"
            :disabled="loading || !selectedHomeworkId"
            @click="loadSubmissions()"
          >
            <ArrowPathIcon class="size-4" aria-hidden="true" />
            刷新
          </button>
        </div>
      </div>

      <EmptyState v-if="!selectedHomeworkId" class="mt-4" title="未选择作业" description="从上方作业列表进入批阅，或在这里选择一份作业。" />
      <div v-else class="mt-5 grid gap-5 lg:grid-cols-[320px_minmax(0,1fr)]">
        <div>
          <div class="flex items-center justify-between">
            <p class="text-sm font-medium text-slate-700 dark:text-slate-200">提交队列</p>
            <span class="text-xs text-slate-500 dark:text-slate-400">
              待批阅 {{ submissions.filter((item) => item.status === 1).length }}
            </span>
          </div>
          <EmptyState v-if="submissions.length === 0" class="mt-3" title="暂无提交" description="学生提交后会显示在这里。" />
          <ul v-else class="mt-3 divide-y divide-slate-100 rounded-md border border-slate-200 dark:divide-white/10 dark:border-white/10">
            <li v-for="submission in submissions" :key="submission.id">
              <button
                type="button"
                class="focus-ring flex w-full items-center justify-between gap-3 px-3 py-3 text-left hover:bg-slate-50 dark:hover:bg-white/5"
                :class="submission.id === activeSubmissionId ? 'bg-slate-50 dark:bg-white/5' : ''"
                @click="selectSubmission(submission)"
              >
                <span class="min-w-0">
                  <span class="block truncate text-sm font-medium text-slate-950 dark:text-white">
                    学生 #{{ submission.studentId }} · 第 {{ submission.attemptNo }} 次
                  </span>
                  <span class="mt-1 block text-xs text-slate-500 dark:text-slate-400">
                    {{ formatDateTime(submission.submittedAt) }} · 得分 {{ scoreText(submission.totalScore) }}
                  </span>
                </span>
                <span
                  class="shrink-0 rounded-md px-2 py-1 text-xs"
                  :class="submission.status === 1 ? 'bg-amber-50 text-amber-700 dark:bg-amber-400/10 dark:text-amber-200' : 'bg-slate-100 text-slate-600 dark:bg-white/10 dark:text-slate-300'"
                >
                  {{ submissionStatusLabel(submission.status) }}
                </span>
              </button>
            </li>
          </ul>
        </div>

        <div class="min-w-0">
          <EmptyState v-if="!activeSubmission" title="未选择提交" description="从左侧队列选择一份答卷进行查看或批阅。" />
          <div v-else class="space-y-4">
            <div class="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 pb-3 dark:border-white/10">
              <div>
                <p class="text-sm font-semibold text-slate-950 dark:text-white">
                  学生 #{{ activeSubmission.studentId }} · 第 {{ activeSubmission.attemptNo }} 次提交
                </p>
                <p class="mt-1 text-xs text-slate-500 dark:text-slate-400">
                  {{ submissionStatusLabel(activeSubmission.status) }} · 当前得分 {{ scoreText(activeSubmission.totalScore) }} / {{ scoreText(selectedHomework?.totalScore) }}
                </p>
              </div>
              <button
                v-if="gradableAnswers.length > 0"
                type="button"
                class="btn-primary focus-ring inline-flex h-9 items-center gap-2 px-3 text-sm"
                :disabled="loading"
                @click="submitGrades"
              >
                <CheckCircleIcon class="size-4" aria-hidden="true" />
                提交批阅
              </button>
            </div>

            <ul class="divide-y divide-slate-100 rounded-md border border-slate-200 dark:divide-white/10 dark:border-white/10">
              <li v-for="answer in activeSubmission.answers" :key="answer.questionId" class="p-4">
                <div class="flex flex-wrap items-start justify-between gap-3">
                  <div class="min-w-0">
                    <p class="text-sm font-medium text-slate-950 dark:text-white">
                      {{ questionFor(answer.questionId)?.stem || `题目 #${answer.questionId}` }}
                    </p>
                    <p class="mt-1 text-xs text-slate-500 dark:text-slate-400">
                      {{ typeLabel(questionFor(answer.questionId)?.type || 5) }} · 当前得分 {{ scoreText(answer.score) }}
                    </p>
                  </div>
                  <span
                    class="rounded-md px-2 py-1 text-xs"
                    :class="answer.isCorrect === 1 ? 'bg-emerald-50 text-emerald-700 dark:bg-emerald-400/10 dark:text-emerald-200' : answer.isCorrect === 0 ? 'bg-rose-50 text-rose-700 dark:bg-rose-400/10 dark:text-rose-200' : 'bg-amber-50 text-amber-700 dark:bg-amber-400/10 dark:text-amber-200'"
                  >
                    {{ answer.isCorrect === 1 ? '正确' : answer.isCorrect === 0 ? '错误' : '待人工' }}
                  </span>
                </div>
                <p class="mt-3 whitespace-pre-wrap rounded-md bg-slate-50 px-3 py-2 text-sm text-slate-700 dark:bg-white/5 dark:text-slate-200">
                  {{ answer.answer || '未作答' }}
                </p>
                <div v-if="canGradeAnswer(answer)" class="mt-3 grid gap-3 sm:grid-cols-[120px_minmax(0,1fr)]">
                  <label>
                    <span class="text-xs font-medium text-slate-500 dark:text-slate-400">给分</span>
                    <input
                      v-model.number="gradeMap[answer.questionId]"
                      type="number"
                      min="0"
                      step="0.5"
                      class="focus-ring mt-1 block h-9 w-full rounded-md border border-slate-300 bg-white px-3 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white"
                    />
                  </label>
                  <label>
                    <span class="text-xs font-medium text-slate-500 dark:text-slate-400">评语</span>
                    <input
                      v-model.trim="commentMap[answer.questionId]"
                      class="focus-ring mt-1 block h-9 w-full rounded-md border border-slate-300 bg-white px-3 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white"
                    />
                  </label>
                </div>
                <p v-else-if="answer.comment" class="mt-3 text-sm text-slate-500 dark:text-slate-400">
                  评语：{{ answer.comment }}
                </p>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>
