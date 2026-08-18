<template>
  <div class="artist-page">
    <header class="artist-hero page-container">
      <h1>画师大厅</h1>
      <p>发现适合企划与委托需求的认证画师</p>
    </header>

    <main class="artist-main page-container">
      <section class="artist-toolbar" aria-label="画师筛选">
        <el-select
          v-model="filters.subjects"
          class="subject-select"
          multiple
          collapse-tags
          collapse-tags-tooltip
          clearable
          placeholder="题材风格（可多选）"
          @change="reloadArtists"
        >
          <el-option v-for="item in subjectOptions" :key="item" :label="item" :value="item" />
        </el-select>

        <el-input
          v-model="filters.keyword"
          class="artist-search"
          clearable
          placeholder="搜索画师昵称、账号、简介或题材风格"
          @keyup.enter="reloadArtists"
          @clear="reloadArtists"
        >
          <template #prefix>⌕</template>
        </el-input>
        <el-button type="primary" @click="reloadArtists">搜索</el-button>
      </section>

      <div v-loading="loading" class="artist-results">
        <article
          v-for="artist in artists"
          :key="artist.id"
          class="artist-row"
          @click="goProfile(artist.id)"
        >
          <section class="artist-summary">
            <el-avatar :size="70" :src="artist.avatar || undefined" class="artist-avatar">
              {{ artist.nickname?.slice(0, 1) || '绘' }}
            </el-avatar>
            <div class="artist-copy">
              <div class="artist-name-line">
                <h2>{{ artist.nickname || artist.username }}</h2>
                <el-tag size="small" type="success" effect="light">已认证</el-tag>
              </div>
              <p class="artist-account">@{{ artist.username }}</p>
              <p class="artist-bio">{{ artist.bio || '这位画师暂未填写个人简介。' }}</p>
              <div class="artist-tags">
                <el-tag v-for="tag in splitTags(artist.tags)" :key="tag" size="small" effect="plain">
                  {{ tag }}
                </el-tag>
                <span v-if="splitTags(artist.tags).length === 0" class="muted">暂未设置题材风格</span>
              </div>
            </div>
          </section>

          <section class="artist-works">
            <div
              v-for="work in artist.works"
              :key="work.id"
              class="work-preview"
              :title="work.title"
            >
              <img :src="work.image" :alt="work.title" />
              <span>{{ work.title }}</span>
            </div>
            <div v-if="!artist.works?.length" class="no-works">
              <span>暂无公开作品</span>
              <small>画师已认证，可通过题材风格了解擅长方向</small>
            </div>
          </section>
        </article>

        <EmptyState
          v-if="!loading && artists.length === 0"
          description="没有找到符合条件的认证画师，请调整筛选条件"
        />
        <div ref="loadSentinel" class="artist-load-sentinel" aria-live="polite">
          <span v-if="loadingMore">正在加载更多画师…</span>
          <span v-else-if="artists.length && !hasMore">已加载全部 {{ total }} 名画师</span>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import EmptyState from '@/components/common/EmptyState.vue'
import { getArtists } from '@/api/user'
import { SUBJECT_OPTIONS } from '@/constants/subjects'

const router = useRouter()
const loading = ref(false)
const loadingMore = ref(false)
const artists = ref([])
const page = ref(1)
const size = 10
const total = ref(0)
const hasMore = ref(true)
const loadSentinel = ref(null)
const subjectOptions = SUBJECT_OPTIONS
const filters = reactive({
  subjects: [],
  keyword: '',
})
let loadObserver = null
let loadVersion = 0

onMounted(() => {
  reloadArtists()
  loadObserver = new IntersectionObserver((entries) => {
    if (entries.some((entry) => entry.isIntersecting)) loadArtists(false)
  }, { rootMargin: '500px 0px' })
  if (loadSentinel.value) loadObserver.observe(loadSentinel.value)
})

onBeforeUnmount(() => loadObserver?.disconnect())

function reloadArtists() {
  loadArtists(true)
}

async function loadArtists(reset = false) {
  if (!reset && (loading.value || loadingMore.value || !hasMore.value)) return
  const version = reset ? ++loadVersion : loadVersion
  const targetPage = reset ? 1 : page.value + 1
  if (reset) {
    page.value = 1
    artists.value = []
    total.value = 0
    hasMore.value = true
    loading.value = true
  } else {
    loadingMore.value = true
  }
  try {
    const data = await getArtists({
      page: targetPage,
      size,
      subjects: filters.subjects.length ? filters.subjects.join(',') : undefined,
      keyword: filters.keyword.trim() || undefined,
    })
    if (version !== loadVersion) return
    const records = data.records || []
    if (reset) {
      artists.value = records
    } else {
      const knownIds = new Set(artists.value.map((artist) => artist.id))
      artists.value = [...artists.value, ...records.filter((artist) => !knownIds.has(artist.id))]
    }
    page.value = targetPage
    total.value = data.total || 0
    hasMore.value = artists.value.length < total.value && records.length === size
  } finally {
    if (version === loadVersion) {
      loading.value = false
      loadingMore.value = false
    }
  }
}

