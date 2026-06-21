<script setup lang="ts">
import type { Component } from 'vue'
import { ArrowTrendingUpIcon } from '@heroicons/vue/24/outline'

withDefaults(
  defineProps<{
    label: string
    value: string | number
    hint?: string
    icon?: Component
  }>(),
  {
    hint: '',
    icon: ArrowTrendingUpIcon,
  },
)
</script>

<template>
  <div class="rounded-md border border-slate-200 bg-white p-4 shadow-sm dark:border-white/10 dark:bg-slate-900">
    <div class="flex items-start justify-between gap-3">
      <div class="min-w-0">
        <p class="text-sm text-slate-500 dark:text-slate-400">{{ label }}</p>
        <p class="mt-2 truncate text-2xl font-semibold text-slate-950 dark:text-white">{{ value }}</p>
      </div>
      <span class="grid size-10 shrink-0 place-items-center rounded-md bg-[rgb(var(--color-brand-soft))] text-[rgb(var(--color-brand-strong))] dark:text-white">
        <component :is="icon" class="size-5" aria-hidden="true" />
      </span>
    </div>
    <p v-if="hint" class="mt-3 text-xs text-slate-500 dark:text-slate-400">{{ hint }}</p>
  </div>
</template>
