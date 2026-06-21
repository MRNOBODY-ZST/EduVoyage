<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import {
  AcademicCapIcon,
  ArrowPathIcon,
  BookOpenIcon,
  CheckCircleIcon,
  ClockIcon,
  DocumentTextIcon,
  ExclamationCircleIcon,
  LinkIcon,
  MapIcon,
  PencilSquareIcon,
} from '@heroicons/vue/24/outline'

import KnowledgeGraphCanvas from '@/components/charts/KnowledgeGraphCanvas.vue'
import StatCard from '@/components/data/StatCard.vue'
import EmptyState from '@/components/state/EmptyState.vue'
import ErrorState from '@/components/state/ErrorState.vue'
import LoadingState from '@/components/state/LoadingState.vue'
import { courseStatusLabel, formatDateTime, formatDuration, formatNumber, formatPercent } from '@/lib/format'
import {
  createChapter,
  createCourseware,
  createGraphEdge,
  createKnowledgeNode,
  enrollCourse,
  fetchChapters,
  fetchCourse,
  fetchCoursewares,
  fetchGraph,
  fetchHomeworks,
  fetchKnowledgeNodes,
  fetchLearningPath,
  fetchWrongBook,
} from '@/lib/services'
import { useAuthStore } from '@/stores/auth'
import type {
  ChapterNode,
  CourseResponse,
  CoursewareResponse,
  GraphView,
  HomeworkResponse,
  KnowledgeNodeResponse,
  LearningPath,
  WrongBookEntry,
} from '@/types/api'

type TabKey = 'graph' | 'path' | 'materials' | 'homeworks' | 'wrong-book' | 'authoring'

interface FlatChapter extends ChapterNode {
  level: number
}

const route = useRoute()
const auth = useAuthStore()
const loading = ref(true)
const nodeLoading = ref(false)
const enrolling = ref(false)
const error = ref('')
const activeTab = ref<TabKey>('graph')

const course = ref<CourseResponse | null>(null)
const chapters = ref<ChapterNode[]>([])
const nodes = ref<KnowledgeNodeResponse[]>([])
const graph = ref<GraphView | null>(null)
const learningPath = ref<LearningPath | null>(null)
const homeworks = ref<HomeworkResponse[]>([])
const wrongBook = ref<WrongBookEntry[]>([])
const coursewares = ref<CoursewareResponse[]>([])
const selectedNodeId = ref<number | null>(null)
const authoringBusy = ref(false)
const authoringMessage = ref('')
const authoringError = ref('')

const chapterForm = reactive({
  title: '',
  parentId: 0,
  sortNo: 0,
})

const nodeForm = reactive({
  name: '',
  chapterId: 0,
  description: '',
  learnGoal: '',
  estMinutes: 30,
})

const edgeForm = reactive({
  fromId: 0,
  toId: 0,
  type: 'PREREQUISITE' as 'PREREQUISITE' | 'RELATED',
  weight: 1,
})

const coursewareForm = reactive({
  title: '',
  type: 4,
  contentRef: '',
  fileId: null as number | null,
  durationSec: null as number | null,
  sortNo: 0,
})

const courseId = computed(() => Number(route.params.courseId))

const flatChapters = computed(() => flatten(chapters.value))
const selectedNode = computed(() => nodes.value.find((node) => node.id === selectedNodeId.value) || null)
const masteredPercent = computed(() => {
  if (!learningPath.value?.totalCount) {
    return 0
  }
  return Math.round((learningPath.value.masteredCount / learningPath.value.totalCount) * 100)
})

const stats = computed(() => [
  { label: '章节', value: formatNumber(flatChapters.value.length), hint: '课程章节树', icon: BookOpenIcon },
  { label: '知识点', value: formatNumber(nodes.value.length), hint: '图谱节点数', icon: MapIcon },
  { label: '可学节点', value: formatNumber(learningPath.value?.learnable.length), hint: '前置已满足', icon: CheckCircleIcon },
  { label: '作业', value: formatNumber(homeworks.value.length), hint: '课程作业数', icon: DocumentTextIcon },
])

