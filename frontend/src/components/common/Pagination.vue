<template>
  <el-pagination
    v-model:current-page="current"
    v-model:page-size="pageSize"
    :total="total"
    layout="total, prev, pager, next, sizes"
    :page-sizes="[10, 20, 50]"
    background
    @change="emit('change')"
  />
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  page: { type: Number, default: 1 },
  size: { type: Number, default: 10 },
  total: { type: Number, default: 0 },
})

const emit = defineEmits(['update:page', 'update:size', 'change'])

const current = computed({
  get: () => props.page,
  set: (value) => emit('update:page', value),
})

const pageSize = computed({
  get: () => props.size,
  set: (value) => emit('update:size', value),
})
</script>
