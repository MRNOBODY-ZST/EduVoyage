import type { Component } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

interface RouteMetaShape {
  title?: string
  i18nKey?: string
  public?: boolean
  permission?: string
  roles?: string[]
}

const routes = [
  {
    path: '/home',
    name: 'home',
    component: () => import('@/views/shared/HomeView.vue'),
    meta: { title: 'EduVoyage', i18nKey: 'app.name', public: true },
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { title: '登录', public: true },
  },
  {
    path: '/share/:token',
    name: 'share-access',
    component: () => import('@/views/shared/ShareAccessView.vue'),
    meta: { title: '访问分享', public: true },
  },
  {
    path: '/',
    component: () => import('@/components/layout/AppShell.vue'),
    children: [
      { path: '', redirect: () => defaultPath(useAuthStore().primaryRole) },
      {
        path: 'student/dashboard',
        name: 'student-dashboard',
        component: () => import('@/views/student/StudentDashboardView.vue'),
        meta: { title: '学习仪表盘', i18nKey: 'nav.studentDashboard', permission: 'analytics:view', roles: ['STUDENT'] },
      },
      {
        path: 'student/analytics',
        name: 'student-analytics',
        component: () => import('@/views/student/StudentDashboardView.vue'),
        meta: { title: '我的学情', i18nKey: 'nav.studentAnalytics', permission: 'analytics:view', roles: ['STUDENT'] },
      },
      {
        path: 'teacher/courses',
        name: 'teacher-courses',
        component: () => import('@/views/teacher/TeacherCoursesView.vue'),
        meta: { title: '课程工作台', i18nKey: 'nav.teacherCourses', permission: 'course:read', roles: ['TEACHER'] },
      },
      {
        path: 'teacher/analytics',
        name: 'teacher-analytics',
        component: () => import('@/views/teacher/TeacherAnalyticsView.vue'),
        meta: { title: '教学分析', i18nKey: 'nav.teacherAnalytics', permission: 'analytics:view', roles: ['TEACHER'] },
      },
      {
        path: 'admin/dashboard',
        name: 'admin-dashboard',
        component: () => import('@/views/admin/AdminDashboardView.vue'),
        meta: { title: '运营大盘', i18nKey: 'nav.adminDashboard', permission: 'analytics:view', roles: ['ADMIN'] },
      },
      {
        path: 'admin/users',
        name: 'admin-users',
        component: () => import('@/views/admin/AdminUsersView.vue'),
        meta: { title: '用户管理', i18nKey: 'nav.adminUsers', permission: 'user:read', roles: ['ADMIN'] },
      },
      {
        path: 'admin/org',
        name: 'admin-org',
        component: () => import('@/views/admin/AdminOrgView.vue'),
        meta: { title: '组织架构', i18nKey: 'nav.adminOrg', permission: 'org:manage', roles: ['ADMIN'] },
      },
      {
        path: 'courses',
        name: 'courses',
        component: () => import('@/views/shared/CoursesView.vue'),
        meta: { title: '课程中心', i18nKey: 'nav.courses' },
      },
      {
        path: 'courses/:courseId',
        name: 'course-detail',
        component: () => import('@/views/shared/CourseDetailView.vue'),
        meta: { title: '课程学习工作台', permission: 'course:read' },
      },
      {
        path: 'drive',
        name: 'drive',
        component: () => import('@/views/shared/DriveView.vue'),
        meta: { title: '课程网盘', i18nKey: 'nav.drive', permission: 'drive:read' },
      },
      {
        path: 'discussions',
        name: 'discussions',
        component: () => import('@/views/shared/DiscussionsView.vue'),
        meta: { title: '课程讨论', i18nKey: 'nav.discussions', permission: 'discussion:read' },
      },
      {
        path: 'settings',
        name: 'settings',
        component: () => import('@/views/shared/SettingsView.vue'),
        meta: { title: '偏好设置', i18nKey: 'nav.settings' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/shared/NotFoundView.vue'),
    meta: { title: '页面不存在', public: true },
  },
] satisfies Array<{ path: string; name?: string; component?: Component | (() => Promise<Component>); children?: unknown[]; meta?: RouteMetaShape; redirect?: unknown }>

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  },
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (!auth.bootstrapped) {
    await auth.fetchMe()
  }

  if (to.path === '/' && !auth.isAuthenticated) {
    return { name: 'home' }
  }

  const meta = to.meta as RouteMetaShape
  if (meta.public) {
    if ((to.name === 'login' || to.name === 'home') && auth.isAuthenticated) {
      return defaultPath(auth.primaryRole)
    }
    return true
  }

  if (!auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (meta.roles?.length && !meta.roles.some((role) => auth.hasRole(role))) {
    return defaultPath(auth.primaryRole)
  }

  if (meta.permission && !auth.hasPermission(meta.permission)) {
    return defaultPath(auth.primaryRole)
  }

  document.title = `${meta.title || '工作台'} - EduVoyage`
  return true
})

function defaultPath(role: string) {
  if (role === 'ADMIN') {
    return '/admin/dashboard'
  }
  if (role === 'TEACHER') {
    return '/teacher/courses'
  }
  return '/student/dashboard'
}

export default router
