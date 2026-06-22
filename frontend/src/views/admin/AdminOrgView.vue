<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ArrowPathIcon, CheckIcon, PencilSquareIcon, PlusIcon, TrashIcon, XMarkIcon } from '@heroicons/vue/24/outline'

import EmptyState from '@/components/state/EmptyState.vue'
import ErrorState from '@/components/state/ErrorState.vue'
import LoadingState from '@/components/state/LoadingState.vue'
import {
  createDepartment,
  createMajor,
  createOrgClass,
  deleteDepartment,
  deleteMajor,
  deleteOrgClass,
  fetchClasses,
  fetchDepartments,
  fetchMajors,
  updateDepartment,
  updateMajor,
  updateOrgClass,
} from '@/lib/services'
import { useAuthStore } from '@/stores/auth'
import type { ClassResponse, DepartmentResponse, MajorResponse } from '@/types/api'

const auth = useAuthStore()
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const message = ref('')
const departments = ref<DepartmentResponse[]>([])
const majors = ref<MajorResponse[]>([])
const classes = ref<ClassResponse[]>([])
const editingDepartmentId = ref(0)
const editingMajorId = ref(0)
const editingClassId = ref(0)

const departmentForm = reactive({ name: '', code: '' })
const majorForm = reactive({ departmentId: 0, name: '', code: '' })
const classForm = reactive({ majorId: 0, name: '', grade: new Date().getFullYear() })
const departmentEditForm = reactive({ name: '', code: '' })
const majorEditForm = reactive({ departmentId: 0, name: '', code: '' })
const classEditForm = reactive({ majorId: 0, name: '', grade: new Date().getFullYear() })

const majorsByDepartment = computed(() => {
  const result = new Map<number, MajorResponse[]>()
  majors.value.forEach((major) => {
    result.set(major.departmentId, [...(result.get(major.departmentId) || []), major])
  })
  return result
})

const classesByMajor = computed(() => {
  const result = new Map<number, ClassResponse[]>()
  classes.value.forEach((item) => {
    result.set(item.majorId, [...(result.get(item.majorId) || []), item])
  })
  return result
})

function departmentName(id: number) {
  return departments.value.find((item) => item.id === id)?.name || '-'
}

function majorName(id: number) {
  return majors.value.find((item) => item.id === id)?.name || '-'
}

function canManage() {
  return auth.hasPermission('org:manage')
}

function startDepartmentEdit(item: DepartmentResponse) {
  editingDepartmentId.value = item.id
  departmentEditForm.name = item.name
  departmentEditForm.code = item.code || ''
}

function startMajorEdit(item: MajorResponse) {
  editingMajorId.value = item.id
  majorEditForm.departmentId = item.departmentId
  majorEditForm.name = item.name
  majorEditForm.code = item.code || ''
}

function startClassEdit(item: ClassResponse) {
  editingClassId.value = item.id
  classEditForm.majorId = item.majorId
  classEditForm.name = item.name
  classEditForm.grade = item.grade || new Date().getFullYear()
}

function cancelDepartmentEdit() {
  editingDepartmentId.value = 0
  departmentEditForm.name = ''
  departmentEditForm.code = ''
}

function cancelMajorEdit() {
  editingMajorId.value = 0
  majorEditForm.departmentId = 0
  majorEditForm.name = ''
  majorEditForm.code = ''
}

function cancelClassEdit() {
  editingClassId.value = 0
  classEditForm.majorId = 0
  classEditForm.name = ''
  classEditForm.grade = new Date().getFullYear()
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [departmentList, majorList, classList] = await Promise.all([fetchDepartments(), fetchMajors(), fetchClasses()])
    departments.value = departmentList
    majors.value = majorList
    classes.value = classList
    majorForm.departmentId = majorForm.departmentId || departmentList[0]?.id || 0
    classForm.majorId = classForm.majorId || majorList[0]?.id || 0
  } catch (e) {
    error.value = e instanceof Error ? e.message : '组织数据加载失败'
  } finally {
    loading.value = false
  }
}

async function submitDepartment() {
  if (!departmentForm.name.trim()) return
  saving.value = true
  try {
    await createDepartment({ name: departmentForm.name.trim(), code: departmentForm.code || undefined })
    departmentForm.name = ''
    departmentForm.code = ''
    message.value = '院系已创建'
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '院系创建失败'
  } finally {
    saving.value = false
  }
}

