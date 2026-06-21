<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { DocumentIcon, FolderIcon, LinkIcon } from '@heroicons/vue/24/outline'

import EmptyState from '@/components/state/EmptyState.vue'
import ErrorState from '@/components/state/ErrorState.vue'
import LoadingState from '@/components/state/LoadingState.vue'
import { formatBytes, formatDateTime } from '@/lib/format'
import { accessDriveShare, downloadFromUrl } from '@/lib/services'
import type { ShareViewResponse } from '@/types/api'

const route = useRoute()
const extractCode = ref('')
const loading = ref(false)
const error = ref('')
const view = ref<ShareViewResponse | null>(null)

const token = computed(() => String(route.params.token || ''))

async function submit() {
  if (!token.value) {
    error.value = '分享链接不完整'
    return
  }
  loading.value = true
  error.value = ''
  try {
    view.value = await accessDriveShare(token.value, extractCode.value)
  } catch (e) {
    view.value = null
    error.value = e instanceof Error ? e.message : '分享访问失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="min-h-screen bg-[rgb(var(--color-bg))] px-4 py-8 text-slate-950 dark:text-slate-100 sm:px-6 lg:px-8">
    <div class="mx-auto max-w-4xl space-y-6">
      <header class="flex items-center justify-between gap-4">
        <div class="flex items-center gap-3">
          <span class="grid size-10 place-items-center rounded-md bg-[rgb(var(--color-brand))] text-sm font-bold text-white">EV</span>
          <div>
            <p class="text-base font-semibold text-slate-950 dark:text-white">EduVoyage 分享</p>
            <p class="text-sm text-slate-500 dark:text-slate-400">安全访问课程与个人网盘资源</p>
          </div>
        </div>
        <a class="text-sm font-semibold text-[rgb(var(--color-brand))] hover:underline" href="/login">登录平台</a>
      </header>

      <section class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900">
        <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <h1 class="text-xl font-semibold text-slate-950 dark:text-white">访问分享资源</h1>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">输入提取码后查看分享内容。文件预览链接由服务端短时签发。</p>
          </div>
          <form class="flex flex-col gap-3 sm:flex-row" @submit.prevent="submit">
            <input
              v-model.trim="extractCode"
              class="focus-ring h-10 rounded-md border border-slate-300 bg-white px-3 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white"
              maxlength="16"
              placeholder="提取码"
            />
            <button type="submit" class="btn-primary focus-ring h-10 px-4 text-sm" :disabled="loading">访问</button>
          </form>
        </div>
      </section>

      <LoadingState v-if="loading" />
      <ErrorState v-else-if="error" :message="error" @retry="submit" />
      <EmptyState v-else-if="!view" title="等待访问" description="输入提取码后会显示分享内容。" />

      <section v-else class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900">
        <div class="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
          <div class="min-w-0">
            <div class="flex items-center gap-3">
              <FolderIcon v-if="view.node.directory" class="size-6 shrink-0 text-amber-500" aria-hidden="true" />
              <DocumentIcon v-else class="size-6 shrink-0 text-slate-400" aria-hidden="true" />
              <div class="min-w-0">
                <h2 class="truncate text-lg font-semibold text-slate-950 dark:text-white">{{ view.node.name }}</h2>
                <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
                  {{ view.node.directory ? '目录分享' : `${view.node.mime || '文件'} · ${formatBytes(view.node.size)}` }}
                </p>
              </div>
            </div>
          </div>
          <div class="shrink-0 text-sm text-slate-500 dark:text-slate-400">
            <p>浏览 {{ view.share.viewCount }} 次</p>
            <p>{{ view.share.expireAt ? `过期 ${formatDateTime(view.share.expireAt)}` : '长期有效' }}</p>
          </div>
        </div>

        <div v-if="view.url" class="mt-5 rounded-md bg-slate-50 p-4 dark:bg-white/5">
          <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <p class="text-sm font-medium text-slate-900 dark:text-white">文件访问链接</p>
              <p class="mt-1 text-xs text-slate-500 dark:text-slate-400">有效期至 {{ formatDateTime(view.urlExpireAt) }}</p>
            </div>
            <button type="button" class="btn-primary focus-ring inline-flex h-10 items-center gap-2 px-4 text-sm" @click="downloadFromUrl(view.url!)">
              <LinkIcon class="size-4" aria-hidden="true" />
              打开文件
            </button>
          </div>
        </div>

        <div v-if="view.node.directory" class="mt-6">
          <h3 class="text-base font-semibold text-slate-950 dark:text-white">目录内容</h3>
          <EmptyState v-if="view.children.length === 0" class="mt-4" title="目录为空" description="分享目录当前没有可见子项。" />
          <ul v-else class="mt-4 divide-y divide-slate-100 dark:divide-white/10">
            <li v-for="child in view.children" :key="child.id" class="flex items-center justify-between gap-4 py-3">
              <div class="flex min-w-0 items-center gap-3">
                <FolderIcon v-if="child.directory" class="size-5 shrink-0 text-amber-500" aria-hidden="true" />
                <DocumentIcon v-else class="size-5 shrink-0 text-slate-400" aria-hidden="true" />
                <span class="truncate text-sm font-medium text-slate-950 dark:text-white">{{ child.name }}</span>
              </div>
              <span class="shrink-0 text-sm text-slate-500 dark:text-slate-400">{{ child.directory ? '目录' : formatBytes(child.size) }}</span>
            </li>
          </ul>
        </div>
      </section>
    </div>
  </main>
</template>
