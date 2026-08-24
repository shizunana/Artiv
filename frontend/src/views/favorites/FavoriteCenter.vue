<template>
  <section class="favorite-center wb-card">
    <header class="favorite-heading">
      <div>
        <h1>我的收藏</h1>
        <p>统一管理关注的画师、收藏的作品和橱窗。</p>
      </div>
      <el-button :loading="loading" @click="load">刷新</el-button>
    </header>

    <div class="favorite-tabs" role="tablist">
      <button
        v-for="tab in tabs"
        :key="tab.type"
        type="button"
        :class="{ active: activeType === tab.type }"
        @click="selectType(tab.type)"
      >{{ tab.label }}</button>
    </div>

    <div v-loading="loading" class="favorite-body">
      <div v-if="activeType === 'painters' && records.length" class="painter-list">
        <article v-for="item in records" :key="item.painterId" class="painter-item">
          <el-avatar :size="58" :src="item.avatar || undefined">{{ initial(item.nickname, '画') }}</el-avatar>
          <div class="painter-copy">
            <strong>{{ item.nickname }}</strong>
            <p>{{ item.bio || '这位画师暂未填写个人简介。' }}</p>
            <div class="item-tags">
              <el-tag v-for="tag in splitTags(item.tags)" :key="tag" size="small" effect="plain">{{ tag }}</el-tag>
            </div>
          </div>
          <div class="item-actions">
            <el-button type="primary" plain @click="openPainter(item.painterId)">查看主页</el-button>
            <el-button @click="removePainter(item.painterId)">取消关注</el-button>
          </div>
        </article>
      </div>

      <div v-else-if="activeType === 'works' && records.length" class="favorite-grid">
        <article v-for="item in records" :key="item.workId" class="favorite-card" @click="openWork(item.workId)">
          <img v-if="item.image" :src="item.image" :alt="item.title" />
          <div v-else class="card-placeholder">暂无图片</div>
          <div class="card-copy">
            <strong>{{ item.title }}</strong>
            <span>{{ item.authorNickname }}</span>
            <div class="card-actions"><el-button link type="danger" @click.stop="removeWork(item.workId)">取消收藏</el-button></div>
          </div>
        </article>
      </div>

      <div v-else-if="activeType === 'stalls' && records.length" class="favorite-grid">
        <article v-for="item in records" :key="item.stallId" class="favorite-card" @click="openStall(item.stallId)">
          <img v-if="item.cover" :src="item.cover" :alt="item.title" />
          <div v-else class="card-placeholder">暂无封面</div>
          <div class="card-copy">
            <strong>{{ item.title }}</strong>
            <span>{{ item.painterNickname }}</span>
            <div class="stall-card-meta">
              <b>¥{{ item.price }}</b>
              <el-button link type="danger" @click.stop="removeStall(item.stallId)">取消收藏</el-button>
            </div>
          </div>
        </article>
      </div>

      <EmptyState v-else-if="!loading" :description="emptyText" />
    </div>

    <div v-if="total > size" class="favorite-pagination">
      <el-pagination v-model:current-page="page" background layout="prev, pager, next" :page-size="size" :total="total" @current-change="load" />
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import EmptyState from '@/components/common/EmptyState.vue'
import {
  getFavoriteStalls,
  getFavoriteWorks,
  getFollowedPainters,
  unfavoriteStall,
  unfavoriteWork,
  unfollowPainter,
} from '@/api/social'

const route = useRoute()
const router = useRouter()
const tabs = [
  { type: 'painters', label: '关注的画师' },
  { type: 'works', label: '收藏的作品' },
  { type: 'stalls', label: '收藏的橱窗' },
]
const page = ref(1)
const size = 12
const total = ref(0)
const records = ref([])
const loading = ref(false)
const validTypes = new Set(tabs.map((item) => item.type))
const activeType = computed(() => validTypes.has(route.query.type) ? route.query.type : 'painters')
const emptyText = computed(() => ({
  painters: '暂未关注画师',
  works: '暂未收藏作品',
  stalls: '暂未收藏橱窗',
}[activeType.value]))

onMounted(load)
watch(activeType, () => {
  page.value = 1
  load()
})

function selectType(type) {
  if (type === activeType.value) return
  router.replace({ query: { ...route.query, type } })
}

