<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ChartBarIcon, ClockIcon, PresentationChartLineIcon, UsersIcon } from '@heroicons/vue/24/outline'

import EChart from '@/components/charts/EChart.vue'
import StatCard from '@/components/data/StatCard.vue'
import EmptyState from '@/components/state/EmptyState.vue'
import ErrorState from '@/components/state/ErrorState.vue'
import LoadingState from '@/components/state/LoadingState.vue'
import { formatDuration, formatNumber, formatPercent } from '@/lib/format'
import { fetchCourseAnalytics, fetchCourses } from '@/lib/services'
import { useAuthStore } from '@/stores/auth'
import type { CourseAnalyticsResponse, CourseResponse } from '@/types/api'

const auth = useAuthStore()
const courses = ref<CourseResponse[]>([])
const selectedCourseId = ref<number | null>(null)
const loadingCourses = ref(true)
const loadingAnalytics = ref(false)
const error = ref('')
const analytics = ref<CourseAnalyticsResponse | null>(null)

const stats = computed(() => [
  { label: '选课人数', value: formatNumber(analytics.value?.enrolledCount), hint: '当前课程学生数', icon: UsersIcon },
  { label: '活跃学习者', value: formatNumber(analytics.value?.activeLearners), hint: '有学习行为记录', icon: PresentationChartLineIcon },
  { label: '累计学习', value: formatDuration(analytics.value?.totalDurationSec), hint: '课程维度累计时长', icon: ClockIcon },
  { label: '提交率', value: formatPercent(analytics.value?.submissionRate), hint: '作业平均提交率', icon: ChartBarIcon },
])

const masteryOption = computed(() => {
  const rows = analytics.value?.masteryHeatmap || []
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 36, right: 16, top: 16, bottom: 44 },
    xAxis: { type: 'category', data: rows.map((row) => row.nodeName), axisLabel: { color: '#64748b', rotate: rows.length > 4 ? 18 : 0 } },
    yAxis: { type: 'value', max: 100, axisLabel: { color: '#64748b' }, splitLine: { lineStyle: { color: '#e2e8f0' } } },
    series: [
      {
        type: 'bar',
        data: rows.map((row) => Number(row.masteryRate || 0)),
        itemStyle: { color: '#0d9488', borderRadius: [4, 4, 0, 0] },
      },
    ],
  }
})

async function loadCourses() {
  loadingCourses.value = true
  error.value = ''
  try {
    const page = await fetchCourses({ teacherId: auth.user?.id, pageNo: 1, pageSize: 50 })
    courses.value = page.records
    selectedCourseId.value = selectedCourseId.value || page.records[0]?.id || null
  } catch (e) {
    error.value = e instanceof Error ? e.message : '课程加载失败'
  } finally {
    loadingCourses.value = false
  }
}

async function loadAnalytics(courseId: number | null) {
  if (!courseId) {
    analytics.value = null
    return
  }
  loadingAnalytics.value = true
  error.value = ''
  try {
    analytics.value = await fetchCourseAnalytics(courseId)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '教学分析加载失败'
  } finally {
    loadingAnalytics.value = false
  }
}

watch(selectedCourseId, loadAnalytics)
onMounted(loadCourses)
</script>

