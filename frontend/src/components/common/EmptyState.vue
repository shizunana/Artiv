<template>
  <el-empty :description="description">
    <template v-if="$slots.default" #default>
      <slot />
    </template>
  </el-empty>
</template>

<script setup>
defineProps({
  description: {
    type: String,
    default: '暂无数据',
  },
})
</script>
