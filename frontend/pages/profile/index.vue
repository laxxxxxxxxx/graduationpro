<template>
  <view class="container">
    <!-- 用户信息卡片 -->
    <view class="user-card">
      <view class="avatar-wrap">
        <text class="avatar">{{ displayName.charAt(0).toUpperCase() }}</text>
      </view>
      <text class="username">{{ displayName }}</text>
      <view class="info-tags">
        <text class="tag" v-if="profile.university">{{ profile.university }}</text>
        <text class="tag" v-if="profile.major">{{ profile.major }}</text>
        <text class="tag" v-if="profile.grade">{{ profile.grade }}</text>
      </view>
      <view class="info-detail" v-if="profile.gender || profile.age">
        <text v-if="profile.gender !== null && profile.gender !== undefined">
          {{ profile.gender === 1 ? '♂ 男' : profile.gender === 0 ? '♀ 女' : '其他' }}
        </text>
        <text v-if="profile.age"> · {{ profile.age }}岁</text>
      </view>
      <view class="edit-btn" @click="goEdit">编辑资料</view>
    </view>

    <!-- 统计面板 -->
    <view class="stats-card">
      <view class="stat-item">
        <text class="stat-num">{{ profile.assessmentCount || 0 }}</text>
        <text class="stat-label">测评次数</text>
      </view>
      <view class="stat-item">
        <text class="stat-num">{{ profile.diaryCount || 0 }}</text>
        <text class="stat-label">情绪日记</text>
      </view>
      <view class="stat-item">
        <text class="stat-num">{{ profile.learningCount || 0 }}</text>
        <text class="stat-label">学习记录</text>
      </view>
      <view class="stat-item">
        <text class="stat-num">{{ profile.communityPostCount || 0 }}</text>
        <text class="stat-label">社区帖子</text>
      </view>
    </view>

    <!-- 最近测评结果 -->
    <view class="section-card" v-if="profile.lastAssessmentScale">
      <view class="section-header">
        <text class="section-title">📊 最近测评</text>
        <text class="section-date">{{ profile.lastAssessmentDate }}</text>
      </view>
      <view class="assessment-summary">
        <text class="scale-name">{{ profile.lastAssessmentScale }}</text>
        <text class="result-badge" :class="latestResultClass">
          {{ profile.lastAssessmentResult || '暂无结果' }}
        </text>
      </view>
    </view>

    <!-- 测评历史 -->
    <view class="section-card" v-if="profile.assessmentHistory && profile.assessmentHistory.length > 0">
      <view class="section-header">
        <text class="section-title">📋 测评记录</text>
        <text class="section-more" @click="goAssessmentHistory">全部 ›</text>
      </view>
      <view 
        class="history-item" 
        v-for="item in enrichedAssessmentHistory" 
        :key="item.id"
        @click="viewReport(item.id)"
      >
        <view class="item-left">
          <text class="item-scale">{{ item.scaleName }}</text>
          <text class="item-date">{{ formatDate(item.createdAt) }}</text>
        </view>
        <view class="item-right">
          <text class="item-level" :class="item.resultClass">{{ item.resultLevel || '—' }}</text>
          <text class="item-arrow">›</text>
        </view>
      </view>
    </view>

    <!-- 功能菜单 -->
    <view class="menu-card">
      <view class="menu-item" @click="goAssessmentHistory">
        <text class="menu-icon">📝</text>
        <text class="menu-label">测评档案</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="goLearning">
        <text class="menu-icon">📚</text>
        <text class="menu-label">学习记录</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="goPrivacy">
        <text class="menu-icon">🔒</text>
        <text class="menu-label">隐私设置</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item logout-item" @click="logout">
        <text class="menu-icon">🚪</text>
        <text class="menu-label">退出登录</text>
        <text class="menu-arrow">›</text>
      </view>
    </view>
  </view>
</template>

<script>
import { getUserProfileFull } from '@/api/auth'

