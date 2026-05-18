<template>
  <view class="container">
    <!-- 顶部选项卡 -->
    <view class="tabs">
      <view 
        v-for="(tab, index) in tabs" 
        :key="index" 
        class="tab-item" 
        :class="{active: currentTab === tab.value}"
        @click="switchTab(tab.value)"
      >
        <text class="tab-label">{{ tab.label }}</text>
        <view class="active-bar"></view>
      </view>
    </view>
    
    <scroll-view scroll-y class="content-scroll">
      <!-- 学习记录 -->
      <view v-if="currentTab === 'records'" class="record-list">
        <view v-if="records.length === 0" class="empty">暂无学习记录</view>
        <view 
          v-for="record in records" 
          :key="record.id" 
          class="record-item card"
          @click="goResourceDetail(record.resourceId)"
        >
          <view class="record-info">
            <text class="resource-name">{{ record.resourceName || '未知资源' }}</text>
            <view class="meta">
              <text class="progress">进度: {{ record.progress }}%</text>
              <text class="time">{{ formatDate(record.lastStudyTime) }}</text>
            </view>
          </view>
          <view class="status" :class="{completed: record.completed}">
            {{ record.completed ? '已完成' : '学习中' }}
          </view>
        </view>
      </view>

      <!-- 我的收藏 -->
      <view v-if="currentTab === 'favorites'" class="resource-list">
        <view v-if="favorites.length === 0" class="empty">暂无收藏资源</view>
        <view 
          v-for="item in favorites" 
          :key="item.id" 
          class="resource-item card"
          @click="goResourceDetail(item.id)"
        >
          <image :src="resolveCoverUrl(item.coverUrl)" mode="aspectFill" class="cover"></image>
          <view class="info">
            <text class="title text-ellipsis-2">{{ item.title }}</text>
            <view class="meta">
              <text class="type">{{ getTypeText(item.type) }}</text>
              <text class="views">👁 {{ item.viewCount }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 我的点赞 -->
      <view v-if="currentTab === 'likes'" class="resource-list">
        <view v-if="likes.length === 0" class="empty">暂无点赞资源</view>
        <view 
          v-for="item in likes" 
          :key="item.id" 
          class="resource-item card"
          @click="goResourceDetail(item.id)"
        >
          <image :src="resolveCoverUrl(item.coverUrl)" mode="aspectFill" class="cover"></image>
          <view class="info">
            <text class="title text-ellipsis-2">{{ item.title }}</text>
            <view class="meta">
              <text class="type">{{ getTypeText(item.type) }}</text>
              <text class="views">👁 {{ item.viewCount }}</text>
            </view>
          </view>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import { getLearningRecords, getUserFavorites, getUserLikes } from '@/api/resource'
import { resolveUrl } from '@/utils/request'

const DEFAULT_COVER = '/static/images/resource-default-cover.png'

export default {
  data() {
    return {
      tabs: [
        { label: '学习记录', value: 'records' },
        { label: '我的收藏', value: 'favorites' },
        { label: '我的点赞', value: 'likes' }
      ],
      currentTab: 'records',
      records: [],
      favorites: [],
      likes: [],
      loading: false
    }
  },
  
  onLoad(options) {
    if (options.tab) {
      this.currentTab = options.tab
    }
    this.loadData()
  },
  
  methods: {
    switchTab(value) {
      this.currentTab = value
      this.loadData()
    },

    async loadData() {
      if (this.loading) return
      this.loading = true
      
      try {
        if (this.currentTab === 'records') {
          this.records = await getLearningRecords() || []
        } else if (this.currentTab === 'favorites') {
          this.favorites = await getUserFavorites() || []
        } else if (this.currentTab === 'likes') {
          this.likes = await getUserLikes() || []
        }
      } catch (error) {
        console.error('加载数据失败:', error)
      } finally {
        this.loading = false
      }
    },

    goResourceDetail(id) {
      if (!id) return
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
      return resolveUrl(url)
    },

    formatDate(dateStr) {
      if (!dateStr) return ''
      const date = new Date(dateStr)
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
    }
  }
}
</script>

<style lang="scss" scoped>
.container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f8f9fa;
}

.tabs {
  display: flex;
  background: #fff;
  padding: 0 $spacing-md;
  box-shadow: 0 2rpx 10rpx rgba(0,0,0,0.05);
  z-index: 10;

  .tab-item {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: $spacing-md 0;
    position: relative;

    .tab-label {
      font-size: $font-md;
      color: $text-secondary;
      transition: all 0.3s;
    }

    .active-bar {
      width: 40rpx;
      height: 6rpx;
      background: transparent;
      border-radius: 3rpx;
      margin-top: $spacing-xs;
      transition: all 0.3s;
    }

    &.active {
      .tab-label {
        color: $primary-color;
        font-weight: 600;
      }
      .active-bar {
        background: $primary-color;
      }
    }
  }
}

.content-scroll {
  flex: 1;
  padding: $spacing-md;
}

.empty {
  text-align: center;
  padding: 100rpx 0;
  color: $text-tertiary;
  font-size: $font-sm;
}

.record-list {
  .record-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: $spacing-md;
    margin-bottom: $spacing-md;

    .record-info {
      flex: 1;
      .resource-name {
        display: block;
        font-size: $font-md;
        font-weight: 500;
        color: $text-primary;
        margin-bottom: $spacing-xs;
      }
      .meta {
        display: flex;
        gap: $spacing-md;
        .progress, .time {
          font-size: $font-xs;
          color: $text-tertiary;
        }
      }
    }

    .status {
      font-size: $font-xs;
      color: $primary-color;
      background: $primary-light;
      padding: 6rpx 16rpx;
      border-radius: $radius-sm;

      &.completed {
        color: #52c41a;
        background: #f6ffed;
      }
    }
  }
}

.resource-list {
  .resource-item {
    display: flex;
    padding: $spacing-md;
    margin-bottom: $spacing-md;

    .cover {
      width: 180rpx;
      height: 120rpx;
      border-radius: $radius-md;
      margin-right: $spacing-md;
    }

    .info {
      flex: 1;
      display: flex;
      flex-direction: column;
      justify-content: space-between;

      .title {
        font-size: $font-md;
        color: $text-primary;
        font-weight: 500;
      }

      .meta {
        display: flex;
        justify-content: space-between;
        .type {
          font-size: $font-xs;
          color: $primary-color;
          background: $primary-light;
          padding: 4rpx 12rpx;
          border-radius: 4rpx;
        }
        .views {
          font-size: $font-xs;
          color: $text-tertiary;
        }
      }
    }
  }
}
</style>
