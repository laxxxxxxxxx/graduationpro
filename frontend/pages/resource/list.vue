<template>
  <view class="container">
    <!-- 搜索栏 -->
    <view class="search-bar">
      <input 
        class="search-input" 
        v-model="keyword"
        placeholder="搜索心理健康资源..."
        @confirm="handleSearch"
      />
      <button class="search-btn" @click="handleSearch">搜索</button>
    </view>
    
    <!-- 分类导航 -->
    <scroll-view class="category-scroll" scroll-x>
      <view class="category-list">
        <view 
          class="category-item" 
          :class="{active: currentCategory === null}"
          @click="selectCategory(null)"
        >
          <text>全部</text>
        </view>
        <view 
          class="category-item" 
          v-for="cat in categories" 
          :key="cat.id"
          :class="{active: currentCategory === cat.id}"
          @click="selectCategory(cat.id)"
        >
          <text>{{ cat.name }}</text>
        </view>
      </view>
    </scroll-view>
    
    <!-- 类型筛选 -->
    <view class="filter-bar">
      <view 
        class="filter-item"
        :class="{active: currentType === null}"
        @click="selectType(null)"
      >
        <text>全部</text>
      </view>
      <view 
        class="filter-item"
        :class="{active: currentType === 1}"
        @click="selectType(1)"
      >
        <text>📄 文章</text>
      </view>
      <view 
        class="filter-item"
        :class="{active: currentType === 2}"
        @click="selectType(2)"
      >
        <text>🎥 视频</text>
      </view>
      <view 
        class="filter-item"
        :class="{active: currentType === 3}"
        @click="selectType(3)"
      >
        <text>🎵 音频</text>
      </view>
    </view>
    
    <!-- 资源列表 -->
    <view class="resource-list" v-if="resources.length > 0">
      <view 
        class="resource-item" 
        v-for="item in resources" 
        :key="item.id"
        @click="goDetail(item)"
      >
        <view class="resource-cover" v-if="item.coverUrl">
          <image class="cover-img" :src="item.coverUrl" mode="aspectFill"></image>
        </view>
        <view class="resource-info">
          <text class="title">{{ item.title }}</text>
          <view class="tags" v-if="item.tags">
            <text class="tag" v-for="tag in item.tags.split(',').slice(0, 3)" :key="tag">
              {{ tag }}
            </text>
          </view>
          <view class="meta">
            <text :class="item.typeClass">{{ getTypeName(item.type) }}</text>
            <text class="difficulty" v-if="item.difficulty">{{ getDifficultyName(item.difficulty) }}</text>
            <text class="views">👁 {{ item.viewCount || 0 }}</text>
            <text class="likes">❤️ {{ item.likeCount || 0 }}</text>
          </view>
        </view>
        <view class="arrow">›</view>
      </view>
    </view>
    
    <!-- 加载更多 -->
    <view class="load-more" v-if="hasMore && !loading">
      <button class="load-btn" @click="loadMore">加载更多</button>
    </view>
    
    <!-- 加载状态 -->
    <view class="loading" v-if="loading">
      <text>加载中...</text>
    </view>
    
    <!-- 空状态 -->
    <view class="empty" v-if="!loading && resources.length === 0">
      <text class="empty-icon">📚</text>
      <text class="empty-text">暂无资源</text>
      <text class="empty-hint">请选择其他分类或筛选条件</text>
    </view>
  </view>
</template>

<script>
import { getResourceList, getCategories } from '@/api/resource'

export default {
  data() {
    return {
      keyword: '',
      categories: [],
      currentCategory: null,
      currentType: null,
      resources: [],
      pageNum: 1,
      pageSize: 10,
      total: 0,
      loading: false,
      hasMore: true
    }
  },
  
  onLoad() {
    this.loadCategories()
    this.loadResources()
  },
  
  methods: {
    // 加载分类列表
    async loadCategories() {
      try {
        const res = await getCategories()
        this.categories = res || []
      } catch (error) {
        console.error('加载分类失败:', error)
      }
    },
    
    // 加载资源列表
    async loadResources() {
      if (this.loading) return
      
      try {
        this.loading = true
        
        const params = {
          pageNum: this.pageNum,
          pageSize: this.pageSize
        }
        
        if (this.currentType !== null) {
          params.type = this.currentType
        }
        
        if (this.currentCategory !== null) {
          params.categoryId = this.currentCategory
        }
        
        const res = await getResourceList(params)
        const enriched = (res.records || []).map(r => ({
          ...r,
          typeClass: 'type-badge type-' + r.type
        }))
        
        if (this.pageNum === 1) {
          this.resources = enriched
        } else {
          this.resources = [...this.resources, ...enriched]
        }
        
        this.total = res.total || 0
        this.hasMore = this.resources.length < this.total
      } catch (error) {
        console.error('加载资源失败:', error)
        uni.showToast({
          title: '加载失败',
          icon: 'none'
        })
      } finally {
        this.loading = false
      }
    },
    
    // 选择分类
    selectCategory(categoryId) {
      this.currentCategory = categoryId
      this.pageNum = 1
      this.loadResources()
    },
    
    // 选择类型
    selectType(type) {
      this.currentType = type
      this.pageNum = 1
      this.loadResources()
    },
    
    // 搜索
    handleSearch() {
      if (!this.keyword.trim()) {
        uni.showToast({
          title: '请输入搜索关键词',
          icon: 'none'
        })
        return
      }
      
      uni.navigateTo({
        url: `/pages/resource/list?keyword=${encodeURIComponent(this.keyword)}`
      })
    },
    
    // 加载更多
    loadMore() {
      this.pageNum++
      this.loadResources()
    },
    
    // 跳转详情
    goDetail(item) {
      uni.navigateTo({
        url: `/pages/resource/detail?id=${item.id}`
      })
    },
    
    // 获取类型名称
    getTypeName(type) {
      const typeMap = {
        1: '文章',
        2: '视频',
        3: '音频',
        4: '课程'
      }
      return typeMap[type] || '其他'
    },
    
    // 获取难度名称
    getDifficultyName(difficulty) {
      const difficultyMap = {
        1: '入门',
        2: '进阶',
        3: '高级'
      }
      return difficultyMap[difficulty] || ''
    }
  }
}
</script>

