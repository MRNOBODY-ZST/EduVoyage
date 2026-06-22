<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import {
  AcademicCapIcon,
  ArrowRightIcon,
  ChartBarSquareIcon,
  CloudArrowUpIcon,
  MapIcon,
  RectangleGroupIcon,
} from '@heroicons/vue/24/outline'
import type { EChartsOption } from 'echarts/core'

import EChart from '@/components/charts/EChart.vue'
import { useI18n } from '@/i18n'

const { t } = useI18n()

const navItems = computed(() => [
  { label: t('home.nav.features'), href: '#features' },
  { label: t('home.nav.workflow'), href: '#workflow' },
  { label: t('home.nav.roles'), href: '#roles' },
])

const metrics = computed(() => [
  { label: t('home.metricCourses'), value: '6+' },
  { label: t('home.metricGraph'), value: 'Graph' },
  { label: t('home.metricRoles'), value: '3' },
  { label: t('home.metricStorage'), value: 'Drive' },
])

const features = computed(() => [
  {
    title: t('home.featureGraph'),
    description: t('home.featureGraphDesc'),
    icon: MapIcon,
    class: 'lg:col-span-3',
  },
  {
    title: t('home.featureAssessment'),
    description: t('home.featureAssessmentDesc'),
    icon: AcademicCapIcon,
    class: 'lg:col-span-3',
  },
  {
    title: t('home.featureDrive'),
    description: t('home.featureDriveDesc'),
    icon: CloudArrowUpIcon,
    class: 'lg:col-span-2',
  },
  {
    title: t('home.featureAnalytics'),
    description: t('home.featureAnalyticsDesc'),
    icon: ChartBarSquareIcon,
    class: 'lg:col-span-2',
  },
  {
    title: t('app.workspace'),
    description: t('home.heroDescription'),
    icon: RectangleGroupIcon,
    class: 'lg:col-span-2',
  },
])

const heroOption = computed<EChartsOption>(() => ({
  backgroundColor: 'transparent',
  tooltip: { trigger: 'axis' },
  grid: { left: 0, right: 0, top: 36, bottom: 0 },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun', 'Next'],
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { show: false },
  },
  yAxis: {
    type: 'value',
    min: 0,
    max: 100,
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { show: false },
    splitLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.14)' } },
  },
  series: [
    {
      name: 'Mastery',
      type: 'line',
      smooth: true,
      symbolSize: 7,
      data: [32, 44, 51, 64, 72, 78, 86, 91],
      areaStyle: { color: 'rgba(14, 165, 233, 0.16)' },
      lineStyle: { color: '#38bdf8', width: 3 },
      itemStyle: { color: '#38bdf8' },
    },
    {
      name: 'Engagement',
      type: 'line',
      smooth: true,
      symbolSize: 5,
      data: [48, 42, 57, 53, 68, 71, 79, 88],
      areaStyle: { color: 'rgba(45, 212, 191, 0.12)' },
      lineStyle: { color: '#2dd4bf', width: 2 },
      itemStyle: { color: '#2dd4bf' },
    },
    {
      name: 'Risk',
      type: 'bar',
      barWidth: 18,
      data: [22, 28, 18, 20, 16, 12, 10, 8],
      itemStyle: { color: 'rgba(251, 191, 36, 0.42)', borderRadius: [4, 4, 0, 0] },
    },
  ],
}))
</script>

