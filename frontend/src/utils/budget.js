function numericBudget(value) {
  if (value === null || value === undefined || value === '') return null
  const result = Number(value)
  return Number.isFinite(result) ? result : null
}

function formatAmount(value) {
  return Number(value).toLocaleString('zh-CN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  })
}

export function projectBudgetBounds(project) {
  const legacy = numericBudget(project?.budget)
  const min = numericBudget(project?.budgetMin) ?? legacy
  const max = numericBudget(project?.budgetMax) ?? legacy
  return { min, max }
}

export function formatProjectBudget(project, currency = '¥') {
  const { min, max } = projectBudgetBounds(project)
  if (min === null && max === null) return '待商议'
  if (min === null) return `${currency}${formatAmount(max)}`
  if (max === null || min === max) return `${currency}${formatAmount(min)}`
  return `${currency}${formatAmount(min)} – ${currency}${formatAmount(max)}`
}
