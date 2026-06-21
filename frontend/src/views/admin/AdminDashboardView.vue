<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  AcademicCapIcon,
  ArchiveBoxIcon,
  ClipboardDocumentCheckIcon,
  DocumentTextIcon,
  ServerStackIcon,
  UsersIcon,
} from '@heroicons/vue/24/outline'

import EChart from '@/components/charts/EChart.vue'
import StatCard from '@/components/data/StatCard.vue'
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
    <section class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <StatCard v-for="item in stats" :key="item.label" v-bind="item" />
    </section>

    <section class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900">
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
    </section>
  </div>
</template>
