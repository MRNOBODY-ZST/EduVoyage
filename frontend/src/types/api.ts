export interface Result<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  pageNo: number
  pageSize: number
  totalPages: number
}

export interface UserProfile {
  id: number
  username: string
  realName?: string
  email?: string
  phone?: string
  avatarUrl?: string
  gender?: number
  status?: number
  classId?: number
  lastLoginAt?: string
  roles?: string[]
}

export interface MeResponse {
  profile: UserProfile
  roles: string[]
  permissions: string[]
}

export interface TokenResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: UserProfile
}

export interface CourseResponse {
  id: number
  title: string
  coverUrl?: string
  intro?: string
  credit?: number
  teacherId: number
  visibility: number
  status: number
  startDate?: string
  endDate?: string
  createdAt?: string
  classScope?: number[]
  enrolled?: boolean
  favorite?: boolean
}

export interface ChapterNode {
  id: number
  courseId: number
  parentId: number
  title: string
  sortNo?: number
  children: ChapterNode[]
}

export interface KnowledgeNodeResponse {
  id: number
  courseId: number
  chapterId?: number
  graphId: number
  name: string
  description?: string
  learnGoal?: string
  estMinutes?: number
  posX?: number
  posY?: number
  createdAt?: string
}

export interface CoursewareResponse {
  id: number
  nodeId: number
  title: string
  type: number
  contentRef?: string
  fileId?: number
  durationSec?: number
  sortNo?: number
  createdAt?: string
}

export interface GraphNode {
  id: number
  name: string
  chapterId?: number
  estMinutes?: number
  posX?: number
  posY?: number
}

export interface GraphLink {
  id: number
  fromId: number
  toId: number
  type: 'PREREQUISITE' | 'RELATED' | string
  weight?: number
}

export interface GraphView {
  graphId: number
  courseId: number
  name: string
  nodes: GraphNode[]
  links: GraphLink[]
}

export interface PathNode {
  id: number
  name: string
  estMinutes?: number
  mastered: boolean
}

export interface LearningPath {
  graphId: number
  courseId: number
  masteredCount: number
  totalCount: number
  learnable: PathNode[]
  recommended: PathNode[]
}

export interface HomeworkResponse {
  id: number
  courseId: number
  title: string
  totalScore: number
  timeLimit?: number
  deadline?: string
  maxAttempts?: number
  shuffle: boolean
  antiSwitch: boolean
  status: number
  questionCount: number
  createdAt?: string
}

export interface WrongBookEntry {
  id: number
  questionId: number
  nodeId?: number
  wrongCount: number
  lastWrongAt?: string
  mastered: boolean
}

export interface NotificationResponse {
  id: string
  toUserId: number
  type: string
  title: string
  body: string
  refId?: string
  category?: string
  read: boolean
  ts: string
}

export interface StudentDashboardResponse {
  studentId: number
  totalDurationSec: number
  activeDays: number
  enrolledCourses: number
  todoHomeworks: number
  averageScore: number
  masteryPercent: number
  gradeTrend: Array<{ homeworkId: number; title: string; score: number; submittedAt?: string }>
  recentLogs: Array<{
    id: string
    userId: number
    courseId: number
    nodeId?: number
    action: string
    durationSec: number
    ts: string
  }>
}

export interface CourseAnalyticsResponse {
  courseId: number
  enrolledCount: number
  activeLearners: number
  totalDurationSec: number
  submissionRate: number
  averageScore: number
  homeworkStats: Array<{
    homeworkId: number
    title: string
    submittedCount: number
    totalStudents: number
    submissionRate: number
    averageScore: number
  }>
  studentRankings: Array<{
    studentId: number
    studentName: string
    averageScore: number
    submittedCount: number
  }>
  masteryHeatmap: Array<{
    nodeId: number
    nodeName: string
    averageProgress: number
    masteryRate: number
  }>
  weakNodes: Array<{
    nodeId: number
    nodeName: string
    averageProgress: number
    masteryRate: number
  }>
}

export interface AdminDashboardResponse {
  totalUsers: number
  activeUsers30d: number
  newUsers30d: number
  totalCourses: number
  totalHomeworks: number
  totalSubmissions: number
  storageUsedBytes: number
}

export interface DriveNodeResponse {
  id: number
  ownerId: number
  spaceType: number
  courseId?: number
  parentId: number
  name: string
  directory: boolean
  fileId?: number
  sha256?: string
  size?: number
  mime?: string
  createdAt?: string
  updatedAt?: string
}

export interface DriveTreeNode {
  node: DriveNodeResponse
  children: DriveTreeNode[]
}

export interface BreadcrumbItem {
  id: number
  name: string
  parentId: number
}

export interface FileUrlResponse {
  nodeId: number
  url: string
  expireAt: string
}

export interface QuotaResponse {
  userId: number
  totalBytes: number
  usedBytes: number
  remainingBytes: number
}

export interface ShareResponse {
  id: number
  nodeId: number
  ownerId: number
  token: string
  extractCode: string
  expireAt?: string
  viewCount: number
  createdAt: string
  accessPath: string
}

export interface DiscussionResponse {
  id: string
  courseId: number
  nodeId?: number
  authorId: number
  title: string
  content: string
  parentId?: string
  likeCount: number
  liked: boolean
  replyCount: number
  ts: string
}

export interface UnreadCountResponse {
  unread: number
}

export type RoleCode = 'ADMIN' | 'TEACHER' | 'STUDENT' | string
