<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  ArrowPathIcon,
  CloudArrowUpIcon,
  DocumentIcon,
  FolderIcon,
  LinkIcon,
  PlusIcon,
  TrashIcon,
} from '@heroicons/vue/24/outline'

import EmptyState from '@/components/state/EmptyState.vue'
import ErrorState from '@/components/state/ErrorState.vue'
import LoadingState from '@/components/state/LoadingState.vue'
import { formatBytes, formatDateTime } from '@/lib/format'
import {
  createDriveDirectory,
  createDriveShare,
  deleteDriveNode,
  downloadFromUrl,
  fetchDriveBreadcrumb,
  fetchDriveNodes,
  fetchDriveQuota,
  fetchDriveTree,
  fetchMyShares,
  getDrivePreviewUrl,
  moveDriveNode,
  renameDriveNode,
  uploadDriveFile,
} from '@/lib/services'
import type { BreadcrumbItem, DriveNodeResponse, DriveTreeNode, QuotaResponse, ShareResponse } from '@/types/api'

interface DirectoryOption {
  id: number
  name: string
  level: number
}

const loading = ref(true)
const busy = ref(false)
const error = ref('')
const nodes = ref<DriveNodeResponse[]>([])
const tree = ref<DriveTreeNode[]>([])
const breadcrumb = ref<BreadcrumbItem[]>([])
const quota = ref<QuotaResponse | null>(null)
const shares = ref<ShareResponse[]>([])
const currentParent = ref(0)
const spaceType = ref(1)
const courseId = ref<number | null>(null)
const directoryName = ref('')
const selectedFile = ref<File | null>(null)
const shareNode = ref<DriveNodeResponse | null>(null)
const shareResult = ref<ShareResponse | null>(null)
const shareCode = ref('')
const shareExpireAt = ref('')
const moveNode = ref<DriveNodeResponse | null>(null)
const targetParentId = ref(0)

const quotaPercent = computed(() => {
  if (!quota.value?.totalBytes) {
    return 0
  }
  return Math.min(100, Math.round((quota.value.usedBytes / quota.value.totalBytes) * 100))
})

const params = computed(() => ({
  parentId: currentParent.value || undefined,
  spaceType: spaceType.value,
  courseId: spaceType.value === 2 ? courseId.value || undefined : undefined,
}))

const treeParams = computed(() => ({
  spaceType: spaceType.value,
  courseId: spaceType.value === 2 ? courseId.value || undefined : undefined,
}))

const directoryOptions = computed(() => flattenDirectories(tree.value))

function flattenDirectories(items: DriveTreeNode[], level = 0): DirectoryOption[] {
  return items.flatMap((item) => {
    if (!item.node.directory) {
      return []
    }
    return [{ id: item.node.id, name: item.node.name, level }, ...flattenDirectories(item.children || [], level + 1)]
  })
}

function shareUrl(token: string) {
  return `${window.location.origin}/share/${token}`
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [list, treeList, quotaInfo, myShares] = await Promise.all([
      fetchDriveNodes(params.value),
      fetchDriveTree(treeParams.value).catch(() => []),
      fetchDriveQuota(),
      fetchMyShares().catch(() => []),
    ])
    nodes.value = list
    tree.value = treeList
    quota.value = quotaInfo
    shares.value = myShares
    breadcrumb.value = currentParent.value ? await fetchDriveBreadcrumb(currentParent.value) : []
  } catch (e) {
    error.value = e instanceof Error ? e.message : '网盘加载失败'
  } finally {
    loading.value = false
  }
}

async function enterDirectory(node: DriveNodeResponse) {
  if (!node.directory) {
    await preview(node)
    return
  }
  currentParent.value = node.id
  await load()
}

async function goRoot() {
  currentParent.value = 0
  await load()
}

async function goDirectory(id: number) {
  currentParent.value = id
  await load()
}

async function goBreadcrumb(item: BreadcrumbItem) {
  currentParent.value = item.id
  await load()
}

