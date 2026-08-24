<template>
  <div class="list-page page-container">
    <section class="work-controls" aria-label="作品搜索">
      <el-input v-model="keyword" clearable class="search-input" placeholder="搜索作品标题、作品说明" @keyup.enter="reload" @clear="reload">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
    </section>

    <div v-loading="loading" class="work-waterfall" aria-label="作品瀑布流">
      <section v-for="(column, index) in waterfallColumns" :key="index" class="waterfall-column">
        <aside v-if="index === 0" class="filter-panel" aria-label="作品筛选">
          <section class="filter-group">
            <h3>作品分类</h3>
            <div class="category-options">
              <button v-for="category in categories" :key="category" type="button" class="category-option" :class="{ 'is-active': activeCategory === category }" @click="setCategory(category)">
                {{ category }}
              </button>
            </div>
          </section>

          <section class="filter-group">
            <h3>题材风格</h3>
            <el-select v-model="selectedSubjects" multiple collapse-tags collapse-tags-tooltip clearable class="subject-select" placeholder="选择题材风格" @change="reload">
              <el-option v-for="item in subjectOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </section>

          <section class="filter-group filter-group--last">
            <h3>发布时间</h3>
            <div class="sort-options">
              <button type="button" class="sort-option" :class="{ 'is-active': sort === 'newest' }" @click="setSort('newest')">最新发布</button>
              <button type="button" class="sort-option" :class="{ 'is-active': sort === 'oldest' }" @click="setSort('oldest')">最早发布</button>
            </div>
          </section>
        </aside>

        <article v-for="work in column" :key="work.id" class="work-card" :aria-label="work.title" @click="openWork(work.id)">
          <img v-if="work.image" :src="work.image" :alt="work.title" class="work-image">
          <div v-else class="work-placeholder" :title="work.title"><span>🎨</span></div>
        </article>
      </section>
    </div>

    <EmptyState v-if="!loading && works.length === 0" description="暂无符合筛选条件的公开作品" />
    <div ref="loadSentinel" class="load-sentinel" aria-live="polite">
      <span v-if="loadingMore">正在加载更多作品…</span>
      <span v-else-if="works.length && !hasMore">已加载全部 {{ total }} 件作品</span>
    </div>

    <el-dialog v-model="detailVisible" width="900px" top="7vh" append-to-body class="work-detail-dialog" :show-close="false" @closed="handleDetailClosed">
      <template #header>
        <div class="detail-author">
          <el-avatar :size="52" :src="detail.authorAvatar">{{ authorInitial }}</el-avatar>
          <div class="detail-author__copy">
            <strong>{{ detail.authorName }}</strong>
            <span v-if="detail.authorCertStatus === 2" class="cert-label">已认证画师</span>
            <p>{{ detail.authorBio || '这位画师暂未填写个人简介。' }}</p>
          </div>
          <div class="detail-author__actions">
            <el-button v-if="!isDetailOwner" class="work-favorite-button" :loading="favoriteLoading" @click="toggleFavorite">
              <FavoriteIcon :active="favorited" />
              {{ favorited ? '已收藏' : '收藏' }}
            </el-button>
            <el-button type="primary" plain class="profile-button" @click="openProfilePlaceholder">进入主页</el-button>
          </div>
        </div>
      </template>

      <div v-loading="detailLoading" class="work-detail-content">
        <img v-if="detail.image" :src="detail.image" :alt="detail.title" class="work-detail-image">
        <div v-else class="work-detail-empty">暂无作品图片</div>
        <section class="work-detail-info">
          <div class="work-detail-heading">
            <div>
              <h2>{{ detail.title }}</h2>
              <p>{{ formatDate(detail.createdAt) }}</p>
            </div>
            <el-button text type="danger" disabled>举报作品</el-button>
          </div>
          <p v-if="detail.description" class="work-description">{{ detail.description }}</p>
          <div class="work-tags">
            <span class="tag-title">作品标签</span>
            <el-tag v-for="tag in detailTags" :key="tag" effect="plain">{{ tag }}</el-tag>
            <span v-if="detailTags.length === 0" class="tag-empty">未设置题材风格</span>
          </div>
        </section>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { getWorkDetail, getWorks } from '@/api/work'
