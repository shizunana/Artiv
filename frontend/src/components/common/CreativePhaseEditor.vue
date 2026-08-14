<template>
  <div class="phase-editor">
    <div v-for="(phase, index) in phases" :key="index" class="phase-row">
      <span class="phase-index">{{ index + 1 }}</span>
      <el-input
        :model-value="phase.name"
        maxlength="20"
        show-word-limit
        placeholder="阶段名称"
        @update:model-value="updateName(index, $event)"
      />
      <el-button link :disabled="index === 0" @click="move(index, -1)">上移</el-button>
      <el-button link :disabled="index === phases.length - 1" @click="move(index, 1)">下移</el-button>
      <el-button link type="danger" :disabled="phases.length === 1" @click="remove(index)">删除</el-button>
    </div>
    <el-button class="add-phase" plain :disabled="phases.length >= 10" @click="add">+ 添加阶段</el-button>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: {
    type: Array,
    required: true,
  },
})

const emit = defineEmits(['update:modelValue'])
const phases = computed(() => props.modelValue)

function commit(next) {
  emit('update:modelValue', next)
}

function updateName(index, name) {
  commit(props.modelValue.map((phase, phaseIndex) => phaseIndex === index ? { name } : { ...phase }))
}

function add() {
  if (props.modelValue.length < 10) commit([...props.modelValue, { name: '' }])
}

function remove(index) {
  if (props.modelValue.length > 1) commit(props.modelValue.filter((_, phaseIndex) => phaseIndex !== index))
}

function move(index, offset) {
  const target = index + offset
  if (target < 0 || target >= props.modelValue.length) return
  const next = props.modelValue.map((phase) => ({ ...phase }))
  ;[next[index], next[target]] = [next[target], next[index]]
  commit(next)
}
</script>

<style scoped>
.phase-editor { width: 100%; }
.phase-row { display: grid; grid-template-columns: 30px minmax(180px, 1fr) auto auto auto; gap: 8px; align-items: center; margin-bottom: 10px; }
.phase-index { display: grid; place-items: center; width: 26px; height: 26px; border-radius: 50%; background: #eef8fc; color: var(--artiv-primary); font-size: 12px; font-weight: 600; }
.add-phase { width: 100%; margin-top: 2px; border-style: dashed; }
@media (max-width: 640px) {
  .phase-row { grid-template-columns: 28px minmax(0, 1fr); }
  .phase-row :deep(.el-button) { margin-left: 0; justify-self: start; }
}
</style>
