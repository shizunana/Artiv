<template>
  <button class="delete-manage-button" type="button">
    <el-icon aria-hidden="true"><Delete /></el-icon>
    <span><slot /></span>
  </button>
</template>

<script setup>
import { Delete } from '@element-plus/icons-vue'

// 仅复用删除入口的外观；点击事件透传，删除条件与流程仍由各业务页面处理。
</script>

<style scoped>
.delete-manage-button { display: inline-flex; align-items: center; justify-content: center; gap: 7px; min-height: 34px; padding: 7px 14px; border: 1px solid #e4e8ed; border-radius: 999px; background: #f8fafc; color: #617080; font: inherit; font-size: 13px; font-weight: 500; line-height: 18px; cursor: pointer; transition: background .18s ease, border-color .18s ease, color .18s ease; }
.delete-manage-button .el-icon { font-size: 16px; }
.delete-manage-button:hover { color: #c34851; background: #fff4f4; border-color: #efd0d3; }
.delete-manage-button:active { background: #ffe9eb; }
.delete-manage-button:focus-visible { outline: 2px solid var(--artiv-primary); outline-offset: 3px; }
</style>
