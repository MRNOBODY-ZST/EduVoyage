<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  AcademicCapIcon,
  ArrowTrendingUpIcon,
  BookOpenIcon,
  CalendarDaysIcon,
  CheckCircleIcon,
  ClockIcon,
  PresentationChartLineIcon,
  TrophyIcon,
} from '@heroicons/vue/24/outline'

import EChart from '@/components/charts/EChart.vue'
import EmptyState from '@/components/state/EmptyState.vue'
import ErrorState from '@/components/state/ErrorState.vue'
import LoadingState from '@/components/state/LoadingState.vue'
import { formatDateTime, formatDuration, formatNumber, formatPercent } from '@/lib/format'
import { fetchStudentDashboard } from '@/lib/services'
import type { StudentDashboardResponse } from '@/types/api'

const loading = ref(true)
const error = ref('')
const dashboard = ref<StudentDashboardResponse | null>(null)

const summaryStats = computed(() => {
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

const heroStats = computed(() => summaryStats.value.slice(0, 4))
const masteryPercentValue = computed(() => Math.max(0, Math.min(100, Number(dashboard.value?.masteryPercent ?? 0))))
const latestLog = computed(() => dashboard.value?.recentLogs[0] || null)

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

const activityOption = computed(() => {
  const data = dashboard.value
  const hours = Number(((data?.totalDurationSec || 0) / 3600).toFixed(1))
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 34, right: 12, top: 18, bottom: 28 },
    xAxis: {
      type: 'category',
      data: ['学习小时', '活跃天', '课程', '待作业'],
      axisLabel: { color: '#64748b' },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#64748b' },
      splitLine: { lineStyle: { color: '#e2e8f0' } },
    },
    series: [
      {
        name: '学习概览',
        type: 'bar',
        barWidth: 24,
        data: [hours, data?.activeDays || 0, data?.enrolledCourses || 0, data?.todoHomeworks || 0],
        itemStyle: { color: '#0f766e', borderRadius: [4, 4, 0, 0] },
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
    <section class="overflow-hidden rounded-md bg-slate-950 shadow-sm">
      <div class="grid gap-0 lg:grid-cols-[minmax(0,1.1fr)_minmax(360px,0.9fr)]">
        <div class="p-6 sm:p-8">
          <p class="text-sm font-semibold text-sky-200">Learning overview</p>
          <h2 class="mt-3 text-2xl font-semibold text-white">今天从可学节点开始，稳稳推进下一章。</h2>
          <p class="mt-3 max-w-2xl text-sm/6 text-slate-300">
            汇总学习时长、课程、作业和掌握度，帮助你快速判断下一步该复习、提交还是继续学习。
          </p>
          <div class="mt-6 flex flex-wrap gap-3">
            <span class="inline-flex items-center gap-2 rounded-md bg-white/10 px-3 py-2 text-sm font-medium text-white">
              <CheckCircleIcon class="size-4 text-emerald-300" aria-hidden="true" />
              掌握度 {{ formatPercent(dashboard.masteryPercent) }}
            </span>
            <span class="inline-flex items-center gap-2 rounded-md bg-white/10 px-3 py-2 text-sm font-medium text-white">
              <PresentationChartLineIcon class="size-4 text-sky-300" aria-hidden="true" />
              平均成绩 {{ Number(dashboard.averageScore || 0).toFixed(1) }}
            </span>
          </div>
        </div>

        <dl class="grid grid-cols-2 border-t border-white/10 lg:border-l lg:border-t-0">
          <div v-for="item in heroStats" :key="item.label" class="border-b border-r border-white/10 p-5 last:border-r-0">
            <dt class="flex items-center gap-2 text-xs/5 text-slate-300">
              <component :is="item.icon" class="size-4 text-slate-400" aria-hidden="true" />
              {{ item.label }}
            </dt>
            <dd class="mt-2 truncate text-xl font-semibold text-white">{{ item.value }}</dd>
          </div>
        </dl>
      </div>
    </section>

    <section class="grid gap-4 lg:grid-cols-6">
      <article class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900 lg:col-span-4">
        <div class="flex items-center justify-between gap-4">
          <div>
            <h2 class="text-base font-semibold text-slate-950 dark:text-white">成绩趋势</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">最近已提交作业的评分变化</p>
          </div>
          <ArrowTrendingUpIcon class="size-6 text-slate-300" aria-hidden="true" />
        </div>
        <EChart v-if="dashboard.gradeTrend.length" class="mt-4" :option="trendOption" height="320px" />
        <EmptyState v-else class="mt-4" title="暂无成绩趋势" description="提交并评分后会生成趋势图。" />
      </article>

      <article class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900 lg:col-span-2">
        <h2 class="text-base font-semibold text-slate-950 dark:text-white">掌握度与待办</h2>
        <div class="mt-5">
          <div class="flex items-center justify-between text-sm">
            <span class="text-slate-500 dark:text-slate-400">知识点平均掌握</span>
            <span class="font-semibold text-slate-950 dark:text-white">{{ formatPercent(masteryPercentValue) }}</span>
          </div>
          <div class="mt-2 h-2 rounded-full bg-slate-100 dark:bg-white/10">
            <div class="h-2 rounded-full bg-[rgb(var(--color-brand))]" :style="{ width: `${masteryPercentValue}%` }" />
          </div>
        </div>
        <dl class="mt-6 divide-y divide-slate-100 border-y border-slate-100 text-sm dark:divide-white/10 dark:border-white/10">
          <div class="flex items-center justify-between py-3">
            <dt class="text-slate-500 dark:text-slate-400">待完成作业</dt>
            <dd class="font-semibold text-slate-950 dark:text-white">{{ formatNumber(dashboard.todoHomeworks) }}</dd>
          </div>
          <div class="flex items-center justify-between py-3">
            <dt class="text-slate-500 dark:text-slate-400">已选课程</dt>
            <dd class="font-semibold text-slate-950 dark:text-white">{{ formatNumber(dashboard.enrolledCourses) }}</dd>
          </div>
          <div class="flex items-center justify-between py-3">
            <dt class="text-slate-500 dark:text-slate-400">活跃天数</dt>
            <dd class="font-semibold text-slate-950 dark:text-white">{{ formatNumber(dashboard.activeDays) }}</dd>
          </div>
        </dl>
      </article>

      <article class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900 lg:col-span-3">
        <h2 class="text-base font-semibold text-slate-950 dark:text-white">学习结构</h2>
        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">时长、活跃、课程和作业的横向对比</p>
        <EChart class="mt-4" :option="activityOption" height="260px" />
      </article>

      <article class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900 lg:col-span-3">
        <h2 class="text-base font-semibold text-slate-950 dark:text-white">最近学习记录</h2>
        <div v-if="latestLog" class="mt-4 rounded-md bg-slate-50 p-4 dark:bg-white/5">
          <p class="truncate text-sm font-semibold text-slate-950 dark:text-white">{{ latestLog.action }}</p>
          <p class="mt-1 text-xs text-slate-500 dark:text-slate-400">
            课程 {{ latestLog.courseId }} · {{ formatDateTime(latestLog.ts) }} · {{ formatDuration(latestLog.durationSec) }}
          </p>
        </div>
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
      </article>
    </section>
  </div>
</template>
