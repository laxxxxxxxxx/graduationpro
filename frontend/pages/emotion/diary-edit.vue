<template>
  <view class="container">
    <view class="form">
      <view class="form-item">
        <text class="label">日期</text>
        <picker mode="date" :value="form.diaryDate" @change="onDateChange">
          <view class="picker">{{ form.diaryDate || '选择日期' }}</view>
        </picker>
      </view>
      
      <view class="form-item">
        <text class="label">心情评分</text>
        <slider 
          :value="form.moodScore" 
          min="1" 
          max="10" 
          show-value
          @change="onMoodChange"
        />
      </view>
      
      <view class="form-item">
        <text class="label">情绪标签</text>
        <view class="tags">
          <view 
            class="tag" 
            v-for="tag in emotionTags"
            :key="tag"
            :class="{active: selectedTags.includes(tag)}"
            @click="toggleTag(tag)"
          >
            {{ tag }}
          </view>
        </view>
      </view>
      
      <view class="form-item">
        <text class="label">日记内容</text>
        <textarea 
          class="textarea" 
          v-model="form.content" 
          placeholder="记录今天的心情..."
          maxlength="1000"
        />
      </view>
    </view>
    
    <button class="submit-btn" @click="submit">保存</button>
  </view>
</template>

<script>
import { createDiary, updateDiary, getDiaryDetail } from '@/api/emotion'

export default {
  data() {
    return {
      diaryId: null,
      form: {
        diaryDate: new Date().toISOString().split('T')[0],
        moodScore: 5,
        content: '',
        moodTags: ''
      },
      emotionTags: ['开心', '平静', '焦虑', '悲伤', '愤怒', '疲惫', '兴奋', '孤独'],
      selectedTags: []
    }
  },
  
  onLoad(options) {
    if (options.id) {
      this.diaryId = options.id
      this.loadDiaryDetail(options.id)
    }
  },
  
  methods: {
    async loadDiaryDetail(id) {
      try {
        const data = await getDiaryDetail(id)
        
        // 填充表单
        this.form = {
          diaryDate: this.formatDate(data.diaryDate),
          moodScore: data.moodScore || 5,
          content: data.content || '',
          moodTags: data.moodTags || ''
        }
        
        // 恢复选中的标签
        if (data.moodTags) {
          this.selectedTags = data.moodTags.split(',')
        }
      } catch (error) {
        console.error('加载日记详情失败:', error)
      }
    },
    
    formatDate(dateStr) {
      if (!dateStr) return new Date().toISOString().split('T')[0]
      const date = new Date(dateStr)
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}`
    },
    
    onDateChange(e) {
      this.form.diaryDate = e.detail.value
    },
    
    onMoodChange(e) {
      this.form.moodScore = e.detail.value
    },
    
    toggleTag(tag) {
      const index = this.selectedTags.indexOf(tag)
      if (index > -1) {
        this.selectedTags.splice(index, 1)
      } else {
        this.selectedTags.push(tag)
      }
      // 更新moodTags字段
      this.form.moodTags = this.selectedTags.join(',')
    },
    
    async submit() {
      if (!this.form.content.trim()) {
        uni.showToast({ title: '请填写内容', icon: 'none' })
        return
      }
      
      try {
        uni.showLoading({ title: '保存中...', mask: true })
        
        // 确保日期格式正确 (YYYY-MM-DD)
        const submitData = { ...this.form }
        console.log('提交日记数据:', submitData)
        
        if (this.diaryId) {
          // 更新
          await updateDiary(this.diaryId, submitData)
        } else {
          // 创建
          await createDiary(submitData)
        }
        
        uni.hideLoading()
        uni.showToast({ title: '保存成功', icon: 'success' })
        
        setTimeout(() => {
          uni.navigateBack()
        }, 1500)
      } catch (error) {
        uni.hideLoading()
        console.error('保存失败:', error)
        const msg = error.message || (error.data && error.data.message) || '保存失败'
        uni.showToast({ title: msg, icon: 'none', duration: 3000 })
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.container {
  padding: $spacing-md;
  padding-bottom: $spacing-xl;
}

.form {
  background: #ffffff;
  border-radius: 32rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 8rpx 24rpx rgba(255, 140, 140, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.8);
  padding: 40rpx;
  margin-bottom: $spacing-lg;
  
  .form-item {
    margin-bottom: $spacing-xl;
    
    &:last-child {
      margin-bottom: 0;
    }
    
    .label {
      display: block;
      font-size: $font-sm;
      color: $text-secondary;
      margin-bottom: 20rpx;
      font-weight: 500;
      padding-left: 8rpx;
    }
    
    .picker {
      padding: 24rpx 30rpx;
      background: $bg-color;
      border: 1rpx solid transparent;
      border-radius: $radius-md;
      font-size: $font-md;
      color: $text-primary;
      transition: all 0.3s;
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      &::after {
        content: '›';
        font-size: 40rpx;
        color: $text-tertiary;
      }
    }
    
    .tags {
      display: flex;
      flex-wrap: wrap;
      gap: 16rpx;
      
      .tag {
        padding: 14rpx 32rpx;
        background: $bg-color;
        border-radius: $radius-xl;
        font-size: $font-sm;
        color: $text-secondary;
        transition: all 0.3s;
        border: 1rpx solid transparent;
        
        &.active {
          background: $primary-light;
          color: $primary-color;
          border-color: $primary-color;
          font-weight: 600;
          box-shadow: $shadow-sm;
        }
      }
    }
    
    .textarea {
      width: 100%;
      height: 400rpx;
      padding: 30rpx;
      background: $bg-color;
      border: 1rpx solid transparent;
      border-radius: $radius-md;
      font-size: $font-md;
      color: $text-primary;
      line-height: 1.6;
      box-sizing: border-box;
      
      &:focus {
        background: #fff;
        border-color: $primary-color;
        box-shadow: $shadow-sm;
      }
    }
  }
}

// 深度自定义 slider 样式可能受平台限制，这里主要优化间距
slider {
  margin: 20rpx 0;
}

.submit-btn {
  background: linear-gradient(135deg, #FF9A9E 0%, #FECFEF 100%);
  color: #fff;
  border-radius: 50rpx;
  border: none;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6rpx 16rpx rgba(255, 140, 140, 0.2);
  width: 100%;
  height: 100rpx;
  font-size: $font-lg;
  margin-top: $spacing-xl;
}
</style>
