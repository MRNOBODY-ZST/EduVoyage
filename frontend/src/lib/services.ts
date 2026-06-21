import { api, deleteData, getData, postData, putData } from '@/lib/http'
import type {
  AdminDashboardResponse,
  BreadcrumbItem,
  CourseAnalyticsResponse,
  CourseResponse,
  DiscussionResponse,
  DriveNodeResponse,
  DriveTreeNode,
  FileUrlResponse,
  NotificationResponse,
  PageResult,
  QuotaResponse,
  ShareResponse,
  StudentDashboardResponse,
  UnreadCountResponse,
  UserProfile,
} from '@/types/api'

export function fetchCourses(params: { keyword?: string; teacherId?: number; pageNo?: number; pageSize?: number }) {
  return getData<PageResult<CourseResponse>>('/api/courses', { params })
}

export function createCourse(payload: {
  title: string
  coverUrl?: string
  intro?: string
  credit?: number
  visibility?: number
  startDate?: string
  endDate?: string
  classScope?: number[]
}) {
  return postData<CourseResponse>('/api/courses', payload)
}

export function publishCourse(courseId: number) {
  return postData<CourseResponse>(`/api/courses/${courseId}/publish`)
}

export function enrollCourse(courseId: number) {
  return postData(`/api/courses/${courseId}/enroll`)
}

export function fetchStudentDashboard() {
  return getData<StudentDashboardResponse>('/api/analytics/me')
}

export function fetchCourseAnalytics(courseId: number) {
  return getData<CourseAnalyticsResponse>(`/api/analytics/courses/${courseId}`)
}

export function fetchAdminDashboard() {
  return getData<AdminDashboardResponse>('/api/analytics/admin')
}

export function fetchNotifications(params: { read?: boolean; pageNo?: number; pageSize?: number } = {}) {
  return getData<PageResult<NotificationResponse>>('/api/notifications', { params })
}

export function fetchUnreadCount() {
  return getData<UnreadCountResponse>('/api/notifications/unread-count')
}

export function markAllNotificationsRead() {
  return putData('/api/notifications/read-all')
}

export function fetchDriveNodes(params: { parentId?: number; spaceType?: number; courseId?: number }) {
  return getData<DriveNodeResponse[]>('/api/drive/nodes', { params })
}

export function fetchDriveTree(params: { spaceType?: number; courseId?: number }) {
  return getData<DriveTreeNode[]>('/api/drive/tree', { params })
}

export function fetchDriveBreadcrumb(nodeId: number) {
  return getData<BreadcrumbItem[]>(`/api/drive/nodes/${nodeId}/breadcrumb`)
}

export function fetchDriveQuota() {
  return getData<QuotaResponse>('/api/drive/quota')
}

export function createDriveDirectory(payload: { name: string; parentId?: number; spaceType?: number; courseId?: number }) {
  return postData<DriveNodeResponse>('/api/drive/directories', payload)
}

export function uploadDriveFile(file: File, params: { parentId?: number; spaceType?: number; courseId?: number }) {
  const data = new FormData()
  data.append('file', file)
  return postData<DriveNodeResponse>('/api/drive/files', data, {
    params,
  })
}

export function renameDriveNode(id: number, name: string) {
  return putData<DriveNodeResponse>(`/api/drive/nodes/${id}/name`, { name })
}

export function deleteDriveNode(id: number) {
  return deleteData<void>(`/api/drive/nodes/${id}`)
}

export function getDrivePreviewUrl(id: number) {
  return getData<FileUrlResponse>(`/api/drive/nodes/${id}/preview-url`)
}

export function createDriveShare(payload: { nodeId: number; extractCode?: string; expireAt?: string }) {
  return postData<ShareResponse>('/api/drive/shares', payload)
}

export function fetchMyShares() {
  return getData<ShareResponse[]>('/api/drive/shares/my')
}

export function fetchDiscussions(courseId: number, params: { pageNo?: number; pageSize?: number; nodeId?: number } = {}) {
  return getData<PageResult<DiscussionResponse>>(`/api/courses/${courseId}/discussions`, { params })
}

export function createDiscussion(courseId: number, payload: { title: string; content: string; nodeId?: number }) {
  return postData<DiscussionResponse>(`/api/courses/${courseId}/discussions`, payload)
}

export function toggleDiscussionLike(id: string) {
  return postData<DiscussionResponse>(`/api/discussions/${id}/like`)
}

export function updateProfile(payload: Pick<UserProfile, 'realName' | 'email' | 'phone' | 'avatarUrl' | 'gender'>) {
  return putData<UserProfile>('/api/users/me/profile', payload)
}

export function downloadFromUrl(url: string) {
  window.open(url, '_blank', 'noopener,noreferrer')
}

export function createNotificationStream(onMessage: (event: MessageEvent) => void) {
  const source = new EventSource('/api/sse/notifications', { withCredentials: false })
  source.onmessage = onMessage
  return source
}

export async function probeBackend() {
  await api.get('/actuator/health')
}
