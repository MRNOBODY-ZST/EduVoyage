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
import { useI18n } from '@/i18n'
import { formatDateTime, formatDuration, formatNumber, formatPercent } from '@/lib/format'
import { fetchStudentDashboard } from '@/lib/services'
import type { StudentDashboardResponse } from '@/types/api'

const { t } = useI18n()
const loading = ref(true)
const error = ref('')
const dashboard = ref<StudentDashboardResponse | null>(null)

const summaryStats = computed(() => {
  const d = dashboard.value
  return [
    { label: t('dashboard.totalDuration'), value: formatDuration(d?.totalDurationSec), hint: t('dashboard.durationHint'), icon: ClockIcon },
    { label: t('dashboard.activeDays'), value: formatNumber(d?.activeDays), hint: t('dashboard.activeDaysHint'), icon: CalendarDaysIcon },
    { label: t('dashboard.enrolledCourses'), value: formatNumber(d?.enrolledCourses), hint: t('dashboard.coursesHint'), icon: BookOpenIcon },
    { label: t('dashboard.todoHomeworks'), value: formatNumber(d?.todoHomeworks), hint: t('dashboard.homeworkHint'), icon: AcademicCapIcon },
    { label: t('dashboard.avgScore'), value: Number(d?.averageScore ?? 0).toFixed(1), hint: t('dashboard.scoreHint'), icon: TrophyIcon },
    { label: t('dashboard.mastery'), value: formatPercent(d?.masteryPercent), hint: t('dashboard.masteryHint'), icon: AcademicCapIcon },
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
      data: rows.map((r) => r.title),
      axisLabel: { color: '#64748b', interval: 0, rotate: rows.length > 4 ? 18 : 0 },
    },
    yAxis: {
      type: 'value', min: 0, max: 100,
      axisLabel: { color: '#64748b' },
      splitLine: { lineStyle: { color: '#e2e8f0' } },
    },
    series: [{
      name: t('dashboard.gradeTrend'),
      type: 'line',
      smooth: true,
      symbolSize: 8,
      data: rows.map((r) => Number(r.score || 0)),
      areaStyle: { color: 'rgba(37,99,235,0.10)' },
      lineStyle: { color: '#2563eb', width: 3 },
      itemStyle: { color: '#2563eb' },
    }],
  }
})

const activityOption = computed(() => {
  const d = dashboard.value
  const hours = Number(((d?.totalDurationSec || 0) / 3600).toFixed(1))
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 34, right: 12, top: 18, bottom: 28 },
    xAxis: {
      type: 'category',
      data: [t('dashboard.totalDuration'), t('dashboard.activeDays'), t('dashboard.enrolledCourses'), t('dashboard.todoHomeworks')],
      axisLabel: { color: '#64748b' },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#64748b' },
      splitLine: { lineStyle: { color: '#e2e8f0' } },
    },
    series: [{
      type: 'bar',
      barWidth: 28,
      data: [hours, d?.activeDays || 0, d?.enrolledCourses || 0, d?.todoHomeworks || 0],
      itemStyle: { color: '#0f766e', borderRadius: [4, 4, 0, 0] },
    }],
  }
})

async function load() {
  loading.value = true; error.value = ''
  try {
    dashboard.value = await fetchStudentDashboard()
  } catch (e) {
    error.value = e instanceof Error ? e.message : t('dashboard.noLogsDesc')
  } finally {
    loading.value = false
  }
}
onMounted(load)
</script>

