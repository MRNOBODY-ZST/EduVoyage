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

export type ApiId = string | number

export interface UserProfile {
  id: ApiId
  username: string
  realName?: string
  email?: string
  phone?: string
  avatarUrl?: string
  gender?: number
  status?: number
  classId?: ApiId
  lastLoginAt?: string
  roles?: string[]
}

export interface RoleResponse {
  id: ApiId
  code: string
  name: string
  description?: string
  permissions: string[]
}

export interface DepartmentResponse {
  id: ApiId
  name: string
  code?: string
}

export interface MajorResponse {
  id: ApiId
  departmentId: ApiId
  name: string
  code?: string
}

export interface ClassResponse {
  id: ApiId
  majorId: ApiId
  name: string
  grade?: number
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
  id: ApiId
  title: string
  coverUrl?: string
  intro?: string
  credit?: number
  teacherId: ApiId
  visibility: number
  status: number
  startDate?: string
  endDate?: string
  createdAt?: string
  classScope?: ApiId[]
  enrolled?: boolean
  favorite?: boolean
}

export interface ChapterNode {
  id: ApiId
  courseId: ApiId
  parentId: ApiId
  title: string
  sortNo?: number
  children: ChapterNode[]
}

export interface KnowledgeNodeResponse {
  id: ApiId
  courseId: ApiId
  chapterId?: ApiId
  graphId: ApiId
  name: string
  description?: string
  learnGoal?: string
  estMinutes?: number
  posX?: number
  posY?: number
  createdAt?: string
}

export interface CoursewareResponse {
  id: ApiId
  nodeId: ApiId
  title: string
  type: number
  contentRef?: string
  fileId?: ApiId
  durationSec?: number
  sortNo?: number
  createdAt?: string
}

export interface GraphNode {
  id: ApiId
  name: string
  chapterId?: ApiId
  estMinutes?: number
  posX?: number
  posY?: number
}

export interface GraphLink {
  id: ApiId
  fromId: ApiId
  toId: ApiId
  type: 'PREREQUISITE' | 'RELATED' | string
  weight?: number
}

export interface GraphView {
  graphId: ApiId
  courseId: ApiId
  name: string
  nodes: GraphNode[]
  links: GraphLink[]
}

export interface PathNode {
  id: ApiId
  name: string
  estMinutes?: number
  mastered: boolean
}

export interface LearningPath {
  graphId: ApiId
  courseId: ApiId
  masteredCount: number
  totalCount: number
  learnable: PathNode[]
  recommended: PathNode[]
}

export interface HomeworkResponse {
  id: ApiId
  courseId: ApiId
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
  items?: Array<{ questionId: ApiId; score: number; sortNo?: number }>
}

export interface QuestionOptionView {
  id: ApiId
  optionKey: string
  content: string
  correct: boolean
  sortNo?: number
}

export interface QuestionResponse {
  id: ApiId
  courseId?: ApiId
  type: number
  stem: string
  answer?: string
  analysis?: string
  difficulty?: number
  nodeId?: ApiId
  lang?: string
  options: QuestionOptionView[]
  createdAt?: string
}

export interface StudentOption {
  optionKey: string
  content: string
}

export interface StudentQuestion {
  id: ApiId
  type: number
  stem: string
  difficulty?: number
  lang?: string
  score: number
  options: StudentOption[]
}

export interface ExamPaper {
  submissionId: ApiId
  homeworkId: ApiId
  title: string
  attemptNo: number
  timeLimit?: number
  deadline?: string
  totalScore: number
  questions: StudentQuestion[]
}

export interface AnswerResult {
  questionId: ApiId
  answer?: string
  score?: number
  isCorrect?: number
  comment?: string
}

export interface SubmissionResult {
  id: ApiId
  homeworkId: ApiId
  studentId: ApiId
  attemptNo: number
  status: number
  totalScore?: number
  submittedAt?: string
  answers: AnswerResult[]
}

export interface WrongBookEntry {
  id: ApiId
  questionId: ApiId
  nodeId?: ApiId
  wrongCount: number
  lastWrongAt?: string
  mastered: boolean
}

export interface NotificationResponse {
  id: string
  toUserId: ApiId
  type: string
  title: string
  body: string
  refId?: string
  category?: string
  read: boolean
  ts: string
}

export interface StudentDashboardResponse {
  studentId: ApiId
  totalDurationSec: number
  activeDays: number
  enrolledCourses: number
  todoHomeworks: number
  averageScore: number
  masteryPercent: number
  gradeTrend: Array<{ homeworkId: ApiId; title: string; score: number; submittedAt?: string }>
  recentLogs: Array<{
    id: string
    userId: ApiId
    courseId: ApiId
    nodeId?: ApiId
    action: string
    durationSec: number
    ts: string
  }>
}

export interface CourseAnalyticsResponse {
  courseId: ApiId
  enrolledCount: number
  activeLearners: number
  totalDurationSec: number
  submissionRate: number
  averageScore: number
  homeworkStats: Array<{
    homeworkId: ApiId
    title: string
    submittedCount: number
    totalStudents: number
    submissionRate: number
    averageScore: number
  }>
  studentRankings: Array<{
    studentId: ApiId
    studentName: string
    averageScore: number
    submittedCount: number
  }>
  masteryHeatmap: Array<{
    nodeId: ApiId
    nodeName: string
    averageProgress: number
    masteryRate: number
  }>
  weakNodes: Array<{
    nodeId: ApiId
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
  id: ApiId
  ownerId: ApiId
  spaceType: number
  courseId?: ApiId
  parentId: ApiId
  name: string
  directory: boolean
  fileId?: ApiId
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
  id: ApiId
  name: string
  parentId: ApiId
}

export interface FileUrlResponse {
  nodeId: ApiId
  url: string
  expireAt: string
}

export interface QuotaResponse {
  userId: ApiId
  totalBytes: number
  usedBytes: number
  remainingBytes: number
}

export interface ShareResponse {
  id: ApiId
  nodeId: ApiId
  ownerId: ApiId
  token: string
  extractCode: string
  expireAt?: string
  viewCount: number
  createdAt: string
  accessPath: string
}

export interface ShareViewResponse {
  share: ShareResponse
  node: DriveNodeResponse
  children: DriveNodeResponse[]
  url?: string
  urlExpireAt?: string
}

export interface DiscussionResponse {
  id: string
  courseId: ApiId
  nodeId?: ApiId
  authorId: ApiId
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
