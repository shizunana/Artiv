<template>
  <el-dialog :model-value="modelValue" title="交易评价" width="560px" destroy-on-close @close="close">
    <div class="review-order"><span>评价订单</span><strong>{{ orderTitle || `订单 #${orderId}` }}</strong></div>
    <div class="score-row"><span>合作评分</span><el-rate v-model="form.score" :colors="['#f4a620', '#f4a620', '#f4a620']" /></div>
    <el-input v-model="form.content" type="textarea" :rows="5" maxlength="500" show-word-limit placeholder="分享本次合作体验（选填）" />
    <div class="upload-head"><span>评价图片（选填）</span><small>最多 6 张</small></div>
    <el-upload
      v-model:file-list="fileList"
      list-type="picture-card"
      :http-request="uploadImage"
      :limit="6"
      :on-remove="removeImage"
      accept="image/png,image/jpeg,image/gif,image/webp"
    >
      <span class="upload-plus">＋</span>
    </el-upload>
    <el-alert class="review-tip" title="评价提交后立即公开，且不能修改或撤回；请确认内容真实、与本次交易相关。" type="info" :closable="false" />
    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">提交评价</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createReview } from '@/api/review'
import { uploadFile } from '@/api/upload'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  orderId: { type: [Number, String], default: null },
  orderTitle: { type: String, default: '' },
})
const emit = defineEmits(['update:modelValue', 'submitted'])
const form = reactive({ score: 5, content: '', images: [] })
const fileList = ref([])
const submitting = ref(false)

watch(() => props.modelValue, (opened) => {
  if (opened) reset()
})

function reset() {
  form.score = 5
  form.content = ''
  form.images = []
  fileList.value = []
}
function close() { emit('update:modelValue', false) }
async function uploadImage(options) {
  try {
    const url = await uploadFile(options.file)
    form.images.push(url)
    options.onSuccess(url)
  } catch (error) { options.onError(error) }
}
function removeImage(file) {
  const url = file.response || file.url
  form.images = form.images.filter((item) => item !== url)
}
async function submit() {
  if (!props.orderId) return
  submitting.value = true
  try {
    const review = await createReview(props.orderId, {
      score: form.score,
      content: form.content.trim() || undefined,
      images: form.images,
    })
    ElMessage.success('评价已提交')
    emit('submitted', review)
    close()
  } finally { submitting.value = false }
}
</script>

<style scoped>
.review-order { display: grid; gap: 6px; padding: 13px 15px; margin-bottom: 18px; background: #f5f8fb; border-radius: 6px; }
.review-order span, .upload-head small { color: #8a94a0; font-size: 12px; }
.score-row, .upload-head { display: flex; align-items: center; justify-content: space-between; margin: 18px 0 12px; }
.score-row { justify-content: flex-start; gap: 20px; margin-top: 0; }
.upload-plus { color: #87919c; font-size: 28px; }
.review-tip { margin-top: 18px; }
</style>
