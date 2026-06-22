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
import { useI18n } from '@/i18n'
import { roleLabel } from '@/lib/format'
import { useAuthStore } from '@/stores/auth'

interface NavigationItem {
  labelKey: string
  to: string
  icon: Component
  permission?: string
  roles?: string[]
}

interface TeamItem {
  labelKey: string
  to: string
  initial: string
  permission?: string
  roles?: string[]
}

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const { t } = useI18n()
const sidebarOpen = ref(false)

const navigation: NavigationItem[] = [
  { labelKey: 'nav.studentDashboard', to: '/student/dashboard', icon: HomeIcon, permission: 'analytics:view', roles: ['STUDENT'] },
  { labelKey: 'nav.studentAnalytics', to: '/student/analytics', icon: ChartBarSquareIcon, permission: 'analytics:view', roles: ['STUDENT'] },
  { labelKey: 'nav.teacherCourses', to: '/teacher/courses', icon: RectangleStackIcon, permission: 'course:read', roles: ['TEACHER'] },
  { labelKey: 'nav.teacherAnalytics', to: '/teacher/analytics', icon: ChartBarSquareIcon, permission: 'analytics:view', roles: ['TEACHER'] },
  { labelKey: 'nav.adminDashboard', to: '/admin/dashboard', icon: ServerStackIcon, permission: 'analytics:view', roles: ['ADMIN'] },
  { labelKey: 'nav.adminUsers', to: '/admin/users', icon: UsersIcon, permission: 'user:read', roles: ['ADMIN'] },
  { labelKey: 'nav.adminOrg', to: '/admin/org', icon: BuildingLibraryIcon, permission: 'org:manage', roles: ['ADMIN'] },
  { labelKey: 'nav.courses', to: '/courses', icon: AcademicCapIcon },
  { labelKey: 'nav.drive', to: '/drive', icon: FolderIcon, permission: 'drive:read' },
  { labelKey: 'nav.discussions', to: '/discussions', icon: ChatBubbleLeftRightIcon, permission: 'discussion:read' },
  { labelKey: 'nav.settings', to: '/settings', icon: Cog6ToothIcon },
]

const studentTeams: TeamItem[] = [
  { labelKey: 'nav.groupGraph', to: '/courses', initial: '图' },
  { labelKey: 'nav.groupHomework', to: '/student/dashboard', initial: '作', permission: 'analytics:view', roles: ['STUDENT'] },
  { labelKey: 'nav.groupFiles', to: '/drive', initial: '资', permission: 'drive:read' },
  { labelKey: 'nav.groupTeaching', to: '/teacher/courses', initial: '教', permission: 'course:read', roles: ['TEACHER'] },
  { labelKey: 'nav.groupOps', to: '/admin/dashboard', initial: '运', permission: 'analytics:view', roles: ['ADMIN'] },
]

const visibleNavigation = computed(() => navigation.filter(canShow))
const visibleTeams = computed(() => studentTeams.filter(canShow))
const currentTitle = computed(() => t(String(route.meta.i18nKey || ''), String(route.meta.title || t('app.workspace'))))
const userInitial = computed(() => (auth.displayName || 'E').slice(0, 1).toUpperCase())

function canShow(item: NavigationItem | TeamItem) {
  const roleAllowed = !item.roles?.length || item.roles.some((role) => auth.hasRole(role))
  const permissionAllowed = !item.permission || auth.hasPermission(item.permission)
  return roleAllowed && permissionAllowed
}

function isActive(item: NavigationItem | TeamItem) {
  return route.path === item.to || route.path.startsWith(`${item.to}/`)
}

async function logout() {
  auth.logout()
  await router.push('/login')
}
</script>