async function createDirectory() {
  if (!directoryName.value.trim()) {
    return
  }
  busy.value = true
  try {
    await createDriveDirectory({
      name: directoryName.value.trim(),
      parentId: currentParent.value || undefined,
      spaceType: spaceType.value,
      courseId: spaceType.value === 2 ? courseId.value || undefined : undefined,
    })
    directoryName.value = ''
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '目录创建失败'
  } finally {
    busy.value = false
  }
}

async function upload() {
  if (!selectedFile.value) {
    return
  }
  busy.value = true
  try {
    await uploadDriveFile(selectedFile.value, params.value)
    selectedFile.value = null
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '文件上传失败'
  } finally {
    busy.value = false
  }
}

async function preview(node: DriveNodeResponse) {
  if (node.directory) {
    return
  }
  try {
    const result = await getDrivePreviewUrl(node.id)
    downloadFromUrl(result.url)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '预览链接生成失败'
  }
}

async function rename(node: DriveNodeResponse) {
  const name = window.prompt('重命名', node.name)
  if (!name || name.trim() === node.name) {
    return
  }
  await renameDriveNode(node.id, name.trim())
  await load()
}

async function remove(node: DriveNodeResponse) {
  if (!window.confirm(`确认删除“${node.name}”？`)) {
    return
  }
  await deleteDriveNode(node.id)
  await load()
}

function prepareMove(node: DriveNodeResponse) {
  moveNode.value = node
  targetParentId.value = currentParent.value
}

function onFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  selectedFile.value = input.files?.[0] || null
}

async function submitShare() {
  if (!shareNode.value) {
    return
  }
  busy.value = true
  try {
    shareResult.value = await createDriveShare({
      nodeId: shareNode.value.id,
      extractCode: shareCode.value || undefined,
      expireAt: shareExpireAt.value || undefined,
    })
    shareNode.value = null
    shareCode.value = ''
    shareExpireAt.value = ''
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '分享创建失败'
  } finally {
    busy.value = false
  }
}

async function submitMove() {
  if (!moveNode.value) {
    return
  }
  if (moveNode.value.id === targetParentId.value) {
    error.value = '不能移动到自身'
    return
  }
  busy.value = true
  try {
    await moveDriveNode(moveNode.value.id, targetParentId.value)
    moveNode.value = null
    targetParentId.value = 0
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '移动失败'
  } finally {
    busy.value = false
  }
}

async function copyShare(token: string) {
  await navigator.clipboard?.writeText(shareUrl(token)).catch(() => undefined)
}

onMounted(load)
</script>

