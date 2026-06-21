<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ArrowPathIcon, PlusIcon, TrashIcon } from '@heroicons/vue/24/outline'

import EmptyState from '@/components/state/EmptyState.vue'
import ErrorState from '@/components/state/ErrorState.vue'
import LoadingState from '@/components/state/LoadingState.vue'
import { formatDateTime, roleLabel } from '@/lib/format'
import { createUser, deleteUser, fetchClasses, fetchRoles, fetchUsers, updateUser } from '@/lib/services'
import { useAuthStore } from '@/stores/auth'
import type { ClassResponse, RoleResponse, UserProfile } from '@/types/api'

const auth = useAuthStore()
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const message = ref('')
const keyword = ref('')
const status = ref<number | ''>('')
const pageNo = ref(1)
const pageSize = 12
const total = ref(0)
const users = ref<UserProfile[]>([])
const roles = ref<RoleResponse[]>([])
const classes = ref<ClassResponse[]>([])
const creating = ref(false)

const form = reactive({
  username: '',
  password: 'User@1234',
  realName: '',
  email: '',
  phone: '',
  gender: 0,
  classId: 0,
  roleCodes: ['STUDENT'],
})

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

function statusLabel(value?: number) {
  if (value === 0) return '禁用'
  if (value === 2) return '锁定'
  return '正常'
}

function statusClass(value?: number) {
  if (value === 0) return 'bg-rose-50 text-rose-700 dark:bg-rose-400/10 dark:text-rose-200'
  if (value === 2) return 'bg-amber-50 text-amber-700 dark:bg-amber-400/10 dark:text-amber-200'
  return 'bg-emerald-50 text-emerald-700 dark:bg-emerald-400/10 dark:text-emerald-200'
}

function className(id?: number) {
  return classes.value.find((item) => item.id === id)?.name || '-'
}

function toggleRole(code: string) {
  if (form.roleCodes.includes(code)) {
    form.roleCodes = form.roleCodes.filter((item) => item !== code)
  } else {
    form.roleCodes = [...form.roleCodes, code]
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [page, roleList, classList] = await Promise.all([
      fetchUsers({
        keyword: keyword.value || undefined,
        status: status.value === '' ? undefined : status.value,
        pageNo: pageNo.value,
        pageSize,
      }),
      fetchRoles().catch(() => []),
      fetchClasses().catch(() => []),
    ])
    users.value = page.records
    total.value = page.total
    roles.value = roleList
    classes.value = classList
  } catch (e) {
    error.value = e instanceof Error ? e.message : '用户加载失败'
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!form.username.trim() || !form.password || form.roleCodes.length === 0) {
    error.value = '请填写账号、密码并至少选择一个角色'
    return
  }
  saving.value = true
  error.value = ''
  message.value = ''
  try {
    await createUser({
      username: form.username.trim(),
      password: form.password,
      realName: form.realName || undefined,
      email: form.email || undefined,
      phone: form.phone || undefined,
      gender: form.gender,
      classId: form.classId || undefined,
      roleCodes: form.roleCodes,
    })
    form.username = ''
    form.password = 'User@1234'
    form.realName = ''
    form.email = ''
    form.phone = ''
    form.gender = 0
    form.classId = 0
    form.roleCodes = ['STUDENT']
    creating.value = false
    message.value = '用户已创建'
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '用户创建失败'
  } finally {
    saving.value = false
  }
}

async function setStatus(user: UserProfile, nextStatus: number) {
  saving.value = true
  try {
    await updateUser(user.id, { status: nextStatus })
    message.value = `${user.username} 已${nextStatus === 1 ? '启用' : '禁用'}`
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '状态更新失败'
  } finally {
    saving.value = false
  }
}

async function remove(user: UserProfile) {
  if (!window.confirm(`确认删除用户“${user.username}”？`)) {
    return
  }
  saving.value = true
  try {
    await deleteUser(user.id)
    message.value = '用户已删除'
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '用户删除失败'
  } finally {
    saving.value = false
  }
}

async function changePage(delta: number) {
  pageNo.value = Math.min(totalPages.value, Math.max(1, pageNo.value + delta))
  await load()
}

onMounted(load)
</script>

