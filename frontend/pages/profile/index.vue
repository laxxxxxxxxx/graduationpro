<template>
  <view class="container">
    <!-- 用户信息卡片 -->
    <view class="user-header">
      <view class="user-info" @click="navigateTo('/pages/profile/edit')">
        <view class="avatar-wrapper">
          <image class="avatar" :src="userInfo.avatar || '/static/icons/profile-active.png'" mode="aspectFill"></image>
        </view>
        <view class="details">
          <text class="username">{{ userInfo.realName || userInfo.username || '同学' }}</text>
          <text class="school">{{ userInfo.university || '未设置学校' }} · {{ userInfo.major || '未设置专业' }}</text>
        </view>
        <text class="edit-btn">编辑资料</text>
      </view>
    </view>

    <!-- 统计面板 -->
    <view class="stats-card">
      <view class="stat-item" @click="navigateTo('/pages/assessment/history')">
        <text class="stat-num">{{ profile.assessmentCount || 0 }}</text>
        <text class="stat-label">测评记录</text>
      </view>
      <view class="stat-item" @click="navigateTo('/pages/emotion/diary-list')">
        <text class="stat-num">{{ profile.diaryCount || 0 }}</text>
        <text class="stat-label">情绪日记</text>
      </view>
      <view class="stat-item" @click="navigateTo('/pages/community/post-list?mine=true')">
        <text class="stat-num">{{ profile.communityPostCount || 0 }}</text>
        <text class="stat-label">发布帖数</text>
      </view>
      <view class="stat-item" @click="navigateTo('/pages/profile/learning')">
        <text class="stat-num">{{ profile.learningCount || 0 }}</text>
        <text class="stat-label">学习资源</text>
      </view>
    </view>

    <!-- 最近测评摘要 -->
    <view class="section-card" v-if="profile.lastAssessmentScale">
      <view class="section-header">
        <text class="section-title">最近测评</text>
        <text class="section-date">{{ profile.lastAssessmentDate }}</text>
      </view>
      <view class="assessment-summary" @click="navigateTo('/pages/assessment/history')">
        <text class="scale-name">{{ profile.lastAssessmentScale }}</text>
        <view class="result-badge" :class="computeLevelClass(profile.lastAssessmentResult)">
          {{ profile.lastAssessmentResult }}
        </view>
      </view>
    </view>

    <!-- 测评历史简表 -->
    <view class="section-card" v-if="profile.assessmentHistory && profile.assessmentHistory.length > 0">
      <view class="section-header">
        <text class="section-title">测评历史</text>
        <text class="section-more" @click="navigateTo('/pages/assessment/history')">查看全部 ></text>
      </view>
      <view class="history-list">
        <view 
          class="history-item" 
          v-for="item in profile.assessmentHistory" 
          :key="item.id"
          @click="viewReport(item.id)"
        >
          <view class="item-left">
            <text class="item-scale">{{ item.scaleName }}</text>
            <text class="item-date">{{ formatDate(item.createdAt) }}</text>
          </view>
          <view class="item-right">
            <text class="item-level" :class="computeLevelClass(item.resultLevel)">{{ item.resultLevel }}</text>
            <text class="item-arrow">›</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 功能菜单 -->
    <view class="menu-card">
      <view class="menu-item" @click="navigateTo('/pages/profile/learning?tab=records')">
        <text class="menu-icon">📖</text>
        <text class="menu-label">学习记录</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="navigateTo('/pages/profile/learning?tab=favorites')">
        <text class="menu-icon">⭐</text>
        <text class="menu-label">我的收藏</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="navigateTo('/pages/profile/learning?tab=likes')">
        <text class="menu-icon">👍</text>
        <text class="menu-label">我的点赞</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="navigateTo('/pages/profile/privacy')">
        <text class="menu-icon">🛡️</text>
        <text class="menu-label">隐私设置</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item logout-item" @click="handleLogout">
        <text class="menu-icon">🚪</text>
        <text class="menu-label">退出登录</text>
      </view>
    </view>
  </view>
</template>

<script>
import { getUserProfileFull } from '@/api/auth'

