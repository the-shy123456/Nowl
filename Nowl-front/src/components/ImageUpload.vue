<script setup lang="ts">
import { ref } from 'vue'
import { Camera, X } from 'lucide-vue-next'
import { ElMessage } from '@/utils/feedback'
import { uploadFile } from '@/api/modules/file'

interface Props {
  modelValue: string[]
  maxCount?: number
  maxSize?: number // MB
}

interface Emits {
  (e: 'update:modelValue', value: string[]): void
}

const props = withDefaults(defineProps<Props>(), {
  maxCount: 9,
  maxSize: 10,
})

const emit = defineEmits<Emits>()

const uploading = ref(false)
const fileInput = ref<HTMLInputElement>()

// 触发文件选择
const triggerUpload = () => {
  fileInput.value?.click()
}

// 处理文件选择
const handleFileChange = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const files = target.files

  if (!files || files.length === 0) return

  // 检查数量限制
  if (props.modelValue.length + files.length > props.maxCount) {
    ElMessage.warning(`最多只能上传${props.maxCount}张图片`)
    return
  }

  uploading.value = true

  try {
    const uploadPromises = Array.from(files).map(async (file) => {
      // 检查文件大小
      if (file.size > props.maxSize * 1024 * 1024) {
        ElMessage.warning(`图片大小不能超过${props.maxSize}MB`)
        return null
      }

      // 检查文件类型
      if (!file.type.startsWith('image/')) {
        ElMessage.warning('只能上传图片文件')
        return null
      }

      // 上传文件
      const formData = new FormData()
      formData.append('file', file)

      const res = await uploadFile(formData)
      return res
    })

    const results = await Promise.all(uploadPromises)
    const validUrls = results.filter((url) => url !== null) as string[]

    if (validUrls.length > 0) {
      emit('update:modelValue', [...props.modelValue, ...validUrls])
    }
  } catch {
    ElMessage.error('上传失败')
  } finally {
    uploading.value = false
    // 清空input，允许重复选择相同文件
    if (target) target.value = ''
  }
}

// 删除图片
const removeImage = (index: number) => {
  const newImages = [...props.modelValue]
  newImages.splice(index, 1)
  emit('update:modelValue', newImages)
}
</script>

<template>
  <div class="image-upload">
    <div class="grid grid-cols-3 gap-4">
      <!-- 已上传的图片 -->
      <div
        v-for="(url, index) in modelValue"
        :key="url"
        class="relative aspect-square rounded-2xl overflow-hidden border-2 border-slate-200 group"
      >
        <img :src="url" class="w-full h-full object-cover" />
        <button
          @click="removeImage(index)"
          class="absolute top-2 right-2 p-1.5 bg-red-500 text-white rounded-full opacity-0 group-hover:opacity-100 transition-opacity"
        >
          <X :size="16" />
        </button>
        <div
          v-if="index === 0"
          class="absolute bottom-2 left-2 px-2 py-1 bg-warm-500 text-white text-xs font-semibold rounded-lg"
        >
          封面
        </div>
      </div>

      <!-- 上传按钮 -->
      <button
        v-if="modelValue.length < maxCount"
        @click="triggerUpload"
        :disabled="uploading"
        class="aspect-square bg-slate-50 rounded-2xl border-2 border-dashed border-slate-200 flex flex-col items-center justify-center text-slate-400 hover:border-warm-300 hover:bg-warm-50 hover:text-warm-500 transition-all cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
      >
        <Camera :size="32" />
        <span class="text-xs font-bold mt-2">
          {{ uploading ? '上传中...' : '上传图片' }}
        </span>
        <span class="text-[10px] mt-1"> {{ modelValue.length }}/{{ maxCount }} </span>
      </button>
    </div>

    <!-- 隐藏的文件输入 -->
    <input
      ref="fileInput"
      type="file"
      accept="image/*"
      multiple
      class="hidden"
      @change="handleFileChange"
    />

    <!-- 提示信息 -->
    <div class="mt-2 text-xs text-slate-400">
      <p>• 支持jpg、png、gif、webp格式</p>
      <p>• 单张图片不超过{{ maxSize }}MB</p>
      <p>• 第一张图片将作为封面</p>
    </div>
  </div>
</template>