export default {
  data() {
    return {
      profile: {}
    }
  },

  computed: {
    displayName() {
      return this.profile.realName || this.profile.username || '用户'
    },
    // 最近测评结果的样式类
    latestResultClass() {
      return this.computeResultClass(this.profile.lastAssessmentResult)
    },
    // 为测评历史预计算 resultClass
    enrichedAssessmentHistory() {
      if (!this.profile.assessmentHistory) return []
      return this.profile.assessmentHistory.map(item => ({
        ...item,
        resultClass: this.computeResultClass(item.resultLevel)
      }))
    }
  },

  onShow() {
    this.loadProfile()
  },

  methods: {
    async loadProfile() {
      try {
        const data = await getUserProfileFull()
        this.profile = data || {}
        // 同步更新本地存储
        const basicInfo = {
          id: data.id,
          username: data.username,
          realName: data.realName,
          avatar: data.avatar
        }
        uni.setStorageSync('userInfo', basicInfo)
      } catch (error) {
        console.error('加载个人主页失败:', error)
      }
    },

    computeResultClass(level) {
      if (!level) return ''
      if (level.includes('正常') || level.includes('一类')) return 'level-normal'
      if (level.includes('轻度') || level.includes('二类')) return 'level-mild'
      if (level.includes('中度')) return 'level-moderate'
      if (level.includes('重度') || level.includes('三类')) return 'level-severe'
      return ''
    },

    formatDate(dateStr) {
      if (!dateStr) return ''
      if (typeof dateStr === 'string') {
        return dateStr.substring(0, 10)
      }
      return ''
    },

    goEdit() {
      uni.navigateTo({ url: '/pages/profile/edit' })
    },

    goAssessmentHistory() {
      uni.navigateTo({ url: '/pages/assessment/history' })
    },

    goLearning() {
      uni.navigateTo({ url: '/pages/profile/learning' })
    },

    goMyPosts() {
      uni.navigateTo({ url: '/pages/community/post-list?mine=true' })
    },

    goMyLikes() {
      uni.navigateTo({ url: '/pages/community/post-list?likes=true' })
    },

    goPrivacy() {
      uni.navigateTo({ url: '/pages/profile/privacy' })
    },

    viewReport(id) {
      uni.navigateTo({ url: `/pages/assessment/result?id=${id}` })
    },

    logout() {
      uni.showModal({
        title: '提示',
        content: '确定要退出登录吗?',
        success: (res) => {
          if (res.confirm) {
            uni.clearStorageSync()
            uni.reLaunch({ url: '/pages/login/login' })
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

// 用户卡片
.user-card {
  background: $primary-gradient;
  border-radius: $radius-lg;
  padding: 60rpx 40rpx 40rpx;
  text-align: center;
  margin-bottom: $spacing-lg;
  position: relative;
  box-shadow: $shadow-lg;

  .avatar-wrap {
    width: 140rpx;
    height: 140rpx;
    line-height: 140rpx;
    background: rgba(255,255,255,0.3);
    border-radius: $radius-round;
    margin: 0 auto $spacing-md;
    border: 4rpx solid rgba(255,255,255,0.5);

    .avatar {
      font-size: 60rpx;
      color: #fff;
      font-weight: 700;
    }
  }

  .username {
    display: block;
    font-size: $font-xl;
    color: #fff;
    font-weight: 700;
    margin-bottom: $spacing-sm;
  }

  .info-tags {
    display: flex;
    justify-content: center;
    flex-wrap: wrap;
    gap: 12rpx;
    margin-bottom: $spacing-sm;

    .tag {
      font-size: $font-xs;
      color: rgba(255,255,255,0.95);
      background: rgba(255,255,255,0.2);
      padding: 6rpx 20rpx;
      border-radius: $radius-xl;
    }
  }

  .info-detail {
    font-size: $font-sm;
    color: rgba(255,255,255,0.85);
    margin-bottom: $spacing-lg;
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

// 统计面板
.stats-card {
  @extend %card;
  display: flex;
  padding: 40rpx 20rpx;
  
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
  @extend %card;

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
  @extend %card;
  padding: 0;
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
      font-size: 40rpx;
      color: $text-tertiary;
    }
  }
}
</style>