<template>
  <div class="min-h-screen bg-white text-gray-900">
    <TransitionRoot as="template" :show="sidebarOpen">
      <Dialog class="relative z-50 lg:hidden" @close="sidebarOpen = false">
        <TransitionChild
          as="template"
          enter="transition-opacity ease-linear duration-300"
          enter-from="opacity-0"
          enter-to="opacity-100"
          leave="transition-opacity ease-linear duration-300"
          leave-from="opacity-100"
          leave-to="opacity-0"
        >
          <div class="fixed inset-0 bg-gray-900/80" />
        </TransitionChild>

        <div class="fixed inset-0 flex">
          <TransitionChild
            as="template"
            enter="transition ease-in-out duration-300 transform"
            enter-from="-translate-x-full"
            enter-to="translate-x-0"
            leave="transition ease-in-out duration-300 transform"
            leave-from="translate-x-0"
            leave-to="-translate-x-full"
          >
            <DialogPanel class="relative mr-16 flex w-full max-w-xs flex-1">
              <TransitionChild
                as="template"
                enter="ease-in-out duration-300"
                enter-from="opacity-0"
                enter-to="opacity-100"
                leave="ease-in-out duration-300"
                leave-from="opacity-100"
                leave-to="opacity-0"
              >
                <div class="absolute left-full top-0 flex w-16 justify-center pt-5">
                  <button type="button" class="-m-2.5 p-2.5" :title="t('shell.closeSidebar')" @click="sidebarOpen = false">
                    <span class="sr-only">{{ t('shell.closeSidebar') }}</span>
                    <XMarkIcon class="size-6 text-white" aria-hidden="true" />
                  </button>
                </div>
              </TransitionChild>

              <div class="flex grow flex-col gap-y-5 overflow-y-auto bg-white px-6 pb-4">
                <div class="flex h-16 shrink-0 items-center">
                  <LogoMark />
                </div>
                <nav class="flex flex-1 flex-col">
                  <ul role="list" class="flex flex-1 flex-col gap-y-7">
                    <li>
                      <ul role="list" class="-mx-2 space-y-1">
                        <li v-for="item in visibleNavigation" :key="item.to">
                          <RouterLink
                            :to="item.to"
                            :class="[
                              isActive(item)
                                ? 'bg-gray-50 text-indigo-600'
                                : 'text-gray-700 hover:bg-gray-50 hover:text-indigo-600',
                              'group flex gap-x-3 rounded-md p-2 text-sm/6 font-semibold',
                            ]"
                            @click="sidebarOpen = false"
                          >
                            <component
                              :is="item.icon"
                              :class="[
                                isActive(item) ? 'text-indigo-600' : 'text-gray-400 group-hover:text-indigo-600',
                                'size-6 shrink-0',
                              ]"
                              aria-hidden="true"
                            />
                            {{ t(item.labelKey) }}
                          </RouterLink>
                        </li>
                      </ul>
                    </li>
                    <li>
                      <div class="text-xs/6 font-semibold text-gray-400">{{ t('nav.studentGroups') }}</div>
                      <ul role="list" class="-mx-2 mt-2 space-y-1">
                        <li v-for="team in visibleTeams" :key="team.labelKey">
                          <RouterLink
                            :to="team.to"
                            :class="[
                              isActive(team)
                                ? 'bg-gray-50 text-indigo-600'
                                : 'text-gray-700 hover:bg-gray-50 hover:text-indigo-600',
                              'group flex gap-x-3 rounded-md p-2 text-sm/6 font-semibold',
                            ]"
                            @click="sidebarOpen = false"
                          >
                            <span
                              :class="[
                                isActive(team)
                                  ? 'border-indigo-600 text-indigo-600'
                                  : 'border-gray-200 text-gray-400 group-hover:border-indigo-600 group-hover:text-indigo-600',
                                'flex size-6 shrink-0 items-center justify-center rounded-lg border bg-white text-[0.625rem] font-medium',
                              ]"
                            >
                              {{ team.initial }}
                            </span>
                            <span class="truncate">{{ t(team.labelKey) }}</span>
                          </RouterLink>
                        </li>
                      </ul>
                    </li>
                    <li class="mt-auto">
                      <RouterLink
                        to="/settings"
                        class="group -mx-2 flex gap-x-3 rounded-md p-2 text-sm/6 font-semibold text-gray-700 hover:bg-gray-50 hover:text-indigo-600"
                        @click="sidebarOpen = false"
                      >
                        <Cog6ToothIcon class="size-6 shrink-0 text-gray-400 group-hover:text-indigo-600" aria-hidden="true" />
                        {{ t('nav.personalSettings') }}
                      </RouterLink>
                    </li>
                  </ul>
                </nav>
              </div>
            </DialogPanel>
          </TransitionChild>
        </div>
      </Dialog>
    </TransitionRoot>

    <div class="hidden lg:fixed lg:inset-y-0 lg:z-50 lg:flex lg:w-72 lg:flex-col">
      <div class="flex grow flex-col gap-y-5 overflow-y-auto border-r border-gray-200 bg-white px-6 pb-4">
        <div class="flex h-16 shrink-0 items-center">
          <LogoMark />
        </div>
        <nav class="flex flex-1 flex-col">
          <ul role="list" class="flex flex-1 flex-col gap-y-7">
            <li>
              <ul role="list" class="-mx-2 space-y-1">
                <li v-for="item in visibleNavigation" :key="item.to">
                  <RouterLink
                    :to="item.to"
                    :class="[
                      isActive(item)
                        ? 'bg-gray-50 text-indigo-600'
                        : 'text-gray-700 hover:bg-gray-50 hover:text-indigo-600',
                      'group flex gap-x-3 rounded-md p-2 text-sm/6 font-semibold',
                    ]"
                  >
                    <component
                      :is="item.icon"
                      :class="[
                        isActive(item) ? 'text-indigo-600' : 'text-gray-400 group-hover:text-indigo-600',
                        'size-6 shrink-0',
                      ]"
                      aria-hidden="true"
                    />
                    {{ t(item.labelKey) }}
                  </RouterLink>
                </li>
              </ul>
            </li>
            <li>
              <div class="text-xs/6 font-semibold text-gray-400">{{ t('nav.studentGroups') }}</div>
              <ul role="list" class="-mx-2 mt-2 space-y-1">
                <li v-for="team in visibleTeams" :key="team.labelKey">
                  <RouterLink
                    :to="team.to"
                    :class="[
                      isActive(team)
                        ? 'bg-gray-50 text-indigo-600'
                        : 'text-gray-700 hover:bg-gray-50 hover:text-indigo-600',
                      'group flex gap-x-3 rounded-md p-2 text-sm/6 font-semibold',
                    ]"
                  >
                    <span
                      :class="[
                        isActive(team)
                          ? 'border-indigo-600 text-indigo-600'
                          : 'border-gray-200 text-gray-400 group-hover:border-indigo-600 group-hover:text-indigo-600',
                        'flex size-6 shrink-0 items-center justify-center rounded-lg border bg-white text-[0.625rem] font-medium',
                      ]"
                    >
                      {{ team.initial }}
                    </span>
                    <span class="truncate">{{ t(team.labelKey) }}</span>
                  </RouterLink>
                </li>
              </ul>
            </li>
            <li class="mt-auto">
              <RouterLink
                to="/settings"
                class="group -mx-2 flex gap-x-3 rounded-md p-2 text-sm/6 font-semibold text-gray-700 hover:bg-gray-50 hover:text-indigo-600"
              >
                <Cog6ToothIcon class="size-6 shrink-0 text-gray-400 group-hover:text-indigo-600" aria-hidden="true" />
                {{ t('nav.personalSettings') }}
              </RouterLink>
            </li>
          </ul>
        </nav>
      </div>
    </div>

    <div class="lg:pl-72">
      <header class="sticky top-0 z-40 flex h-16 shrink-0 items-center gap-x-4 border-b border-gray-200 bg-white px-4 shadow-sm sm:gap-x-6 sm:px-6 lg:px-8">
        <button type="button" class="-m-2.5 p-2.5 text-gray-700 lg:hidden" :title="t('shell.openSidebar')" @click="sidebarOpen = true">
          <span class="sr-only">{{ t('shell.openSidebar') }}</span>
          <Bars3Icon class="size-6" aria-hidden="true" />
        </button>

        <div class="h-6 w-px bg-gray-200 lg:hidden" aria-hidden="true" />

        <div class="grid flex-1 grid-cols-1">
          <input
            name="search"
            :aria-label="t('shell.search')"
            class="col-start-1 row-start-1 block size-full bg-white py-1.5 pl-8 text-base text-gray-900 outline-none placeholder:text-gray-400 sm:text-sm/6"
            :placeholder="t('shell.search')"
          />
          <MagnifyingGlassIcon class="pointer-events-none col-start-1 row-start-1 size-5 self-center text-gray-400" aria-hidden="true" />
        </div>

        <div class="flex items-center gap-x-4 lg:gap-x-6">
          <NotificationBell v-if="auth.hasPermission('notification:read')" />

          <div class="hidden lg:block lg:h-6 lg:w-px lg:bg-gray-200" aria-hidden="true" />

          <Menu as="div" class="relative">
            <MenuButton class="-m-1.5 flex items-center p-1.5">
              <span class="sr-only">{{ t('shell.openUserMenu') }}</span>
              <span class="grid size-8 place-items-center rounded-full bg-indigo-50 text-sm font-semibold text-indigo-700">
                {{ userInitial }}
              </span>
              <span class="hidden lg:flex lg:items-center">
                <span class="ml-4 text-sm/6 font-semibold text-gray-900" aria-hidden="true">{{ auth.displayName }}</span>
                <ChevronDownIcon class="ml-2 size-5 text-gray-400" aria-hidden="true" />
              </span>
            </MenuButton>
            <transition
              enter-active-class="transition ease-out duration-100"
              enter-from-class="transform opacity-0 scale-95"
              enter-to-class="transform opacity-100 scale-100"
              leave-active-class="transition ease-in duration-75"
              leave-from-class="transform opacity-100 scale-100"
              leave-to-class="transform opacity-0 scale-95"
            >
              <MenuItems class="absolute right-0 z-10 mt-2.5 w-40 origin-top-right rounded-md bg-white py-2 shadow-lg ring-1 ring-gray-900/5 focus:outline-none">
                <MenuItem v-slot="{ active }">
                  <div :class="[active ? 'bg-gray-50' : '', 'px-3 py-2']">
                    <p class="truncate text-sm font-semibold text-gray-900">{{ auth.displayName }}</p>
                    <p class="mt-0.5 text-xs text-gray-500">{{ roleLabel(auth.primaryRole) }}</p>
                  </div>
                </MenuItem>
                <MenuItem v-slot="{ active }">
                  <RouterLink :class="[active ? 'bg-gray-50' : '', 'block px-3 py-1 text-sm/6 text-gray-900']" to="/settings">
                    {{ t('nav.personalSettings') }}
                  </RouterLink>
                </MenuItem>
                <MenuItem v-slot="{ active }">
                  <button
                    type="button"
                    :class="[active ? 'bg-gray-50' : '', 'block w-full px-3 py-1 text-left text-sm/6 text-gray-900']"
                    @click="logout"
                  >
                    {{ t('shell.logout') }}
                  </button>
                </MenuItem>
              </MenuItems>
            </transition>
          </Menu>
        </div>
      </header>

      <main class="py-10">
        <div class="px-4 sm:px-6 lg:px-8">
          <div class="mb-8">
            <p class="text-sm/6 font-medium text-gray-500">{{ t('app.name') }}</p>
            <h1 class="mt-2 text-2xl font-semibold text-gray-900">{{ currentTitle }}</h1>
          </div>
          <RouterView />
        </div>
      </main>
    </div>
  </div>
</template>
