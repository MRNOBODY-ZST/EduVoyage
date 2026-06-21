<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Popover, PopoverButton, PopoverPanel } from '@headlessui/vue'
import { BellIcon } from '@heroicons/vue/24/outline'

import { fetchNotifications, fetchUnreadCount, markAllNotificationsRead } from '@/lib/services'
import { formatDateTime } from '@/lib/format'
import type { NotificationResponse } from '@/types/api'

const loading = ref(false)
const unread = ref(0)
const notifications = ref<NotificationResponse[]>([])

async function load() {
  loading.value = true
  try {
    const [count, page] = await Promise.all([
      fetchUnreadCount().catch(() => ({ unread: 0 })),
      fetchNotifications({ pageNo: 1, pageSize: 6 }).catch(() => ({ records: [], total: 0, pageNo: 1, pageSize: 6, totalPages: 0 })),
    ])
    unread.value = count.unread
    notifications.value = page.records
  } finally {
    loading.value = false
  }
}

async function readAll() {
  await markAllNotificationsRead().catch(() => undefined)
  unread.value = 0
  notifications.value = notifications.value.map((item) => ({ ...item, read: true }))
}

onMounted(load)
</script>

<template>
  <Popover class="relative">
    <PopoverButton
      class="focus-ring relative inline-flex size-9 items-center justify-center rounded-md text-slate-500 hover:bg-slate-100 hover:text-slate-700 dark:text-slate-300 dark:hover:bg-white/10 dark:hover:text-white"
      title="通知"
    >
      <BellIcon class="size-5" aria-hidden="true" />
      <span v-if="unread > 0" class="absolute right-1.5 top-1.5 size-2 rounded-full bg-rose-500 ring-2 ring-white dark:ring-slate-900" />
      <span class="sr-only">通知</span>
    </PopoverButton>
    <PopoverPanel
      class="absolute right-0 z-20 mt-2 w-80 overflow-hidden rounded-md bg-white shadow-lg outline-1 outline-slate-900/5 dark:bg-slate-800 dark:outline-white/10"
    >
      <div class="flex items-center justify-between border-b border-slate-200 px-4 py-3 dark:border-white/10">
        <div>
          <p class="text-sm font-semibold text-slate-950 dark:text-white">站内通知</p>
          <p class="text-xs text-slate-500 dark:text-slate-400">{{ unread }} 条未读</p>
        </div>
        <button type="button" class="text-xs font-medium text-[rgb(var(--color-brand))] hover:underline" @click="readAll">全部已读</button>
      </div>
      <div class="max-h-96 overflow-y-auto">
        <div v-if="loading" class="px-4 py-8 text-center text-sm text-slate-500 dark:text-slate-400">加载中</div>
        <div v-else-if="notifications.length === 0" class="px-4 py-8 text-center text-sm text-slate-500 dark:text-slate-400">
          暂无通知
        </div>
        <div
          v-for="item in notifications"
          v-else
          :key="item.id"
          class="border-b border-slate-100 px-4 py-3 last:border-0 dark:border-white/5"
        >
          <div class="flex items-start gap-3">
            <span
              class="mt-1 size-2 rounded-full"
              :class="item.read ? 'bg-slate-300 dark:bg-slate-600' : 'bg-[rgb(var(--color-brand))]'"
            />
            <div class="min-w-0 flex-1">
              <p class="truncate text-sm font-medium text-slate-900 dark:text-white">{{ item.title }}</p>
              <p class="mt-1 line-clamp-2 text-xs leading-5 text-slate-500 dark:text-slate-400">{{ item.body }}</p>
              <p class="mt-1 text-xs text-slate-400">{{ formatDateTime(item.ts) }}</p>
            </div>
          </div>
        </div>
      </div>
    </PopoverPanel>
  </Popover>
</template>