import { CATEGORY_OPTIONS, SUBJECT_OPTIONS } from '@/constants/subjects'
import EmptyState from '@/components/common/EmptyState.vue'
import FavoriteIcon from '@/components/common/FavoriteIcon.vue'
import { favoriteWork, getWorkFavoriteStatus, unfavoriteWork } from '@/api/social'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const works = ref([])
const loading = ref(false)
const loadingMore = ref(false)
const page = ref(1)
const size = 25
const total = ref(0)
const hasMore = ref(true)
const loadSentinel = ref(null)
const activeCategory = ref('全部')
const selectedSubjects = ref([])
const keyword = ref('')
const sort = ref('newest')
const categories = ['全部', ...CATEGORY_OPTIONS]
const subjectOptions = SUBJECT_OPTIONS
const waterfallColumns = ref([])
const columnCount = ref(getColumnCount())
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = reactive({})
const favorited = ref(false)
const favoriteLoading = ref(false)
const detailTags = computed(() => (detail.subject || '').split(',').map(tag => tag.trim()).filter(Boolean))
const authorInitial = computed(() => (detail.authorName || '画').slice(0, 1))
const isDetailOwner = computed(() => Number(userStore.user?.id) === Number(detail.userId))
const imageRatioCache = new Map()
let loadObserver = null
let loadVersion = 0
let arrangeVersion = 0

function getColumnCount() {
  if (window.innerWidth <= 560) return 2
  if (window.innerWidth <= 900) return 3
  return 5
}

function getImageRatio(src) {
  if (!src) return Promise.resolve(1)
  if (imageRatioCache.has(src)) return Promise.resolve(imageRatioCache.get(src))
  return new Promise(resolve => {
    const image = new Image()
    image.onload = () => {
      const ratio = image.naturalWidth && image.naturalHeight ? image.naturalWidth / image.naturalHeight : 1
      imageRatioCache.set(src, ratio)
      resolve(ratio)
    }
    image.onerror = () => {
      imageRatioCache.set(src, 1)
      resolve(1)
    }
    image.src = src
  })
}

async function arrangeWorks() {
  const version = ++arrangeVersion
  const ratios = await Promise.all(works.value.map(work => getImageRatio(work.image)))
  if (version !== arrangeVersion) return
  const columnHeights = Array(columnCount.value).fill(0)
  const columns = Array.from({ length: columnCount.value }, () => [])

  works.value.forEach((work, index) => {
    const targetIndex = columnHeights.indexOf(Math.min(...columnHeights))
    columns[targetIndex].push(work)
    columnHeights[targetIndex] += 1 / ratios[index] + .07
  })
  waterfallColumns.value = columns
}

function handleResize() {
  const nextColumnCount = getColumnCount()
  if (nextColumnCount !== columnCount.value) {
    columnCount.value = nextColumnCount
    arrangeWorks()
  }
}

function setCategory(category) {
  activeCategory.value = category
  reload()
}

function setSort(value) {
  sort.value = value
  reload()
}

function reload() {
  load(true)
}

