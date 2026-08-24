<template>
  <nav class="favorite-shortcuts" aria-label="收藏快捷入口">
    <button v-for="item in items" :key="item.type" type="button" @click="open(item.type)">
      <el-icon><component :is="item.icon" /></el-icon>
      <span>{{ item.label }}</span>
    </button>
  </nav>
</template>

<script setup>
import { Picture, Shop, User } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

const props = defineProps({ workbench: { type: String, required: true } })
const router = useRouter()
const items = [
  { type: 'painters', label: '画师收藏', icon: User },
  { type: 'works', label: '作品收藏', icon: Picture },
  { type: 'stalls', label: '橱窗收藏', icon: Shop },
]

function open(type) {
  router.push({ path: `/${props.workbench}/favorites`, query: { type } })
}
</script>

<style scoped>
.favorite-shortcuts {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  padding: 18px 10px;
  background: #fff;
  border: 1px solid #e4ebf0;
  border-radius: 12px;
  box-shadow: 0 8px 28px rgb(42 67 84 / 6%);
}
.favorite-shortcuts button {
  min-width: 0;
  padding: 2px 4px;
  color: #47545f;
  cursor: pointer;
  background: transparent;
  border: 0;
  border-right: 1px solid #edf1f4;
  font: inherit;
  transition: color .2s ease;
}
.favorite-shortcuts button:last-child { border-right: 0; }
.favorite-shortcuts button:hover { color: var(--artiv-primary); }
.favorite-shortcuts .el-icon { display: block; margin: 0 auto 7px; font-size: 20px; }
.favorite-shortcuts span { display: block; overflow: hidden; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
</style>
