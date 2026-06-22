<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  AcademicCapIcon,
  ArchiveBoxIcon,
  ArrowTrendingUpIcon,
  ClipboardDocumentCheckIcon,
  DocumentTextIcon,
  ServerStackIcon,
  UsersIcon,
} from '@heroicons/vue/24/outline'

import EChart from '@/components/charts/EChart.vue'
import ErrorState from '@/components/state/ErrorState.vue'
import LoadingState from '@/components/state/LoadingState.vue'
import { useI18n } from '@/i18n'
import { formatBytes, formatNumber } from '@/lib/format'
import { fetchAdminDashboard } from '@/lib/services'
import type { AdminDashboardResponse } from '@/types/api'

const { t } = useI18n()
const loading = ref(true)
const error = ref('')
const data = ref<AdminDashboardResponse | null>(null)

const stats = computed(() => [
  { label: t('admin.totalUsers'), value: formatNumber(data.value?.totalUsers), hint: t('admin.usersHint'), icon: UsersIcon },
  { label: t('admin.activeUsers'), value: formatNumber(data.value?.activeUsers30d), hint: t('admin.activeHint'), icon: ServerStackIcon },
  { label: t('admin.newUsers'), value: formatNumber(data.value?.newUsers30d), hint: t('admin.newHint'), icon: UsersIcon },
  { label: t('admin.totalCourses'), value: formatNumber(data.value?.totalCourses), hint: t('admin.coursesHint'), icon: AcademicCapIcon },
  { label: t('admin.totalHomeworks'), value: formatNumber(data.value?.totalHomeworks), hint: t('admin.homeworksHint'), icon: DocumentTextIcon },
  { label: t('admin.totalSubmissions'), value: formatNumber(data.value?.totalSubmissions), hint: t('admin.submissionsHint'), icon: ClipboardDocumentCheckIcon },
  { label: t('admin.storage'), value: formatBytes(data.value?.storageUsedBytes), hint: t('admin.storageHint'), icon: ArchiveBoxIcon },
])

const primaryStats = computed(() => stats.value.slice(0, 4))

const overviewOption = computed(() => {
  const d = data.value
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 36, right: 16, top: 16, bottom: 34 },
    xAxis: {
      type: 'category',
      data: [t('admin.totalUsers'), t('admin.activeUsers'), t('admin.newUsers'), t('admin.totalCourses'), t('admin.totalHomeworks'), t('admin.totalSubmissions')],
      axisLabel: { color: '#64748b' },
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#64748b' },
      splitLine: { lineStyle: { color: '#e2e8f0' } },
    },
    series: [{
      type: 'bar', barWidth: 28,
      data: [d?.totalUsers || 0, d?.activeUsers30d || 0, d?.newUsers30d || 0, d?.totalCourses || 0, d?.totalHomeworks || 0, d?.totalSubmissions || 0],
      itemStyle: { color: '#2563eb', borderRadius: [4, 4, 0, 0] },
    }],
  }
})

const trendOption = computed(() => {
  const d = data.value
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 36, right: 16, top: 18, bottom: 34 },
    xAxis: {
      type: 'category',
      data: [t('admin.totalUsers'), t('admin.activeUsers'), t('admin.newUsers'), t('admin.totalSubmissions')],
      axisLabel: { color: '#64748b' },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#64748b' },
      splitLine: { lineStyle: { color: '#e2e8f0' } },
    },
    series: [{
      name: t('admin.opsHeat'),
      type: 'line',
      smooth: true,
      symbolSize: 8,
      data: [d?.totalUsers || 0, d?.activeUsers30d || 0, d?.newUsers30d || 0, d?.totalSubmissions || 0],
      areaStyle: { color: 'rgba(14,165,233,0.10)' },
      lineStyle: { color: '#0284c7', width: 3 },
      itemStyle: { color: '#0284c7' },
    }],
  }
})

async function load() {
  loading.value = true; error.value = ''
  try { data.value = await fetchAdminDashboard() }
  catch (e) { error.value = e instanceof Error ? e.message : t('admin.overview') }
  finally { loading.value = false }
}
onMounted(load)
</script>

