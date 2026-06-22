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
  ArrowPathRoundedSquareIcon,
  UsersIcon,
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

const bentoFeatures = computed(() => [
  {
    title: t('home.featureGraph'),
    description: t('home.featureGraphDesc'),
    icon: MapIcon,
    span: 'lg:col-span-2 lg:row-span-2',
    accent: 'from-sky-500/20 to-blue-600/10',
  },
  {
    title: t('home.featureAssessment'),
    description: t('home.featureAssessmentDesc'),
    icon: AcademicCapIcon,
    span: 'lg:col-span-2',
    accent: 'from-violet-500/15 to-purple-600/10',
  },
  {
    title: t('home.featureDrive'),
    description: t('home.featureDriveDesc'),
    icon: CloudArrowUpIcon,
    span: 'lg:col-span-1',
    accent: 'from-teal-500/15 to-emerald-600/10',
  },
  {
    title: t('home.featureAnalytics'),
    description: t('home.featureAnalyticsDesc'),
    icon: ChartBarSquareIcon,
    span: 'lg:col-span-1',
    accent: 'from-amber-500/15 to-orange-600/10',
  },
  {
    title: t('home.featurePath'),
    description: t('home.featurePathDesc'),
    icon: ArrowPathRoundedSquareIcon,
    span: 'lg:col-span-1',
    accent: 'from-rose-500/15 to-pink-600/10',
  },
  {
    title: t('home.featureRoles'),
    description: t('home.featureRolesDesc'),
    icon: UsersIcon,
    span: 'lg:col-span-1',
    accent: 'from-indigo-500/15 to-blue-600/10',
  },
])