const tabs = computed(() => [
  { key: 'graph' as const, label: '知识图谱', icon: MapIcon, visible: auth.hasPermission('graph:read') },
  { key: 'path' as const, label: '学习路径', icon: CheckCircleIcon, visible: auth.hasPermission('graph:read') },
  { key: 'materials' as const, label: '课件资源', icon: LinkIcon, visible: true },
  { key: 'homeworks' as const, label: '课程作业', icon: DocumentTextIcon, visible: auth.hasPermission('homework:read') },
  { key: 'wrong-book' as const, label: '错题本', icon: ExclamationCircleIcon, visible: auth.hasPermission('homework:submit') },
  { key: 'authoring' as const, label: '内容维护', icon: PencilSquareIcon, visible: auth.hasPermission('course:update') },
])

function flatten(items: ChapterNode[], level = 0): FlatChapter[] {
  return items.flatMap((item) => [{ ...item, level }, ...flatten(item.children || [], level + 1)])
}

function nodesForChapter(chapterId: number) {
  return nodes.value.filter((node) => node.chapterId === chapterId)
}

function ungroupedNodes() {
  const chapterIds = new Set(flatChapters.value.map((chapter) => chapter.id))
  return nodes.value.filter((node) => !node.chapterId || !chapterIds.has(node.chapterId))
}

function selectNode(id: number, tab: TabKey = activeTab.value) {
  selectedNodeId.value = id
  activeTab.value = tab
}

function coursewareTypeLabel(type: number) {
  if (type === 1) return '视频'
  if (type === 2) return '文档'
  if (type === 3) return '图文'
  return '链接'
}

function homeworkStatusLabel(status: number) {
  if (status === 1) return '已发布'
  if (status === 2) return '已关闭'
  return '草稿'
}

async function load() {
  if (!Number.isFinite(courseId.value)) {
    error.value = '课程 ID 不正确'
    loading.value = false
    return
  }
  loading.value = true
  error.value = ''
  try {
    const [courseInfo, chapterList, nodeList, graphInfo, pathInfo, homeworkList, wrongBookList] = await Promise.all([
      fetchCourse(courseId.value),
      fetchChapters(courseId.value).catch(() => []),
      fetchKnowledgeNodes(courseId.value).catch(() => []),
      auth.hasPermission('graph:read') ? fetchGraph(courseId.value).catch(() => null) : Promise.resolve(null),
      auth.hasPermission('graph:read') ? fetchLearningPath(courseId.value).catch(() => null) : Promise.resolve(null),
      auth.hasPermission('homework:read') ? fetchHomeworks(courseId.value).catch(() => []) : Promise.resolve([]),
      auth.hasPermission('homework:submit') ? fetchWrongBook(true).catch(() => []) : Promise.resolve([]),
    ])
    course.value = courseInfo
    chapters.value = chapterList
    nodes.value = nodeList
    graph.value = graphInfo
    learningPath.value = pathInfo
    homeworks.value = homeworkList
    wrongBook.value = wrongBookList
    selectedNodeId.value = selectedNodeId.value || nodeList[0]?.id || graphInfo?.nodes[0]?.id || null
  } catch (e) {
    error.value = e instanceof Error ? e.message : '课程详情加载失败'
  } finally {
    loading.value = false
  }
}

async function loadCoursewares(nodeId: number | null) {
  coursewares.value = []
  if (!nodeId) {
    return
  }
  nodeLoading.value = true
  try {
    coursewares.value = await fetchCoursewares(nodeId).catch(() => [])
  } finally {
    nodeLoading.value = false
  }
}

async function enroll() {
  if (!course.value) {
    return
  }
  enrolling.value = true
  try {
    await enrollCourse(course.value.id)
    course.value = { ...course.value, enrolled: true }
  } catch (e) {
    error.value = e instanceof Error ? e.message : '选课失败'
  } finally {
    enrolling.value = false
  }
}

async function runAuthoring(action: () => Promise<void>, success: string) {
  authoringBusy.value = true
  authoringError.value = ''
  authoringMessage.value = ''
  try {
    await action()
    authoringMessage.value = success
  } catch (e) {
    authoringError.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    authoringBusy.value = false
  }
}