<template>
  <LoadingState v-if="loading" />
  <ErrorState v-else-if="error" :message="error" @retry="load" />
  <div v-else class="space-y-5">
    <!-- Hero bento top -->
    <section class="overflow-hidden rounded-xl bg-slate-950 shadow-sm">
      <div class="grid gap-0 lg:grid-cols-[minmax(0,1fr)_minmax(360px,0.9fr)]">
        <div class="p-6 sm:p-8">
          <p class="text-xs font-semibold tracking-widest text-sky-400 uppercase">{{ t('admin.commandCenter') }}</p>
          <h2 class="mt-3 text-xl font-semibold text-white sm:text-2xl">{{ t('admin.overview') }}</h2>
          <p class="mt-3 max-w-xl text-sm/6 text-slate-400">{{ t('admin.overviewDesc') }}</p>
        </div>
        <dl class="grid grid-cols-2 border-t border-white/10 lg:border-l lg:border-t-0">
          <div v-for="item in primaryStats" :key="item.label" class="border-b border-r border-white/10 p-5 last:border-r-0">
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

    <!-- Bento grid -->
    <section class="grid gap-4 lg:grid-cols-6">
      <!-- Overview chart – wide -->
      <article class="rounded-xl border border-slate-200 bg-white p-5 shadow-xs dark:border-white/10 dark:bg-slate-900 lg:col-span-4">
        <div class="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 class="text-sm font-semibold text-slate-950 dark:text-white">{{ t('admin.platformOverview') }}</h2>
            <p class="mt-0.5 text-xs text-slate-500 dark:text-slate-400">{{ t('admin.platformOverviewDesc') }}</p>
          </div>
          <span class="rounded-md bg-[rgb(var(--color-brand-soft))] px-2.5 py-1 text-xs font-medium text-[rgb(var(--color-brand-strong))] dark:text-white">
            {{ t('admin.opsBadge') }}
          </span>
        </div>
        <EChart class="mt-4" :option="overviewOption" height="280px" />
      </article>

      <!-- Trend – narrow -->
      <article class="rounded-xl border border-slate-200 bg-white p-5 shadow-xs dark:border-white/10 dark:bg-slate-900 lg:col-span-2">
        <div class="flex items-center justify-between gap-3">
          <div>
            <h2 class="text-sm font-semibold text-slate-950 dark:text-white">{{ t('admin.opsHeat') }}</h2>
            <p class="mt-0.5 text-xs text-slate-500 dark:text-slate-400">{{ t('admin.opsHeatDesc') }}</p>
          </div>
          <ArrowTrendingUpIcon class="size-5 text-slate-300" aria-hidden="true" />
        </div>
        <EChart class="mt-4" :option="trendOption" height="200px" />
      </article>

      <!-- Content & assessment -->
      <article class="rounded-xl border border-slate-200 bg-white p-5 shadow-xs dark:border-white/10 dark:bg-slate-900 lg:col-span-3">
        <h2 class="text-sm font-semibold text-slate-950 dark:text-white">{{ t('admin.contentAssessment') }}</h2>
        <dl class="mt-4 divide-y divide-slate-100 border-y border-slate-100 text-sm dark:divide-white/10 dark:border-white/10">
          <div v-for="item in stats.slice(3, 6)" :key="item.label" class="flex items-center justify-between py-3">
            <dt class="flex items-center gap-2 text-slate-500 dark:text-slate-400">
              <component :is="item.icon" class="size-4 shrink-0" aria-hidden="true" />
              {{ item.label }}
            </dt>
            <dd class="font-semibold text-slate-950 dark:text-white">{{ item.value }}</dd>
          </div>
        </dl>
      </article>

      <!-- Storage -->
      <article class="rounded-xl border border-slate-200 bg-white p-5 shadow-xs dark:border-white/10 dark:bg-slate-900 lg:col-span-3">
        <h2 class="text-sm font-semibold text-slate-950 dark:text-white">{{ t('admin.resourceCapacity') }}</h2>
        <div class="mt-5 flex items-center gap-4">
          <span class="grid size-12 place-items-center rounded-xl bg-[rgb(var(--color-brand-soft))] text-[rgb(var(--color-brand-strong))] dark:text-white">
            <ArchiveBoxIcon class="size-6" aria-hidden="true" />
          </span>
          <div>
            <p class="text-2xl font-bold text-slate-950 dark:text-white">{{ formatBytes(data?.storageUsedBytes) }}</p>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">{{ t('admin.storageDesc') }}</p>
          </div>
        </div>
        <dl class="mt-5 grid grid-cols-3 gap-3">
          <div v-for="item in stats.slice(0, 3)" :key="item.label" class="rounded-lg bg-slate-50 p-3 text-center dark:bg-white/5">
            <p class="text-xs text-slate-500 dark:text-slate-400">{{ item.label }}</p>
            <p class="mt-1 text-lg font-bold text-slate-950 dark:text-white">{{ item.value }}</p>
          </div>
        </dl>
      </article>
    </section>
  </div>
</template>