const workflowSteps = computed(() => [
  { num: '01', title: t('home.workflow1Title'), desc: t('home.workflow1Desc') },
  { num: '02', title: t('home.workflow2Title'), desc: t('home.workflow2Desc') },
  { num: '03', title: t('home.workflow3Title'), desc: t('home.workflow3Desc') },
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
    <!-- Hero -->
    <section class="relative isolate min-h-[90vh] overflow-hidden bg-slate-950">
      <EChart class="absolute inset-x-0 bottom-0 top-16 opacity-60" :option="heroOption" height="calc(100vh - 4rem)" />
      <div class="absolute inset-0 bg-linear-to-b from-slate-950/60 via-slate-950/50 to-slate-950/80" aria-hidden="true" />

      <!-- Navbar -->
      <header class="relative z-10">
        <nav class="mx-auto flex max-w-7xl items-center justify-between px-6 py-6 lg:px-8" aria-label="Global">
          <RouterLink to="/home" class="-m-1.5 flex items-center gap-3 p-1.5">
            <span class="sr-only">{{ t('app.name') }}</span>
            <span class="grid size-9 place-items-center rounded-lg bg-[rgb(var(--color-brand))] text-sm font-bold text-white shadow-sm">EV</span>
            <span class="hidden sm:block">
              <span class="block text-sm font-semibold text-white">{{ t('app.name') }}</span>
              <span class="block text-xs text-slate-400">{{ t('app.tagline') }}</span>
            </span>
          </RouterLink>
          <div class="hidden items-center gap-x-10 md:flex">
            <a v-for="item in navItems" :key="item.href" :href="item.href" class="text-sm/6 font-medium text-slate-300 transition hover:text-white">
              {{ item.label }}
            </a>
          </div>
          <RouterLink to="/login" class="inline-flex items-center gap-1.5 rounded-lg bg-white/10 px-4 py-2 text-sm font-semibold text-white ring-1 ring-white/20 transition hover:bg-white/20">
            {{ t('home.login') }}
            <ArrowRightIcon class="size-4" aria-hidden="true" />
          </RouterLink>
        </nav>
      </header>

      <!-- Hero content -->
      <div class="relative z-10 mx-auto flex max-w-7xl flex-col px-6 pb-20 pt-24 sm:pt-32 lg:px-8 lg:pt-40">
        <!-- Eyebrow badge -->
        <div class="mb-6 inline-flex w-fit items-center gap-2 rounded-full border border-sky-500/30 bg-sky-500/10 px-4 py-1.5">
          <span class="size-1.5 rounded-full bg-sky-400" aria-hidden="true" />
          <span class="text-xs/5 font-semibold text-sky-300">{{ t('home.heroEyebrow') }}</span>
        </div>

        <div class="max-w-3xl">
          <h1 class="text-4xl font-semibold tracking-tight text-white sm:text-6xl lg:text-7xl">EduVoyage</h1>
          <p class="mt-5 max-w-2xl text-xl font-semibold leading-snug text-white sm:text-2xl">{{ t('home.heroTitle') }}</p>
          <p class="mt-5 max-w-2xl text-base/7 text-slate-300">{{ t('home.heroDescription') }}</p>
          <div class="mt-10 flex flex-wrap items-center gap-4">
            <RouterLink to="/login" class="inline-flex h-11 items-center gap-2 rounded-lg bg-white px-5 text-sm font-semibold text-slate-950 shadow-sm transition hover:bg-slate-100">
              {{ t('home.cta') }}
              <ArrowRightIcon class="size-4" aria-hidden="true" />
            </RouterLink>
            <a href="#features" class="inline-flex h-11 items-center gap-2 rounded-lg border border-white/20 px-5 text-sm font-semibold text-white transition hover:bg-white/10">
              {{ t('home.secondaryCta') }}
            </a>
          </div>
        </div>

        <!-- Metrics strip -->
        <dl class="mt-16 grid max-w-2xl grid-cols-2 gap-px overflow-hidden rounded-xl bg-white/10 text-white sm:grid-cols-4">
          <div v-for="item in metrics" :key="item.label" class="bg-slate-900/60 px-5 py-5 backdrop-blur-sm">
            <dt class="text-xs/5 text-slate-400">{{ item.label }}</dt>
            <dd class="mt-1.5 text-2xl font-bold tracking-tight">{{ item.value }}</dd>
          </div>
        </dl>
      </div>
    </section>

    <!-- Bento features -->
    <section id="features" class="bg-slate-50 py-24 dark:bg-slate-900 sm:py-32">
      <div class="mx-auto max-w-2xl px-6 lg:max-w-7xl lg:px-8">
        <p class="text-base/7 font-semibold text-[rgb(var(--color-brand-strong))]">{{ t('home.bentoTitle') }}</p>
        <h2 class="mt-2 max-w-3xl text-4xl font-semibold tracking-tight text-slate-950 dark:text-white sm:text-5xl">
          {{ t('home.bentoLead') }}
        </h2>

        <div class="mt-12 grid grid-cols-1 gap-4 lg:grid-cols-4 lg:grid-rows-2">
          <article
            v-for="(feature, i) in bentoFeatures"
            :key="feature.title"
            :class="[feature.span, 'relative overflow-hidden rounded-2xl border border-slate-200 bg-white p-8 shadow-xs dark:border-white/10 dark:bg-slate-800/60']"
          >
            <!-- Gradient accent -->
            <div :class="`absolute inset-0 bg-linear-to-br ${feature.accent} opacity-60`" aria-hidden="true" />
            <div class="relative">
              <div class="grid size-11 place-items-center rounded-xl bg-[rgb(var(--color-brand-soft))] text-[rgb(var(--color-brand))] dark:bg-white/10 dark:text-white">
                <component :is="feature.icon" class="size-6" aria-hidden="true" />
              </div>
              <h3 class="mt-5 text-lg font-semibold text-slate-950 dark:text-white">{{ feature.title }}</h3>
              <p class="mt-2.5 text-sm/6 text-slate-600 dark:text-slate-300">{{ feature.description }}</p>
            </div>
          </article>
        </div>
      </div>
    </section>

    <!-- Workflow steps -->
    <section id="workflow" class="bg-white py-20 dark:bg-slate-950">
      <div class="mx-auto max-w-7xl px-6 lg:px-8">
        <div class="mx-auto max-w-2xl text-center">
          <h2 class="text-3xl font-semibold tracking-tight text-slate-950 dark:text-white sm:text-4xl">{{ t('home.workflowTitle') }}</h2>
        </div>
        <div id="roles" class="mt-16 grid gap-8 lg:grid-cols-3">
          <div
            v-for="step in workflowSteps"
            :key="step.num"
            class="relative rounded-2xl border border-slate-200 p-8 dark:border-white/10"
          >
            <p class="text-4xl font-bold text-[rgb(var(--color-brand-soft))] dark:text-white/10">{{ step.num }}</p>
            <h3 class="mt-4 text-lg font-semibold text-slate-950 dark:text-white">{{ step.title }}</h3>
            <p class="mt-2 text-sm/6 text-slate-600 dark:text-slate-400">{{ step.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- CTA banner -->
    <section class="bg-[rgb(var(--color-brand))] py-16">
      <div class="mx-auto max-w-7xl px-6 text-center lg:px-8">
        <h2 class="text-3xl font-semibold text-white sm:text-4xl">{{ t('home.ctaBannerTitle') }}</h2>
        <p class="mx-auto mt-4 max-w-xl text-base/7 text-white/80">{{ t('home.ctaBannerDesc') }}</p>
        <RouterLink
          to="/login"
          class="mt-8 inline-flex h-11 items-center gap-2 rounded-lg bg-white px-6 text-sm font-semibold text-[rgb(var(--color-brand))] shadow-sm transition hover:bg-slate-100"
        >
          {{ t('home.cta') }}
          <ArrowRightIcon class="size-4" aria-hidden="true" />
        </RouterLink>
      </div>
    </section>
  </main>
</template>
