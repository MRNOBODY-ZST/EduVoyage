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
import { formatBytes, formatNumber } from '@/lib/format'
import { fetchAdminDashboard } from '@/lib/services'
import type { AdminDashboardResponse } from '@/types/api'

const loading = ref(true)
const error = ref('')
const data = ref<AdminDashboardResponse | null>(null)

const stats = computed(() => [
  { label: '总用户', value: formatNumber(data.value?.totalUsers), hint: '系统用户规模', icon: UsersIcon },
  { label: '30 日活跃', value: formatNumber(data.value?.activeUsers30d), hint: '近 30 天学习活跃', icon: ServerStackIcon },
  { label: '30 日新增', value: formatNumber(data.value?.newUsers30d), hint: '新增注册用户', icon: UsersIcon },
  { label: '课程总数', value: formatNumber(data.value?.totalCourses), hint: '已创建课程', icon: AcademicCapIcon },
  { label: '作业总数', value: formatNumber(data.value?.totalHomeworks), hint: '平台作业量', icon: DocumentTextIcon },
  { label: '提交总数', value: formatNumber(data.value?.totalSubmissions), hint: '学生提交记录', icon: ClipboardDocumentCheckIcon },
  { label: '存储占用', value: formatBytes(data.value?.storageUsedBytes), hint: '网盘物理文件占用', icon: ArchiveBoxIcon },
])

const primaryStats = computed(() => stats.value.slice(0, 4))

const option = computed(() => {
  const current = data.value
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 36, right: 16, top: 16, bottom: 34 },
    xAxis: {
      type: 'category',
      data: ['总用户', '活跃', '新增', '课程', '作业', '提交'],
      axisLabel: { color: '#64748b' },
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#64748b' },
      splitLine: { lineStyle: { color: '#e2e8f0' } },
    },
    series: [
      {
        type: 'bar',
        barWidth: 28,
        data: [
          current?.totalUsers || 0,
          current?.activeUsers30d || 0,
          current?.newUsers30d || 0,
          current?.totalCourses || 0,
          current?.totalHomeworks || 0,
          current?.totalSubmissions || 0,
        ],
        itemStyle: { color: '#2563eb', borderRadius: [4, 4, 0, 0] },
      },
    ],
  }
})

const trendOption = computed(() => {
  const current = data.value
  const users = current?.totalUsers || 0
  const active = current?.activeUsers30d || 0
  const newUsers = current?.newUsers30d || 0
  const submissions = current?.totalSubmissions || 0
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 36, right: 16, top: 18, bottom: 34 },
    xAxis: {
      type: 'category',
      data: ['用户基数', '活跃学习', '新增用户', '提交行为'],
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
        name: '运营热度',
        type: 'line',
        smooth: true,
        symbolSize: 8,
        data: [users, active, newUsers, submissions],
        areaStyle: { color: 'rgba(14, 165, 233, 0.12)' },
        lineStyle: { color: '#0284c7', width: 3 },
        itemStyle: { color: '#0284c7' },
      },
    ],
  }
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    data.value = await fetchAdminDashboard()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '运营数据加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <LoadingState v-if="loading" />
  <ErrorState v-else-if="error" :message="error" @retry="load" />
  <div v-else class="space-y-6">
    <section class="overflow-hidden rounded-md bg-slate-950 shadow-sm">
      <div class="grid gap-0 lg:grid-cols-[minmax(0,1fr)_minmax(360px,0.9fr)]">
        <div class="p-6 sm:p-8">
          <p class="text-sm font-semibold text-sky-200">Operations command center</p>
          <h2 class="mt-3 text-2xl font-semibold text-white">平台运行、课程增长和学习行为集中监控。</h2>
          <p class="mt-3 max-w-2xl text-sm/6 text-slate-300">
            关注用户规模、近 30 日活跃、新增用户、课程与作业提交，帮助管理员快速定位平台状态。
          </p>
        </div>
        <dl class="grid grid-cols-2 border-t border-white/10 lg:border-l lg:border-t-0">
          <div v-for="item in primaryStats" :key="item.label" class="border-b border-r border-white/10 p-5 last:border-r-0">
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
        <div class="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 class="text-base font-semibold text-slate-950 dark:text-white">平台概览</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">用户、课程、作业和提交量的横向对比</p>
          </div>
          <span class="rounded-md bg-[rgb(var(--color-brand-soft))] px-2.5 py-1 text-xs font-medium text-[rgb(var(--color-brand-strong))] dark:text-white">
            运营指标
          </span>
        </div>
        <EChart class="mt-4" :option="option" height="340px" />
      </article>

      <article class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900 lg:col-span-2">
        <div class="flex items-center justify-between gap-3">
          <div>
            <h2 class="text-base font-semibold text-slate-950 dark:text-white">运营热度</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">规模、活跃和提交趋势</p>
          </div>
          <ArrowTrendingUpIcon class="size-6 text-slate-300" aria-hidden="true" />
        </div>
        <EChart class="mt-4" :option="trendOption" height="240px" />
      </article>

      <article class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900 lg:col-span-3">
        <h2 class="text-base font-semibold text-slate-950 dark:text-white">内容与测评</h2>
        <dl class="mt-4 divide-y divide-slate-100 border-y border-slate-100 text-sm dark:divide-white/10 dark:border-white/10">
          <div class="flex items-center justify-between py-3">
            <dt class="text-slate-500 dark:text-slate-400">课程总数</dt>
            <dd class="font-semibold text-slate-950 dark:text-white">{{ formatNumber(data?.totalCourses) }}</dd>
          </div>
          <div class="flex items-center justify-between py-3">
            <dt class="text-slate-500 dark:text-slate-400">作业总数</dt>
            <dd class="font-semibold text-slate-950 dark:text-white">{{ formatNumber(data?.totalHomeworks) }}</dd>
          </div>
          <div class="flex items-center justify-between py-3">
            <dt class="text-slate-500 dark:text-slate-400">提交总数</dt>
            <dd class="font-semibold text-slate-950 dark:text-white">{{ formatNumber(data?.totalSubmissions) }}</dd>
          </div>
        </dl>
      </article>

      <article class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900 lg:col-span-3">
        <h2 class="text-base font-semibold text-slate-950 dark:text-white">资源容量</h2>
        <div class="mt-5 flex items-center gap-4">
          <span class="grid size-12 place-items-center rounded-md bg-[rgb(var(--color-brand-soft))] text-[rgb(var(--color-brand-strong))] dark:text-white">
            <ArchiveBoxIcon class="size-6" aria-hidden="true" />
          </span>
          <div>
            <p class="text-2xl font-semibold text-slate-950 dark:text-white">{{ formatBytes(data?.storageUsedBytes) }}</p>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">网盘物理文件占用，按去重文件统计。</p>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>
