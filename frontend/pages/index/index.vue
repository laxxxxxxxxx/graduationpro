<template>
  <view class="home-page">
    <!-- 顶部欢迎区域 -->
    <view class="header">
      <view class="welcome">
        <text class="greeting">你好,{{ userInfo.username || '同学' }}</text>
        <text class="subtitle">关注心理健康,拥抱美好生活</text>
      </view>
    </view>
    
    <!-- 快捷功能 -->
    <view class="quick-actions card">
      <view class="action-item" @click="navigateTo('/pages/assessment/scale-list')">
        <view class="icon-wrapper assessment">
          <text class="icon">📝</text>
        </view>
        <text class="label">心理测评</text>
      </view>
      <view class="action-item" @click="navigateTo('/pages/emotion/diary-edit')">
        <view class="icon-wrapper diary">
          <text class="icon">📔</text>
        </view>
        <text class="label">情绪日记</text>
      </view>
      <view class="action-item" @click="navigateTo('/pages/community/post-list')">
        <view class="icon-wrapper community">
          <text class="icon">💬</text>
        </view>
        <text class="label">匿名社区</text>
      </view>
      <view class="action-item" @click="navigateTo('/pages/resource/list')">
        <view class="icon-wrapper resource">
          <text class="icon">📚</text>
        </view>
        <text class="label">学习资源</text>
      </view>
    </view>
    
    <!-- 今日推荐 -->
    <view class="section">
      <view class="section-header">
        <text class="title">今日推荐</text>
        <text class="more" @click="navigateTo('/pages/resource/list')">更多 ></text>
      </view>
      <view class="recommend-list">
        <view 
          v-for="item in recommendList" 
          :key="item.id" 
          class="recommend-item card"
          @click="goResourceDetail(item.id)"
        >
          <image :src="resolveCoverUrl(item.coverUrl)" mode="aspectFill" class="cover"></image>
          <view class="info">
            <text class="resource-title text-ellipsis-2">{{ item.title }}</text>
            <view class="meta">
              <text class="type">{{ getTypeText(item.type) }}</text>
              <text class="views">👁 {{ item.viewCount }}</text>
            </view>
          </view>
        </view>
      </view>
    </view>
    
    <!-- 心理小知识 -->
    <view class="section">
      <view class="section-header">
        <text class="title">心理小知识</text>
      </view>
      <view class="tips-card card">
        <view class="tip-item">
          <text class="tip-icon">💡</text>
          <text class="tip-text">每天记录三件感恩的事,可以提升幸福感</text>
        </view>
        <view class="tip-item">
          <text class="tip-icon">🧘</text>
          <text class="tip-text">深呼吸练习有助于缓解焦虑情绪</text>
        </view>
        <view class="tip-item">
          <text class="tip-icon">😴</text>
          <text class="tip-text">保持规律作息,每晚7-8小时睡眠</text>
        </view>
      </view>
    </view>
    
    <!-- 危机干预热线 -->
    <view class="crisis-hotline card">
      <view class="hotline-header">
        <text class="hotline-icon">🆘</text>
        <text class="hotline-title">心理危机干预热线</text>
      </view>
      <view class="hotline-content">
        <text class="hotline-number">400-161-9995</text>
        <text class="hotline-desc">24小时免费心理咨询</text>
      </view>
    </view>
  </view>
</template>

<script>
import { getUserInfo } from '@/api/auth'
import { getResourceList } from '@/api/resource'
import { getApiBaseUrl } from '@/utils/request'

const DEFAULT_COVER = '/static/images/resource-default-cover.png'

export default {
  data() {
    return {
      userInfo: {},
      recommendList: []
    }
  },
  
  onShow() {
    // 检查登录状态
    if (!this.checkLogin()) return
    this.loadUserInfo()
    this.loadRecommendations()
  },
  
  onPullDownRefresh() {
    this.loadUserInfo()
    this.loadRecommendations()
    setTimeout(() => {
      uni.stopPullDownRefresh()
    }, 1000)
  },
  
  methods: {
    checkLogin() {
      const token = uni.getStorageSync('token')
      if (!token) {
        // 未登录，跳转到登录页
        uni.reLaunch({
          url: '/pages/login/login'
        })
        return false
      }
      return true
    },
    
    async loadUserInfo() {
      try {
        this.userInfo = await getUserInfo()
      } catch (error) {
        console.error('获取用户信息失败', error)
        // Token失效，跳转到登录页
        uni.removeStorageSync('token')
        uni.removeStorageSync('userInfo')
        uni.reLaunch({
          url: '/pages/login/login'
        })
      }
    },
    
    async loadRecommendations() {
      try {
        const res = await getResourceList({ 
          pageNum: 1, 
          pageSize: 3,
          sortBy: 'view_count'
        })
        this.recommendList = res.records || []
      } catch (error) {
        console.error('获取推荐资源失败', error)
      }
    },
    
    navigateTo(url) {
      const tabBarPages = [
        '/pages/index/index',
        '/pages/resource/list',
        '/pages/assessment/scale-list',
        '/pages/community/post-list',
        '/pages/profile/index'
      ]
      const path = url.split('?')[0]

      if (tabBarPages.includes(path)) {
        uni.switchTab({ url: path })
        return
      }

      uni.navigateTo({ url })
    },
    
    goResourceDetail(id) {
      uni.navigateTo({
        url: `/pages/resource/detail?id=${id}`
      })
    },
    
    getTypeText(type) {
      const typeMap = {
        1: '文章',
        2: '视频',
        3: '音频',
        4: '课程'
      }
      return typeMap[type] || '资源'
    },

    resolveCoverUrl(url) {
      if (!url) return DEFAULT_COVER
      if (url.startsWith('http') || url.startsWith('https') || url.startsWith('data:')) {
        return url
      }
      if (url.startsWith('/')) {
        return getApiBaseUrl() + url
      }
      return getApiBaseUrl() + '/' + url
    }
  }
}
</script>

