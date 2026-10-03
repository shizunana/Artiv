// 与合作方式、公开稿件权限分离；未知旧快照不能推断为个人用途。
export const PROJECT_USAGE_OPTIONS = [
  { value: 'personal', label: '个人用途' },
  { value: 'commercial', label: '商业用途' },
]

export function projectUsageLabel(value) {
  return PROJECT_USAGE_OPTIONS.find((item) => item.value === value)?.label || '未记录用途'
}

export function isProjectUsage(value) {
  return PROJECT_USAGE_OPTIONS.some((item) => item.value === value)
}