async function load() {
  loading.value = true
  try {
    const params = { page: page.value, size }
    let data
    if (activeType.value === 'painters') data = await getFollowedPainters(params)
    if (activeType.value === 'works') data = await getFavoriteWorks(params)
    if (activeType.value === 'stalls') data = await getFavoriteStalls(params)
    records.value = data?.records || []
    total.value = Number(data?.total || 0)
  } finally {
    loading.value = false
  }
}

function initial(value, fallback) { return String(value || fallback).slice(0, 1) }
function splitTags(value) { return String(value || '').split(',').map((item) => item.trim()).filter(Boolean).slice(0, 4) }
function openPainter(id) { router.push({ name: 'ArtistProfile', params: { id }, query: { role: 'painter' } }) }
function openWork(id) { router.push({ name: 'Works', query: { workId: id } }) }
function openStall(id) { router.push({ name: 'StallDetail', params: { id } }) }

async function removePainter(id) { await unfollowPainter(id); await refreshAfterRemove() }
async function removeWork(id) { await unfavoriteWork(id); await refreshAfterRemove() }
async function removeStall(id) { await unfavoriteStall(id); await refreshAfterRemove() }
async function refreshAfterRemove() {
  if (records.value.length === 1 && page.value > 1) page.value -= 1
  await load()
}
</script>

<style scoped>
.favorite-center { min-height: 520px; padding: 24px; background: #fff; border: 1px solid #e4ebf0; border-radius: 12px; box-shadow: 0 8px 28px rgb(42 67 84 / 5%); }
.favorite-heading { display: flex; align-items: start; justify-content: space-between; gap: 20px; }
.favorite-heading h1 { margin: 0; color: #26323d; font-size: 22px; }
.favorite-heading p { margin: 7px 0 0; color: var(--artiv-text-secondary); }
.favorite-tabs { display: flex; gap: 34px; margin-top: 22px; border-bottom: 1px solid var(--artiv-border); }
.favorite-tabs button { position: relative; padding: 0 2px 13px; color: #56636f; cursor: pointer; background: transparent; border: 0; font: inherit; }
.favorite-tabs button.active { color: var(--artiv-primary); font-weight: 600; }
.favorite-tabs button.active::after { position: absolute; right: 0; bottom: -1px; left: 0; height: 2px; content: ''; background: var(--artiv-primary); }
.favorite-body { min-height: 330px; padding-top: 18px; }
.painter-list { display: flex; flex-direction: column; }
.painter-item { display: flex; align-items: center; gap: 16px; padding: 18px 4px; border-bottom: 1px solid #edf1f4; }
.painter-copy { flex: 1; min-width: 0; }
.painter-copy strong { color: #27323d; font-size: 16px; }
.painter-copy p { margin: 7px 0; overflow: hidden; color: #6f7b87; text-overflow: ellipsis; white-space: nowrap; }
.item-tags { display: flex; flex-wrap: wrap; gap: 5px; }
.item-actions { display: flex; flex-shrink: 0; }
.favorite-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 18px; }
.favorite-card { overflow: hidden; cursor: pointer; background: #fff; border: 1px solid #e2e8ed; border-radius: 9px; transition: transform .2s ease, box-shadow .2s ease; }
.favorite-card:hover { transform: translateY(-2px); box-shadow: 0 9px 24px rgb(43 68 86 / 10%); }
.favorite-card img, .card-placeholder { width: 100%; aspect-ratio: 4 / 3; object-fit: cover; }
.card-placeholder { display: grid; color: #9aa6b1; background: #edf2f5; place-items: center; }
.card-copy { padding: 13px 14px 10px; }
.card-copy strong { display: block; overflow: hidden; color: #26323d; font-size: 15px; text-overflow: ellipsis; white-space: nowrap; }
.card-copy > span { display: block; margin-top: 7px; color: #7a8691; font-size: 12px; }
.card-actions { margin-top: 5px; text-align: right; }
.stall-card-meta { display: flex; align-items: center; justify-content: space-between; margin-top: 7px; }
.stall-card-meta b { color: var(--artiv-accent); font-size: 17px; }
.favorite-pagination { display: flex; justify-content: center; margin-top: 24px; }
@media (max-width: 760px) {
  .favorite-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .painter-item { align-items: flex-start; }
  .item-actions { flex-direction: column; }
}
</style>
