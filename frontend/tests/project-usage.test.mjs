import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { projectUsageLabel, isProjectUsage, PROJECT_USAGE_OPTIONS } from '../src/utils/projectUsage.js'
const source = path => readFileSync(new URL('../src/' + path, import.meta.url), 'utf8')

test('usage labels do not invent personal usage for missing historical snapshots', () => {
  assert.equal(projectUsageLabel('personal'), '个人用途')
  assert.equal(projectUsageLabel('commercial'), '商业用途')
  for (const missing of [null, undefined, '', 'other']) {
    assert.equal(projectUsageLabel(missing), '未记录用途')
    assert.equal(isProjectUsage(missing), false)
  }
  assert.equal(PROJECT_USAGE_OPTIONS.length, 2)
})

test('create and edit submit usage; edit restores the persisted value', () => {
  assert.match(source('views/dashboard/ProjectNew.vue'), /usageType: form\.usage/)
  const edit = source('views/dashboard/ProjectEdit.vue')
  assert.match(edit, /form\.usageType = project\.value\.usageType/)
  assert.match(edit, /usageType: form\.usageType/)
  assert.match(edit, /isProjectUsage\(form\.usageType\)/)
})

test('hall replaces mode badge; detail shows usage and order only reads immutable snapshot', () => {
  const hall = source('views/projects/ProjectList.vue')
  assert.match(hall, /projectUsageLabel\(project\.usageType\)/)
  assert.doesNotMatch(hall, /project\.mode === 1 \? '定向邀请' : '公开招募'/)
  assert.match(source('views/projects/ProjectDetail.vue'), /projectUsageLabel\(project\.value\?\.usageType\)/)
  assert.match(source('views/orders/OrderDetail.vue'), /projectUsageLabel\(projectSnapshot\.usageType\)/)
  assert.doesNotMatch(source('utils/projectFilters.js'), /usageType/)
})
