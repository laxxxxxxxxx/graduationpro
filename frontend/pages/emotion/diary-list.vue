<template>
  <view class="container">
    <view class="diary-list">
      <view 
        class="diary-item" 
        v-for="diary in diaries" 
        :key="diary.id"
        @click="goEdit(diary.id)"
      >
        <view class="diary-header">
          <text class="date">{{ diary.diaryDate }}</text>
          <view class="mood-score">
            <text>心情</text>
            <text class="score">{{ diary.moodScore }}/10</text>
          </view>
        </view>
        
        <text class="content">{{ diary.content }}</text>
        
        <view class="tags" v-if="diary.moodTags">
          <text class="tag" v-for="tag in diary.moodTags.split(',')" :key="tag">
            {{ tag }}
          </text>
        </view>
      </view>
    </view>
    
    <button class="fab" @click="createDiary">+</button>
  </view>
</template>

<script>
import { getDiaryList } from '@/api/emotion'

export default {
  data() {
    return {
      diaries: [],
      loading: false
    }
  },
  
  onShow() {
    this.loadDiaries()
  },
  
  methods: {
    async loadDiaries() {
      try {
        this.loading = true
        const data = await getDiaryList({
          pageNum: 1,
          pageSize: 50
        })
        
        // 格式化日期
        this.diaries = (data || []).map(diary => ({
          ...diary,
          diaryDate: this.formatDate(diary.diaryDate)
        }))
      } catch (error) {
        console.error('加载日记失败:', error)
      } finally {
        this.loading = false
      }
    },
    
    formatDate(dateStr) {
      if (!dateStr) return ''
      const date = new Date(dateStr)
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}`
    },
    
    goEdit(id) {
      uni.navigateTo({
        url: `/pages/emotion/diary-edit?id=${id}`
      })
    },
    
    createDiary() {
      uni.navigateTo({
        url: '/pages/emotion/diary-edit'
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.container {
  padding: $spacing-md;
  padding-bottom: $spacing-xl;
}

.diary-list {
  .diary-item {
    background: #ffffff;
    border-radius: 32rpx;
    padding: 32rpx;
    margin-bottom: 24rpx;
    box-shadow: 0 8rpx 24rpx rgba(255, 140, 140, 0.08);
    border: 1px solid rgba(255, 255, 255, 0.8);
    padding: $spacing-lg;
    transition: transform 0.2s;
    
    &:active {
      transform: scale(0.99);
    }
    
    .diary-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: $spacing-md;
      
      .date {
        font-size: $font-sm;
        color: $text-tertiary;
        font-weight: 500;
      }
      
      .mood-score {
        display: flex;
        align-items: center;
        gap: 10rpx;
        font-size: $font-xs;
        color: $text-secondary;
        
        .score {
          font-size: $font-xl;
          font-weight: 700;
          color: $primary-color;
        }
      }
    }
    
    .content {
      display: block;
      font-size: $font-md;
      color: $text-primary;
      line-height: 1.6;
      margin-bottom: $spacing-md;
      overflow: hidden;
      text-overflow: ellipsis;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
    }
    
    .tags {
      display: flex;
      gap: 12rpx;
      flex-wrap: wrap;
      
      .tag {
        font-size: $font-xs;
        color: $primary-color;
        background: $primary-light;
        padding: 6rpx 20rpx;
        border-radius: $radius-sm;
      }
    }
  }
}

.fab {
  position: fixed;
  right: 40rpx;
  bottom: 60rpx;
  width: 110rpx;
  height: 110rpx;
  line-height: 104rpx;
  background: $primary-gradient;
  color: #fff;
  border-radius: $radius-round;
  font-size: 70rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10rpx 24rpx rgba(255, 140, 140, 0.4);
  z-index: 100;
  
  &:active {
    transform: scale(0.9);
    opacity: 0.9;
  }
}
</style>
