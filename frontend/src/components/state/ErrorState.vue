<script setup lang="ts">
import { ExclamationTriangleIcon } from '@heroicons/vue/24/outline'

defineProps<{
  message: string
}>()

const emit = defineEmits<{
  retry: []
}>()
</script>

<template>
  <div class="rounded-md border border-rose-200 bg-rose-50 px-6 py-5 dark:border-rose-400/20 dark:bg-rose-400/10">
    <div class="flex items-start gap-3">
      <ExclamationTriangleIcon class="mt-0.5 size-5 shrink-0 text-rose-500" aria-hidden="true" />
      <div class="min-w-0 flex-1">
        <p class="text-sm font-semibold text-rose-900 dark:text-rose-100">请求失败</p>
        <p class="mt-1 text-sm text-rose-700 dark:text-rose-200">{{ message }}</p>
      </div>
      <button type="button" class="text-sm font-semibold text-rose-700 hover:underline dark:text-rose-200" @click="emit('retry')">
        重试
      </button>
    </div>
  </div>
</template>
