<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  AcademicCapIcon,
  BookOpenIcon,
  CalendarDaysIcon,
  ClockIcon,
  TrophyIcon,
} from '@heroicons/vue/24/outline'

import EChart from '@/components/charts/EChart.vue'
import StatCard from '@/components/data/StatCard.vue'
import EmptyState from '@/components/state/EmptyState.vue'
import ErrorState from '@/components/state/ErrorState.vue'
import LoadingState from '@/components/state/LoadingState.vue'
import { formatDateTime, formatDuration, formatNumber, formatPercent } from '@/lib/format'
import { fetchStudentDashboard } from '@/lib/services'
import type { StudentDashboardResponse } from '@/types/api'

const loading = ref(true)
const error = ref('')
const dashboard = ref<StudentDashboardResponse | null>(null)

const stats = computed(() => {
  const data = dashboard.value
  return [
    { label: '累计学习', value: formatDuration(data?.totalDurationSec), hint: '行为日志累计时长', icon: ClockIcon },
    { label: '活跃天数', value: formatNumber(data?.activeDays), hint: '最近学习活跃记录', icon: CalendarDaysIcon },
    { label: '已选课程', value: formatNumber(data?.enrolledCourses), hint: '当前账号课程数', icon: BookOpenIcon },
    { label: '待完成作业', value: formatNumber(data?.todoHomeworks), hint: '未提交或未关闭', icon: AcademicCapIcon },
    { label: '平均成绩', value: Number(data?.averageScore ?? 0).toFixed(1), hint: '已评分作业均分', icon: TrophyIcon },
    { label: '掌握度', value: formatPercent(data?.masteryPercent), hint: '知识点平均掌握', icon: AcademicCapIcon },
  ]
})

const trendOption = computed(() => {
  const rows = dashboard.value?.gradeTrend || []
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 36, right: 16, top: 20, bottom: 36 },
    xAxis: {
      type: 'category',
      data: rows.map((row) => row.title),
      axisLabel: { color: '#64748b', interval: 0, rotate: rows.length > 4 ? 18 : 0 },
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 100,
      axisLabel: { color: '#64748b' },
      splitLine: { lineStyle: { color: '#e2e8f0' } },
    },
    series: [
      {
        name: '成绩',
        type: 'line',
        smooth: true,
        symbolSize: 8,
        data: rows.map((row) => Number(row.score || 0)),
        areaStyle: { color: 'rgba(37, 99, 235, 0.12)' },
        lineStyle: { color: '#2563eb', width: 3 },
        itemStyle: { color: '#2563eb' },
      },
    ],
  }
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    dashboard.value = await fetchStudentDashboard()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '学情数据加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <LoadingState v-if="loading" />
  <ErrorState v-else-if="error" :message="error" @retry="load" />
  <div v-else-if="dashboard" class="space-y-6">
    <section class="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
      <StatCard v-for="item in stats" :key="item.label" v-bind="item" />
    </section>

    <section class="grid gap-6 xl:grid-cols-[minmax(0,1.3fr)_minmax(320px,0.7fr)]">
      <div class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900">
        <div class="flex items-center justify-between gap-4">
          <div>
            <h2 class="text-base font-semibold text-slate-950 dark:text-white">成绩趋势</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">最近已提交作业的评分变化</p>
          </div>
        </div>
        <EChart v-if="dashboard.gradeTrend.length" class="mt-4" :option="trendOption" height="320px" />
        <EmptyState v-else class="mt-4" title="暂无成绩趋势" description="提交并评分后会生成趋势图。" />
      </div>

      <div class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900">
        <h2 class="text-base font-semibold text-slate-950 dark:text-white">最近学习记录</h2>
        <div v-if="dashboard.recentLogs.length" class="mt-4 flow-root">
          <ul class="-my-3 divide-y divide-slate-100 dark:divide-white/10">
            <li v-for="log in dashboard.recentLogs" :key="log.id" class="py-3">
              <div class="flex items-start justify-between gap-3">
                <div class="min-w-0">
                  <p class="truncate text-sm font-medium text-slate-900 dark:text-white">{{ log.action }}</p>
                  <p class="mt-1 text-xs text-slate-500 dark:text-slate-400">课程 {{ log.courseId }} · {{ formatDateTime(log.ts) }}</p>
                </div>
                <span class="shrink-0 rounded-md bg-slate-100 px-2 py-1 text-xs text-slate-600 dark:bg-white/10 dark:text-slate-300">
                  {{ formatDuration(log.durationSec) }}
                </span>
              </div>
            </li>
          </ul>
        </div>
        <EmptyState v-else class="mt-4" title="暂无学习记录" description="学习行为上报后会显示在这里。" />
      </div>
    </section>
  </div>
</template>