async function submitDepartmentEdit(item: DepartmentResponse) {
  if (!departmentEditForm.name.trim()) return
  saving.value = true
  error.value = ''
  try {
    await updateDepartment(item.id, {
      name: departmentEditForm.name.trim(),
      code: departmentEditForm.code || undefined,
    })
    cancelDepartmentEdit()
    message.value = '院系已更新'
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '院系更新失败'
  } finally {
    saving.value = false
  }
}

async function submitMajor() {
  if (!majorForm.departmentId || !majorForm.name.trim()) return
  saving.value = true
  try {
    await createMajor({
      departmentId: majorForm.departmentId,
      name: majorForm.name.trim(),
      code: majorForm.code || undefined,
    })
    majorForm.name = ''
    majorForm.code = ''
    message.value = '专业已创建'
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '专业创建失败'
  } finally {
    saving.value = false
  }
}

async function submitMajorEdit(item: MajorResponse) {
  if (!majorEditForm.departmentId || !majorEditForm.name.trim()) return
  saving.value = true
  error.value = ''
  try {
    await updateMajor(item.id, {
      departmentId: majorEditForm.departmentId,
      name: majorEditForm.name.trim(),
      code: majorEditForm.code || undefined,
    })
    cancelMajorEdit()
    message.value = '专业已更新'
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '专业更新失败'
  } finally {
    saving.value = false
  }
}

async function submitClass() {
  if (!classForm.majorId || !classForm.name.trim()) return
  saving.value = true
  try {
    await createOrgClass({
      majorId: classForm.majorId,
      name: classForm.name.trim(),
      grade: classForm.grade || undefined,
    })
    classForm.name = ''
    message.value = '班级已创建'
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '班级创建失败'
  } finally {
    saving.value = false
  }
}

async function submitClassEdit(item: ClassResponse) {
  if (!classEditForm.majorId || !classEditForm.name.trim()) return
  saving.value = true
  error.value = ''
  try {
    await updateOrgClass(item.id, {
      majorId: classEditForm.majorId,
      name: classEditForm.name.trim(),
      grade: classEditForm.grade || undefined,
    })
    cancelClassEdit()
    message.value = '班级已更新'
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '班级更新失败'
  } finally {
    saving.value = false
  }
}

async function removeDepartment(item: DepartmentResponse) {
  if (!window.confirm(`确认删除院系“${item.name}”？`)) return
  saving.value = true
  try {
    await deleteDepartment(item.id)
    message.value = '院系已删除'
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '院系删除失败'
  } finally {
    saving.value = false
  }
}

async function removeMajor(item: MajorResponse) {
  if (!window.confirm(`确认删除专业“${item.name}”？`)) return
  saving.value = true
  try {
    await deleteMajor(item.id)
    message.value = '专业已删除'
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '专业删除失败'
  } finally {
    saving.value = false
  }
}

