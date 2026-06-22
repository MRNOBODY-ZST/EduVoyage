const LOCALE_KEY = 'eduvoyage.locale'

function currentLocale() {
  return localStorage.getItem(LOCALE_KEY) || 'zh-CN'
}

function isEnglish() {
  return currentLocale().startsWith('en')
}

export function formatNumber(value: number | string | null | undefined) {
  const n = Number(value ?? 0)
  return new Intl.NumberFormat(currentLocale()).format(Number.isFinite(n) ? n : 0)
}

export function formatPercent(value: number | string | null | undefined, digits = 1) {
  const n = Number(value ?? 0)
  return `${(Number.isFinite(n) ? n : 0).toFixed(digits)}%`
}

export function formatBytes(value: number | null | undefined) {
  const bytes = Number(value ?? 0)
  if (!Number.isFinite(bytes) || bytes <= 0) {
    return '0 B'
  }
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  const index = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1)
  return `${(bytes / 1024 ** index).toFixed(index === 0 ? 0 : 1)} ${units[index]!}`
}

export function formatDuration(seconds: number | null | undefined) {
  const total = Math.max(0, Number(seconds ?? 0))
  const hours = Math.floor(total / 3600)
  const minutes = Math.floor((total % 3600) / 60)
  if (isEnglish()) {
    if (hours > 0) {
      return `${hours}h ${minutes}m`
    }
    return `${minutes}m`
  }
  if (hours > 0) {
    return `${hours} 小时 ${minutes} 分钟`
  }
  return `${minutes} 分钟`
}

export function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return '-'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return new Intl.DateTimeFormat(currentLocale(), {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

export function courseStatusLabel(status: number | null | undefined) {
  if (status === 1) {
    return isEnglish() ? 'Published' : '已发布'
  }
  if (status === 2) {
    return isEnglish() ? 'Archived' : '已归档'
  }
  return isEnglish() ? 'Draft' : '草稿'
}

export function roleLabel(role: string | null | undefined) {
  if (role === 'ADMIN') {
    return isEnglish() ? 'Admin' : '管理员'
  }
  if (role === 'TEACHER') {
    return isEnglish() ? 'Teacher' : '教师'
  }
  return isEnglish() ? 'Student' : '学生'
}
