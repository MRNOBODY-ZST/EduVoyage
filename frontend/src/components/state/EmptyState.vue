<script setup lang="ts">
import type { Component } from 'vue'
import { InboxIcon } from '@heroicons/vue/24/outline'

withDefaults(
  defineProps<{
    title?: string
    description?: string
    icon?: Component
  }>(),
  {
    title: '暂无数据',
    description: '当前条件下没有可展示的记录。',
    icon: InboxIcon,
  },
)
</script>

<template>
  <div class="rounded-md border border-dashed border-slate-300 bg-white px-6 py-12 text-center dark:border-white/15 dark:bg-slate-900">
    <component :is="icon" class="mx-auto size-10 text-slate-300 dark:text-slate-600" aria-hidden="true" />
    <h3 class="mt-3 text-sm font-semibold text-slate-950 dark:text-white">{{ title }}</h3>
    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">{{ description }}</p>
    <div v-if="$slots.default" class="mt-5">
      <slot />
    </div>
  </div>
</template>