function splitTags(value) {
  return (value || '').split(',').map((item) => item.trim()).filter(Boolean)
}

function goProfile(id) {
  router.push({ name: 'ArtistProfile', params: { id }, query: { role: 'painter' } })
}

</script>

<style scoped>
.artist-page {
  min-height: calc(100vh - 56px);
  padding: 42px 0 64px;
  background: #f5f7fa;
}

.artist-hero {
  text-align: center;
  margin-bottom: 28px;
}

.artist-hero h1 {
  margin: 0;
  color: var(--artiv-text-primary);
  font-size: 30px;
  font-weight: 600;
}

.artist-hero p {
  margin: 10px 0 0;
  color: var(--artiv-text-secondary);
  font-size: 14px;
}

.artist-main {
  max-width: 1180px;
}

.artist-toolbar {
  display: grid;
  grid-template-columns: minmax(280px, 360px) minmax(320px, 1fr) auto;
  gap: 12px;
  align-items: center;
  padding: 18px 20px;
  margin-bottom: 14px;
  background: #fff;
  border: 1px solid var(--artiv-border);
  border-radius: 8px;
  box-shadow: 0 4px 18px rgba(42, 63, 84, 0.04);
}

.subject-select,
.artist-search {
  width: 100%;
}

.artist-results {
  min-height: 220px;
  background: #fff;
  border: 1px solid var(--artiv-border);
  border-radius: 8px;
  overflow: hidden;
}

.artist-load-sentinel {
  display: flex;
  min-height: 52px;
  align-items: center;
  justify-content: center;
  color: var(--artiv-text-secondary);
  font-size: 13px;
}

.artist-row {
  display: grid;
  grid-template-columns: minmax(300px, 360px) minmax(0, 1fr);
  gap: 30px;
  min-height: 190px;
  padding: 26px 28px;
  border-bottom: 1px solid #edf0f4;
  cursor: pointer;
  transition: background 0.2s ease;
}

.artist-row:last-child {
  border-bottom: 0;
}

.artist-row:hover {
  background: #fbfdff;
}

.artist-summary {
  display: flex;
  gap: 18px;
  min-width: 0;
  align-items: flex-start;
}

.artist-avatar {
  flex: 0 0 auto;
  background: linear-gradient(145deg, #e9f8ff, #00a8e8);
  color: #fff;
  font-size: 24px;
  border: 3px solid #fff;
  box-shadow: 0 2px 10px rgba(0, 168, 232, 0.16);
}

.artist-copy {
  min-width: 0;
}

.artist-name-line {
  display: flex;
  gap: 10px;
  align-items: center;
}

.artist-name-line h2 {
  margin: 2px 0 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--artiv-text-primary);
}

.artist-account {
  margin: 6px 0;
  color: #9aa2ad;
  font-size: 12px;
}

.artist-bio {
  display: -webkit-box;
  margin: 10px 0 14px;
  overflow: hidden;
  color: var(--artiv-text-secondary);
  font-size: 13px;
  line-height: 1.7;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.artist-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.muted {
  color: #a4abb4;
  font-size: 12px;
}

.artist-works {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
  align-self: center;
}

.work-preview {
  position: relative;
  height: 132px;
  padding: 0;
  overflow: hidden;
  border: 0;
  border-radius: 5px;
  background: #f0f3f7;
}

.work-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.25s ease;
}

.work-preview:hover img {
  transform: scale(1.04);
}

.work-preview span {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  padding: 18px 8px 7px;
  overflow: hidden;
  color: #fff;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.65));
  font-size: 12px;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.no-works {
  grid-column: 1 / -1;
  display: flex;
  min-height: 132px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 1px dashed #d9e1e8;
  border-radius: 5px;
  color: #87919c;
  background: #fafbfd;
}

.no-works small {
  margin-top: 7px;
  color: #b1b8c0;
}

@media (max-width: 1000px) {
  .artist-toolbar {
    grid-template-columns: 1fr 1fr;
  }

  .artist-row {
    grid-template-columns: 1fr;
  }

  .artist-works {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .artist-page {
    padding-top: 24px;
  }

  .artist-toolbar {
    grid-template-columns: 1fr;
  }

  .artist-row {
    padding: 22px 18px;
  }

  .artist-summary {
    flex-direction: column;
  }

  .artist-works {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