async function removeClass(item: ClassResponse) {
  if (!window.confirm(`确认删除班级“${item.name}”？`)) return
  saving.value = true
  try {
    await deleteOrgClass(item.id)
    message.value = '班级已删除'
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '班级删除失败'
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="space-y-6">
    <section class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h2 class="text-base font-semibold text-slate-950 dark:text-white">组织架构</h2>
          <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
            {{ departments.length }} 个院系，{{ majors.length }} 个专业，{{ classes.length }} 个班级。
          </p>
        </div>
        <button
          type="button"
          class="focus-ring inline-flex h-10 items-center justify-center gap-2 rounded-md border border-slate-200 bg-white px-3 text-sm font-semibold text-slate-700 hover:bg-slate-50 dark:border-white/10 dark:bg-white/5 dark:text-slate-200 dark:hover:bg-white/10"
          @click="load"
        >
          <ArrowPathIcon class="size-4" aria-hidden="true" />
          刷新
        </button>
      </div>
    </section>

    <p v-if="message" class="rounded-md bg-emerald-50 px-3 py-2 text-sm text-emerald-700 dark:bg-emerald-400/10 dark:text-emerald-200">
      {{ message }}
    </p>
    <ErrorState v-if="error" :message="error" @retry="load" />
    <LoadingState v-if="loading" />

    <template v-else>
      <section v-if="canManage()" class="grid gap-6 xl:grid-cols-3">
        <form class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900" @submit.prevent="submitDepartment">
          <h3 class="text-sm font-semibold text-slate-950 dark:text-white">新增院系</h3>
          <label class="mt-4 block">
            <span class="text-sm font-medium text-slate-700 dark:text-slate-200">名称</span>
            <input v-model.trim="departmentForm.name" class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white" required />
          </label>
          <label class="mt-4 block">
            <span class="text-sm font-medium text-slate-700 dark:text-slate-200">编码</span>
            <input v-model.trim="departmentForm.code" class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white" />
          </label>
          <button type="submit" class="btn-primary focus-ring mt-4 inline-flex h-10 items-center gap-2 px-4 text-sm" :disabled="saving">
            <PlusIcon class="size-4" aria-hidden="true" />
            创建院系
          </button>
        </form>

        <form class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900" @submit.prevent="submitMajor">
          <h3 class="text-sm font-semibold text-slate-950 dark:text-white">新增专业</h3>
          <label class="mt-4 block">
            <span class="text-sm font-medium text-slate-700 dark:text-slate-200">院系</span>
            <select v-model="majorForm.departmentId" class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-slate-900 dark:text-white">
              <option :value="0">请选择</option>
              <option v-for="department in departments" :key="department.id" :value="department.id">{{ department.name }}</option>
            </select>
          </label>
          <label class="mt-4 block">
            <span class="text-sm font-medium text-slate-700 dark:text-slate-200">名称</span>
            <input v-model.trim="majorForm.name" class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white" required />
          </label>
          <label class="mt-4 block">
            <span class="text-sm font-medium text-slate-700 dark:text-slate-200">编码</span>
            <input v-model.trim="majorForm.code" class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white" />
          </label>
          <button type="submit" class="btn-primary focus-ring mt-4 inline-flex h-10 items-center gap-2 px-4 text-sm" :disabled="saving || !majorForm.departmentId">
            <PlusIcon class="size-4" aria-hidden="true" />
            创建专业
          </button>
        </form>

        <form class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900" @submit.prevent="submitClass">
          <h3 class="text-sm font-semibold text-slate-950 dark:text-white">新增班级</h3>
          <label class="mt-4 block">
            <span class="text-sm font-medium text-slate-700 dark:text-slate-200">专业</span>
            <select v-model="classForm.majorId" class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-slate-900 dark:text-white">
              <option :value="0">请选择</option>
              <option v-for="major in majors" :key="major.id" :value="major.id">{{ major.name }}</option>
            </select>
          </label>
          <label class="mt-4 block">
            <span class="text-sm font-medium text-slate-700 dark:text-slate-200">名称</span>
            <input v-model.trim="classForm.name" class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white" required />
          </label>
          <label class="mt-4 block">
            <span class="text-sm font-medium text-slate-700 dark:text-slate-200">年级</span>
            <input v-model.number="classForm.grade" type="number" class="focus-ring mt-2 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-white/5 dark:text-white" />
          </label>
          <button type="submit" class="btn-primary focus-ring mt-4 inline-flex h-10 items-center gap-2 px-4 text-sm" :disabled="saving || !classForm.majorId">
            <PlusIcon class="size-4" aria-hidden="true" />
            创建班级
          </button>
        </form>
      </section>

      <section class="grid gap-6 xl:grid-cols-[minmax(280px,0.9fr)_minmax(0,1.1fr)]">
        <div class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900">
          <h3 class="text-base font-semibold text-slate-950 dark:text-white">院系与专业</h3>
          <EmptyState v-if="departments.length === 0" class="mt-4" title="暂无院系" description="创建院系后可继续添加专业。" />
          <div v-else class="mt-4 space-y-4">
            <article v-for="department in departments" :key="department.id" class="rounded-md border border-slate-200 p-4 dark:border-white/10">
              <div class="flex items-start justify-between gap-3">
                <div class="min-w-0">
                  <h4 class="truncate text-sm font-semibold text-slate-950 dark:text-white">{{ department.name }}</h4>
                  <p class="mt-1 text-xs text-slate-500 dark:text-slate-400">{{ department.code || '-' }}</p>
                </div>
                <div v-if="canManage()" class="flex shrink-0 items-center gap-2">
                  <button type="button" class="text-[rgb(var(--color-brand))] hover:underline" :disabled="saving" @click="startDepartmentEdit(department)">
                    <PencilSquareIcon class="size-4" aria-hidden="true" />
                    <span class="sr-only">编辑院系</span>
                  </button>
                  <button type="button" class="text-rose-600 hover:underline" :disabled="saving" @click="removeDepartment(department)">
                    <TrashIcon class="size-4" aria-hidden="true" />
                    <span class="sr-only">删除院系</span>
                  </button>
                </div>
              </div>
              <form v-if="editingDepartmentId === department.id" class="mt-3 grid gap-2 sm:grid-cols-[minmax(0,1fr)_140px_auto]" @submit.prevent="submitDepartmentEdit(department)">
                <input v-model.trim="departmentEditForm.name" class="focus-ring rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-slate-900 dark:text-white" required />
                <input v-model.trim="departmentEditForm.code" class="focus-ring rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-slate-900 dark:text-white" />
                <div class="flex gap-2">
                  <button type="submit" class="focus-ring inline-flex h-9 items-center rounded-md bg-[rgb(var(--color-brand))] px-3 text-sm font-semibold text-white" :disabled="saving">
                    <CheckIcon class="size-4" aria-hidden="true" />
                  </button>
                  <button type="button" class="focus-ring inline-flex h-9 items-center rounded-md border border-slate-200 px-3 text-sm font-semibold text-slate-700 dark:border-white/10 dark:text-slate-200" @click="cancelDepartmentEdit">
                    <XMarkIcon class="size-4" aria-hidden="true" />
                  </button>
                </div>
              </form>
              <ul class="mt-3 space-y-2">
                <li
                  v-for="major in majorsByDepartment.get(department.id) || []"
                  :key="major.id"
                  class="rounded-md bg-slate-50 px-3 py-2 text-sm dark:bg-white/5"
                >
                  <div class="flex items-center justify-between gap-3">
                    <span class="truncate text-slate-700 dark:text-slate-200">{{ major.name }} · {{ major.code || '-' }}</span>
                    <div v-if="canManage()" class="flex shrink-0 items-center gap-2">
                      <button type="button" class="text-[rgb(var(--color-brand))] hover:underline" :disabled="saving" @click="startMajorEdit(major)">
                        <PencilSquareIcon class="size-4" aria-hidden="true" />
                        <span class="sr-only">编辑专业</span>
                      </button>
                      <button type="button" class="text-rose-600 hover:underline" :disabled="saving" @click="removeMajor(major)">
                        <TrashIcon class="size-4" aria-hidden="true" />
                        <span class="sr-only">删除专业</span>
                      </button>
                    </div>
                  </div>
                  <form v-if="editingMajorId === major.id" class="mt-3 grid gap-2 sm:grid-cols-[140px_minmax(0,1fr)_120px_auto]" @submit.prevent="submitMajorEdit(major)">
                    <select v-model="majorEditForm.departmentId" class="focus-ring rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-slate-900 dark:text-white">
                      <option v-for="option in departments" :key="option.id" :value="option.id">{{ option.name }}</option>
                    </select>
                    <input v-model.trim="majorEditForm.name" class="focus-ring rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-slate-900 dark:text-white" required />
                    <input v-model.trim="majorEditForm.code" class="focus-ring rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-slate-900 dark:text-white" />
                    <div class="flex gap-2">
                      <button type="submit" class="focus-ring inline-flex h-9 items-center rounded-md bg-[rgb(var(--color-brand))] px-3 text-sm font-semibold text-white" :disabled="saving">
                        <CheckIcon class="size-4" aria-hidden="true" />
                      </button>
                      <button type="button" class="focus-ring inline-flex h-9 items-center rounded-md border border-slate-200 px-3 text-sm font-semibold text-slate-700 dark:border-white/10 dark:text-slate-200" @click="cancelMajorEdit">
                        <XMarkIcon class="size-4" aria-hidden="true" />
                      </button>
                    </div>
                  </form>
                </li>
              </ul>
            </article>
          </div>
        </div>

        <div class="rounded-md border border-slate-200 bg-white p-5 shadow-sm dark:border-white/10 dark:bg-slate-900">
          <h3 class="text-base font-semibold text-slate-950 dark:text-white">班级列表</h3>
          <EmptyState v-if="classes.length === 0" class="mt-4" title="暂无班级" description="创建班级后可用于学生账号归属。" />
          <div v-else class="mt-4 overflow-x-auto">
            <table class="min-w-full divide-y divide-slate-200 dark:divide-white/10">
              <thead class="bg-slate-50 dark:bg-white/5">
                <tr>
                  <th class="px-4 py-3 text-left text-sm font-semibold text-slate-900 dark:text-white">班级</th>
                  <th class="px-4 py-3 text-left text-sm font-semibold text-slate-900 dark:text-white">专业</th>
                  <th class="px-4 py-3 text-left text-sm font-semibold text-slate-900 dark:text-white">院系</th>
                  <th class="px-4 py-3 text-left text-sm font-semibold text-slate-900 dark:text-white">年级</th>
                  <th class="px-4 py-3 text-right text-sm font-semibold text-slate-900 dark:text-white">操作</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100 dark:divide-white/10">
                <template v-for="item in classes" :key="item.id">
                  <tr>
                    <td class="px-4 py-3 text-sm font-medium text-slate-950 dark:text-white">{{ item.name }}</td>
                    <td class="px-4 py-3 text-sm text-slate-500 dark:text-slate-400">{{ majorName(item.majorId) }}</td>
                    <td class="px-4 py-3 text-sm text-slate-500 dark:text-slate-400">
                      {{ departmentName(majors.find((major) => major.id === item.majorId)?.departmentId || 0) }}
                    </td>
                    <td class="px-4 py-3 text-sm text-slate-500 dark:text-slate-400">{{ item.grade || '-' }}</td>
                    <td class="px-4 py-3 text-right">
                      <div v-if="canManage()" class="flex justify-end gap-2">
                        <button type="button" class="inline-flex items-center gap-1 text-sm font-semibold text-[rgb(var(--color-brand))] hover:underline" :disabled="saving" @click="startClassEdit(item)">
                          <PencilSquareIcon class="size-4" aria-hidden="true" />
                          编辑
                        </button>
                        <button type="button" class="inline-flex items-center gap-1 text-sm font-semibold text-rose-600 hover:underline" :disabled="saving" @click="removeClass(item)">
                          <TrashIcon class="size-4" aria-hidden="true" />
                          删除
                        </button>
                      </div>
                    </td>
                  </tr>
                  <tr v-if="editingClassId === item.id" class="bg-slate-50/80 dark:bg-white/5">
                    <td colspan="5" class="px-4 py-4">
                      <form class="grid gap-3 md:grid-cols-[180px_minmax(0,1fr)_120px_auto]" @submit.prevent="submitClassEdit(item)">
                        <select v-model="classEditForm.majorId" class="focus-ring rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-slate-900 dark:text-white">
                          <option v-for="major in majors" :key="major.id" :value="major.id">{{ major.name }}</option>
                        </select>
                        <input v-model.trim="classEditForm.name" class="focus-ring rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-slate-900 dark:text-white" required />
                        <input v-model.number="classEditForm.grade" type="number" class="focus-ring rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 dark:border-white/10 dark:bg-slate-900 dark:text-white" />
                        <div class="flex gap-2">
                          <button type="submit" class="focus-ring inline-flex h-10 items-center rounded-md bg-[rgb(var(--color-brand))] px-3 text-sm font-semibold text-white" :disabled="saving">
                            <CheckIcon class="size-4" aria-hidden="true" />
                          </button>
                          <button type="button" class="focus-ring inline-flex h-10 items-center rounded-md border border-slate-200 px-3 text-sm font-semibold text-slate-700 dark:border-white/10 dark:text-slate-200" @click="cancelClassEdit">
                            <XMarkIcon class="size-4" aria-hidden="true" />
                          </button>
                        </div>
                      </form>
                    </td>
                  </tr>
                </template>
              </tbody>
            </table>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>
