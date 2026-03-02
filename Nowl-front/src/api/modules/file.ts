import { request } from '../request'
import { FILE_API } from '@/config/apiPaths'

/**
 * 文件上传相关API
 */

// 单文件上传
export const uploadFile = (fileOrFormData: File | FormData) => {
  const formData = fileOrFormData instanceof FormData
    ? fileOrFormData
    : (() => { const fd = new FormData(); fd.append('file', fileOrFormData); return fd; })()

  return request.post<string>(FILE_API.UPLOAD, formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 批量文件上传（后端参数名固定为 files）
export const uploadFiles = (filesOrFormData: File[] | FormData) => {
  const formData = filesOrFormData instanceof FormData
    ? filesOrFormData
    : (() => {
      const fd = new FormData()
      filesOrFormData.forEach(file => fd.append('files', file))
      return fd
    })()

  return request.post<string[]>(FILE_API.UPLOAD_BATCH, formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 删除文件
export const deleteFile = (url: string) => {
  return request.delete(FILE_API.DELETE, { params: { url } })
}
