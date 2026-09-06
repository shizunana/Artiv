import { test } from 'node:test'
import assert from 'node:assert/strict'
import { guideReferenceRoute } from '../src/utils/guideReferences.js'
test('only verified public resource kinds map to real internal routes', () => {
  assert.deepEqual(guideReferenceRoute({kind:'project',id:'12'}), {name:'ProjectDetail',params:{id:'12'}})
  assert.deepEqual(guideReferenceRoute({kind:'artist',id:'21'}), {name:'ArtistProfile',params:{id:'21'},query:{role:'painter'}})
  for (const ref of [null, {kind:'order',id:'1'}, {kind:'artist',id:'javascript:alert(1)'}, {kind:'project',id:'../12'}, {kind:'project',id:'0'}, {kind:'project',id:'-1'}]) assert.equal(guideReferenceRoute(ref), null)
})