export default {
  data() {
    return {
      userInfo: {},
      profile: {}
    }
  },
  
  onShow() {
    this.loadProfile()
  },
  
  methods: {
    async loadProfile() {
      try {
        const data = await getUserProfileFull()
        this.profile = data
        this.userInfo = {
          username: data.username,
          realName: data.realName,
          avatar: data.avatar,
          university: data.university,
          major: data.major
        }
      } catch (error) {
        console.error('加载个人资料失败:', error)
      }
    },
    
    navigateTo(url) {
      uni.navigateTo({ url })
    },
    
    viewReport(id) {
      uni.navigateTo({
        url: `/pages/assessment/result?id=${id}`
      })
    },
    
    computeLevelClass(level) {
      if (!level) return 'level-normal'
      if (level.includes('正常') || level.includes('一类')) return 'level-normal'
      if (level.includes('轻度') || level.includes('二类')) return 'level-mild'
      if (level.includes('中度')) return 'level-moderate'
      if (level.includes('重度') || level.includes('三类')) return 'level-severe'
      return 'level-normal'
    },
    
    formatDate(dateStr) {
      if (!dateStr) return ''
      return dateStr.substring(0, 10)
    },
    
    handleLogout() {
      uni.showModal({
        title: '提示',
        content: '确定要退出登录吗？',
        success: (res) => {
          if (res.confirm) {
            uni.removeStorageSync('token')
            uni.removeStorageSync('userInfo')
            uni.reLaunch({
              url: '/pages/login/login'
            })
          }
        }
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

.user-header {
  background: $primary-gradient;
  border-radius: $radius-lg;
  padding: 60rpx 40rpx;
  margin-bottom: $spacing-lg;
  box-shadow: $shadow-md;
  
  .user-info {
    display: flex;
    align-items: center;
    position: relative;
    
    .avatar-wrapper {
      width: 130rpx;
      height: 130rpx;
      border-radius: $radius-round;
      border: 6rpx solid rgba(255, 255, 255, 0.4);
      margin-right: 30rpx;
      overflow: hidden;
      background: #fff;
      
      .avatar {
        width: 100%;
        height: 100%;
      }
    }
    
    .details {
      flex: 1;
      
      .username {
        display: block;
        font-size: 40rpx;
        font-weight: 700;
        color: #fff;
        margin-bottom: 8rpx;
      }
      
      .school {
        display: block;
        font-size: $font-xs;
        color: rgba(255, 255, 255, 0.9);
      }
    }
    
    .edit-btn {
      display: inline-block;
      font-size: $font-sm;
      color: #fff;
      border: 2rpx solid rgba(255,255,255,0.6);
      padding: 12rpx 40rpx;
      border-radius: $radius-xl;
      transition: all 0.2s;
      
      &:active {
        background: rgba(255,255,255,0.1);
      }
    }
  }
}

// 统计面板
.stats-card {
  background: #ffffff;
  border-radius: 32rpx;
  padding: 40rpx 20rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 8rpx 24rpx rgba(255, 140, 140, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.8);
  display: flex;
  
  .stat-item {
    flex: 1;
    text-align: center;
    position: relative;
    
    &:not(:last-child)::after {
      content: '';
      position: absolute;
      right: 0;
      top: 20%;
      height: 60%;
      width: 1rpx;
      background: $border-color;
    }

    .stat-num {
      display: block;
      font-size: $font-xl;
      font-weight: 700;
      color: $text-primary;
      margin-bottom: 8rpx;
    }

    .stat-label {
      font-size: $font-xs;
      color: $text-secondary;
    }
  }
}

// 区块卡片
.section-card {
  background: #ffffff;
  border-radius: 32rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 8rpx 24rpx rgba(255, 140, 140, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.8);

  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: $spacing-lg;

    .section-title {
      font-size: $font-lg;
      font-weight: 600;
      color: $text-primary;
    }

    .section-date {
      font-size: $font-xs;
      color: $text-tertiary;
    }

    .section-more {
      font-size: $font-sm;
      color: $primary-color;
    }
  }

  .assessment-summary {
    display: flex;
    justify-content: space-between;
    align-items: center;
    background: $bg-color;
    padding: $spacing-md;
    border-radius: $radius-md;

    .scale-name {
      font-size: $font-md;
      color: $text-primary;
      font-weight: 500;
    }

    .result-badge {
      font-size: $font-xs;
      padding: 10rpx 24rpx;
      border-radius: $radius-xl;
      font-weight: 600;

      &.level-normal {
        background: #E1F5FE;
        color: #039BE5;
      }

      &.level-mild {
        background: #FFF9C4;
        color: #FBC02D;
      }

      &.level-moderate {
        background: #FFE0B2;
        color: #FB8C00;
      }

      &.level-severe {
        background: #FFCDD2;
        color: #E53935;
      }
    }
  }

  // 历史列表
  .history-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: $spacing-md 0;
    border-bottom: 1rpx solid $border-color;

    &:last-child {
      border-bottom: none;
    }

    .item-left {
      .item-scale {
        display: block;
        font-size: $font-md;
        color: $text-primary;
        margin-bottom: 6rpx;
      }

      .item-date {
        font-size: $font-xs;
        color: $text-tertiary;
      }
    }

    .item-right {
      display: flex;
      align-items: center;
      gap: 12rpx;

      .item-level {
        font-size: $font-xs;
        padding: 6rpx 16rpx;
        border-radius: $radius-sm;

        &.level-normal {
          background: #E8F5E9;
          color: #43A047;
        }

        &.level-mild {
          background: #FFFDE7;
          color: #FBC02D;
        }

        &.level-moderate {
          background: #FFF3E0;
          color: #FB8C00;
        }

        &.level-severe {
          background: #FFEBEE;
          color: #E53935;
        }
      }

      .item-arrow {
        font-size: 36rpx;
        color: $text-tertiary;
      }
    }
  }
}

// 菜单
.menu-card {
  background: #ffffff;
  border-radius: 32rpx;
  padding: 0;
  margin-bottom: 24rpx;
  box-shadow: 0 8rpx 24rpx rgba(255, 140, 140, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.8);
  overflow: hidden;

  .menu-item {
    display: flex;
    align-items: center;
    padding: $spacing-lg;
    border-bottom: 1rpx solid $border-color;
    transition: background 0.2s;

    &:active {
      background: $bg-color;
    }

    &:last-child {
      border-bottom: none;
    }

    &.logout-item {
      .menu-icon { color: $error-color; }
      .menu-label { color: $error-color; }
    }

    .menu-icon {
      font-size: 40rpx;
      margin-right: $spacing-md;
    }

    .menu-label {
      flex: 1;
      font-size: $font-md;
      color: $text-primary;
      font-weight: 500;
    }

    .menu-arrow {
      font-size: 36rpx;
      color: $text-tertiary;
    }
  }
}
</style>
