import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { PROJECT_CATEGORY_OPTIONS } from '../src/constants/projectCategories.js'
import { CATEGORY_OPTIONS, SUBJECT_OPTIONS } from '../src/constants/subjects.js'
const source = path => readFileSync(new URL('../src/'+path, import.meta.url), 'utf8')

test('project six categories stay separate from ten supply categories and 35 subjects', () => {
  assert.deepEqual(PROJECT_CATEGORY_OPTIONS, ['头像','立绘','角色设计','插画','平面设计','自设/OC'])
  assert.equal(CATEGORY_OPTIONS.length, 10)
  assert.equal(SUBJECT_OPTIONS.length, 35)
  assert.ok(CATEGORY_OPTIONS.includes('Live2d'))
  assert.ok(!PROJECT_CATEGORY_OPTIONS.includes('Live2d'))
  const options=JSON.parse(readFileSync(new URL('../../backend/src/main/resources/guide/search-options.json',import.meta.url),'utf8'))
  assert.deepEqual(options.projectCategories,PROJECT_CATEGORY_OPTIONS)
  assert.deepEqual(options.categories,CATEGORY_OPTIONS)
})
test('create and edit submit a single projectCategory and keep subject independent', () => {
  for (const view of ['views/dashboard/ProjectNew.vue','views/dashboard/ProjectEdit.vue']) {
    const text=source(view)
    assert.match(text,/projectCategory: form.projectCategory/)
    assert.match(text,/subject: form.subjects/)
    assert.doesNotMatch(text,/\bcategory:\s*form\./)
    assert.match(text,/PROJECT_CATEGORY_OPTIONS.includes\(form.projectCategory\)/)
  }
})
test('project hall applies a single server-side union query only after confirmation', () => {
  const text=source('views/projects/ProjectList.vue')
  assert.match(text,/projectFilterParams\(appliedFilter.value\)/)
  assert.match(text,/@apply="applyFilter"/)
  assert.match(text,/version !== requestVersion/)
  assert.doesNotMatch(text,/params.category\b|categoryGroup/)
})
test('statistics separate supply pie and completed-order sources', () => {
  const text=source('views/admin/Stats.vue')
  assert.match(text,/企划订单成交价格分层/)
  assert.match(text,/data.value.stallCategories/)
  assert.match(text,/projectTopCategories/)
  assert.match(text,/stallTopCategories/)
  assert.doesNotMatch(text,/\{category:priceCategory/)
})
