import request from '@/utils/request'

/** 上传文件，返回相对路径（如 /uploads/xxx.png） */
export function uploadFile(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