async function submitChapter() {
  if (!chapterForm.title.trim()) return
  await runAuthoring(async () => {
    await createChapter(courseId.value, {
      title: chapterForm.title.trim(),
      parentId: chapterForm.parentId || undefined,
      sortNo: chapterForm.sortNo || undefined,
    })
    chapterForm.title = ''
    chapterForm.parentId = 0
    chapterForm.sortNo = 0
    await load()
  }, '章节已创建')
}

async function submitNode() {
  if (!nodeForm.name.trim()) return
  await runAuthoring(async () => {
    const created = await createKnowledgeNode(courseId.value, {
      name: nodeForm.name.trim(),
      chapterId: nodeForm.chapterId || undefined,
      description: nodeForm.description || undefined,
      learnGoal: nodeForm.learnGoal || undefined,
      estMinutes: nodeForm.estMinutes || undefined,
    })
    selectedNodeId.value = created.id
    nodeForm.name = ''
    nodeForm.description = ''
    nodeForm.learnGoal = ''
    nodeForm.estMinutes = 30
    await load()
  }, '知识点已创建')
}

async function submitEdge() {
  if (!edgeForm.fromId || !edgeForm.toId || edgeForm.fromId === edgeForm.toId) {
    authoringError.value = '请选择两个不同的知识点'
    return
  }
  await runAuthoring(async () => {
    await createGraphEdge(courseId.value, {
      fromId: edgeForm.fromId,
      toId: edgeForm.toId,
      type: edgeForm.type,
      weight: edgeForm.weight || undefined,
    })
    edgeForm.fromId = 0
    edgeForm.toId = 0
    edgeForm.type = 'PREREQUISITE'
    edgeForm.weight = 1
    await load()
  }, '图谱关系已创建')
}

async function submitCourseware() {
  if (!selectedNodeId.value || !coursewareForm.title.trim()) return
  await runAuthoring(async () => {
    await createCourseware(selectedNodeId.value!, {
      title: coursewareForm.title.trim(),
      type: coursewareForm.type,
      contentRef: coursewareForm.contentRef || undefined,
      fileId: coursewareForm.fileId || undefined,
      durationSec: coursewareForm.durationSec || undefined,
      sortNo: coursewareForm.sortNo || undefined,
    })
    coursewareForm.title = ''
    coursewareForm.contentRef = ''
    coursewareForm.fileId = null
    coursewareForm.durationSec = null
    coursewareForm.sortNo = 0
    await loadCoursewares(selectedNodeId.value)
  }, '课件已创建')
}

watch(selectedNodeId, loadCoursewares)
onMounted(load)
</script>

