export const PROJECT_MIN_DEADLINE_DAYS = 2

export function localDateText(date = new Date()) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export function minimumProjectDeadline() {
  const date = new Date()
  date.setHours(0, 0, 0, 0)
  date.setDate(date.getDate() + PROJECT_MIN_DEADLINE_DAYS)
  return date
}

export function minimumProjectDeadlineText() {
  return localDateText(minimumProjectDeadline())
}

export function disableBeforeProjectDeadline(date) {
  const candidate = new Date(date)
  candidate.setHours(0, 0, 0, 0)
  return candidate.getTime() < minimumProjectDeadline().getTime()
}

export function isProjectExpired(deadline) {
  return !deadline || String(deadline).slice(0, 10) < localDateText()
}
