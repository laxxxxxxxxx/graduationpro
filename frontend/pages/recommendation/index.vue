<template>
  <view class="container">
    <!-- 推荐标题 -->
    <view class="header">
      <text class="title">为您推荐</text>
      <text class="subtitle">基于四路混合算法智能推荐</text>
    </view>
    
    <!-- 算法切换标签 -->
    <view class="tabs">
      <view 
        class="tab" 
        :class="{active: currentAlgorithm === 'hybrid'}"
        @click="switchAlgorithm('hybrid')"
      >
        混合推荐
      </view>
      <view 
        class="tab" 
        :class="{active: currentAlgorithm === 'user-cf'}"
        @click="switchAlgorithm('user-cf')"
      >
        协同过滤
      </view>
      <view 
        class="tab" 
        :class="{active: currentAlgorithm === 'content'}"
        @click="switchAlgorithm('content')"
      >
        兴趣匹配
      </view>
      <view 
        class="tab" 
        :class="{active: currentAlgorithm === 'profile'}"
        @click="switchAlgorithm('profile')"
      >
        心理画像
      </view>
    </view>
    
    <!-- 刷新标签按钮 -->
    <view class="refresh-bar" v-if="currentAlgorithm === 'hybrid'">
      <view class="refresh-btn" @click="refreshTags">
        <text>🔄 刷新兴趣标签</text>
      </view>
    </view>
    
    <!-- 推荐列表 -->
    <view class="recommend-list" v-if="recommendations.length > 0">
      <view 
        class="recommend-item" 
        v-for="(item, index) in recommendations" 
        :key="item.resourceId"
        @click="goDetail(item)"
      >
        <view class="item-header">
          <text class="rank">{{ index + 1 }}</text>
          <view class="info">
            <text class="title">{{ item.title }}</text>
            <view class="tags" v-if="item.tags">
              <text class="tag" v-for="tag in item.tags.split(',').slice(0, 3)" :key="tag">
                {{ tag }}
              </text>
            </view>
          </view>
        </view>
        
        <view class="item-footer">
          <text class="reason">💡 {{ getReason(item) }}</text>
          <view class="confidence">
            <text>匹配度</text>
            <text class="score">{{ getMatchScore(item) }}%</text>
          </view>
        </view>
      </view>
    </view>
    
    <!-- 空状态 -->
    <view class="empty" v-else-if="!loading">
      <text class="empty-icon">📚</text>
      <text class="empty-text">暂无推荐内容</text>
      <text class="empty-hint">多浏览、点赞资源，让我们更了解您</text>
      <view class="refresh-btn-empty" @click="refreshTags">
        <text>刷新兴趣标签</text>
      </view>
    </view>
    
    <!-- 加载状态 -->
    <view class="loading" v-if="loading">
      <text>加载中...</text>
    </view>
  </view>
</template>

<script>
import { getPersonalizedRecommendations, getUserBasedCF, getContentBased, getProfileBased, recordClick, refreshInterestTags } from '@/api/recommendation'

