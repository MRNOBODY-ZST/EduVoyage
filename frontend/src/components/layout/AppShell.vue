<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import {
  Dialog,
  DialogPanel,
  Menu,
  MenuButton,
  MenuItem,
  MenuItems,
  TransitionChild,
  TransitionRoot,
} from '@headlessui/vue'
import {
  AcademicCapIcon,
  Bars3Icon,
  BuildingLibraryIcon,
  ChartBarSquareIcon,
  ChatBubbleLeftRightIcon,
  Cog6ToothIcon,
  FolderIcon,
  HomeIcon,
  MagnifyingGlassIcon,
  RectangleStackIcon,
  ServerStackIcon,
  UsersIcon,
  XMarkIcon,
} from '@heroicons/vue/24/outline'
import { ChevronDownIcon } from '@heroicons/vue/20/solid'
import type { Component } from 'vue'

import LogoMark from '@/components/layout/LogoMark.vue'
import NotificationBell from '@/components/nav/NotificationBell.vue'
import ThemeSwitcher from '@/components/nav/ThemeSwitcher.vue'
import { roleLabel } from '@/lib/format'
import { useAuthStore } from '@/stores/auth'

interface NavigationItem {
  name: string
  to: string
  icon: Component
  permission?: string
  roles?: string[]
}

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const sidebarOpen = ref(false)

const groups: Array<{ name: string; items: NavigationItem[] }> = [
  {
    name: '工作台',
    items: [
      { name: '学习仪表盘', to: '/student/dashboard', icon: HomeIcon, permission: 'analytics:view', roles: ['STUDENT'] },
      { name: '课程工作台', to: '/teacher/courses', icon: RectangleStackIcon, permission: 'course:read', roles: ['TEACHER'] },
      { name: '教学分析', to: '/teacher/analytics', icon: ChartBarSquareIcon, permission: 'analytics:view', roles: ['TEACHER'] },
      { name: '运营大盘', to: '/admin/dashboard', icon: ServerStackIcon, permission: 'analytics:view', roles: ['ADMIN'] },
      { name: '用户管理', to: '/admin/users', icon: UsersIcon, permission: 'user:read', roles: ['ADMIN'] },
      { name: '组织架构', to: '/admin/org', icon: BuildingLibraryIcon, permission: 'org:manage', roles: ['ADMIN'] },
    ],
  },
  {
    name: '学习协同',
    items: [
      { name: '课程中心', to: '/courses', icon: AcademicCapIcon },
      { name: '课程网盘', to: '/drive', icon: FolderIcon, permission: 'drive:read' },
      { name: '课程讨论', to: '/discussions', icon: ChatBubbleLeftRightIcon, permission: 'discussion:read' },
    ],
  },
]

const visibleGroups = computed(() =>
  groups
    .map((group) => ({
      ...group,
      items: group.items.filter((item) => {
        const roleAllowed = !item.roles?.length || item.roles.some((role) => auth.hasRole(role))
        const permissionAllowed = !item.permission || auth.hasPermission(item.permission)
        return roleAllowed && permissionAllowed
      }),
    }))
    .filter((group) => group.items.length > 0),
)

const currentTitle = computed(() => String(route.meta.title || '工作台'))
const userInitial = computed(() => (auth.displayName || 'E').slice(0, 1).toUpperCase())

function isActive(item: NavigationItem) {
  return route.path === item.to || route.path.startsWith(`${item.to}/`)
}

async function logout() {
  auth.logout()
  await router.push('/login')
}
</script>

