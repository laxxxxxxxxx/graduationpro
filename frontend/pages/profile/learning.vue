<template>
  <view class="container">
    <view class="header">
      <text class="title">学习记录</text>
    </view>
    
    <view class="record-list">
      <view class="record-item" v-for="record in records" :key="record.id">
        <view class="record-info">
          <text class="resource-name">{{ record.resourceName }}</text>
          <text class="progress">进度: {{ record.progress }}%</text>
        </view>
        <view class="status" :class="{completed: record.completed}">
          {{ record.completed ? '已完成' : '学习中' }}
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { getLearningRecords } from '@/api/resource'

export default {
  data() {
    return {
      records: [],
      loading: true
    }
  },
  
  onLoad() {
    this.loadLearningRecords()
  },
  
  methods: {
    async loadLearningRecords() {
      try {
        const data = await getLearningRecords()
        this.records = data || []
      } catch (error) {
        console.error('加载学习记录失败:', error)
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.container {
  padding: 30rpx;
  background: #f5f5f5;
  min-height: 100vh;
}

.header {
  text-align: center;
  margin-bottom: 30rpx;
  
  .title {
    font-size: 40rpx;
    font-weight: bold;
    color: #333;
  }
}

.record-list {
  .record-item {
    background: #fff;
    border-radius: 16rpx;
    padding: 30rpx;
    margin-bottom: 20rpx;
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .record-info {
      flex: 1;
      
      .resource-name {
        display: block;
        font-size: 30rpx;
        color: #333;
        margin-bottom: 10rpx;
      }
      
      .progress {
        font-size: 24rpx;
        color: #999;
      }
    }
    
    .status {
      font-size: 24rpx;
      color: #667eea;
      background: #f0f2ff;
      padding: 8rpx 16rpx;
      border-radius: 8rpx;
      
      &.completed {
        color: #52c41a;
        background: #f6ffed;
      }
    }
  }
}
</style>