<template>
  <div class="space-y-6">
    <section class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <h2 class="text-base font-semibold text-slate-950 dark:text-white">用户管理</h2>
          <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">共 {{ total }} 个用户，按账号、姓名、邮箱或状态筛选。</p>
        </div>
        <div class="flex flex-col gap-3 sm:flex-row">
          <input
            v-model.trim="keyword"
            class="focus-ring h-10 rounded-md border border-slate-300 bg-white px-3 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white"
            placeholder="搜索用户"
            @keyup.enter="load"
          />
          <select
            v-model="status"
            class="focus-ring h-10 rounded-md border border-slate-300 bg-white px-3 text-sm text-slate-950 dark:border-white/10 dark:bg-slate-900 dark:text-white"
            @change="load"
          >
            <option value="">全部状态</option>
            <option :value="1">正常</option>
            <option :value="0">禁用</option>
            <option :value="2">锁定</option>
          </select>
          <button
            type="button"
            class="focus-ring inline-flex h-10 items-center justify-center gap-2 rounded-md border border-slate-200 bg-white px-3 text-sm font-semibold text-slate-700 hover:bg-slate-50 dark:border-white/10 dark:bg-white/5 dark:text-slate-200 dark:hover:bg-white/10"
            @click="load"
          >
            <ArrowPathIcon class="size-4" aria-hidden="true" />
            刷新
          </button>
          <button
            v-if="auth.hasPermission('user:create')"
            type="button"
            class="btn-primary focus-ring inline-flex h-10 items-center justify-center gap-2 px-3 text-sm"
            @click="creating = !creating"
          >
            <PlusIcon class="size-4" aria-hidden="true" />
            新建用户
          </button>
        </div>
      </div>

      <form v-if="creating" class="mt-5 grid gap-4 border-t border-slate-200 pt-5 md:grid-cols-3 dark:border-white/10" @submit.prevent="submit">
        <label>
          <span class="text-sm font-medium text-slate-700 dark:text-slate-200">账号</span>
          <input v-model.trim="form.username" class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white" required />
        </label>
        <label>
          <span class="text-sm font-medium text-slate-700 dark:text-slate-200">初始密码</span>
          <input v-model="form.password" type="password" class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white" required />
        </label>
        <label>
          <span class="text-sm font-medium text-slate-700 dark:text-slate-200">真实姓名</span>
          <input v-model.trim="form.realName" class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white" />
        </label>
        <label>
          <span class="text-sm font-medium text-slate-700 dark:text-slate-200">邮箱</span>
          <input v-model.trim="form.email" type="email" class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white" />
        </label>
        <label>
          <span class="text-sm font-medium text-slate-700 dark:text-slate-200">手机号</span>
          <input v-model.trim="form.phone" class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white" />
        </label>
        <label>
          <span class="text-sm font-medium text-slate-700 dark:text-slate-200">班级</span>
          <select v-model.number="form.classId" class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-slate-900 dark:text-white">
            <option :value="0">不分配</option>
            <option v-for="item in classes" :key="item.id" :value="item.id">{{ item.name }}</option>
          </select>
        </label>
        <div class="md:col-span-3">
          <p class="text-sm font-medium text-slate-700 dark:text-slate-200">角色</p>
          <div class="mt-2 flex flex-wrap gap-2">
            <button
              v-for="role in roles"
              :key="role.code"
              type="button"
              :class="[
                form.roleCodes.includes(role.code)
                  ? 'border-[rgb(var(--color-brand))] bg-[rgb(var(--color-brand-soft))] text-[rgb(var(--color-brand-strong))] dark:text-white'
                  : 'border-slate-200 bg-white text-slate-700 dark:border-white/10 dark:bg-white/5 dark:text-slate-200',
                'focus-ring rounded-md border px-3 py-2 text-sm font-medium',
              ]"
              @click="toggleRole(role.code)"
            >
              {{ role.name }}
            </button>
          </div>
        </div>
        <div class="md:col-span-3">
          <button type="submit" class="btn-primary focus-ring inline-flex h-10 items-center px-4 text-sm" :disabled="saving">保存用户</button>
        </div>
      </form>
    </section>

    <p v-if="message" class="rounded-md bg-emerald-50 px-3 py-2 text-sm text-emerald-700 dark:bg-emerald-400/10 dark:text-emerald-200">
      {{ message }}
    </p>
    <ErrorState v-if="error" :message="error" @retry="load" />

    <LoadingState v-if="loading" />
    <EmptyState v-else-if="users.length === 0" title="暂无用户" description="调整筛选条件或创建新用户。" />
    <section v-else class="overflow-hidden rounded-md border border-slate-200 bg-white shadow-sm dark:border-white/10 dark:bg-slate-900">
      <div class="overflow-x-auto">
        <table class="min-w-full divide-y divide-slate-200 dark:divide-white/10">
          <thead class="bg-slate-50 dark:bg-white/5">
            <tr>
              <th class="px-5 py-3 text-left text-sm font-semibold text-slate-900 dark:text-white">用户</th>
              <th class="px-5 py-3 text-left text-sm font-semibold text-slate-900 dark:text-white">角色</th>
              <th class="px-5 py-3 text-left text-sm font-semibold text-slate-900 dark:text-white">班级</th>
              <th class="px-5 py-3 text-left text-sm font-semibold text-slate-900 dark:text-white">状态</th>
              <th class="px-5 py-3 text-left text-sm font-semibold text-slate-900 dark:text-white">最近登录</th>
              <th class="px-5 py-3 text-right text-sm font-semibold text-slate-900 dark:text-white">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100 dark:divide-white/10">
            <tr v-for="user in users" :key="user.id">
              <td class="px-5 py-4">
                <p class="text-sm font-medium text-slate-950 dark:text-white">{{ user.realName || user.username }}</p>
                <p class="mt-1 text-xs text-slate-500 dark:text-slate-400">{{ user.username }} · {{ user.email || '-' }}</p>
              </td>
              <td class="px-5 py-4">
                <div class="flex flex-wrap gap-1">
                  <span v-for="role in user.roles || []" :key="role" class="rounded-md bg-slate-100 px-2 py-1 text-xs text-slate-600 dark:bg-white/10 dark:text-slate-300">
                    {{ roleLabel(role) }}
                  </span>
                </div>
              </td>
              <td class="px-5 py-4 text-sm text-slate-500 dark:text-slate-400">{{ className(user.classId) }}</td>
              <td class="px-5 py-4">
                <span :class="[statusClass(user.status), 'rounded-md px-2 py-1 text-xs font-medium']">{{ statusLabel(user.status) }}</span>
              </td>
              <td class="px-5 py-4 text-sm text-slate-500 dark:text-slate-400">{{ formatDateTime(user.lastLoginAt) }}</td>
              <td class="px-5 py-4">
                <div class="flex justify-end gap-2">
                  <button
                    v-if="auth.hasPermission('user:update')"
                    type="button"
                    class="text-sm font-semibold text-[rgb(var(--color-brand))] hover:underline"
                    :disabled="saving"
                    @click="setStatus(user, user.status === 1 ? 0 : 1)"
                  >
                    {{ user.status === 1 ? '禁用' : '启用' }}
                  </button>
                  <button
                    v-if="auth.hasPermission('user:delete')"
                    type="button"
                    class="inline-flex items-center gap-1 text-sm font-semibold text-rose-600 hover:underline"
                    :disabled="saving"
                    @click="remove(user)"
                  >
                    <TrashIcon class="size-4" aria-hidden="true" />
                    删除
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="flex items-center justify-between border-t border-slate-200 px-5 py-3 text-sm dark:border-white/10">
        <span class="text-slate-500 dark:text-slate-400">第 {{ pageNo }} / {{ totalPages }} 页</span>
        <div class="flex gap-2">
          <button class="focus-ring rounded-md border border-slate-200 px-3 py-1.5 font-medium text-slate-700 disabled:opacity-50 dark:border-white/10 dark:text-slate-200" :disabled="pageNo <= 1" @click="changePage(-1)">上一页</button>
          <button class="focus-ring rounded-md border border-slate-200 px-3 py-1.5 font-medium text-slate-700 disabled:opacity-50 dark:border-white/10 dark:text-slate-200" :disabled="pageNo >= totalPages" @click="changePage(1)">下一页</button>
        </div>
      </div>
    </section>
  </div>
</template>