<template>
  <main class="min-h-screen bg-white text-slate-950 dark:bg-slate-950 dark:text-white">
    <section class="relative isolate min-h-[88vh] overflow-hidden bg-slate-950">
      <EChart class="absolute inset-x-0 bottom-0 top-16 opacity-70" :option="heroOption" height="calc(100vh - 4rem)" />
      <div class="absolute inset-0 bg-slate-950/70" aria-hidden="true" />

      <header class="relative z-10">
        <nav class="mx-auto flex max-w-7xl items-center justify-between px-6 py-6 lg:px-8" aria-label="Global">
          <RouterLink to="/home" class="-m-1.5 flex items-center gap-3 p-1.5">
            <span class="sr-only">{{ t('app.name') }}</span>
            <span class="grid size-9 place-items-center rounded-md bg-[rgb(var(--color-brand))] text-sm font-bold text-white shadow-sm">EV</span>
            <span class="hidden sm:block">
              <span class="block text-sm font-semibold text-white">{{ t('app.name') }}</span>
              <span class="block text-xs text-slate-300">{{ t('app.tagline') }}</span>
            </span>
          </RouterLink>
          <div class="hidden items-center gap-x-10 md:flex">
            <a v-for="item in navItems" :key="item.href" :href="item.href" class="text-sm/6 font-semibold text-slate-200 hover:text-white">
              {{ item.label }}
            </a>
          </div>
          <RouterLink to="/login" class="text-sm/6 font-semibold text-white">
            {{ t('home.login') }} <span aria-hidden="true">-&gt;</span>
          </RouterLink>
        </nav>
      </header>

      <div class="relative z-10 mx-auto flex max-w-7xl flex-col px-6 pb-16 pt-20 sm:pt-28 lg:px-8 lg:pt-32">
        <div class="max-w-3xl">
          <p class="text-sm/6 font-semibold text-sky-200">{{ t('home.heroEyebrow') }}</p>
          <h1 class="mt-4 text-5xl font-semibold tracking-normal text-white sm:text-7xl">EduVoyage</h1>
          <p class="mt-6 max-w-2xl text-2xl font-semibold leading-9 text-white sm:text-3xl">{{ t('home.heroTitle') }}</p>
          <p class="mt-6 max-w-2xl text-base/7 text-slate-300 sm:text-lg/8">{{ t('home.heroDescription') }}</p>
          <div class="mt-10 flex flex-wrap items-center gap-4">
            <RouterLink to="/login" class="inline-flex h-11 items-center gap-2 rounded-md bg-white px-4 text-sm font-semibold text-slate-950 shadow-sm hover:bg-slate-100">
              {{ t('home.cta') }}
              <ArrowRightIcon class="size-4" aria-hidden="true" />
            </RouterLink>
            <a href="#features" class="text-sm/6 font-semibold text-white">{{ t('home.secondaryCta') }} <span aria-hidden="true">-&gt;</span></a>
          </div>
        </div>

        <dl class="mt-16 grid max-w-4xl grid-cols-2 gap-px overflow-hidden rounded-md bg-white/10 text-white sm:grid-cols-4">
          <div v-for="item in metrics" :key="item.label" class="bg-slate-900/70 px-4 py-5">
            <dt class="text-xs/5 text-slate-300">{{ item.label }}</dt>
            <dd class="mt-2 text-2xl font-semibold">{{ item.value }}</dd>
          </div>
        </dl>
      </div>
    </section>

    <section id="features" class="bg-slate-50 py-20 dark:bg-slate-950 sm:py-24">
      <div class="mx-auto max-w-2xl px-6 lg:max-w-7xl lg:px-8">
        <h2 class="text-base/7 font-semibold text-[rgb(var(--color-brand-strong))]">{{ t('home.bentoTitle') }}</h2>
        <p class="mt-2 max-w-3xl text-4xl font-semibold tracking-normal text-slate-950 dark:text-white sm:text-5xl">
          {{ t('home.bentoLead') }}
        </p>

        <div class="mt-12 grid grid-cols-1 gap-4 lg:grid-cols-6">
          <article
            v-for="feature in features"
            :key="feature.title"
            :class="feature.class"
            class="relative overflow-hidden rounded-md bg-white p-8 shadow-sm outline outline-1 outline-slate-200 dark:bg-slate-900 dark:outline-white/10"
          >
            <div class="grid size-11 place-items-center rounded-md bg-[rgb(var(--color-brand-soft))] text-[rgb(var(--color-brand-strong))] dark:text-white">
              <component :is="feature.icon" class="size-6" aria-hidden="true" />
            </div>
            <h3 class="mt-6 text-lg font-semibold text-slate-950 dark:text-white">{{ feature.title }}</h3>
            <p class="mt-3 text-sm/6 text-slate-600 dark:text-slate-300">{{ feature.description }}</p>
          </article>
        </div>
      </div>
    </section>

    <section id="workflow" class="bg-white py-16 dark:bg-slate-950">
      <div class="mx-auto grid max-w-7xl gap-8 px-6 lg:grid-cols-3 lg:px-8">
        <div class="rounded-md border border-slate-200 p-6 dark:border-white/10">
          <p class="text-sm font-semibold text-[rgb(var(--color-brand-strong))]">01</p>
          <h3 class="mt-3 text-base font-semibold text-slate-950 dark:text-white">教师建课</h3>
          <p class="mt-2 text-sm/6 text-slate-600 dark:text-slate-300">维护章节、知识点、图谱关系、课件与作业。</p>
        </div>
        <div class="rounded-md border border-slate-200 p-6 dark:border-white/10">
          <p class="text-sm font-semibold text-[rgb(var(--color-brand-strong))]">02</p>
          <h3 class="mt-3 text-base font-semibold text-slate-950 dark:text-white">学生学习</h3>
          <p class="mt-2 text-sm/6 text-slate-600 dark:text-slate-300">按章节和推荐路径学习，提交作业并沉淀错题。</p>
        </div>
        <div id="roles" class="rounded-md border border-slate-200 p-6 dark:border-white/10">
          <p class="text-sm font-semibold text-[rgb(var(--color-brand-strong))]">03</p>
          <h3 class="mt-3 text-base font-semibold text-slate-950 dark:text-white">平台运营</h3>
          <p class="mt-2 text-sm/6 text-slate-600 dark:text-slate-300">管理员查看用户、课程、作业、提交和存储指标。</p>
        </div>
      </div>
    </section>
  </main>
</template>
