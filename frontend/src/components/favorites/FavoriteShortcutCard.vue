<template>
  <nav class="favorite-shortcuts" aria-label="收藏快捷入口">
    <button v-for="item in items" :key="item.route.name" type="button" @click="open(item.route)">
      <el-icon><component :is="item.icon" /></el-icon>
      <span>{{ item.label }}</span>
    </button>
  </nav>
</template>

<script setup>
import { Picture, Shop, User } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const items = [
  { label: '画师收藏', icon: User, route: { name: 'Artists', query: { view: 'following' } } },
  { label: '作品收藏', icon: Picture, route: { name: 'Works', query: { view: 'favorites' } } },
  { label: '橱窗收藏', icon: Shop, route: { name: 'Stalls', query: { view: 'favorites' } } },
]

function open(route) {
  router.push(route)
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
