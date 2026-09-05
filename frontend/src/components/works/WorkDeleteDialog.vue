<template>
  <el-dialog
    v-model="visible"
    title="删除作品"
    width="min(720px, calc(100vw - 32px))"
    append-to-body
    align-center
    destroy-on-close
    :close-on-click-modal="!deleting"
    :close-on-press-escape="!deleting"
    :show-close="!deleting"
    @open="open"
    @closed="reset"
  >
    <p class="delete-description">选择需要删除的作品。公开和隐藏的作品均可删除，删除后会同时隐藏，不再出现在作品广场，且无法恢复。</p>

    <div class="selection-toolbar">
      <el-checkbox
        :model-value="allPageSelected"
        :indeterminate="somePageSelected && !allPageSelected"
        :disabled="loading || deleting || !works.length || !!loadError"
        @change="selectPage"
      >全选本页</el-checkbox>
      <span>已选 <strong>{{ selectedIds.length }}</strong> 件<span v-if="selectedIds.length">（含其他页）</span></span>
      <el-button link :disabled="deleting || !selectedIds.length" @click="selectedIds = []">清空选择</el-button>
    </div>

    <div v-loading="loading" class="delete-work-list" :aria-busy="loading || deleting">
      <div v-if="loadError" class="load-error" role="alert">
        <p>{{ loadError }}</p>
        <el-button :disabled="loading || deleting" @click="loadWorks">重新加载</el-button>
      </div>
      <el-empty v-else-if="!loading && !works.length" description="还没有可删除的作品" />
      <label
        v-for="work in works"
        v-else
        :key="work.id"
        class="delete-work-row"
        :class="{ 'is-selected': selectedIds.includes(work.id), 'is-disabled': deleting || loading }"
      >
        <input
          type="checkbox"
          :checked="selectedIds.includes(work.id)"
          :disabled="deleting || loading"
          :aria-label="`选择作品：${work.title || '未命名作品'}`"
          @change="toggleSelection(work.id, $event.target.checked)"
        >
        <el-image class="work-thumbnail" :src="work.image" fit="cover">
          <template #error><span class="image-fallback">暂无图片</span></template>
        </el-image>
        <span class="work-copy">
          <strong :title="work.title">{{ work.title || '未命名作品' }}</strong>
          <small>{{ work.category || '未分类' }} · {{ work.subject || '未设置题材' }}</small>
        </span>
        <span class="visibility-label" :class="{ 'is-public': Number(work.visibility) === 1 }">
          {{ Number(work.visibility) === 1 ? '公开' : '仅自己可见' }}
        </span>
      </label>
    </div>

    <el-pagination
      v-if="total > pageSize"
      class="delete-pagination"
      :current-page="page"
      :page-size="pageSize"
      :total="total"
      :pager-count="5"
      :disabled="loading || deleting"
      layout="prev, pager, next"
      @current-change="changePage"
    />

    <template #footer>
      <el-button :disabled="deleting" @click="visible = false">取消</el-button>
      <el-button type="danger" :disabled="loading || !selectedIds.length || !!loadError" :loading="deleting" @click="confirmDelete">
        {{ deleting ? `正在删除 ${processedCount} / ${batchCount}` : `删除选中作品（${selectedIds.length}）` }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteWork, getMyWorks } from '@/api/work'

const props = defineProps({ modelValue: { type: Boolean, default: false } })
const emit = defineEmits(['update:modelValue', 'deleted'])
const visible = computed({
  get: () => props.modelValue,
  set: (value) => { if (!deleting.value) emit('update:modelValue', value) },
})
const works = ref([])
const selectedIds = ref([])
const loading = ref(false)
const deleting = ref(false)
const loadError = ref('')
const page = ref(1)
const pageSize = 12
const total = ref(0)
const processedCount = ref(0)
const batchCount = ref(0)
let loadVersion = 0

const allPageSelected = computed(() => works.value.length > 0 && works.value.every((work) => selectedIds.value.includes(work.id)))
const somePageSelected = computed(() => works.value.some((work) => selectedIds.value.includes(work.id)))

function toggleSelection(id, checked) {
  if (deleting.value || loading.value) return
  selectedIds.value = checked
    ? [...new Set([...selectedIds.value, id])]
    : selectedIds.value.filter((item) => item !== id)
}

function selectPage(checked) {
  if (deleting.value || loading.value) return
  const pageIds = works.value.map((work) => work.id)
  selectedIds.value = checked
    ? [...new Set([...selectedIds.value, ...pageIds])]
    : selectedIds.value.filter((id) => !pageIds.includes(id))
}

function open() {
  reset()
  loadWorks()
}

