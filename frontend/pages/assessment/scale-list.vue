<template>
  <view class="container">
    <view class="header">
      <text class="title">心理测评</text>
      <text class="subtitle">专业量表,科学评估</text>
      <view class="header-actions">
        <text class="action-link" @click="goHistory">📋 我的测评档案</text>
      </view>
    </view>
    
    <view class="scale-list">
      <view 
        class="scale-item" 
        v-for="scale in scales" 
        :key="scale.id"
        @click="goTest(scale)"
      >
        <view class="scale-info">
          <text class="scale-name">{{ scale.name }}</text>
          <text class="scale-desc">{{ scale.description }}</text>
          <view class="scale-meta">
            <text class="question-count">{{ scale.totalQuestions }}题</text>
            <text class="duration">约{{ scale.estimatedTime }}分钟</text>
          </view>
        </view>
        <view class="arrow">›</view>
      </view>
    </view>
  </view>
</template>

<script>
import { getScaleList } from '@/api/assessment.js'

export default {
  data() {
    return {
      scales: []
    }
  },
  
  onLoad() {
    this.loadScales()
  },
  
  methods: {
    async loadScales() {
      try {
        const res = await getScaleList()
        this.scales = res.records || res
      } catch (err) {
        console.error('加载量表失败', err)
      }
    },
    
    goTest(scale) {
      uni.navigateTo({
        url: `/pages/assessment/test?id=${scale.id}`
      })
    },
    
    goHistory() {
      uni.navigateTo({
        url: '/pages/assessment/history'
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

.header {
  text-align: center;
  margin-bottom: $spacing-xl;
  padding: $spacing-xl 0;
  
  .title {
    display: block;
    font-size: 52rpx;
    font-weight: 700;
    color: $text-primary;
    margin-bottom: $spacing-xs;
    letter-spacing: 2rpx;
  }
  
  .subtitle {
    font-size: $font-md;
    color: $text-secondary;
  }
  
  .header-actions {
    margin-top: $spacing-lg;
    
    .action-link {
      display: inline-block;
      font-size: $font-sm;
      color: $primary-color;
      padding: 16rpx 40rpx;
      background: #fff;
      border-radius: $radius-xl;
      box-shadow: $shadow-sm;
      font-weight: 500;
      
      &:active {
        background: $bg-color;
      }
    }
  }
}

.scale-list {
  .scale-item {
    background: #ffffff;
    border-radius: 32rpx;
    padding: 32rpx;
    margin-bottom: 24rpx;
    box-shadow: 0 8rpx 24rpx rgba(255, 140, 140, 0.08);
    border: 1px solid rgba(255, 255, 255, 0.8);
    padding: 40rpx;
    display: flex;
    justify-content: space-between;
    align-items: center;
    transition: transform 0.2s;
    
    &:active {
      transform: scale(0.98);
    }
    
    .scale-info {
      flex: 1;
      
      .scale-name {
        display: block;
        font-size: $font-lg;
        font-weight: 700;
        color: $text-primary;
        margin-bottom: 12rpx;
      }
      
      .scale-desc {
        display: block;
        font-size: $font-sm;
        color: $text-secondary;
        margin-bottom: 20rpx;
        line-height: 1.5;
      }
      
      .scale-meta {
        display: flex;
        gap: 20rpx;
        
        .question-count, .duration {
          font-size: $font-xs;
          color: $text-tertiary;
          background: $bg-color;
          padding: 6rpx 20rpx;
          border-radius: $radius-sm;
        }
      }
    }
    
    .arrow {
      font-size: 44rpx;
      color: $text-tertiary;
      margin-left: $spacing-md;
    }
  }
}
</style>
