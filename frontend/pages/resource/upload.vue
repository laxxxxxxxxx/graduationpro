<template>
  <view class="container">
    <view class="header">
      <text class="header-title">上传教育资源</text>
      <text class="header-subtitle">请填写资源详细信息</text>
    </view>

    <view class="form-box">
      <!-- 资源标题 -->
      <view class="form-item">
        <text class="label">资源标题</text>
        <input class="input" v-model="form.title" placeholder="请输入标题" />
      </view>

      <!-- 资源类型 -->
      <view class="form-item">
        <text class="label">资源类型</text>
        <picker @change="onTypeChange" :value="typeIndex" :range="types" range-key="name">
          <view class="picker-value">{{ types[typeIndex].name }}</view>
        </picker>
      </view>

      <!-- 资源分类 -->
      <view class="form-item">
        <text class="label">资源分类</text>
        <picker @change="onCategoryChange" :value="categoryIndex" :range="categories" range-key="name">
          <view class="picker-value">{{ categories[categoryIndex] ? categories[categoryIndex].name : '加载中...' }}</view>
        </picker>
      </view>

      <!-- 难度等级 -->
      <view class="form-item">
        <text class="label">难度等级</text>
        <picker @change="onDifficultyChange" :value="difficultyIndex" :range="difficulties" range-key="name">
          <view class="picker-value">{{ difficulties[difficultyIndex].name }}</view>
        </picker>
      </view>

      <!-- 封面图 -->
      <view class="form-item">
        <text class="label">封面图</text>
        <view class="upload-area" @click="chooseImage">
          <image v-if="form.coverUrl" :src="resolveCoverUrl(form.coverUrl)" mode="aspectFill" class="preview-img"></image>
          <view v-else class="upload-placeholder">
            <text class="icon">+</text>
            <text>上传封面</text>
          </view>
        </view>
      </view>

      <!-- 资源内容/文件 -->
      <view class="form-item">
        <text class="label">{{ contentLabel }}</text>
        
        <!-- 文章类型：显示输入框 -->
        <textarea 
          v-if="form.type === 1" 
          class="textarea" 
          v-model="form.content" 
          placeholder="请输入文章内容（支持简单HTML）" 
          maxlength="-1"
        ></textarea>
        
        <!-- 媒体类型：显示上传按钮 -->
        <view v-else class="upload-area" @click="chooseMedia">
          <view v-if="form.mediaUrl || form.content" class="media-preview">
            <text class="media-name">{{ fileName || '已上传媒体文件' }}</text>
            <text class="re-upload">重新上传</text>
          </view>
          <view v-else class="upload-placeholder">
            <text class="icon">📁</text>
            <text>上传{{ types[typeIndex].name }}文件</text>
          </view>
        </view>
      </view>

      <!-- 资源时长 (可选) -->
      <view class="form-item" v-if="form.type !== 1">
        <text class="label">资源时长 (秒)</text>
        <input class="input" type="number" v-model="form.duration" placeholder="例如: 600" />
      </view>

      <!-- 标签 -->
      <view class="form-item">
        <text class="label">标签 (用逗号分隔)</text>
        <input class="input" v-model="form.tags" placeholder="例如: 压力, 放松, 冥想" />
      </view>

      <!-- 作者/来源 -->
      <view class="form-row">
        <view class="form-item half">
          <text class="label">作者</text>
          <input class="input" v-model="form.author" placeholder="作者名称" />
        </view>
        <view class="form-item half">
          <text class="label">来源</text>
          <input class="input" v-model="form.source" placeholder="资源来源" />
        </view>
      </view>

      <!-- 资源描述 -->
      <view class="form-item">
        <text class="label">资源描述</text>
        <textarea class="textarea small" v-model="form.description" placeholder="请输入资源简介"></textarea>
      </view>

      <button class="submit-btn" :loading="submitting" @click="handleSubmit">提交资源</button>
    </view>
  </view>
</template>

<script>
import { getCategories, createResource } from '@/api/resource'
import { uploadFile } from '@/api/common'
import { resolveUrl } from '@/utils/request'