<template>
  <div class="min-h-screen bg-[rgb(var(--color-bg))] text-slate-950 dark:text-slate-100">
    <TransitionRoot as="template" :show="sidebarOpen">
      <Dialog class="relative z-50 lg:hidden" @close="sidebarOpen = false">
        <TransitionChild
          as="template"
          enter="transition-opacity ease-linear duration-200"
          enter-from="opacity-0"
          enter-to="opacity-100"
          leave="transition-opacity ease-linear duration-200"
          leave-from="opacity-100"
          leave-to="opacity-0"
        >
          <div class="fixed inset-0 bg-slate-950/70" />
        </TransitionChild>

        <div class="fixed inset-0 flex">
          <TransitionChild
            as="template"
            enter="transition ease-in-out duration-200 transform"
            enter-from="-translate-x-full"
            enter-to="translate-x-0"
            leave="transition ease-in-out duration-200 transform"
            leave-from="translate-x-0"
            leave-to="-translate-x-full"
          >
            <DialogPanel class="relative mr-16 flex w-full max-w-xs flex-1">
              <TransitionChild
                as="template"
                enter="ease-in-out duration-200"
                enter-from="opacity-0"
                enter-to="opacity-100"
                leave="ease-in-out duration-200"
                leave-from="opacity-100"
                leave-to="opacity-0"
              >
                <div class="absolute left-full top-0 flex w-16 justify-center pt-5">
                  <button type="button" class="-m-2.5 p-2.5 text-white" title="关闭侧栏" @click="sidebarOpen = false">
                    <XMarkIcon class="size-6" aria-hidden="true" />
                    <span class="sr-only">关闭侧栏</span>
                  </button>
                </div>
              </TransitionChild>
              <aside class="flex grow flex-col overflow-y-auto bg-white px-5 pb-4 shadow-xl dark:bg-slate-900">
                <div class="flex h-16 shrink-0 items-center">
                  <LogoMark />
                </div>
                <nav class="mt-2 flex flex-1 flex-col gap-7">
                  <div v-for="group in visibleGroups" :key="group.name">
                    <p class="px-2 text-xs font-semibold text-slate-400">{{ group.name }}</p>
                    <ul role="list" class="mt-2 space-y-1">
                      <li v-for="item in group.items" :key="item.to">
                        <RouterLink
                          :to="item.to"
                          :class="[
                            isActive(item)
                              ? 'bg-[rgb(var(--color-brand-soft))] text-[rgb(var(--color-brand-strong))] dark:text-white'
                              : 'text-slate-700 hover:bg-slate-50 hover:text-[rgb(var(--color-brand))] dark:text-slate-300 dark:hover:bg-white/5 dark:hover:text-white',
                            'group flex items-center gap-3 rounded-md px-2 py-2 text-sm font-semibold',
                          ]"
                          @click="sidebarOpen = false"
                        >
                          <component
                            :is="item.icon"
                            :class="[
                              isActive(item) ? 'text-[rgb(var(--color-brand))] dark:text-white' : 'text-slate-400 group-hover:text-[rgb(var(--color-brand))]',
                              'size-5 shrink-0',
                            ]"
                            aria-hidden="true"
                          />
                          {{ item.name }}
                        </RouterLink>
                      </li>
                    </ul>
                  </div>
                  <RouterLink
                    to="/settings"
                    class="mt-auto flex items-center gap-3 rounded-md px-2 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 hover:text-[rgb(var(--color-brand))] dark:text-slate-300 dark:hover:bg-white/5 dark:hover:text-white"
                    @click="sidebarOpen = false"
                  >
                    <Cog6ToothIcon class="size-5 text-slate-400" aria-hidden="true" />
                    偏好设置
                  </RouterLink>
                </nav>
              </aside>
            </DialogPanel>
          </TransitionChild>
        </div>
      </Dialog>
    </TransitionRoot>

    <aside class="hidden lg:fixed lg:inset-y-0 lg:z-40 lg:flex lg:w-72 lg:flex-col">
      <div class="flex grow flex-col overflow-y-auto border-r border-slate-200 bg-white px-6 pb-5 dark:border-white/10 dark:bg-slate-950">
        <div class="flex h-16 shrink-0 items-center">
          <LogoMark />
        </div>
        <nav class="mt-4 flex flex-1 flex-col gap-7">
          <div v-for="group in visibleGroups" :key="group.name">
            <p class="px-2 text-xs font-semibold text-slate-400">{{ group.name }}</p>
            <ul role="list" class="mt-2 space-y-1">
              <li v-for="item in group.items" :key="item.to">
                <RouterLink
                  :to="item.to"
                  :class="[
                    isActive(item)
                      ? 'bg-[rgb(var(--color-brand-soft))] text-[rgb(var(--color-brand-strong))] dark:text-white'
                      : 'text-slate-700 hover:bg-slate-50 hover:text-[rgb(var(--color-brand))] dark:text-slate-300 dark:hover:bg-white/5 dark:hover:text-white',
                    'group flex items-center gap-3 rounded-md px-2 py-2 text-sm font-semibold',
                  ]"
                >
                  <component
                    :is="item.icon"
                    :class="[
                      isActive(item) ? 'text-[rgb(var(--color-brand))] dark:text-white' : 'text-slate-400 group-hover:text-[rgb(var(--color-brand))]',
                      'size-5 shrink-0',
                    ]"
                    aria-hidden="true"
                  />
                  {{ item.name }}
                </RouterLink>
              </li>
            </ul>
          </div>
          <RouterLink
            to="/settings"
            class="mt-auto flex items-center gap-3 rounded-md px-2 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 hover:text-[rgb(var(--color-brand))] dark:text-slate-300 dark:hover:bg-white/5 dark:hover:text-white"
          >
            <Cog6ToothIcon class="size-5 text-slate-400" aria-hidden="true" />
            偏好设置
          </RouterLink>
        </nav>
      </div>
    </aside>

    <div class="lg:pl-72">
      <header
        class="sticky top-0 z-30 flex h-16 shrink-0 items-center gap-x-4 border-b border-slate-200 bg-white/95 px-4 shadow-sm backdrop-blur sm:gap-x-6 sm:px-6 lg:px-8 dark:border-white/10 dark:bg-slate-950/90 dark:shadow-none"
      >
        <button
          type="button"
          class="-m-2.5 p-2.5 text-slate-700 hover:text-slate-950 lg:hidden dark:text-slate-300 dark:hover:text-white"
          title="打开侧栏"
          @click="sidebarOpen = true"
        >
          <Bars3Icon class="size-6" aria-hidden="true" />
          <span class="sr-only">打开侧栏</span>
        </button>

        <div class="h-6 w-px bg-slate-200 lg:hidden dark:bg-white/10" aria-hidden="true" />

        <div class="grid flex-1 grid-cols-1">
          <input
            name="search"
            aria-label="搜索"
            class="col-start-1 row-start-1 block size-full bg-transparent py-1.5 pl-8 text-sm text-slate-900 outline-none placeholder:text-slate-400 dark:text-white dark:placeholder:text-slate-500"
            placeholder="搜索课程、文件、讨论"
          />
          <MagnifyingGlassIcon class="pointer-events-none col-start-1 row-start-1 size-5 self-center text-slate-400" aria-hidden="true" />
        </div>

        <div class="flex items-center gap-x-2 sm:gap-x-3">
          <ThemeSwitcher />
          <NotificationBell v-if="auth.hasPermission('notification:read')" />
          <div class="hidden h-6 w-px bg-slate-200 sm:block dark:bg-white/10" aria-hidden="true" />
          <Menu as="div" class="relative">
            <MenuButton class="flex items-center gap-2 rounded-md p-1.5 hover:bg-slate-100 dark:hover:bg-white/10">
              <span class="grid size-8 place-items-center rounded-full bg-[rgb(var(--color-brand-soft))] text-sm font-semibold text-[rgb(var(--color-brand-strong))] dark:text-white">
                {{ userInitial }}
              </span>
              <span class="hidden min-w-0 text-left lg:block">
                <span class="block max-w-36 truncate text-sm font-semibold text-slate-900 dark:text-white">{{ auth.displayName }}</span>
                <span class="block text-xs text-slate-500 dark:text-slate-400">{{ roleLabel(auth.primaryRole) }}</span>
              </span>
              <ChevronDownIcon class="hidden size-4 text-slate-400 lg:block" aria-hidden="true" />
            </MenuButton>
            <transition
              enter-active-class="transition ease-out duration-100"
              enter-from-class="opacity-0 scale-95"
              enter-to-class="opacity-100 scale-100"
              leave-active-class="transition ease-in duration-75"
              leave-from-class="opacity-100 scale-100"
              leave-to-class="opacity-0 scale-95"
            >
              <MenuItems
                class="absolute right-0 z-20 mt-2 w-40 origin-top-right rounded-md bg-white py-1 shadow-lg outline-1 outline-slate-900/5 dark:bg-slate-800 dark:outline-white/10"
              >
                <MenuItem v-slot="{ active }">
                  <RouterLink :class="[active ? 'bg-slate-50 dark:bg-white/5' : '', 'block px-3 py-2 text-sm text-slate-700 dark:text-slate-100']" to="/settings">
                    偏好设置
                  </RouterLink>
                </MenuItem>
                <MenuItem v-slot="{ active }">
                  <button
                    type="button"
                    :class="[active ? 'bg-slate-50 dark:bg-white/5' : '', 'block w-full px-3 py-2 text-left text-sm text-slate-700 dark:text-slate-100']"
                    @click="logout"
                  >
                    退出登录
                  </button>
                </MenuItem>
              </MenuItems>
            </transition>
          </Menu>
        </div>
      </header>

      <main class="min-h-[calc(100vh-4rem)]">
        <div class="mx-auto w-full max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
          <div class="mb-6">
            <p class="text-sm text-slate-500 dark:text-slate-400">EduVoyage</p>
            <h1 class="mt-1 text-2xl font-semibold text-slate-950 dark:text-white">{{ currentTitle }}</h1>
          </div>
          <RouterView />
        </div>
      </main>
    </div>
  </div>
</template>
