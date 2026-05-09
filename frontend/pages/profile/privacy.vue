<template>
  <view class="container">
    <view class="header">
      <text class="title">隐私设置</text>
    </view>
    
    <view class="settings-list">
      <view class="setting-item">
        <text class="label">匿名发布</text>
        <switch :checked="settings.anonymous" @change="onAnonymousChange" />
      </view>
      
      <view class="setting-item">
        <text class="label">显示学习记录</text>
        <switch :checked="settings.showLearning" @change="onShowLearningChange" />
      </view>
      
      <view class="setting-item">
        <text class="label">允许私信</text>
        <switch :checked="settings.allowMessage" @change="onAllowMessageChange" />
      </view>
    </view>
    
    <view class="tips">
      <text class="tip-text">💡 开启匿名后,您的发帖和评论将使用随机昵称</text>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      settings: {
        anonymous: true,
        showLearning: false,
        allowMessage: true
      }
    }
  },
  
  methods: {
    onAnonymousChange(e) {
      this.settings.anonymous = e.detail.value
    },
    
    onShowLearningChange(e) {
      this.settings.showLearning = e.detail.value
    },
    
    onAllowMessageChange(e) {
      this.settings.allowMessage = e.detail.value
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

.settings-list {
  background: #fff;
  border-radius: 20rpx;
  overflow: hidden;
  
  .setting-item {
    padding: 30rpx;
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 2rpx solid #f0f0f0;
    
    &:last-child {
      border-bottom: none;
    }
    
    .label {
      font-size: 30rpx;
      color: #333;
    }
  }
}

.tips {
  margin-top: 30rpx;
  padding: 30rpx;
  background: #fffbe6;
  border-radius: 16rpx;
  
  .tip-text {
    font-size: 26rpx;
    color: #faad14;
    line-height: 1.6;
  }
}
</style>