<template>
  <LoadingState v-if="loading" />
  <ErrorState v-else-if="error" :message="error" @retry="load" />
  <div v-else-if="dashboard" class="space-y-5">
    <!-- Hero bento top -->
    <section class="overflow-hidden rounded-xl bg-slate-950 shadow-sm">
      <div class="grid gap-0 lg:grid-cols-[minmax(0,1.1fr)_minmax(360px,0.9fr)]">
        <div class="p-6 sm:p-8">
          <p class="text-xs font-semibold tracking-widest text-sky-400 uppercase">{{ t('dashboard.learningOverview') }}</p>
          <h2 class="mt-3 text-xl font-semibold text-white sm:text-2xl">{{ t('dashboard.todayTip') }}</h2>
          <p class="mt-3 max-w-xl text-sm/6 text-slate-400">{{ t('dashboard.todayDesc') }}</p>
          <div class="mt-6 flex flex-wrap gap-2">
            <span class="inline-flex items-center gap-2 rounded-lg bg-white/10 px-3 py-1.5 text-sm font-medium text-white">
              <CheckCircleIcon class="size-4 text-emerald-400" aria-hidden="true" />
              {{ t('dashboard.masteryLabel') }} {{ formatPercent(dashboard.masteryPercent) }}
            </span>
            <span class="inline-flex items-center gap-2 rounded-lg bg-white/10 px-3 py-1.5 text-sm font-medium text-white">
              <PresentationChartLineIcon class="size-4 text-sky-400" aria-hidden="true" />
              {{ t('dashboard.avgScoreLabel') }} {{ Number(dashboard.averageScore || 0).toFixed(1) }}
            </span>
          </div>
        </div>
        <!-- Stats grid -->
        <dl class="grid grid-cols-2 border-t border-white/10 lg:border-l lg:border-t-0">
          <div v-for="item in heroStats" :key="item.label" class="border-b border-r border-white/10 p-5 last:border-r-0">
            <dt class="flex items-center gap-2 text-xs text-slate-400">
              <component :is="item.icon" class="size-3.5 shrink-0" aria-hidden="true" />
              {{ item.label }}
            </dt>
            <dd class="mt-1.5 truncate text-xl font-bold text-white">{{ item.value }}</dd>
            <p class="mt-0.5 text-xs text-slate-500">{{ item.hint }}</p>
          </div>
        </dl>
      </div>
    </section>

    <!-- Bento grid row 2 -->
    <section class="grid gap-4 lg:grid-cols-6">
      <!-- Grade trend – wide -->
      <article class="rounded-xl border border-slate-200 bg-white p-5 shadow-xs dark:border-white/10 dark:bg-slate-900 lg:col-span-4">
        <div class="flex items-center justify-between gap-4">
          <div>
            <h2 class="text-sm font-semibold text-slate-950 dark:text-white">{{ t('dashboard.gradeTrend') }}</h2>
            <p class="mt-0.5 text-xs text-slate-500 dark:text-slate-400">{{ t('dashboard.gradeTrendDesc') }}</p>
          </div>
          <ArrowTrendingUpIcon class="size-5 text-slate-300" aria-hidden="true" />
        </div>
        <EChart v-if="dashboard.gradeTrend.length" class="mt-4" :option="trendOption" height="260px" />
        <EmptyState v-else class="mt-4" :title="t('dashboard.noGradeTrend')" :description="t('dashboard.noGradeTrendDesc')" />
      </article>

      <!-- Mastery & todo – narrow -->
      <article class="rounded-xl border border-slate-200 bg-white p-5 shadow-xs dark:border-white/10 dark:bg-slate-900 lg:col-span-2">
        <h2 class="text-sm font-semibold text-slate-950 dark:text-white">{{ t('dashboard.masteryAndTodo') }}</h2>
        <div class="mt-4">
          <div class="flex items-center justify-between text-xs">
            <span class="text-slate-500 dark:text-slate-400">{{ t('dashboard.avgMastery') }}</span>
            <span class="font-bold text-slate-950 dark:text-white">{{ formatPercent(masteryPercentValue) }}</span>
          </div>
          <div class="mt-2 h-2 overflow-hidden rounded-full bg-slate-100 dark:bg-white/10">
            <div class="h-2 rounded-full bg-[rgb(var(--color-brand))] transition-all duration-700" :style="{ width: `${masteryPercentValue}%` }" />
          </div>
        </div>
        <dl class="mt-5 divide-y divide-slate-100 border-y border-slate-100 text-sm dark:divide-white/10 dark:border-white/10">
          <div v-for="item in summaryStats.slice(2, 5)" :key="item.label" class="flex items-center justify-between py-2.5">
            <dt class="flex items-center gap-1.5 text-slate-500 dark:text-slate-400">
              <component :is="item.icon" class="size-3.5 shrink-0" aria-hidden="true" />
              {{ item.label }}
            </dt>
            <dd class="font-semibold text-slate-950 dark:text-white">{{ item.value }}</dd>
          </div>
        </dl>
      </article>

      <!-- Activity structure -->
      <article class="rounded-xl border border-slate-200 bg-white p-5 shadow-xs dark:border-white/10 dark:bg-slate-900 lg:col-span-3">
        <h2 class="text-sm font-semibold text-slate-950 dark:text-white">{{ t('dashboard.activityStructure') }}</h2>
        <p class="mt-0.5 text-xs text-slate-500 dark:text-slate-400">{{ t('dashboard.activityStructureDesc') }}</p>
        <EChart class="mt-4" :option="activityOption" height="220px" />
      </article>

      <!-- Recent logs -->
      <article class="rounded-xl border border-slate-200 bg-white p-5 shadow-xs dark:border-white/10 dark:bg-slate-900 lg:col-span-3">
        <h2 class="text-sm font-semibold text-slate-950 dark:text-white">{{ t('dashboard.recentLogs') }}</h2>
        <EmptyState v-if="!dashboard.recentLogs.length" class="mt-4" :title="t('dashboard.noLogs')" :description="t('dashboard.noLogsDesc')" />
        <ul v-else class="mt-4 divide-y divide-slate-100 dark:divide-white/10">
          <li v-for="log in dashboard.recentLogs" :key="log.id" class="py-3">
            <div class="flex items-start justify-between gap-3">
              <div class="min-w-0">
                <p class="truncate text-sm font-medium text-slate-900 dark:text-white">{{ log.action }}</p>
                <p class="mt-0.5 text-xs text-slate-500 dark:text-slate-400">{{ formatDateTime(log.ts) }}</p>
              </div>
              <span class="shrink-0 rounded-md bg-slate-100 px-2 py-1 text-xs text-slate-600 dark:bg-white/10 dark:text-slate-300">
                {{ formatDuration(log.durationSec) }}
              </span>
            </div>
          </li>
        </ul>
      </article>
    </section>
  </div>
</template>
