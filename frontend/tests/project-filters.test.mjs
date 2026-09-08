import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { parse, compileScript } from '@vue/compiler-sfc'
import { reactive, effectScope, nextTick, computed, ref, watch } from 'vue'
import { PROJECT_CATEGORY_OPTIONS } from '../src/constants/projectCategories.js'
import { PROJECT_SUBJECT_OPTIONS_BY_CATEGORY, PROJECT_SUBJECT_OPTIONS, retainProjectSubjects, projectSubjects } from '../src/constants/projectSubjects.js'
import { copyProjectFilter, toggleProjectCategory, projectFilterParams } from '../src/utils/projectFilters.js'

test('project subject lists match backend guide and retain only intersection on category change', () => {
  const config = JSON.parse(readFileSync(new URL('../../backend/src/main/resources/guide/search-options.json', import.meta.url), 'utf8'))
  assert.deepEqual(PROJECT_SUBJECT_OPTIONS_BY_CATEGORY, config.projectSubjectsByCategory)
  assert.deepEqual(PROJECT_SUBJECT_OPTIONS, config.projectSubjects)
  assert.equal(PROJECT_SUBJECT_OPTIONS.length, 18)
  assert.equal(config.subjects.length, 35)
  assert.deepEqual(retainProjectSubjects('立绘',['安头','古风','有背景']),['古风','有背景'])
  assert.deepEqual(PROJECT_SUBJECT_OPTIONS_BY_CATEGORY['平面设计'],['日系','古风','黑白','像素','Q版'])
})
test('zero/single/multiple types normalize multi-subject params and clear subjects when type changes', () => {
  assert.deepEqual(projectFilterParams({categories:[],subjects:['古风']}),{})
  const single = {categories:['头像'],subjects:['古风','日系','古风']}
  assert.deepEqual(projectFilterParams(single),{projectCategories:['头像'],subjects:['日系','古风']})
  const multi = toggleProjectCategory(single,'插画')
  assert.deepEqual(multi.subjects,[])
  assert.deepEqual(projectFilterParams(multi),{projectCategories:['头像','插画']})
  assert.deepEqual(toggleProjectCategory(multi,'头像').subjects,[])
  assert.deepEqual(copyProjectFilter({categories:['角色设计'],subjects:['像素']}).subjects,[])
  assert.deepEqual(copyProjectFilter({categories:['头像'],subject:'古风'}),{categories:['头像'],subjects:['古风']})
  assert.deepEqual(single,{categories:['头像'],subjects:['古风','日系','古风']})
})

// 编译实际 SFC setup，注入 Vue 响应式和轻量 UI 替身，不启动服务、不请求业务。
function component(name) {
  const source=readFileSync(new URL('../src/components/projects/'+name+'.vue',import.meta.url),'utf8')
  const script=compileScript(parse(source).descriptor,{id:name}).content
    .replace(/^import .*$/gm,'').replace('export default','return')
  const imports={computed,ref,watch,PROJECT_CATEGORY_OPTIONS,projectSubjects,retainProjectSubjects,
    copyProjectFilter,toggleProjectCategory,ElMessage:{info(){}},ArrowDown:{}}
  return new Function(...Object.keys(imports),script)(...Object.values(imports))
}
const Popover = component('ProjectFilterPopover')

test('popover subject heading and multi-select options share a single-category condition without empty hints', () => {
  const source = readFileSync(new URL('../src/components/projects/ProjectFilterPopover.vue', import.meta.url), 'utf8')
  const { descriptor } = parse(source)
  const template = descriptor.template.content
  assert.match(template, /<template v-if="draft.categories.length === 1">\s*<h3>题材风格<\/h3>\s*<div[^>]+aria-label="题材风格（可多选）"/)
  assert.match(template, /draft\.subjects\.includes\(subject\)/)
  assert.doesNotMatch(template, /filter-hint|选择一个企划类型可细筛题材|多选类型时不筛选题材/)
})
test('popover edits/reset/cancel stay drafts; only confirm emits a copied filter', () => {
  const scope=effectScope(), emitted=[]
  const props=reactive({modelValue:{categories:['头像'],subjects:['古风','日系']}})
  const state=scope.run(() => Popover.setup(props,{expose(){},emit:(...args)=>emitted.push(args)}))
  state.begin()
  state.draft.value=toggleProjectCategory(state.draft.value,'插画')
  assert.equal(emitted.length,0)
  state.close(); state.begin()
  assert.deepEqual(state.draft.value,{categories:['头像'],subjects:['日系','古风']})
  state.toggleSubject('Q版'); state.toggleSubject('日系')
  assert.deepEqual(state.draft.value.subjects,['古风','Q版'])
  state.draft.value=copyProjectFilter()
  assert.equal(emitted.length,0)
  state.apply()
  assert.deepEqual(emitted,[['apply',{categories:[],subjects:[]}]])
  assert.deepEqual(props.modelValue,{categories:['头像'],subjects:['古风','日系']})
  scope.stop()
})

const Picker = component('ProjectSubjectSelect')
test('publish subject buttons use empty selection for unlimited and toggle real tags only', () => {
  const scope = effectScope(), emitted = []
  const props = reactive({category:'头像',modelValue:[],presentation:'buttons'})
  const state = scope.run(() => Picker.setup(props,{expose(){},emit:(name,value) => {
    emitted.push([name,value]); props.modelValue=value
  }}))
  state.toggle('古风'); state.toggle('安头')
  assert.deepEqual(props.modelValue,['古风','安头'])
  state.toggle('古风')
  assert.deepEqual(props.modelValue,['安头'])
  state.clear()
  assert.deepEqual(props.modelValue,[])
  state.toggle('不限'); state.toggle('场景')
  assert.deepEqual(props.modelValue,[])
  assert.equal(emitted.length,4)
  state.toggle('日系'); state.toggle('日系')
  assert.deepEqual(props.modelValue,[])
  scope.stop()
})
test('edit subject picker preserves intersection on a type switch', async () => {
  const scope=effectScope(), emitted=[]
  const props=reactive({category:'头像',modelValue:['古风','有背景','安头']})
  scope.run(() => Picker.setup(props,{expose(){},emit:(...args)=>emitted.push(args)}))
  props.category='立绘'; await nextTick()
  assert.deepEqual(emitted,[['update:modelValue',['古风','有背景']]])
  scope.stop()
})