async function openWork(id) {
  detailVisible.value = true
  detailLoading.value = true
  Object.keys(detail).forEach(key => delete detail[key])
  try {
    Object.assign(detail, await getWorkDetail(id))
    favorited.value = false
    if (userStore.isLoggedIn && !isDetailOwner.value) {
      try {
        const state = await getWorkFavoriteStatus(id)
        favorited.value = Boolean(state?.favorited)
      } catch {
        favorited.value = false
      }
    }
  } catch {
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

async function toggleFavorite() {
  if (!userStore.isLoggedIn) {
    router.push({ name: 'Login', query: { redirect: route.fullPath } })
    return
  }
  if (!detail.id || isDetailOwner.value || favoriteLoading.value) return
  favoriteLoading.value = true
  try {
    const state = favorited.value ? await unfavoriteWork(detail.id) : await favoriteWork(detail.id)
    favorited.value = Boolean(state?.favorited)
    ElMessage.success(favorited.value ? '作品已收藏' : '已取消收藏')
  } finally {
    favoriteLoading.value = false
  }
}

function handleDetailClosed() {
  if (route.name !== 'Works' || !route.query.workId) return
  const query = { ...route.query }
  delete query.workId
  router.replace({ name: 'Works', query })
}

function openProfilePlaceholder() {
  if (!detail.userId) return
  detailVisible.value = false
  router.push({ name: 'ArtistProfile', params: { id: detail.userId }, query: { role: 'painter' } })
}

function formatDate(value) {
  if (!value) return ''
  return `上传时间：${String(value).replace('T', ' ').slice(0, 16)}`
}

async function load(reset = false) {
  if (!reset && (loading.value || loadingMore.value || !hasMore.value)) return
  const version = reset ? ++loadVersion : loadVersion
  const targetPage = reset ? 1 : page.value + 1
  if (reset) {
    page.value = 1
    works.value = []
    waterfallColumns.value = Array.from({ length: columnCount.value }, () => [])
    total.value = 0
    hasMore.value = true
    loading.value = true
  } else {
    loadingMore.value = true
  }
  try {
    const params = { page: targetPage, size, sort: sort.value }
    if (activeCategory.value !== '全部') params.category = activeCategory.value
    if (selectedSubjects.value.length > 0) params.subject = selectedSubjects.value.join(',')
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    const data = await getWorks(params)
    if (version !== loadVersion) return
    const records = data.records || []
    if (reset) {
      works.value = records
    } else {
      const knownIds = new Set(works.value.map(work => work.id))
      works.value = [...works.value, ...records.filter(work => !knownIds.has(work.id))]
    }
    page.value = targetPage
    total.value = data.total || 0
    hasMore.value = works.value.length < total.value && records.length === size
    await arrangeWorks()
  } finally {
    if (version === loadVersion) {
      loading.value = false
      loadingMore.value = false
    }
  }
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
  reload()
  if (route.query.workId) openWork(route.query.workId)
  loadObserver = new IntersectionObserver((entries) => {
    if (entries.some(entry => entry.isIntersecting)) load(false)
  }, { rootMargin: '500px 0px' })
  if (loadSentinel.value) loadObserver.observe(loadSentinel.value)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  loadObserver?.disconnect()
})
</script>

<style scoped>
.list-page { position: relative; z-index: 0; box-sizing: border-box; width: 88vw; max-width: 1600px; min-height: 60vh; padding: 16px 16px 48px; }
.list-page::before { position: fixed; z-index: -1; inset: var(--artiv-header-height) 0 0; background: #e6eaee; content: ''; }
.work-controls { display: flex; justify-content: flex-end; box-sizing: border-box; width: 100%; margin: 0 0 12px; padding: 10px 12px; border: 1px solid rgba(219, 226, 235, .9); border-radius: 12px; background: rgba(255, 255, 255, .92); box-shadow: 0 5px 18px rgba(48, 73, 99, .06); }.search-input { width: 320px; }.search-input :deep(.el-input__wrapper) { border-radius: 20px; background: #f8fafc; }
.work-waterfall { display: flex; align-items: flex-start; min-height: 160px; margin: 0 -7.5px; }
.waterfall-column { width: 20%; min-width: 0; padding: 0 7.5px; }
.filter-panel { box-sizing: border-box; width: 100%; margin: 0 0 15px; padding: 19px 18px; border: 1px solid rgba(219, 226, 235, .95); border-radius: 10px; background: rgba(255, 255, 255, .96); box-shadow: 0 5px 16px rgba(48, 73, 99, .07); }.filter-group { padding: 0 0 16px; margin: 0 0 16px; border-bottom: 1px solid #edf0f4; }.filter-group--last { padding-bottom: 0; margin-bottom: 0; border-bottom: 0; }.filter-group h3 { margin: 0 0 12px; color: var(--artiv-text); font-size: 15px; font-weight: 600; }.category-options { display: flex; flex-wrap: wrap; gap: 9px; }.category-option, .sort-option { padding: 7px 11px; border: 1px solid #dfe5ec; border-radius: 15px; background: #f8fafc; color: var(--artiv-text-secondary); cursor: pointer; font: inherit; font-size: 12px; line-height: 1; }.category-option:hover, .category-option.is-active, .sort-option:hover, .sort-option.is-active { color: var(--artiv-primary); border-color: var(--artiv-primary); background: #edf9ff; }.subject-select { width: 100%; }.subject-select :deep(.el-select__wrapper) { min-height: 38px; }.sort-options { display: flex; flex-wrap: wrap; gap: 9px; }
.work-card { position: relative; margin: 0 0 15px; overflow: hidden; border-radius: 8px; background: #dfe5ee; cursor: zoom-in; }
.work-card:hover .work-image { transform: scale(1.015); }
.work-image { display: block; width: 100%; height: auto; min-height: 110px; transition: transform .25s ease; }
.work-placeholder { display: grid; place-items: center; aspect-ratio: 1 / 1; background: linear-gradient(135deg, #e2f4fd, #efe8ff); color: #6d9db4; font-size: 42px; }
.load-sentinel { display: flex; min-height: 48px; align-items: center; justify-content: center; color: var(--artiv-text-secondary); font-size: 13px; }
.detail-author { display: flex; align-items: center; gap: 13px; padding-right: 38px; }.detail-author__copy { min-width: 0; flex: 1; }.detail-author__copy strong { color: var(--artiv-text); font-size: 17px; }.detail-author__copy p { overflow: hidden; margin: 5px 0 0; color: var(--artiv-text-secondary); font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }.cert-label { margin-left: 8px; color: #2ca46e; font-size: 12px; }.detail-author__actions { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }.work-favorite-button :deep(.el-icon), .work-favorite-button :deep(svg) { margin-right: 4px; }.profile-button { flex-shrink: 0; }.work-detail-content { min-height: 240px; }.work-detail-image { display: block; width: 100%; max-height: 62vh; object-fit: contain; background: #f4f7fa; }.work-detail-empty { display: grid; min-height: 300px; place-items: center; color: var(--artiv-text-secondary); background: #f4f7fa; }.work-detail-info { padding: 18px 22px 22px; }.work-detail-heading { display: flex; align-items: start; justify-content: space-between; gap: 16px; border-bottom: 1px solid var(--artiv-border); padding-bottom: 14px; }.work-detail-heading h2 { margin: 0; color: var(--artiv-text); font-size: 19px; }.work-detail-heading p { margin: 7px 0 0; color: var(--artiv-text-secondary); font-size: 12px; }.work-description { margin: 16px 0; color: var(--artiv-text); line-height: 1.8; white-space: pre-wrap; }.work-tags { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }.tag-title { margin-right: 4px; color: var(--artiv-text-secondary); font-size: 13px; }.tag-empty { color: var(--artiv-text-secondary); font-size: 13px; }
@media (max-width: 1100px) { .list-page { width: 96vw; } }
@media (max-width: 900px) { .list-page { width: 100%; }.waterfall-column { width: 33.333%; } }
@media (max-width: 560px) { .work-controls { padding: 11px; }.search-input { width: 100%; }.waterfall-column { width: 50%; }.detail-author { padding-right: 24px; }.detail-author__copy p { white-space: normal; }.profile-button { padding: 8px 10px; } }
</style>