<template>
  <div class="space-y-6">
    <section class="grid gap-4 xl:grid-cols-[minmax(0,1fr)_320px]">
      <div class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900">
        <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <h2 class="text-base font-semibold text-slate-950 dark:text-white">网盘空间</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
              {{ spaceType === 1 ? '个人空间' : '课程空间' }} · {{ quota ? `${formatBytes(quota.usedBytes)} / ${formatBytes(quota.totalBytes)}` : '配额加载中' }}
            </p>
          </div>
          <div class="flex flex-col gap-3 sm:flex-row">
            <select
              v-model.number="spaceType"
              class="focus-ring h-10 rounded-md border border-slate-300 bg-white px-3 text-sm text-slate-950 dark:border-white/10 dark:bg-slate-900 dark:text-white"
              @change="goRoot"
            >
              <option :value="1">个人空间</option>
              <option :value="2">课程空间</option>
            </select>
            <input
              v-if="spaceType === 2"
              v-model="courseId"
              type="text"
              inputmode="numeric"
              class="focus-ring h-10 w-32 rounded-md border border-slate-300 bg-white px-3 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white"
              placeholder="课程 ID"
              @keyup.enter="goRoot"
            />
            <button
              type="button"
              class="focus-ring inline-flex h-10 items-center justify-center gap-2 rounded-md border border-slate-200 bg-white px-3 text-sm font-semibold text-slate-700 hover:bg-slate-50 dark:border-white/10 dark:bg-white/5 dark:text-slate-200 dark:hover:bg-white/10"
              @click="load"
            >
              <ArrowPathIcon class="size-4" aria-hidden="true" />
              刷新
            </button>
          </div>
        </div>

        <div v-if="quota" class="mt-5">
          <div class="h-2 rounded-full bg-slate-100 dark:bg-white/10">
            <div class="h-2 rounded-full bg-[rgb(var(--color-brand))]" :style="{ width: `${quotaPercent}%` }" />
          </div>
          <p class="mt-2 text-xs text-slate-500 dark:text-slate-400">剩余 {{ formatBytes(quota.remainingBytes) }}</p>
        </div>
      </div>

      <div class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900">
        <h2 class="text-base font-semibold text-slate-950 dark:text-white">快捷操作</h2>
        <form class="mt-4 flex gap-2" @submit.prevent="createDirectory">
          <input
            v-model.trim="directoryName"
            class="focus-ring min-w-0 flex-1 rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white"
            placeholder="目录名称"
          />
          <button type="submit" class="btn-primary focus-ring inline-flex items-center px-3 text-sm" :disabled="busy">
            <PlusIcon class="size-4" aria-hidden="true" />
            <span class="sr-only">创建目录</span>
          </button>
        </form>
        <form class="mt-3 flex gap-2" @submit.prevent="upload">
          <input
            class="focus-ring min-w-0 flex-1 rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-700 file:mr-3 file:rounded-md file:border-0 file:bg-slate-100 file:px-2 file:py-1 file:text-xs file:font-semibold file:text-slate-700 dark:border-white/10 dark:bg-white/5 dark:text-slate-200 dark:file:bg-white/10 dark:file:text-slate-200"
            type="file"
            @change="onFileChange"
          />
          <button type="submit" class="btn-primary focus-ring inline-flex items-center px-3 text-sm" :disabled="busy || !selectedFile">
            <CloudArrowUpIcon class="size-4" aria-hidden="true" />
            <span class="sr-only">上传</span>
          </button>
        </form>
      </div>
    </section>

    <ErrorState v-if="error" :message="error" @retry="load" />

    <section class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h2 class="text-base font-semibold text-slate-950 dark:text-white">目录树</h2>
          <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">快速跳转到任意目录，也可作为移动目标。</p>
        </div>
        <button type="button" class="text-sm font-semibold text-[rgb(var(--color-brand))] hover:underline" @click="goRoot">回到根目录</button>
      </div>
      <div class="mt-4 flex flex-wrap gap-2">
        <button
          type="button"
          :class="[
            currentParent === 0 ? 'border-[rgb(var(--color-brand))] bg-[rgb(var(--color-brand-soft))] text-[rgb(var(--color-brand-strong))] dark:text-white' : 'border-slate-200 text-slate-700 dark:border-white/10 dark:text-slate-200',
            'focus-ring rounded-md border px-3 py-2 text-sm font-medium',
          ]"
          @click="goRoot"
        >
          根目录
        </button>
        <button
          v-for="item in directoryOptions"
          :key="item.id"
          type="button"
          :class="[
            currentParent === item.id ? 'border-[rgb(var(--color-brand))] bg-[rgb(var(--color-brand-soft))] text-[rgb(var(--color-brand-strong))] dark:text-white' : 'border-slate-200 text-slate-700 dark:border-white/10 dark:text-slate-200',
            'focus-ring rounded-md border px-3 py-2 text-sm font-medium',
          ]"
          @click="goDirectory(item.id)"
        >
          {{ '　'.repeat(item.level) }}{{ item.name }}
        </button>
      </div>
    </section>

    <section class="rounded-md border border-slate-200 bg-white shadow-sm dark:border-white/10 dark:bg-slate-900">
      <div class="flex flex-wrap items-center gap-2 border-b border-slate-200 px-5 py-3 text-sm dark:border-white/10">
        <button type="button" class="font-medium text-[rgb(var(--color-brand))] hover:underline" @click="goRoot">根目录</button>
        <template v-for="item in breadcrumb" :key="item.id">
          <span class="text-slate-300">/</span>
          <button type="button" class="text-slate-600 hover:text-[rgb(var(--color-brand))] dark:text-slate-300" @click="goBreadcrumb(item)">
            {{ item.name }}
          </button>
        </template>
      </div>

      <LoadingState v-if="loading" class="m-5" />
      <EmptyState v-else-if="nodes.length === 0" class="m-5" title="目录为空" description="上传文件或创建目录后会显示在这里。" />
      <div v-else class="overflow-x-auto">
        <table class="min-w-full divide-y divide-slate-200 dark:divide-white/10">
          <thead class="bg-slate-50 dark:bg-white/5">
            <tr>
              <th class="px-5 py-3 text-left text-sm font-semibold text-slate-900 dark:text-white">名称</th>
              <th class="px-5 py-3 text-left text-sm font-semibold text-slate-900 dark:text-white">大小</th>
              <th class="px-5 py-3 text-left text-sm font-semibold text-slate-900 dark:text-white">类型</th>
              <th class="px-5 py-3 text-left text-sm font-semibold text-slate-900 dark:text-white">更新时间</th>
              <th class="px-5 py-3 text-right text-sm font-semibold text-slate-900 dark:text-white">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100 dark:divide-white/10">
            <tr v-for="node in nodes" :key="node.id" class="hover:bg-slate-50 dark:hover:bg-white/[0.03]">
              <td class="px-5 py-4">
                <button class="flex min-w-0 items-center gap-3 text-left" type="button" @click="enterDirectory(node)">
                  <FolderIcon v-if="node.directory" class="size-5 shrink-0 text-amber-500" aria-hidden="true" />
                  <DocumentIcon v-else class="size-5 shrink-0 text-slate-400" aria-hidden="true" />
                  <span class="max-w-xs truncate text-sm font-medium text-slate-950 dark:text-white">{{ node.name }}</span>
                </button>
              </td>
              <td class="px-5 py-4 text-sm text-slate-500 dark:text-slate-400">{{ node.directory ? '-' : formatBytes(node.size) }}</td>
              <td class="px-5 py-4 text-sm text-slate-500 dark:text-slate-400">{{ node.directory ? '目录' : node.mime || '文件' }}</td>
              <td class="px-5 py-4 text-sm text-slate-500 dark:text-slate-400">{{ formatDateTime(node.updatedAt) }}</td>
              <td class="px-5 py-4">
                <div class="flex justify-end gap-2">
                  <button type="button" class="text-sm font-medium text-slate-600 hover:text-[rgb(var(--color-brand))] dark:text-slate-300" @click="rename(node)">
                    重命名
                  </button>
                  <button type="button" class="text-sm font-medium text-slate-600 hover:text-[rgb(var(--color-brand))] dark:text-slate-300" @click="prepareMove(node)">
                    移动
                  </button>
                  <button
                    type="button"
                    class="inline-flex items-center gap-1 text-sm font-medium text-slate-600 hover:text-[rgb(var(--color-brand))] dark:text-slate-300"
                    @click="shareNode = node"
                  >
                    <LinkIcon class="size-4" aria-hidden="true" />
                    分享
                  </button>
                  <button type="button" class="inline-flex items-center gap-1 text-sm font-medium text-rose-600 hover:underline" @click="remove(node)">
                    <TrashIcon class="size-4" aria-hidden="true" />
                    删除
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section class="grid gap-6 xl:grid-cols-2">
      <form
        v-if="moveNode"
        class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900"
        @submit.prevent="submitMove"
      >
        <h2 class="text-base font-semibold text-slate-950 dark:text-white">移动 {{ moveNode.name }}</h2>
        <label class="mt-4 block">
          <span class="text-sm font-medium text-slate-700 dark:text-slate-200">目标目录</span>
          <select
            v-model="targetParentId"
            class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-slate-900 dark:text-white"
          >
            <option :value="0">根目录</option>
            <option v-for="item in directoryOptions.filter((option) => option.id !== moveNode?.id)" :key="item.id" :value="item.id">
              {{ '　'.repeat(item.level) }}{{ item.name }}
            </option>
          </select>
        </label>
        <div class="mt-4 flex gap-3">
          <button type="submit" class="btn-primary focus-ring inline-flex h-10 items-center px-4 text-sm" :disabled="busy">确认移动</button>
          <button type="button" class="text-sm font-semibold text-slate-600 dark:text-slate-300" @click="moveNode = null">取消</button>
        </div>
      </form>

      <form
        v-if="shareNode"
        class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900"
        @submit.prevent="submitShare"
      >
        <h2 class="text-base font-semibold text-slate-950 dark:text-white">分享 {{ shareNode.name }}</h2>
        <div class="mt-4 grid gap-4 sm:grid-cols-2">
          <label>
            <span class="text-sm font-medium text-slate-700 dark:text-slate-200">提取码</span>
            <input
              v-model.trim="shareCode"
              maxlength="16"
              class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white"
              placeholder="留空自动生成"
            />
          </label>
          <label>
            <span class="text-sm font-medium text-slate-700 dark:text-slate-200">过期时间</span>
            <input
              v-model="shareExpireAt"
              type="datetime-local"
              class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white"
            />
          </label>
        </div>
        <div class="mt-4 flex gap-3">
          <button type="submit" class="btn-primary focus-ring inline-flex h-10 items-center px-4 text-sm" :disabled="busy">生成分享</button>
          <button type="button" class="text-sm font-semibold text-slate-600 dark:text-slate-300" @click="shareNode = null">取消</button>
        </div>
        <div v-if="shareResult" class="mt-4 rounded-md bg-slate-50 p-3 text-sm dark:bg-white/5">
          <p class="font-medium text-slate-900 dark:text-white">分享链接已生成</p>
          <div class="mt-2 flex flex-wrap items-center gap-2">
            <a class="text-[rgb(var(--color-brand))] hover:underline" :href="shareUrl(shareResult.token)" target="_blank" rel="noreferrer">
              {{ shareUrl(shareResult.token) }}
            </a>
            <button type="button" class="text-sm font-semibold text-slate-600 hover:underline dark:text-slate-300" @click="copyShare(shareResult.token)">复制</button>
          </div>
          <p class="mt-1 text-xs text-slate-500 dark:text-slate-400">提取码 {{ shareResult.extractCode }}</p>
        </div>
      </form>

      <div class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900">
        <h2 class="text-base font-semibold text-slate-950 dark:text-white">我的分享</h2>
        <ul v-if="shares.length" class="mt-4 divide-y divide-slate-100 dark:divide-white/10">
          <li v-for="share in shares" :key="share.id" class="py-3">
            <div class="flex items-center justify-between gap-3">
              <div class="min-w-0">
                <p class="truncate text-sm font-medium text-slate-950 dark:text-white">节点 {{ share.nodeId }}</p>
                <p class="mt-1 text-xs text-slate-500 dark:text-slate-400">提取码 {{ share.extractCode }} · 浏览 {{ share.viewCount }} 次</p>
              </div>
              <div class="flex shrink-0 items-center gap-3">
                <a class="text-xs font-semibold text-[rgb(var(--color-brand))] hover:underline" :href="shareUrl(share.token)" target="_blank" rel="noreferrer">访问页</a>
                <button type="button" class="text-xs font-semibold text-slate-500 hover:underline dark:text-slate-300" @click="copyShare(share.token)">复制</button>
                <span class="text-xs text-slate-400">{{ share.expireAt ? formatDateTime(share.expireAt) : '长期有效' }}</span>
              </div>
            </div>
          </li>
        </ul>
        <EmptyState v-else class="mt-4" title="暂无分享" description="生成分享后会在这里追踪访问次数。" />
      </div>
    </section>
  </div>
</template>