<style lang="scss" scoped>
.home-page {
  padding-bottom: $spacing-xl;
}

.header {
  background: $primary-gradient;
  border-radius: $radius-lg;
  padding: $spacing-xl $spacing-lg;
  margin-bottom: $spacing-lg;
  box-shadow: $shadow-md;
  
  .welcome {
    .greeting {
      display: block;
      font-size: $font-xl;
      font-weight: 600;
      color: #fff;
      margin-bottom: $spacing-xs;
    }
    
    .subtitle {
      display: block;
      font-size: $font-sm;
      color: rgba(255, 255, 255, 0.9);
    }
  }
}

.quick-actions {
  display: flex;
  justify-content: space-around;
  padding: $spacing-lg $spacing-md;
  
  .action-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    
    .icon-wrapper {
      width: 110rpx;
      height: 110rpx;
      border-radius: $radius-md;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: $spacing-sm;
      box-shadow: $shadow-sm;
      
      .icon {
        font-size: 52rpx;
      }
      
      &.assessment {
        background: #E8F0FF;
      }
      
      &.diary {
        background: #FFF0F0;
      }
      
      &.community {
        background: #F0FFF4;
      }
      
      &.resource {
        background: #FFF9E6;
      }
    }
    
    .label {
      font-size: $font-sm;
      color: $text-primary;
      font-weight: 500;
    }
  }
}

.section {
  margin-bottom: $spacing-lg;
  
  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: $spacing-sm $spacing-xs;
    
    .title {
      font-size: $font-lg;
      font-weight: 600;
      color: $text-primary;
    }
    
    .more {
      font-size: $font-sm;
      color: $text-tertiary;
    }
  }
}

.recommend-list {
  .recommend-item {
    display: flex;
    padding: $spacing-md;
    margin-bottom: $spacing-md;
    transition: transform 0.2s;
    
    &:active {
      transform: scale(0.98);
    }
    
    .cover {
      width: 180rpx;
      height: 140rpx;
      border-radius: $radius-md;
      margin-right: $spacing-md;
    }
    
    .info {
      flex: 1;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      
      .resource-title {
        font-size: $font-md;
        color: $text-primary;
        font-weight: 500;
        line-height: 1.4;
      }
      
      .meta {
        display: flex;
        justify-content: space-between;
        align-items: center;
        
        .type {
          font-size: $font-xs;
          color: $primary-color;
          background: $primary-light;
          padding: 6rpx 16rpx;
          border-radius: $radius-sm;
        }
        
        .views {
          font-size: $font-xs;
          color: $text-tertiary;
        }
      }
    }
  }
}

.tips-card {
  .tip-item {
    display: flex;
    align-items: flex-start;
    padding: $spacing-md 0;
    
    &:not(:last-child) {
      border-bottom: 1rpx solid $border-color;
    }
    
    .tip-icon {
      font-size: $font-lg;
      margin-right: $spacing-sm;
    }
    
    .tip-text {
      flex: 1;
      font-size: $font-sm;
      color: $text-secondary;
      line-height: 1.6;
    }
  }
}

.crisis-hotline {
  background: linear-gradient(135deg, #FF7E5F 0%, #FEB47B 100%);
  color: #fff;
  border: none;
  
  .hotline-header {
    display: flex;
    align-items: center;
    margin-bottom: $spacing-sm;
    
    .hotline-icon {
      font-size: $font-xl;
      margin-right: $spacing-sm;
    }
    
    .hotline-title {
      font-size: $font-lg;
      font-weight: 600;
    }
  }
  
  .hotline-content {
    .hotline-number {
      display: block;
      font-size: $font-xl;
      font-weight: 700;
      margin-bottom: $spacing-xs;
      letter-spacing: 2rpx;
    }
    
    .hotline-desc {
      display: block;
      font-size: $font-xs;
      opacity: 0.9;
    }
  }
}
</style>