<template>
  <LoadingState v-if="loading" />
  <ErrorState v-else-if="error && !course" :message="error" @retry="load" />
  <EmptyState v-else-if="!course" title="课程不存在" description="请回到课程中心重新选择课程。" />
  <div v-else class="space-y-6">
    <section class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900">
      <div class="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
        <div class="min-w-0">
          <div class="flex flex-wrap items-center gap-2">
            <span class="rounded-md bg-[rgb(var(--color-brand-soft))] px-2 py-1 text-xs font-medium text-[rgb(var(--color-brand-strong))] dark:text-white">
              {{ courseStatusLabel(course.status) }}
            </span>
            <span class="text-xs text-slate-500 dark:text-slate-400">教师 {{ course.teacherId }} · {{ course.credit ?? 0 }} 学分</span>
          </div>
          <h2 class="mt-3 text-xl font-semibold text-slate-950 dark:text-white">{{ course.title }}</h2>
          <p class="mt-2 max-w-3xl text-sm leading-6 text-slate-600 dark:text-slate-300">{{ course.intro || '暂无课程简介' }}</p>
        </div>
        <button
          v-if="auth.hasPermission('course:enroll')"
          type="button"
          class="btn-primary focus-ring inline-flex h-10 shrink-0 items-center justify-center px-4 text-sm disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="course.enrolled || enrolling"
          @click="enroll"
        >
          {{ course.enrolled ? '已选课' : enrolling ? '处理中' : '加入课程' }}
        </button>
      </div>
    </section>

    <section class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <StatCard v-for="item in stats" :key="item.label" v-bind="item" />
    </section>

    <ErrorState v-if="error" :message="error" @retry="load" />

    <section class="grid gap-6 xl:grid-cols-[320px_minmax(0,1fr)]">
      <aside class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900">
        <div class="flex items-center justify-between gap-3">
          <h2 class="text-base font-semibold text-slate-950 dark:text-white">课程结构</h2>
          <button type="button" class="text-slate-400 hover:text-[rgb(var(--color-brand))]" title="刷新" @click="load">
            <ArrowPathIcon class="size-5" aria-hidden="true" />
            <span class="sr-only">刷新</span>
          </button>
        </div>

        <div v-if="flatChapters.length" class="mt-4 space-y-4">
          <div v-for="chapter in flatChapters" :key="chapter.id">
            <div class="flex items-center gap-2 text-sm font-semibold text-slate-800 dark:text-slate-100" :style="{ marginLeft: `${chapter.level * 14}px` }">
              <BookOpenIcon class="size-4 text-slate-400" aria-hidden="true" />
              <span class="truncate">{{ chapter.title }}</span>
            </div>
            <div class="mt-2 space-y-1" :style="{ marginLeft: `${chapter.level * 14 + 18}px` }">
              <button
                v-for="node in nodesForChapter(chapter.id)"
                :key="node.id"
                type="button"
                :class="[
                  selectedNodeId === node.id
                    ? 'bg-[rgb(var(--color-brand-soft))] text-[rgb(var(--color-brand-strong))] dark:text-white'
                    : 'text-slate-600 hover:bg-slate-50 hover:text-[rgb(var(--color-brand))] dark:text-slate-300 dark:hover:bg-white/5',
                  'block w-full truncate rounded-md px-2 py-1.5 text-left text-sm',
                ]"
                @click="selectNode(node.id, 'materials')"
              >
                {{ node.name }}
              </button>
            </div>
          </div>
        </div>

        <div v-if="ungroupedNodes().length" class="mt-5 border-t border-slate-100 pt-4 dark:border-white/10">
          <p class="text-xs font-semibold text-slate-400">未归入章节</p>
          <div class="mt-2 space-y-1">
            <button
              v-for="node in ungroupedNodes()"
              :key="node.id"
              type="button"
              :class="[
                selectedNodeId === node.id
                  ? 'bg-[rgb(var(--color-brand-soft))] text-[rgb(var(--color-brand-strong))] dark:text-white'
                  : 'text-slate-600 hover:bg-slate-50 hover:text-[rgb(var(--color-brand))] dark:text-slate-300 dark:hover:bg-white/5',
                'block w-full truncate rounded-md px-2 py-1.5 text-left text-sm',
              ]"
              @click="selectNode(node.id, 'materials')"
            >
              {{ node.name }}
            </button>
          </div>
        </div>

        <EmptyState v-if="!flatChapters.length && !nodes.length" class="mt-4" title="暂无课程结构" description="课程章节和知识点创建后会显示在这里。" />
      </aside>

      <div class="space-y-6">
        <div class="rounded-md border border-slate-200 bg-white p-2 shadow-sm dark:border-white/10 dark:bg-slate-900">
          <div class="flex flex-wrap gap-2">
            <button
              v-for="tab in tabs.filter((item) => item.visible)"
              :key="tab.key"
              type="button"
              :class="[
                activeTab === tab.key
                  ? 'bg-[rgb(var(--color-brand))] text-white'
                  : 'text-slate-600 hover:bg-slate-50 dark:text-slate-300 dark:hover:bg-white/5',
                'focus-ring inline-flex h-9 items-center gap-2 rounded-md px-3 text-sm font-semibold',
              ]"
              @click="activeTab = tab.key"
            >
              <component :is="tab.icon" class="size-4" aria-hidden="true" />
              {{ tab.label }}
            </button>
          </div>
        </div>

        <section v-if="activeTab === 'graph'" class="space-y-6">
          <KnowledgeGraphCanvas v-if="graph && graph.nodes.length" :graph="graph" :selected-node-id="selectedNodeId" @select-node="selectNode($event, 'materials')" />
          <EmptyState v-else title="暂无知识图谱" description="课程知识点和关系建立后会生成图谱。" :icon="MapIcon" />

          <div v-if="selectedNode" class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900">
            <div class="flex flex-wrap items-start justify-between gap-4">
              <div>
                <h3 class="text-base font-semibold text-slate-950 dark:text-white">{{ selectedNode.name }}</h3>
                <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">预计 {{ selectedNode.estMinutes ?? 0 }} 分钟</p>
              </div>
              <span class="rounded-md bg-slate-100 px-2 py-1 text-xs text-slate-600 dark:bg-white/10 dark:text-slate-300">
                节点 {{ selectedNode.id }}
              </span>
            </div>
            <p class="mt-4 text-sm leading-6 text-slate-600 dark:text-slate-300">{{ selectedNode.description || '暂无知识点描述' }}</p>
            <p v-if="selectedNode.learnGoal" class="mt-3 text-sm text-slate-700 dark:text-slate-200">学习目标：{{ selectedNode.learnGoal }}</p>
          </div>
        </section>

        <section v-else-if="activeTab === 'path'" class="grid gap-6 xl:grid-cols-[minmax(0,0.85fr)_minmax(0,1.15fr)]">
          <div class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900">
            <h3 class="text-base font-semibold text-slate-950 dark:text-white">掌握进度</h3>
            <div class="mt-5">
              <div class="flex items-center justify-between text-sm">
                <span class="text-slate-500 dark:text-slate-400">{{ learningPath?.masteredCount ?? 0 }} / {{ learningPath?.totalCount ?? 0 }}</span>
                <span class="font-semibold text-slate-950 dark:text-white">{{ formatPercent(masteredPercent) }}</span>
              </div>
              <div class="mt-2 h-2 rounded-full bg-slate-100 dark:bg-white/10">
                <div class="h-2 rounded-full bg-[rgb(var(--color-brand))]" :style="{ width: `${masteredPercent}%` }" />
              </div>
            </div>
            <h4 class="mt-6 text-sm font-semibold text-slate-900 dark:text-white">当前可学</h4>
            <ul v-if="learningPath?.learnable.length" class="mt-3 space-y-2">
              <li v-for="node in learningPath.learnable" :key="node.id">
                <button type="button" class="w-full rounded-md bg-slate-50 px-3 py-2 text-left text-sm text-slate-700 hover:text-[rgb(var(--color-brand))] dark:bg-white/5 dark:text-slate-200" @click="selectNode(node.id, 'materials')">
                  {{ node.name }}
                </button>
              </li>
            </ul>
            <EmptyState v-else class="mt-3" title="暂无可学节点" description="可能已完成或缺少前置关系。" />
          </div>

          <div class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900">
            <h3 class="text-base font-semibold text-slate-950 dark:text-white">推荐顺序</h3>
            <ol v-if="learningPath?.recommended.length" class="mt-4 divide-y divide-slate-100 dark:divide-white/10">
              <li v-for="(node, index) in learningPath.recommended" :key="node.id" class="flex items-center gap-3 py-3">
                <span class="grid size-7 shrink-0 place-items-center rounded-md bg-[rgb(var(--color-brand-soft))] text-xs font-semibold text-[rgb(var(--color-brand-strong))] dark:text-white">
                  {{ index + 1 }}
                </span>
                <button type="button" class="min-w-0 flex-1 truncate text-left text-sm font-medium text-slate-900 hover:text-[rgb(var(--color-brand))] dark:text-white" @click="selectNode(node.id, 'materials')">
                  {{ node.name }}
                </button>
                <span class="text-xs text-slate-500 dark:text-slate-400">{{ node.estMinutes ?? 0 }} 分钟</span>
              </li>
            </ol>
            <EmptyState v-else class="mt-4" title="暂无推荐路径" description="图谱建模后会生成拓扑学习顺序。" />
          </div>
        </section>

        <section v-else-if="activeTab === 'materials'" class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900">
          <div class="flex flex-wrap items-start justify-between gap-4">
            <div>
              <h3 class="text-base font-semibold text-slate-950 dark:text-white">{{ selectedNode?.name || '选择知识点' }}</h3>
              <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">课件资源与知识点绑定</p>
            </div>
            <span v-if="selectedNode" class="inline-flex items-center gap-1 rounded-md bg-slate-100 px-2 py-1 text-xs text-slate-600 dark:bg-white/10 dark:text-slate-300">
              <ClockIcon class="size-3.5" aria-hidden="true" />
              {{ selectedNode.estMinutes ?? 0 }} 分钟
            </span>
          </div>
          <LoadingState v-if="nodeLoading" class="mt-4" />
          <EmptyState v-else-if="!selectedNode" class="mt-4" title="未选择知识点" description="从左侧课程结构或图谱中选择知识点。" />
          <EmptyState v-else-if="coursewares.length === 0" class="mt-4" title="暂无课件" description="教师上传课件后会显示在这里。" />
          <ul v-else class="mt-4 divide-y divide-slate-100 dark:divide-white/10">
            <li v-for="item in coursewares" :key="item.id" class="flex items-center justify-between gap-4 py-3">
              <div class="min-w-0">
                <p class="truncate text-sm font-medium text-slate-950 dark:text-white">{{ item.title }}</p>
                <p class="mt-1 text-xs text-slate-500 dark:text-slate-400">
                  {{ coursewareTypeLabel(item.type) }} · {{ item.durationSec ? formatDuration(item.durationSec) : formatDateTime(item.createdAt) }}
                </p>
              </div>
              <a v-if="item.contentRef && item.type === 4" :href="item.contentRef" class="text-sm font-semibold text-[rgb(var(--color-brand))] hover:underline" target="_blank" rel="noreferrer">
                打开
              </a>
            </li>
          </ul>
        </section>

        <section v-else-if="activeTab === 'homeworks'" class="overflow-hidden rounded-md border border-slate-200 bg-white shadow-sm dark:border-white/10 dark:bg-slate-900">
          <div class="border-b border-slate-200 px-5 py-4 dark:border-white/10">
            <h3 class="text-base font-semibold text-slate-950 dark:text-white">课程作业</h3>
          </div>
          <EmptyState v-if="homeworks.length === 0" class="m-5" title="暂无作业" description="发布作业后会显示在这里。" />
          <div v-else class="overflow-x-auto">
            <table class="min-w-full divide-y divide-slate-200 dark:divide-white/10">
              <thead class="bg-slate-50 dark:bg-white/5">
                <tr>
                  <th class="px-5 py-3 text-left text-sm font-semibold text-slate-900 dark:text-white">标题</th>
                  <th class="px-5 py-3 text-left text-sm font-semibold text-slate-900 dark:text-white">总分</th>
                  <th class="px-5 py-3 text-left text-sm font-semibold text-slate-900 dark:text-white">题量</th>
                  <th class="px-5 py-3 text-left text-sm font-semibold text-slate-900 dark:text-white">截止</th>
                  <th class="px-5 py-3 text-left text-sm font-semibold text-slate-900 dark:text-white">状态</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100 dark:divide-white/10">
                <tr v-for="item in homeworks" :key="item.id">
                  <td class="px-5 py-4 text-sm font-medium text-slate-950 dark:text-white">{{ item.title }}</td>
                  <td class="px-5 py-4 text-sm text-slate-500 dark:text-slate-400">{{ Number(item.totalScore || 0).toFixed(1) }}</td>
                  <td class="px-5 py-4 text-sm text-slate-500 dark:text-slate-400">{{ item.questionCount }}</td>
                  <td class="px-5 py-4 text-sm text-slate-500 dark:text-slate-400">{{ formatDateTime(item.deadline) }}</td>
                  <td class="px-5 py-4">
                    <span class="rounded-md bg-slate-100 px-2 py-1 text-xs font-medium text-slate-700 dark:bg-white/10 dark:text-slate-200">
                      {{ homeworkStatusLabel(item.status) }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section v-else-if="activeTab === 'wrong-book'" class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900">
          <h3 class="text-base font-semibold text-slate-950 dark:text-white">未掌握错题</h3>
          <EmptyState v-if="wrongBook.length === 0" class="mt-4" title="暂无错题" description="作业批改后错题会自动进入错题本。" />
          <ul v-else class="mt-4 divide-y divide-slate-100 dark:divide-white/10">
            <li v-for="item in wrongBook" :key="item.id" class="flex items-center justify-between gap-4 py-3">
              <div class="min-w-0">
                <p class="text-sm font-medium text-slate-950 dark:text-white">题目 {{ item.questionId }}</p>
                <p class="mt-1 text-xs text-slate-500 dark:text-slate-400">知识点 {{ item.nodeId || '-' }} · 最近 {{ formatDateTime(item.lastWrongAt) }}</p>
              </div>
              <span class="rounded-md bg-rose-50 px-2 py-1 text-xs font-medium text-rose-700 dark:bg-rose-400/10 dark:text-rose-200">
                错 {{ item.wrongCount }} 次
              </span>
            </li>
          </ul>
        </section>

        <section v-else-if="activeTab === 'authoring'" class="space-y-6">
          <div class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900">
            <div class="flex flex-wrap items-center justify-between gap-3">
              <div>
                <h3 class="text-base font-semibold text-slate-950 dark:text-white">内容维护</h3>
                <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">章节、知识点、图谱边和课件的轻量维护入口</p>
              </div>
              <span v-if="selectedNode" class="rounded-md bg-slate-100 px-2 py-1 text-xs text-slate-600 dark:bg-white/10 dark:text-slate-300">
                当前节点：{{ selectedNode.name }}
              </span>
            </div>
            <p v-if="authoringMessage" class="mt-4 rounded-md bg-emerald-50 px-3 py-2 text-sm text-emerald-700 dark:bg-emerald-400/10 dark:text-emerald-200">
              {{ authoringMessage }}
            </p>
            <p v-if="authoringError" class="mt-4 rounded-md bg-rose-50 px-3 py-2 text-sm text-rose-700 dark:bg-rose-400/10 dark:text-rose-200">
              {{ authoringError }}
            </p>
          </div>

          <div class="grid gap-6 xl:grid-cols-2">
            <form class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900" @submit.prevent="submitChapter">
              <h4 class="text-sm font-semibold text-slate-950 dark:text-white">新增章节</h4>
              <div class="mt-4 grid gap-4 sm:grid-cols-2">
                <label class="sm:col-span-2">
                  <span class="text-sm font-medium text-slate-700 dark:text-slate-200">标题</span>
                  <input v-model.trim="chapterForm.title" class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white" required />
                </label>
                <label>
                  <span class="text-sm font-medium text-slate-700 dark:text-slate-200">父章节</span>
                  <select v-model.number="chapterForm.parentId" class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-slate-900 dark:text-white">
                    <option :value="0">顶级章节</option>
                    <option v-for="chapter in flatChapters" :key="chapter.id" :value="chapter.id">{{ '　'.repeat(chapter.level) }}{{ chapter.title }}</option>
                  </select>
                </label>
                <label>
                  <span class="text-sm font-medium text-slate-700 dark:text-slate-200">排序</span>
                  <input v-model.number="chapterForm.sortNo" type="number" class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white" />
                </label>
              </div>
              <button type="submit" class="btn-primary focus-ring mt-4 inline-flex h-10 items-center px-4 text-sm" :disabled="authoringBusy">创建章节</button>
            </form>

            <form class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900" @submit.prevent="submitNode">
              <h4 class="text-sm font-semibold text-slate-950 dark:text-white">新增知识点</h4>
              <div class="mt-4 grid gap-4 sm:grid-cols-2">
                <label class="sm:col-span-2">
                  <span class="text-sm font-medium text-slate-700 dark:text-slate-200">名称</span>
                  <input v-model.trim="nodeForm.name" class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white" required />
                </label>
                <label>
                  <span class="text-sm font-medium text-slate-700 dark:text-slate-200">章节</span>
                  <select v-model.number="nodeForm.chapterId" class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-slate-900 dark:text-white">
                    <option :value="0">不归入章节</option>
                    <option v-for="chapter in flatChapters" :key="chapter.id" :value="chapter.id">{{ '　'.repeat(chapter.level) }}{{ chapter.title }}</option>
                  </select>
                </label>
                <label>
                  <span class="text-sm font-medium text-slate-700 dark:text-slate-200">预计分钟</span>
                  <input v-model.number="nodeForm.estMinutes" type="number" min="0" class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white" />
                </label>
                <label class="sm:col-span-2">
                  <span class="text-sm font-medium text-slate-700 dark:text-slate-200">学习目标</span>
                  <input v-model.trim="nodeForm.learnGoal" class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white" />
                </label>
                <label class="sm:col-span-2">
                  <span class="text-sm font-medium text-slate-700 dark:text-slate-200">描述</span>
                  <textarea v-model.trim="nodeForm.description" rows="3" class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white" />
                </label>
              </div>
              <button type="submit" class="btn-primary focus-ring mt-4 inline-flex h-10 items-center px-4 text-sm" :disabled="authoringBusy">创建知识点</button>
            </form>

            <form class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900" @submit.prevent="submitEdge">
              <h4 class="text-sm font-semibold text-slate-950 dark:text-white">新增图谱关系</h4>
              <div class="mt-4 grid gap-4 sm:grid-cols-2">
                <label>
                  <span class="text-sm font-medium text-slate-700 dark:text-slate-200">起点</span>
                  <select v-model.number="edgeForm.fromId" class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-slate-900 dark:text-white" required>
                    <option :value="0">请选择</option>
                    <option v-for="node in nodes" :key="node.id" :value="node.id">{{ node.name }}</option>
                  </select>
                </label>
                <label>
                  <span class="text-sm font-medium text-slate-700 dark:text-slate-200">终点</span>
                  <select v-model.number="edgeForm.toId" class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-slate-900 dark:text-white" required>
                    <option :value="0">请选择</option>
                    <option v-for="node in nodes" :key="node.id" :value="node.id">{{ node.name }}</option>
                  </select>
                </label>
                <label>
                  <span class="text-sm font-medium text-slate-700 dark:text-slate-200">类型</span>
                  <select v-model="edgeForm.type" class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-slate-900 dark:text-white">
                    <option value="PREREQUISITE">前置</option>
                    <option value="RELATED">关联</option>
                  </select>
                </label>
                <label>
                  <span class="text-sm font-medium text-slate-700 dark:text-slate-200">权重</span>
                  <input v-model.number="edgeForm.weight" type="number" min="0" step="0.1" class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white" />
                </label>
              </div>
              <button type="submit" class="btn-primary focus-ring mt-4 inline-flex h-10 items-center px-4 text-sm" :disabled="authoringBusy || nodes.length < 2">创建关系</button>
            </form>

            <form class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900" @submit.prevent="submitCourseware">
              <h4 class="text-sm font-semibold text-slate-950 dark:text-white">新增课件</h4>
              <div class="mt-4 grid gap-4 sm:grid-cols-2">
                <label class="sm:col-span-2">
                  <span class="text-sm font-medium text-slate-700 dark:text-slate-200">标题</span>
                  <input v-model.trim="coursewareForm.title" class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white" required />
                </label>
                <label>
                  <span class="text-sm font-medium text-slate-700 dark:text-slate-200">类型</span>
                  <select v-model.number="coursewareForm.type" class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-slate-900 dark:text-white">
                    <option :value="1">视频</option>
                    <option :value="2">文档</option>
                    <option :value="3">图文</option>
                    <option :value="4">链接</option>
                  </select>
                </label>
                <label>
                  <span class="text-sm font-medium text-slate-700 dark:text-slate-200">排序</span>
                  <input v-model.number="coursewareForm.sortNo" type="number" class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white" />
                </label>
                <label>
                  <span class="text-sm font-medium text-slate-700 dark:text-slate-200">文件 ID</span>
                  <input v-model.number="coursewareForm.fileId" type="number" min="1" class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white" />
                </label>
                <label>
                  <span class="text-sm font-medium text-slate-700 dark:text-slate-200">时长秒</span>
                  <input v-model.number="coursewareForm.durationSec" type="number" min="0" class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white" />
                </label>
                <label class="sm:col-span-2">
                  <span class="text-sm font-medium text-slate-700 dark:text-slate-200">内容引用</span>
                  <input v-model.trim="coursewareForm.contentRef" class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white" placeholder="链接 URL 或图文内容引用" />
                </label>
              </div>
              <button type="submit" class="btn-primary focus-ring mt-4 inline-flex h-10 items-center px-4 text-sm" :disabled="authoringBusy || !selectedNodeId">创建课件</button>
            </form>
          </div>
        </section>

      </div>
    </section>
  </div>
</template>
