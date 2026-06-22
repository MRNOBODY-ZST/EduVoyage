import { api, deleteData, getData, postData, putData } from '@/lib/http'
import type {
  AdminDashboardResponse,
  ApiId,
  BreadcrumbItem,
  ChapterNode,
  ClassResponse,
  CourseAnalyticsResponse,
  CourseResponse,
  CoursewareResponse,
  DepartmentResponse,
  DiscussionResponse,
  DriveNodeResponse,
  DriveTreeNode,
  ExamPaper,
  FileUrlResponse,
  GraphView,
  HomeworkResponse,
  KnowledgeNodeResponse,
  LearningPath,
  MajorResponse,
  NotificationResponse,
  PageResult,
  QuotaResponse,
  QuestionResponse,
  RoleResponse,
  ShareResponse,
  ShareViewResponse,
  SubmissionResult,
  StudentDashboardResponse,
  UnreadCountResponse,
  UserProfile,
  WrongBookEntry,
} from '@/types/api'

export function fetchUsers(params: { keyword?: string; status?: number; classId?: ApiId; pageNo?: number; pageSize?: number }) {
  return getData<PageResult<UserProfile>>('/api/users', { params })
}

export function createUser(payload: {
  username: string
  password: string
  realName?: string
  email?: string
  phone?: string
  gender?: number
  classId?: ApiId
  roleCodes: string[]
}) {
  return postData<UserProfile>('/api/users', payload)
}

export function updateUser(
  id: ApiId,
  payload: {
    realName?: string
    email?: string
    phone?: string
    gender?: number
    avatarUrl?: string
    status?: number
    classId?: ApiId
    roleCodes?: string[]
  },
) {
  return putData<UserProfile>(`/api/users/${id}`, payload)
}

export function deleteUser(id: number) {
  return deleteData<void>(`/api/users/${id}`)
}

export function fetchRoles() {
  return getData<RoleResponse[]>('/api/roles')
}

export function fetchDepartments() {
  return getData<DepartmentResponse[]>('/api/org/departments')
}

export function createDepartment(payload: { name: string; code?: string }) {
  return postData<DepartmentResponse>('/api/org/departments', payload)
}

export function updateDepartment(id: number, payload: { name: string; code?: string }) {
  return putData<DepartmentResponse>(`/api/org/departments/${id}`, payload)
}

export function deleteDepartment(id: number) {
  return deleteData<void>(`/api/org/departments/${id}`)
}

export function fetchMajors(departmentId?: number) {
  return getData<MajorResponse[]>('/api/org/majors', { params: { departmentId } })
}

export function createMajor(payload: { departmentId: number; name: string; code?: string }) {
  return postData<MajorResponse>('/api/org/majors', payload)
}

export function updateMajor(id: number, payload: { departmentId: number; name: string; code?: string }) {
  return putData<MajorResponse>(`/api/org/majors/${id}`, payload)
}

export function deleteMajor(id: number) {
  return deleteData<void>(`/api/org/majors/${id}`)
}

export function fetchClasses(majorId?: number) {
  return getData<ClassResponse[]>('/api/org/classes', { params: { majorId } })
}

export function createOrgClass(payload: { majorId: number; name: string; grade?: number }) {
  return postData<ClassResponse>('/api/org/classes', payload)
}

export function updateOrgClass(id: number, payload: { majorId: number; name: string; grade?: number }) {
  return putData<ClassResponse>(`/api/org/classes/${id}`, payload)
}

export function deleteOrgClass(id: number) {
  return deleteData<void>(`/api/org/classes/${id}`)
}

export function fetchCourses(params: { keyword?: string; teacherId?: ApiId; pageNo?: number; pageSize?: number }) {
  return getData<PageResult<CourseResponse>>('/api/courses', { params })
}

export function fetchCourse(courseId: ApiId) {
  return getData<CourseResponse>(`/api/courses/${courseId}`)
}

export function createCourse(payload: {
  title: string
  coverUrl?: string
  intro?: string
  credit?: number
  visibility?: number
  startDate?: string
  endDate?: string
  classScope?: ApiId[]
}) {
  return postData<CourseResponse>('/api/courses', payload)
}