async function loadWorks() {
  const version = ++loadVersion
  loading.value = true
  loadError.value = ''
  try {
    let data = await getMyWorks({ page: page.value, size: pageSize })
    if (version !== loadVersion) return
    const lastPage = Math.max(1, Math.ceil(Number(data.total || 0) / pageSize))
    if (page.value > lastPage) {
      page.value = lastPage
      data = await getMyWorks({ page: page.value, size: pageSize })
      if (version !== loadVersion) return
    }
    works.value = data.records || []
    total.value = Number(data.total || 0)
  } catch {
    if (version === loadVersion) {
      works.value = []
      loadError.value = '作品列表加载失败，请重试。'
    }
  } finally {
    if (version === loadVersion) loading.value = false
  }
}

function changePage(value) {
  page.value = value
  loadWorks()
}

async function confirmDelete() {
  if (deleting.value || loading.value || !selectedIds.value.length) return
  // 固定本次选中项；确认及执行期间锁定弹窗，避免重复点击或选择变化。
  const ids = [...selectedIds.value]
  deleting.value = true
  processedCount.value = 0
  batchCount.value = ids.length
  try {
    const confirmed = await ElMessageBox.confirm(
      `确定删除选中的 ${ids.length} 件作品吗（包含其他页的选择）？删除后会同时隐藏，且无法恢复。`,
      '确认删除作品',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '保留作品' },
    ).then(() => true).catch(() => false)
    if (!confirmed) return

    const succeeded = []
    const failed = []
    // 复用单件删除的作者校验；每件独立提交，部分失败时保留失败项供重试。
    for (const id of ids) {
      try {
        await deleteWork(id, { silentError: true })
        succeeded.push(id)
      } catch {
        failed.push(id)
      }
      processedCount.value += 1
    }
    selectedIds.value = failed
    if (succeeded.length) {
      ElMessage.success(`已删除 ${succeeded.length} 件作品`)
      emit('deleted', succeeded)
    }
    if (failed.length) {
      ElMessage.warning(`${failed.length} 件作品未能确认删除，请刷新核对后重试`)
      await loadWorks()
    } else {
      emit('update:modelValue', false)
    }
  } finally {
    deleting.value = false
  }
}

function reset() {
  loadVersion += 1
  works.value = []
  selectedIds.value = []
  page.value = 1
  total.value = 0
  loadError.value = ''
  loading.value = false
}
</script>

<style scoped>
.delete-description { margin: 0 0 20px; color: var(--artiv-text-secondary); font-size: 14px; line-height: 1.8; }
.selection-toolbar { display: flex; align-items: center; gap: 14px; padding: 0 4px 12px; border-bottom: 1px solid var(--artiv-border); }
.selection-toolbar > span { margin-left: auto; color: var(--artiv-text-secondary); font-size: 13px; }
.selection-toolbar strong { color: var(--artiv-text); }
.delete-work-list { min-height: 220px; max-height: min(440px, 48vh); overflow-y: auto; padding: 8px 0; }
.delete-work-row { display: flex; align-items: center; gap: 14px; padding: 12px 10px; margin: 3px 0; border: 1px solid transparent; border-radius: 10px; cursor: pointer; }
.delete-work-row:hover { background: #f8fafc; }
.delete-work-row.is-selected { background: #f0f9fd; border-color: #d3edf7; }
.delete-work-row.is-disabled { cursor: default; }
.delete-work-row input { width: 16px; height: 16px; margin: 0; flex-shrink: 0; accent-color: var(--artiv-primary); }
.delete-work-row input:focus-visible { outline: 2px solid var(--artiv-primary); outline-offset: 3px; }
.work-thumbnail { width: 62px; height: 62px; border-radius: 7px; flex-shrink: 0; background: #f1f4f7; }
.image-fallback { display: grid; height: 100%; place-items: center; color: var(--artiv-text-secondary); font-size: 11px; }
.work-copy { display: grid; gap: 7px; min-width: 0; flex: 1; }
.work-copy strong, .work-copy small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.work-copy strong { color: var(--artiv-text); font-size: 14px; font-weight: 500; }
.work-copy small { color: var(--artiv-text-secondary); font-size: 12px; }
.visibility-label { color: #7b8792; font-size: 12px; white-space: nowrap; }
.visibility-label.is-public { color: #3d8772; }
.delete-pagination { justify-content: center; margin-top: 12px; }
.load-error { padding: 48px 16px; text-align: center; color: var(--artiv-text-secondary); }
@media (max-width: 520px) {
  .selection-toolbar { flex-wrap: wrap; gap: 6px 10px; }
  .delete-work-row { gap: 9px; padding-inline: 6px; }
  .work-thumbnail { width: 44px; height: 44px; }
  .visibility-label { font-size: 11px; }
}
</style>