<style lang="scss" scoped>
.container {
  padding: $spacing-md;
  padding-bottom: $spacing-xl;
}

.search-bar {
  display: flex;
  gap: $spacing-sm;
  margin-bottom: $spacing-md;
  
  .search-input {
    flex: 1;
    height: 80rpx;
    padding: 0 40rpx;
    background: #fff;
    border-radius: $radius-xl;
    font-size: $font-md;
    box-shadow: $shadow-sm;
  }
  
  .search-btn {
    width: 140rpx;
    height: 80rpx;
    line-height: 80rpx;
    @extend %btn-primary;
    border-radius: $radius-xl;
    font-size: $font-md;
    padding: 0;
  }
}

.category-scroll {
  white-space: nowrap;
  margin-bottom: $spacing-md;
  
  .category-list {
    display: inline-flex;
    gap: $spacing-sm;
    padding-bottom: 10rpx;
    
    .category-item {
      display: inline-block;
      padding: 14rpx 36rpx;
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
}

.filter-bar {
  display: flex;
  gap: $spacing-sm;
  margin-bottom: $spacing-lg;
  
  .filter-item {
    flex: 1;
    height: 70rpx;
    line-height: 70rpx;
    text-align: center;
    background: #fff;
    border-radius: $radius-md;
    font-size: $font-sm;
    color: $text-secondary;
    transition: all 0.3s;
    box-shadow: $shadow-sm;
    
    &.active {
      background: $primary-light;
      color: $primary-color;
      font-weight: 600;
      border: 1rpx solid $primary-color;
    }
  }
}

.resource-list {
  .resource-item {
    @extend %card;
    padding: $spacing-md;
    display: flex;
    align-items: center;
    gap: $spacing-md;
    transition: transform 0.2s;
    
    &:active {
      transform: scale(0.98);
    }
    
    .resource-cover {
      width: 180rpx;
      height: 140rpx;
      border-radius: $radius-md;
      overflow: hidden;
      flex-shrink: 0;
      
      .cover-img {
        width: 100%;
        height: 100%;
      }
    }
    
    .resource-info {
      flex: 1;
      min-width: 0;
      
      .title {
        display: block;
        font-size: $font-md;
        font-weight: 600;
        color: $text-primary;
        margin-bottom: 12rpx;
        line-height: 1.4;
      }
      
      .tags {
        display: flex;
        gap: 10rpx;
        flex-wrap: wrap;
        margin-bottom: 12rpx;
        
        .tag {
          font-size: $font-xs;
          color: $primary-color;
          background: $primary-light;
          padding: 4rpx 16rpx;
          border-radius: $radius-sm;
        }
      }
      
      .meta {
        display: flex;
        gap: 15rpx;
        align-items: center;
        flex-wrap: wrap;
        
        .type-badge {
          font-size: $font-xs;
          padding: 4rpx 16rpx;
          border-radius: $radius-sm;
          
          &.type-1 {
            color: #1890ff;
            background: #e6f7ff;
          }
          
          &.type-2 {
            color: #722ed1;
            background: #f9f0ff;
          }
          
          &.type-3 {
            color: #13c2c2;
            background: #e6fffb;
          }
          
          &.type-4 {
            color: #52c41a;
            background: #f6ffed;
          }
        }
        
        .difficulty {
          font-size: $font-xs;
          color: $warning-color;
          background: #FFF9E6;
          padding: 4rpx 16rpx;
          border-radius: $radius-sm;
        }
        
        .views, .likes {
          font-size: $font-xs;
          color: $text-tertiary;
        }
      }
    }
    
    .arrow {
      font-size: 40rpx;
      color: $text-tertiary;
      flex-shrink: 0;
    }
  }
}

.load-more {
  text-align: center;
  padding: $spacing-lg 0;
  
  .load-btn {
    background: transparent;
    color: $primary-color;
    border: 2rpx solid $primary-color;
    border-radius: $radius-xl;
    font-size: $font-sm;
    height: 70rpx;
    line-height: 70rpx;
    width: 240rpx;
  }
}

.loading {
  text-align: center;
  padding: 50rpx 0;
  color: $text-tertiary;
  font-size: $font-sm;
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
    font-weight: 500;
  }
  
  .empty-hint {
    font-size: $font-sm;
    color: $text-tertiary;
  }
}
</style>