export function publishCourse(courseId: ApiId) {
  return postData<CourseResponse>(`/api/courses/${courseId}/publish`)
}

export function updateCourse(
  courseId: ApiId,
  payload: {
    title: string
    coverUrl?: string
    intro?: string
    credit?: number
    visibility?: number
    startDate?: string
    endDate?: string
    classScope?: ApiId[]
  },
) {
  return putData<CourseResponse>(`/api/courses/${courseId}`, payload)
}

export function archiveCourse(courseId: ApiId) {
  return postData<CourseResponse>(`/api/courses/${courseId}/archive`)
}

export function deleteCourse(courseId: ApiId) {
  return deleteData<void>(`/api/courses/${courseId}`)
}

export function enrollCourse(courseId: ApiId) {
  return postData(`/api/courses/${courseId}/enroll`)
}

export function fetchChapters(courseId: ApiId) {
  return getData<ChapterNode[]>(`/api/courses/${courseId}/chapters`)
}

export function createChapter(courseId: ApiId, payload: { title: string; parentId?: ApiId; sortNo?: number }) {
  return postData<ChapterNode>(`/api/courses/${courseId}/chapters`, payload)
}

export function updateChapter(id: ApiId, payload: { title: string; parentId?: ApiId; sortNo?: number }) {
  return putData<ChapterNode>(`/api/chapters/${id}`, payload)
}

export function deleteChapter(id: ApiId) {
  return deleteData<void>(`/api/chapters/${id}`)
}

export function fetchKnowledgeNodes(courseId: ApiId, chapterId?: ApiId) {
  return getData<KnowledgeNodeResponse[]>(`/api/courses/${courseId}/nodes`, {
    params: { chapterId },
  })
}

export function createKnowledgeNode(
  courseId: ApiId,
  payload: {
    name: string
    chapterId?: ApiId
    description?: string
    learnGoal?: string
    estMinutes?: number
    posX?: number
    posY?: number
  },
) {
  return postData<KnowledgeNodeResponse>(`/api/courses/${courseId}/nodes`, payload)
}

export function deleteKnowledgeNode(id: ApiId) {
  return deleteData<void>(`/api/nodes/${id}`)
}

export function updateKnowledgeNode(
  id: ApiId,
  payload: {
    name: string
    chapterId?: ApiId
    description?: string
    learnGoal?: string
    estMinutes?: number
    posX?: number
    posY?: number
  },
) {
  return putData<KnowledgeNodeResponse>(`/api/nodes/${id}`, payload)
}

export function fetchCoursewares(nodeId: ApiId) {
  return getData<CoursewareResponse[]>(`/api/nodes/${nodeId}/coursewares`)
}

export function createCourseware(
  nodeId: ApiId,
  payload: {
    title: string
    type: number
    contentRef?: string
    fileId?: ApiId
    durationSec?: number
    sortNo?: number
  },
) {
  return postData<CoursewareResponse>(`/api/nodes/${nodeId}/coursewares`, payload)
}

export function deleteCourseware(id: ApiId) {
  return deleteData<void>(`/api/coursewares/${id}`)
}

export function updateCourseware(
  id: ApiId,
  payload: {
    title: string
    type: number
    contentRef?: string
    fileId?: ApiId
    durationSec?: number
    sortNo?: number
  },
) {
  return putData<CoursewareResponse>(`/api/coursewares/${id}`, payload)
}

export function fetchGraph(courseId: ApiId) {
  return getData<GraphView>(`/api/courses/${courseId}/graph`)
}

export function createGraphEdge(
  courseId: ApiId,
  payload: { fromId: ApiId; toId: ApiId; type: 'PREREQUISITE' | 'RELATED'; weight?: number },
) {
  return postData(`/api/courses/${courseId}/graph/edges`, payload)
}

export function deleteGraphEdge(id: ApiId) {
  return deleteData<void>(`/api/graph/edges/${id}`)
}

export function fetchLearningPath(courseId: ApiId) {
  return getData<LearningPath>(`/api/courses/${courseId}/graph/learning-path`)
}

