/** 状态文案与标签映射（与后端枚举一致） */

export const PROJECT_STATUS = {
  0: { text: '招募中', type: 'success' },
  1: { text: '已选定', type: 'primary' },
  2: { text: '进行中', type: 'warning' },
  3: { text: '已完成', type: 'info' },
  4: { text: '已下架', type: 'info' },
}

export const ORDER_STATUS = {
  0: { text: '待接单', type: 'info' },
  1: { text: '进行中', type: 'primary' },
  2: { text: '待验收', type: 'warning' },
  3: { text: '已完成', type: 'success' },
  4: { text: '已取消', type: 'danger' },
  5: { text: '争议中', type: 'danger' },
}

export const CERT_STATUS = {
  0: { text: '未认证', type: 'info' },
  1: { text: '审核中', type: 'warning' },
  2: { text: '已认证', type: 'success' },
}

export function statusText(map, value, fallback = '-') {
  return map[value]?.text ?? fallback
}

export function statusType(map, value, fallback = 'info') {
  return map[value]?.type ?? fallback
}