<template>
  <LoadingState v-if="loadingCourses" />
  <ErrorState v-else-if="error && !analytics" :message="error" @retry="loadCourses" />
  <EmptyState v-else-if="courses.length === 0" title="暂无可分析课程" description="创建并发布课程后可查看教学分析。" />
  <div v-else class="space-y-6">
    <section class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900">
      <label class="block max-w-sm">
        <span class="text-sm font-medium text-slate-700 dark:text-slate-200">选择课程</span>
        <select
          v-model.number="selectedCourseId"
          class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-slate-900 dark:text-white"
        >
          <option v-for="course in courses" :key="course.id" :value="course.id">{{ course.title }}</option>
        </select>
      </label>
    </section>

    <LoadingState v-if="loadingAnalytics" />
    <ErrorState v-else-if="error" :message="error" @retry="loadAnalytics(selectedCourseId)" />
    <template v-else-if="analytics">
      <section class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard v-for="item in stats" :key="item.label" v-bind="item" />
      </section>

      <section class="grid gap-6 xl:grid-cols-[minmax(0,1.1fr)_minmax(360px,0.9fr)]">
        <div class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900">
          <h2 class="text-base font-semibold text-slate-950 dark:text-white">知识点掌握</h2>
          <EChart v-if="analytics.masteryHeatmap.length" class="mt-4" :option="masteryOption" height="320px" />
          <EmptyState v-else class="mt-4" title="暂无掌握数据" description="学生学习节点后会生成掌握度。" />
        </div>

        <div class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900">
          <h2 class="text-base font-semibold text-slate-950 dark:text-white">薄弱知识点</h2>
          <ul v-if="analytics.weakNodes.length" class="mt-4 divide-y divide-slate-100 dark:divide-white/10">
            <li v-for="node in analytics.weakNodes" :key="node.nodeId" class="flex items-center justify-between gap-4 py-3">
              <div class="min-w-0">
                <p class="truncate text-sm font-medium text-slate-900 dark:text-white">{{ node.nodeName }}</p>
                <p class="mt-1 text-xs text-slate-500 dark:text-slate-400">平均进度 {{ formatPercent(node.averageProgress) }}</p>
              </div>
              <span class="rounded-md bg-amber-50 px-2 py-1 text-xs font-medium text-amber-700 dark:bg-amber-400/10 dark:text-amber-200">
                {{ formatPercent(node.masteryRate) }}
              </span>
            </li>
          </ul>
          <EmptyState v-else class="mt-4" title="暂无薄弱项" description="当前课程没有低掌握度知识点。" />
        </div>
      </section>

      <section class="grid gap-6 xl:grid-cols-2">
        <div class="overflow-hidden rounded-md border border-slate-200 bg-white shadow-sm dark:border-white/10 dark:bg-slate-900">
          <div class="border-b border-slate-200 px-5 py-4 dark:border-white/10">
            <h2 class="text-base font-semibold text-slate-950 dark:text-white">作业统计</h2>
          </div>
          <div class="overflow-x-auto">
            <table class="min-w-full divide-y divide-slate-200 dark:divide-white/10">
              <thead class="bg-slate-50 dark:bg-white/5">
                <tr>
                  <th class="px-4 py-3 text-left text-sm font-semibold text-slate-900 dark:text-white">作业</th>
                  <th class="px-4 py-3 text-left text-sm font-semibold text-slate-900 dark:text-white">提交率</th>
                  <th class="px-4 py-3 text-left text-sm font-semibold text-slate-900 dark:text-white">均分</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100 dark:divide-white/10">
                <tr v-for="row in analytics.homeworkStats" :key="row.homeworkId">
                  <td class="px-4 py-3 text-sm text-slate-900 dark:text-white">{{ row.title }}</td>
                  <td class="px-4 py-3 text-sm text-slate-500 dark:text-slate-300">{{ formatPercent(row.submissionRate) }}</td>
                  <td class="px-4 py-3 text-sm text-slate-500 dark:text-slate-300">{{ Number(row.averageScore || 0).toFixed(1) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="overflow-hidden rounded-md border border-slate-200 bg-white shadow-sm dark:border-white/10 dark:bg-slate-900">
          <div class="border-b border-slate-200 px-5 py-4 dark:border-white/10">
            <h2 class="text-base font-semibold text-slate-950 dark:text-white">学生排行</h2>
          </div>
          <ul class="divide-y divide-slate-100 dark:divide-white/10">
            <li v-for="row in analytics.studentRankings" :key="row.studentId" class="flex items-center justify-between gap-4 px-5 py-3">
              <div>
                <p class="text-sm font-medium text-slate-900 dark:text-white">{{ row.studentName }}</p>
                <p class="mt-1 text-xs text-slate-500 dark:text-slate-400">已提交 {{ formatNumber(row.submittedCount) }} 次</p>
              </div>
              <span class="text-sm font-semibold text-slate-900 dark:text-white">{{ Number(row.averageScore || 0).toFixed(1) }}</span>
            </li>
          </ul>
        </div>
      </section>
    </template>
  </div>
</template>
