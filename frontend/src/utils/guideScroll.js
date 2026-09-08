/** 将向导自己的滚动容器定位到最新消息，不影响页面主体滚动。 */
export function scrollGuideToLatest(area) {
  if (!area) return false
  area.scrollTop = area.scrollHeight
  return true
}
