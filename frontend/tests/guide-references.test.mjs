import { test } from 'node:test'
import assert from 'node:assert/strict'
import { reactive, nextTick } from 'vue'
import { guideReferenceRoute } from '../src/utils/guideReferences.js'
import { watchRouteResource } from '../src/utils/routeResource.js'
import { scrollGuideToLatest } from '../src/utils/guideScroll.js'
test('only verified public resource kinds map to real internal routes', () => {
  assert.deepEqual(guideReferenceRoute({kind:'project',id:'12'}), {name:'ProjectDetail',params:{id:'12'}})
  assert.deepEqual(guideReferenceRoute({kind:'stall',id:'31'}), {name:'StallDetail',params:{id:'31'}})
  assert.deepEqual(guideReferenceRoute({kind:'artist',id:'21'}), {name:'ArtistProfile',params:{id:'21'},query:{role:'painter'}})
  for (const ref of [null, {kind:'order',id:'1'}, {kind:'artist',id:'javascript:alert(1)'}, {kind:'project',id:'../12'}, {kind:'project',id:'0'}, {kind:'project',id:'-1'}]) assert.equal(guideReferenceRoute(ref), null)
})
test('same-kind guide card navigation reloads a reused detail view', async () => {
  const route = reactive({ params: { id: '11' } }), calls = []
  const stop = watchRouteResource(route, (next, previous) => calls.push([next, previous]))
  route.params.id = '12'; await nextTick()
  route.params.id = 12; await nextTick()
  route.params.id = '13'; await nextTick()
  stop()
  assert.deepEqual(calls, [['12', '11'], ['13', 12]])
})
test('opening a restored guide can position its own viewport at the latest message', () => {
  const area = { scrollTop: 0, scrollHeight: 1380 }
  assert.equal(scrollGuideToLatest(area), true)
  assert.equal(area.scrollTop, 1380)
  assert.equal(scrollGuideToLatest(null), false)
})
