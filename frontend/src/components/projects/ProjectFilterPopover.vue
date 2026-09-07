<template>
  <el-popover v-model:visible="visible" trigger="click" placement="bottom-start" :show-arrow="false"
    width="480" popper-class="project-filter-popover" @show="begin">
    <template #reference>
      <button ref="trigger" type="button" class="filter-trigger" :class="{ 'has-value': modelValue.categories.length }"
        :aria-expanded="visible" aria-haspopup="dialog" aria-label="筛选企划类型与题材">
        <span>{{ summary }}</span><el-icon :class="{ rotated: visible }"><ArrowDown /></el-icon>
      </button>
    </template>
    <section role="dialog" aria-label="企划类型与题材筛选" class="filter-panel" @keydown.esc.stop="close">
      <h3>企划类型</h3>
      <div class="option-grid" role="group" aria-label="企划类型（可多选）">
        <button v-for="category in PROJECT_CATEGORY_OPTIONS" :key="category" type="button" class="filter-option"
          :class="{ selected: draft.categories.includes(category) }" :aria-pressed="draft.categories.includes(category)"
          @click="draft = toggleProjectCategory(draft, category)">{{ category }}</button>
      </div>
      <template v-if="draft.categories.length === 1">
        <h3>题材风格</h3>
        <div class="option-grid" role="group" aria-label="题材风格（选填）">
          <button v-for="subject in subjects" :key="subject" type="button" class="filter-option"
            :class="{ selected: draft.subject === subject }" :aria-pressed="draft.subject === subject"
            @click="draft.subject = draft.subject === subject ? '' : subject">{{ subject }}</button>
        </div>
      </template>
      <footer><el-button @click="draft = copyProjectFilter()">重置</el-button><el-button type="primary" @click="apply">确定</el-button></footer>
    </section>
  </el-popover>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ArrowDown } from '@element-plus/icons-vue'
import { PROJECT_CATEGORY_OPTIONS } from '@/constants/projectCategories'
import { projectSubjects } from '@/constants/projectSubjects'
import { copyProjectFilter, toggleProjectCategory } from '@/utils/projectFilters'
const props = defineProps({ modelValue: { type: Object, required: true } })
const emit = defineEmits(['apply'])
const visible = ref(false), trigger = ref(null), draft = ref(copyProjectFilter())
const subjects = computed(() => projectSubjects(draft.value.categories[0]))
const summary = computed(() => {
  const { categories, subject } = props.modelValue
  if (!categories.length) return '类型与题材'
  return categories.length > 1 ? `已选 ${categories.length} 类` : categories[0] + (subject ? ` · ${subject}` : '')
})
function begin() { draft.value = copyProjectFilter(props.modelValue) }
function close() { visible.value = false; trigger.value?.focus() }
function apply() { emit('apply', copyProjectFilter(draft.value)); close() }
</script>

<style scoped>
.filter-trigger { display: flex; align-items: center; justify-content: space-between; gap: 12px; width: var(--project-filter-width, 148px); min-width: 0; flex-shrink: 0; height: var(--project-filter-height, 40px); box-sizing: border-box; padding: 0 14px; border: 1px solid var(--el-border-color); border-radius: 6px; background: white; color: var(--el-text-color-regular); font: inherit; font-size: 14px; cursor: pointer; }
.filter-trigger > span { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.filter-trigger.has-value { color: var(--el-color-primary); }
.filter-trigger .el-icon { flex-shrink: 0; transition: transform .15s; }
.rotated { transform: rotate(180deg); }
.filter-panel { padding: 6px; color: var(--el-text-color-primary); }
h3 { font-size: 13px; font-weight: 400; color: var(--el-text-color-secondary); margin: 2px 0 12px; }
h3:not(:first-child) { margin-top: 24px; }
.option-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 8px; }
.filter-option { min-width: 0; min-height: 38px; padding: 8px 4px; border: 0; border-radius: 5px; background: transparent; color: var(--el-text-color-primary); font: inherit; font-size: 13px; cursor: pointer; }
.filter-option:hover { background: var(--el-color-primary-light-9); }
.filter-option.selected { background: #e9f6ff; color: var(--el-color-primary); font-weight: 500; }
.filter-option:focus-visible, .filter-trigger:focus-visible { outline: 2px solid var(--el-color-primary); outline-offset: 2px; }
footer { display: flex; justify-content: flex-end; gap: 10px; margin-top: 22px; padding-top: 16px; border-top: 1px solid var(--el-border-color-lighter); }
footer .el-button + .el-button { margin-left: 0; }
@media (max-width: 520px) { .option-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); } }
@media (prefers-reduced-motion: reduce) { .filter-trigger .el-icon { transition: none; } }
</style>
<style>
.el-popover.project-filter-popover { max-width: calc(100vw - 32px); box-sizing: border-box; }
</style>
