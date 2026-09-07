<template>
  <div v-if="presentation === 'buttons'" class="subject-buttons" role="group" aria-label="题材风格（可多选）">
    <button type="button" class="subject-button" :class="{ selected: !modelValue.length }"
      :aria-pressed="!modelValue.length" :disabled="!category" @click="clear">不限</button>
    <button v-for="item in projectSubjects(category)" :key="item" type="button" class="subject-button"
      :class="{ selected: modelValue.includes(item) }" :aria-pressed="modelValue.includes(item)"
      :disabled="!category" @click="toggle(item)">{{ item }}</button>
  </div>
  <el-select v-else :model-value="modelValue" multiple filterable clearable collapse-tags collapse-tags-tooltip
    :max-collapse-tags="3" :disabled="!category" placeholder="选择题材风格（选填）"
    class="project-subject-select" @update:model-value="$emit('update:modelValue', $event)">
    <el-option v-for="item in projectSubjects(category)" :key="item" :label="item" :value="item" />
  </el-select>
</template>

<script setup>
import { watch } from 'vue'
import { ElMessage } from 'element-plus'
import { projectSubjects, retainProjectSubjects } from '@/constants/projectSubjects'
const props = defineProps({ category: String, modelValue: { type: Array, default: () => [] }, presentation: { type: String, default: 'select' } })
const emit = defineEmits(['update:modelValue'])
function clear() { emit('update:modelValue', []) }
function toggle(item) {
  if (!projectSubjects(props.category).includes(item)) return
  emit('update:modelValue', props.modelValue.includes(item)
    ? props.modelValue.filter(value => value !== item) : [...props.modelValue, item])
}
watch(() => props.category, category => {
  const retained = retainProjectSubjects(category, props.modelValue)
  if (retained.length !== props.modelValue.length) {
    emit('update:modelValue', retained)
    ElMessage.info('已移除不适用于当前企划类型的题材，其余选择已保留')
  }
}, { flush: 'sync' })
</script>

<style scoped>
.project-subject-select { width: 100%; max-width: 520px; }
.subject-buttons { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; width: 100%; }
.subject-button { min-width: 0; min-height: 42px; padding: 10px 8px; border: 1px solid #e5e8ed; border-radius: 10px; background: #fff; color: var(--artiv-text, #303744); font: inherit; font-size: 14px; cursor: pointer; box-shadow: 0 1px 2px rgb(20 35 55 / 3%); transition: background .15s, border-color .15s, box-shadow .15s; }
.subject-button:hover:not(:disabled) { background: #f7fafc; border-color: #cad8e3; }
.subject-button.selected { color: var(--artiv-primary, #00a8df); background: #edf8ff; border-color: #a5def3; font-weight: 500; box-shadow: 0 1px 3px rgb(0 150 210 / 6%); }
.subject-button:focus-visible { outline: 2px solid var(--artiv-primary, #00a8df); outline-offset: 3px; }
.subject-button:disabled { cursor: not-allowed; opacity: .5; }
@media (max-width: 600px) { .subject-buttons { grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 8px; } }
@media (prefers-reduced-motion: reduce) { .subject-button { transition: none; } }
</style>
