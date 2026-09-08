import { watch } from 'vue'

/** Vue Router 会复用同名详情组件；资源 ID 变化时显式触发重新加载。 */
export function watchRouteResource(route, onChange) {
  return watch(() => route.params.id, (nextId, previousId) => {
    if (String(nextId) === String(previousId)) return
    return onChange(nextId, previousId)
  })
}