export default {
  data() {
    return {
      form: {
        title: '',
        type: 1,
        categoryId: null,
        coverUrl: '',
        content: '',
        mediaUrl: '',
        duration: null,
        difficulty: 1,
        tags: '',
        author: '',
        source: '',
        description: ''
      },
      types: [
        { id: 1, name: '文章' },
        { id: 2, name: '视频' },
        { id: 3, name: '音频' },
        { id: 4, name: '课程' }
      ],
      typeIndex: 0,
      categories: [],
      categoryIndex: 0,
      difficulties: [
        { id: 1, name: '入门' },
        { id: 2, name: '进阶' },
        { id: 3, name: '高级' }
      ],
      difficultyIndex: 0,
      fileName: '',
      submitting: false
    }
  },
  
  computed: {
    contentLabel() {
      return this.form.type === 1 ? '文章内容' : '媒体文件'
    }
  },

  onLoad() {
    this.loadCategories()
  },

  methods: {
    async loadCategories() {
      try {
        const res = await getCategories()
        this.categories = res || []
        if (this.categories.length > 0) {
          this.form.categoryId = this.categories[0].id
        }
      } catch (error) {
        console.error('加载分类失败:', error)
      }
    },

    onTypeChange(e) {
      this.typeIndex = e.detail.value
      this.form.type = this.types[this.typeIndex].id
      // 重置内容和媒体链接
      this.form.content = ''
      this.form.mediaUrl = ''
      this.fileName = ''
    },

    onCategoryChange(e) {
      this.categoryIndex = e.detail.value
      this.form.categoryId = this.categories[this.categoryIndex].id
    },

    onDifficultyChange(e) {
      this.difficultyIndex = e.detail.value
      this.form.difficulty = this.difficulties[this.difficultyIndex].id
    },

    chooseImage() {
      uni.chooseImage({
        count: 1,
        success: async (res) => {
          const tempFilePath = res.tempFilePaths[0]
          uni.showLoading({ title: '上传封面...' })
          try {
            const url = await uploadFile(tempFilePath, 'image')
            this.form.coverUrl = url
            uni.showToast({ title: '上传成功', icon: 'success' })
          } catch (error) {
            console.error('上传封面失败:', error)
            uni.showToast({ title: '上传失败', icon: 'none' })
          } finally {
            uni.hideLoading()
          }
        }
      })
    },

    chooseMedia() {
      const type = this.form.type
      if (type === 2 || type === 4) {
        // 视频
        uni.chooseVideo({
          sourceType: ['album', 'camera'],
          success: (res) => {
            this.uploadMediaFile(res.tempFilePath, 'video')
          }
        })
      } else if (type === 3) {
        // 音频 - 小程序原生 API 较弱，这里简化为从相册选择或文件选择
        // 注意：H5 环境和微信小程序环境表现不同
        uni.chooseFile({
          count: 1,
          type: 'all',
          success: (res) => {
            this.uploadMediaFile(res.tempFilePaths[0], 'audio')
          }
        })
      }
    },

    async uploadMediaFile(filePath, type) {
      uni.showLoading({ title: '上传媒体文件...' })
      try {
        const url = await uploadFile(filePath, type)
        this.form.mediaUrl = url
        this.form.content = url // 某些业务逻辑中内容存的是链接
        this.fileName = filePath.split('/').pop()
        uni.showToast({ title: '上传成功', icon: 'success' })
      } catch (error) {
        console.error('上传媒体失败:', error)
        uni.showToast({ title: '上传失败', icon: 'none' })
      } finally {
        uni.hideLoading()
      }
    },

    async handleSubmit() {
      // 表单校验
      if (!this.form.title.trim()) {
        return uni.showToast({ title: '请输入标题', icon: 'none' })
      }
      if (this.form.type === 1 && !this.form.content.trim()) {
        return uni.showToast({ title: '请输入文章内容', icon: 'none' })
      }
      if (this.form.type !== 1 && !this.form.mediaUrl) {
        return uni.showToast({ title: '请上传媒体文件', icon: 'none' })
      }

      this.submitting = true
      try {
        await createResource(this.form)
        uni.showToast({ title: '资源上传成功', icon: 'success' })
        setTimeout(() => {
          uni.navigateBack()
        }, 1500)
      } catch (error) {
        console.error('提交资源失败:', error)
        uni.showToast({ title: '提交失败', icon: 'none' })
      } finally {
        this.submitting = false
      }
    },

    resolveCoverUrl(url) {
      return resolveUrl(url)
    }
  }
}
</script>

<style lang="scss" scoped>
.container {
  padding: 40rpx;
  background-color: #f8f9fa;
  min-height: 100vh;
}

.header {
  margin-bottom: 40rpx;
  .header-title {
    font-size: 40rpx;
    font-weight: 700;
    color: #333;
    display: block;
  }
  .header-subtitle {
    font-size: 24rpx;
    color: #999;
    margin-top: 8rpx;
    display: block;
  }
}

.form-box {
  background-color: #fff;
  border-radius: 20rpx;
  padding: 30rpx;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.05);
}

.form-item {
  margin-bottom: 30rpx;
  .label {
    font-size: 28rpx;
    font-weight: 600;
    color: #444;
    margin-bottom: 16rpx;
    display: block;
  }
  .input {
    width: 100%;
    height: 90rpx;
    background-color: #f5f6f7;
    border-radius: 12rpx;
    padding: 0 30rpx;
    font-size: 28rpx;
    box-sizing: border-box;
  }
  .textarea {
    width: 100%;
    height: 300rpx;
    background-color: #f5f6f7;
    border-radius: 12rpx;
    padding: 20rpx 30rpx;
    font-size: 28rpx;
    box-sizing: border-box;
    &.small {
      height: 160rpx;
    }
  }
  .picker-value {
    width: 100%;
    height: 90rpx;
    line-height: 90rpx;
    background-color: #f5f6f7;
    border-radius: 12rpx;
    padding: 0 30rpx;
    font-size: 28rpx;
    color: #333;
    box-sizing: border-box;
  }
}

.form-row {
  display: flex;
  gap: 20rpx;
  .half {
    flex: 1;
  }
}

.upload-area {
  width: 100%;
  height: 240rpx;
  background-color: #f5f6f7;
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 2rpx dashed #ddd;

  .preview-img {
    width: 100%;
    height: 100%;
  }

  .upload-placeholder {
    display: flex;
    flex-direction: column;
    align-items: center;
    color: #999;
    .icon {
      font-size: 60rpx;
      margin-bottom: 10rpx;
    }
    text {
      font-size: 24rpx;
    }
  }

  .media-preview {
    display: flex;
    flex-direction: column;
    align-items: center;
    .media-name {
      font-size: 28rpx;
      color: #333;
      margin-bottom: 10rpx;
      text-align: center;
      padding: 0 40rpx;
      word-break: break-all;
    }
    .re-upload {
      font-size: 24rpx;
      color: #FF9A9E;
    }
  }
}

.submit-btn {
  margin-top: 40rpx;
  height: 100rpx;
  line-height: 100rpx;
  background: linear-gradient(135deg, #FF9A9E 0%, #FECFEF 100%);
  color: #fff;
  border-radius: 50rpx;
  font-weight: 700;
  font-size: 32rpx;
  box-shadow: 0 8rpx 20rpx rgba(255, 140, 140, 0.3);
  border: none;
}
</style>
