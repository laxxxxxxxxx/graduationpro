<template>
  <view class="container">
    <!-- 分类筛选标签 -->
    <scroll-view scroll-x class="category-tabs">
      <view 
        class="tab-item" 
        :class="{ active: activeCategory === 0 }"
        @click="switchCategory(0)"
      >全部</view>
      <view 
        class="tab-item" 
        v-for="cat in categories" 
        :key="cat.id"
        :class="{ active: activeCategory === cat.id }"
        @click="switchCategory(cat.id)"
      >{{ cat.icon }} {{ cat.name }}</view>
    </scroll-view>
    
    <view class="post-list">
      <view 
        class="post-item" 
        v-for="post in posts" 
        :key="post.id"
        @click="goDetail(post.id)"
      >
        <view class="post-header">
          <text class="title">{{ post.title }}</text>
          <text class="category" v-if="post.categoryName">{{ post.categoryName }}</text>
        </view>
        
        <text class="content">{{ post.content }}</text>
        
        <view class="post-footer">
          <text class="author">{{ post.anonymousId }}</text>
          <view class="stats">
            <text>❤️ {{ post.likeCount }}</text>
            <text>💬 {{ post.commentCount }}</text>
            <text>👁 {{ post.viewCount }}</text>
          </view>
        </view>
      </view>
      
      <view class="empty" v-if="!loading && posts.length === 0">
        <text>暂无帖子</text>
      </view>
    </view>
    
    <button class="fab" @click="createPost">+</button>
  </view>
</template>

<script>
import { getPostList, getCategories } from '@/api/community'

export default {
  data() {
    return {
      posts: [],
      categories: [],
      activeCategory: 0,
      pageNum: 1,
      pageSize: 10,
      loading: false,
      hasMore: true,
      isMine: false,
      isLikes: false
    }
  },
  
  onLoad(options) {
    if (options.mine) {
      this.isMine = true
      uni.setNavigationBarTitle({ title: '我的发布' })
    }
    if (options.likes) {
      this.isLikes = true
      uni.setNavigationBarTitle({ title: '我的点赞' })
    }
    this.loadCategories()
    this.loadPosts()
  },
  
  onPullDownRefresh() {
    this.pageNum = 1
    this.hasMore = true
    this.loadPosts().then(() => {
      uni.stopPullDownRefresh()
    })
  },
  
  onReachBottom() {
    if (this.hasMore && !this.loading) {
      this.pageNum++
      this.loadMorePosts()
    }
  },
  
  methods: {
    async loadCategories() {
      try {
        const data = await getCategories()
        this.categories = data || []
      } catch (error) {
        console.error('加载分类失败:', error)
      }
    },
    
    switchCategory(categoryId) {
      this.activeCategory = categoryId
      this.pageNum = 1
      this.hasMore = true
      this.posts = []
      this.loadPosts()
    },
    
    async loadPosts() {
      try {
        this.loading = true
        const params = {
          pageNum: this.pageNum,
          pageSize: this.pageSize
        }
        if (this.activeCategory !== 0) {
          params.categoryId = this.activeCategory
        }
        if (this.isMine) {
          params.mine = true
        }
        if (this.isLikes) {
          params.likes = true
        }
        const data = await getPostList(params)
        
        this.posts = data.records || []
        this.hasMore = this.posts.length >= this.pageSize
      } catch (error) {
        console.error('加载帖子失败:', error)
      } finally {
        this.loading = false
      }
    },
    
    async loadMorePosts() {
      try {
        this.loading = true
        const params = {
          pageNum: this.pageNum,
          pageSize: this.pageSize
        }
        if (this.activeCategory !== 0) {
          params.categoryId = this.activeCategory
        }
        if (this.isMine) {
          params.mine = true
        }
        if (this.isLikes) {
          params.likes = true
        }
        const data = await getPostList(params)
        
        const newPosts = data.records || []
        this.posts = [...this.posts, ...newPosts]
        this.hasMore = newPosts.length >= this.pageSize
      } catch (error) {
        console.error('加载更多失败:', error)
        this.pageNum--
      } finally {
        this.loading = false
      }
    },
    
    goDetail(id) {
      uni.navigateTo({
        url: `/pages/community/post-detail?id=${id}`
      })
    },
    
    createPost() {
      uni.navigateTo({
        url: '/pages/community/post-create'
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

.category-tabs {
  white-space: nowrap;
  padding: $spacing-sm 0 $spacing-lg;
  
  .tab-item {
    display: inline-block;
    padding: 14rpx 36rpx;
    margin-right: $spacing-sm;
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

.post-list {
  .post-item {
    @extend %card;
    padding: $spacing-lg;
    transition: transform 0.2s;
    
    &:active {
      transform: scale(0.99);
    }
    
    .post-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: $spacing-sm;
      
      .title {
        font-size: $font-lg;
        font-weight: 700;
        color: $text-primary;
        flex: 1;
        line-height: 1.4;
      }
      
      .category {
        font-size: $font-xs;
        color: $primary-color;
        background: $primary-light;
        padding: 6rpx 16rpx;
        border-radius: $radius-sm;
        margin-left: $spacing-md;
        white-space: nowrap;
      }
    }
    
    .content {
      display: block;
      font-size: $font-md;
      color: $text-secondary;
      line-height: 1.6;
      margin-bottom: $spacing-lg;
      @extend %text-ellipsis-2;
    }
    
    .post-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding-top: $spacing-md;
      border-top: 1rpx solid $border-color;
      
      .author {
        font-size: $font-xs;
        color: $text-tertiary;
        font-weight: 500;
      }
      
      .stats {
        display: flex;
        gap: $spacing-lg;
        font-size: $font-xs;
        color: $text-tertiary;
        
        text {
          display: flex;
          align-items: center;
          gap: 6rpx;
        }
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

.empty {
  text-align: center;
  padding: 100rpx 0;
  color: $text-tertiary;
  font-size: $font-md;
}
</style>