export default {
  data() {
    return {
      currentAlgorithm: 'hybrid',
      recommendations: [],
      loading: false
    }
  },
  
  onShow() {
    this.loadRecommendations()
  },
  
  methods: {
    getReason(item) {
      if (item.reason) {
        return item.reason
      }
      if (item.reasons && item.reasons.length > 0) {
        return item.reasons.join(' / ')
      }
      return '为您推荐'
    },
    
    getMatchScore(item) {
      if (item.confidenceScore) {
        return Math.round(item.confidenceScore)
      }
      if (item.finalScore !== undefined && item.finalScore !== null) {
        return Math.round(item.finalScore * 100)
      }
      return 0
    },
    
    async loadRecommendations() {
      try {
        this.loading = true
        
        let data
        switch (this.currentAlgorithm) {
          case 'hybrid':
            data = await getPersonalizedRecommendations(15)
            break
          case 'user-cf':
            data = await getUserBasedCF(15)
            break
          case 'content':
            data = await getContentBased(15)
            break
          case 'profile':
            data = await getProfileBased(15)
            break
          default:
            data = await getPersonalizedRecommendations(15)
        }
        
        this.recommendations = data || []
      } catch (error) {
        console.error('加载推荐失败:', error)
      } finally {
        this.loading = false
      }
    },
    
    switchAlgorithm(algorithm) {
      this.currentAlgorithm = algorithm
      this.loadRecommendations()
    },
    
    goDetail(item) {
      this.recordClick(item.resourceId)
      uni.navigateTo({
        url: `/pages/resource/detail?id=${item.resourceId}`
      })
    },
    
    async recordClick(resourceId) {
      try {
        await recordClick(resourceId)
      } catch (error) {
        console.error('记录点击失败:', error)
      }
    },
    
    async refreshTags() {
      try {
        uni.showLoading({ title: '刷新中...' })
        await refreshInterestTags()
        uni.hideLoading()
        uni.showToast({ title: '兴趣标签已刷新', icon: 'success' })
        this.loadRecommendations()
      } catch (error) {
        uni.hideLoading()
        uni.showToast({ title: '刷新失败', icon: 'none' })
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

.header {
  margin-bottom: $spacing-lg;
  padding: $spacing-lg 0;
  
  .title {
    display: block;
    font-size: $font-xxl;
    font-weight: 700;
    color: $text-primary;
    margin-bottom: 8rpx;
  }
  
  .subtitle {
    font-size: $font-sm;
    color: $text-secondary;
  }
}

.tabs {
  display: flex;
  gap: $spacing-sm;
  margin-bottom: $spacing-lg;
  
  .tab {
    flex: 1;
    height: 76rpx;
    line-height: 76rpx;
    text-align: center;
    background: #fff;
    border-radius: $radius-xl;
    font-size: $font-sm;
    color: $text-secondary;
    transition: all 0.3s;
    box-shadow: $shadow-sm;
    
    &.active {
      background: $primary-gradient;
      color: #fff;
      font-weight: 600;
      box-shadow: $shadow-md;
    }
  }
}

.recommend-list {
  .recommend-item {
    @extend %card;
    padding: $spacing-lg;
    transition: transform 0.2s;
    
    &:active {
      transform: scale(0.98);
    }
    
    .item-header {
      display: flex;
      gap: $spacing-md;
      margin-bottom: $spacing-md;
      
      .rank {
        width: 50rpx;
        height: 50rpx;
        line-height: 50rpx;
        text-align: center;
        background: $primary-gradient;
        color: #fff;
        border-radius: $radius-round;
        font-size: $font-xs;
        font-weight: 700;
        flex-shrink: 0;
        box-shadow: $shadow-sm;
      }
      
      .info {
        flex: 1;
        
        .title {
          display: block;
          font-size: $font-md;
          font-weight: 700;
          color: $text-primary;
          margin-bottom: 12rpx;
        }
        
        .tags {
          display: flex;
          gap: 12rpx;
          flex-wrap: wrap;
          
          .tag {
            font-size: $font-xs;
            color: $primary-color;
            background: $primary-light;
            padding: 4rpx 16rpx;
            border-radius: $radius-sm;
          }
        }
      }
    }
    
    .item-footer {
      border-top: 1rpx solid $border-color;
      padding-top: $spacing-md;
      
      .reason {
        display: block;
        font-size: $font-sm;
        color: $text-secondary;
        margin-bottom: $spacing-sm;
        font-style: italic;
      }
      
      .confidence {
        display: flex;
        justify-content: space-between;
        align-items: center;
        font-size: $font-xs;
        color: $text-tertiary;
        
        .score {
          font-size: $font-lg;
          font-weight: 700;
          color: $primary-color;
        }
      }
    }
  }
}

.empty {
  text-align: center;
  padding: 100rpx 0;
  
  .empty-icon {
    display: block;
    font-size: 100rpx;
    margin-bottom: $spacing-md;
  }
  
  .empty-text {
    display: block;
    font-size: $font-lg;
    color: $text-secondary;
    margin-bottom: 8rpx;
    font-weight: 600;
  }
  
  .empty-hint {
    font-size: $font-sm;
    color: $text-tertiary;
    margin-bottom: $spacing-xl;
  }
  
  .refresh-btn-empty {
    @extend %btn-primary;
    height: 80rpx;
    padding: 0 60rpx;
    display: inline-flex;
    font-size: $font-md;
  }
}

.refresh-bar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: $spacing-md;
  
  .refresh-btn {
    height: 60rpx;
    line-height: 56rpx;
    padding: 0 30rpx;
    background: $primary-light;
    color: $primary-color;
    border-radius: $radius-xl;
    font-size: $font-xs;
    font-weight: 500;
    border: 1rpx solid rgba($primary-color, 0.2);
    box-shadow: $shadow-sm;
    
    &:active {
      background: #fff;
    }
  }
}

.loading {
  text-align: center;
  padding: 50rpx 0;
  color: $text-tertiary;
  font-size: $font-md;
}
</style>