export function fetchHomeworks(courseId: ApiId) {
  return getData<HomeworkResponse[]>(`/api/courses/${courseId}/homeworks`)
}

export function createHomework(
  courseId: ApiId,
  payload: {
    title: string
    timeLimit?: number
    deadline?: string
    maxAttempts?: number
    shuffle: boolean
    antiSwitch: boolean
    items: Array<{ questionId: ApiId; score: number; sortNo?: number }>
  },
) {
  return postData<HomeworkResponse>(`/api/courses/${courseId}/homeworks`, payload)
}

export function publishHomework(homeworkId: number) {
  return postData<HomeworkResponse>(`/api/homeworks/${homeworkId}/publish`)
}

export function closeHomework(homeworkId: number) {
  return postData<HomeworkResponse>(`/api/homeworks/${homeworkId}/close`)
}

export function deleteHomework(homeworkId: number) {
  return deleteData<void>(`/api/homeworks/${homeworkId}`)
}

export function updateHomework(
  homeworkId: ApiId,
  payload: {
    title: string
    timeLimit?: number
    deadline?: string
    maxAttempts?: number
    shuffle: boolean
    antiSwitch: boolean
    items: Array<{ questionId: ApiId; score: number; sortNo?: number }>
  },
) {
  return putData<HomeworkResponse>(`/api/homeworks/${homeworkId}`, payload)
}

export function fetchQuestions(params: {
  courseId?: ApiId
  keyword?: string
  type?: number
  difficulty?: number
  nodeId?: ApiId
  pageNo?: number
  pageSize?: number
}) {
  return getData<PageResult<QuestionResponse>>('/api/questions', { params })
}

export function createQuestion(payload: {
  courseId?: ApiId
  type: number
  stem: string
  answer?: string
  analysis?: string
  difficulty?: number
  nodeId?: ApiId
  lang?: string
  options?: Array<{ optionKey: string; content: string; correct: boolean; sortNo?: number }>
}) {
  return postData<QuestionResponse>('/api/questions', payload)
}

export function deleteQuestion(id: number) {
  return deleteData<void>(`/api/questions/${id}`)
}

export function updateQuestion(
  id: number,
  payload: {
    courseId?: number
    type: number
    stem: string
    answer?: string
    analysis?: string
    difficulty?: number
    nodeId?: number
    lang?: string
    options?: Array<{ optionKey: string; content: string; correct: boolean; sortNo?: number }>
  },
) {
  return putData<QuestionResponse>(`/api/questions/${id}`, payload)
}

export function startSubmission(homeworkId: number) {
  return postData<ExamPaper>(`/api/homeworks/${homeworkId}/submissions/start`)
}

export function submitSubmission(submissionId: number, payload: { answers: Array<{ questionId: number; answer?: string }>; switchCount?: number }) {
  return postData<SubmissionResult>(`/api/submissions/${submissionId}/submit`, payload)
}

export function fetchMySubmissions(homeworkId: number) {
  return getData<SubmissionResult[]>(`/api/homeworks/${homeworkId}/submissions/me`)
}

export function fetchHomeworkSubmissions(homeworkId: number) {
  return getData<SubmissionResult[]>(`/api/homeworks/${homeworkId}/submissions`)
}

export function gradeSubmission(submissionId: number, payload: { grades: Array<{ questionId: number; score: number; comment?: string }> }) {
  return postData<SubmissionResult>(`/api/submissions/${submissionId}/grade`, payload)
}

export function fetchWrongBook(onlyUnmastered = false) {
  return getData<WrongBookEntry[]>('/api/wrong-book/me', {
    params: { onlyUnmastered },
  })
}

export function fetchStudentDashboard() {
  return getData<StudentDashboardResponse>('/api/analytics/me')
}

export function fetchCourseAnalytics(courseId: ApiId) {
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

export function moveDriveNode(id: number, targetParentId: number) {
  return putData<DriveNodeResponse>(`/api/drive/nodes/${id}/parent`, { targetParentId })
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

export function accessDriveShare(token: string, extractCode?: string) {
  return postData<ShareViewResponse>(`/api/drive/share/${encodeURIComponent(token)}`, { extractCode: extractCode || undefined })
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
