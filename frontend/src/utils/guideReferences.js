// 只映射后端复验过的资源，不执行模型 URL/脚本，也不导航私人业务页面。
export function guideReferenceRoute(reference) {
  if (!reference || !/^[1-9]\d{0,18}$/.test(String(reference.id))) return null
  const name = { project: 'ProjectDetail', artist: 'ArtistProfile' }[reference.kind]
  if (!name) return null
  return { name, params: { id: String(reference.id) }, ...(reference.kind === 'artist' ? { query: { role: 'painter' } } : {}) }
}
