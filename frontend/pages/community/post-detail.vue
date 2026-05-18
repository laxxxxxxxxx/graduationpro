<template>
  <view class="container">
    <!-- 帖子主体 -->
    <view class="post-card card" v-if="post">
      <view class="post-header">
        <view class="author-info">
          <view class="avatar">{{ (post.anonymousId || '用')[0] }}</view>
          <view class="meta">
            <text class="nickname">{{ post.anonymousId }}</text>
            <text class="time">{{ formatTime(post.createdAt) }}</text>
          </view>
        </view>
        <view class="header-right">
          <text class="delete-post" v-if="isAdmin" @click="confirmDeletePost">删除帖子</text>
          <text class="category" v-if="post.categoryName">{{ post.categoryName }}</text>
        </view>
      </view>
      
      <text class="title">{{ post.title }}</text>
      <text class="content">{{ post.content }}</text>
      
      <view class="post-stats">
        <view class="stat-item" @click="handleToggleLike">
          <text class="icon">{{ liked ? '❤️' : '🤍' }}</text>
          <text class="count" :class="{active: liked}">{{ post.likeCount }}</text>
        </view>
        <view class="stat-item">
          <text class="icon">💬</text>
          <text class="count">{{ post.commentCount }}</text>
        </view>
        <view class="stat-item">
          <text class="icon">👁</text>
          <text class="count">{{ post.viewCount }}</text>
        </view>
      </view>
    </view>
    
    <!-- 评论列表 -->
    <view class="comments-section" v-if="post">
      <text class="section-title">评论 ({{ comments.length }})</text>
      <view class="comment-list">
        <view class="comment-item" v-for="(comment, index) in comments" :key="index">
          <view class="comment-top">
            <text class="comment-author">{{ comment.anonymousId }}</text>
            <text class="comment-delete" v-if="isAdmin || currentUserId === comment.userId" @click="confirmDeleteComment(comment.id)">删除</text>
          </view>
          <text class="comment-content">{{ comment.content }}</text>
          <text class="comment-time">{{ formatTime(comment.createdAt) }}</text>
        </view>
        
        <view class="no-comment" v-if="comments.length === 0">
          <image class="empty-image" src="/static/images/empty-state.png" mode="aspectFit"></image>
          <text>暂无评论，来抢沙发吧~</text>
        </view>
      </view>
    </view>
    
    <!-- 底部输入框 -->
    <view class="comment-input-bar" v-if="post">
      <input 
        class="input" 
        v-model="commentText" 
        placeholder="说点什么吧..." 
        confirm-type="send"
        @confirm="sendComment"
      />
      <button class="send-btn" @click="sendComment" :disabled="!commentText.trim()">发送</button>
    </view>
    
    <!-- 加载中 -->
    <view class="loading" v-if="!post">
      <text>加载中...</text>
    </view>
  </view>
</template>

<script>
import { getPostDetail, getComments, createComment, deleteComment, deletePost, likePost, unlikePost } from '@/api/community'
import { resolveUrl } from '@/utils/request'

export default {
  data() {
    return {
      postId: null,
      post: null,
      comments: [],
      commentText: '',
      liked: false,
      currentUserId: null
    }
  },

  computed: {
    isAdmin() {
      const userInfo = uni.getStorageSync('userInfo')
      return userInfo && (userInfo.role === 3 || userInfo.username === 'admin')
    }
  },
  
  onLoad(options) {
    this.postId = options.id
    this.loadPostDetail()
    this.loadComments()
  },
  
  methods: {
    async loadPostDetail() {
      try {
        const data = await getPostDetail(this.postId)
        this.post = data.post || {}
        this.liked = data.liked || false
        this.currentUserId = data.currentUserId || null
      } catch (error) {
        console.error('加载帖子详情失败:', error)
      }
    },
    
    async loadComments() {
      try {
        const data = await getComments(this.postId)
        this.comments = data || []
      } catch (error) {
        console.error('加载评论失败:', error)
      }
    },
    
    async handleToggleLike() {
      if (!this.checkLogin()) return
      
      try {
        if (this.liked) {
          await unlikePost(this.postId)
          this.post.likeCount--
          this.liked = false
        } else {
          await likePost(this.postId)
          this.post.likeCount++
          this.liked = true
        }
      } catch (error) {
        console.error('点赞失败:', error)
      }
    },
    
    async sendComment() {
      if (!this.checkLogin()) return
      if (!this.commentText.trim()) return
      
      try {
        await createComment(this.postId, {
          content: this.commentText.trim()
        })
        this.commentText = ''
        uni.showToast({ title: '发表成功', icon: 'success' })
        this.loadComments()
        this.post.commentCount++
      } catch (error) {
        console.error('发表评论失败:', error)
      }
    },
    
    confirmDeleteComment(commentId) {
      uni.showModal({
        title: '确认删除',
        content: '确定要删除这条评论吗？',
        success: (res) => {
          if (res.confirm) {
            this.handleDeleteComment(commentId)
          }
        }
      })
    },
    
    async handleDeleteComment(commentId) {
      try {
        await deleteComment(commentId)
        uni.showToast({ title: '已删除', icon: 'none' })
        this.loadComments()
        this.post.commentCount--
      } catch (error) {
        console.error('删除失败:', error)
      }
    },

    confirmDeletePost() {
      uni.showModal({
        title: '确认删除',
        content: '管理员，确定要删除这条帖子吗？',
        success: (res) => {
          if (res.confirm) {
            this.handleDeletePost()
          }
        }
      })
    },

    async handleDeletePost() {
      try {
        await deletePost(this.postId)
        uni.showToast({ title: '已删除', icon: 'success' })
        setTimeout(() => {
          uni.navigateBack()
        }, 1500)
      } catch (error) {
        console.error('删除帖子失败:', error)
      }
    },
    
    checkLogin() {
      const token = uni.getStorageSync('token')
      if (!token) {
        uni.navigateTo({ url: '/pages/login/login' })
        return false
      }
      return true
    },
    
    formatTime(dateStr) {
      if (!dateStr) return ''
      const date = new Date(dateStr)
      return `${date.getMonth() + 1}-${date.getDate()} ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`
    },

    resolveCoverUrl(url) {
      return resolveUrl(url)
    }
  }
}
</script>

<style lang="scss" scoped>
.container {
  padding: $spacing-md;
  padding-bottom: 120rpx;
}

.post-card {
  padding: 40rpx;
  
  .post-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 30rpx;
    
    .author-info {
      display: flex;
      align-items: center;
      gap: 20rpx;
      
      .avatar {
        width: 70rpx;
        height: 70rpx;
        background: $primary-gradient;
        border-radius: $radius-round;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #fff;
        font-weight: 700;
        font-size: $font-lg;
      }
      
      .meta {
        display: flex;
        flex-direction: column;
        
        .nickname {
          font-size: $font-sm;
          font-weight: 600;
          color: $text-primary;
        }
        
        .time {
          font-size: $font-xs;
          color: $text-tertiary;
        }
      }
    }
    
    .category {
      font-size: $font-xs;
      color: $primary-color;
      background: $primary-light;
      padding: 6rpx 20rpx;
      border-radius: $radius-sm;
    }

    .header-right {
      display: flex;
      align-items: center;
      gap: 16rpx;

      .delete-post {
        font-size: $font-xs;
        color: $error-color;
        border: 1rpx solid $error-color;
        padding: 4rpx 16rpx;
        border-radius: $radius-sm;
      }
    }
  }
  
  .title {
    display: block;
    font-size: $font-lg;
    font-weight: 700;
    color: $text-primary;
    margin-bottom: 24rpx;
    line-height: 1.4;
  }
  
  .content {
    display: block;
    font-size: $font-md;
    color: $text-secondary;
    line-height: 1.7;
    margin-bottom: 40rpx;
    white-space: pre-wrap;
  }
  
  .post-stats {
    display: flex;
    gap: 40rpx;
    border-top: 1rpx solid $border-color;
    padding-top: 30rpx;
    
    .stat-item {
      display: flex;
      align-items: center;
      gap: 10rpx;
      
      .icon {
        font-size: 36rpx;
      }
      
      .count {
        font-size: $font-sm;
        color: $text-tertiary;
        
        &.active {
          color: $primary-color;
          font-weight: 600;
        }
      }
    }
  }
}

.comments-section {
  margin-top: 40rpx;
  
  .section-title {
    font-size: 32rpx;
    font-weight: 600;
    color: #4A4A4A;
    margin-bottom: 24rpx;
    position: relative;
    padding-left: 20rpx;
    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 50%;
      transform: translateY(-50%);
      width: 8rpx;
      height: 32rpx;
      background: #FF8C8C;
      border-radius: 4rpx;
    }
    margin-bottom: 24rpx;
  }
  
  .comment-list {
    .comment-item {
      background: #ffffff;
      border-radius: 32rpx;
      padding: 32rpx;
      margin-bottom: 24rpx;
      box-shadow: 0 8rpx 24rpx rgba(255, 140, 140, 0.08);
      border: 1px solid rgba(255, 255, 255, 0.8);
      padding: 30rpx;
      margin-bottom: 20rpx;
      
      .comment-top {
        display: flex;
        justify-content: space-between;
        margin-bottom: 16rpx;
        
        .comment-author {
          font-size: $font-sm;
          font-weight: 600;
          color: $text-primary;
        }
        
        .comment-delete {
          font-size: $font-xs;
          color: $error-color;
        }
      }
      
      .comment-content {
        display: block;
        font-size: $font-md;
        color: $text-secondary;
        line-height: 1.5;
        margin-bottom: 12rpx;
      }
      
      .comment-time {
        font-size: $font-xs;
        color: $text-tertiary;
      }
    }
  }
}

.no-comment {
  text-align: center;
  padding: 80rpx 0;
  font-size: $font-sm;
  color: $text-tertiary;

  .empty-image {
    width: 220rpx;
    height: 170rpx;
    margin-bottom: $spacing-sm;
  }

  text {
    display: block;
  }
}

.comment-input-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 110rpx;
  background: #fff;
  display: flex;
  align-items: center;
  padding: 0 30rpx;
  padding-bottom: env(safe-area-inset-bottom);
  gap: 20rpx;
  box-shadow: 0 -4rpx 20rpx rgba(0,0,0,0.05);
  z-index: 100;
  
  .input {
    flex: 1;
    height: 76rpx;
    background: $bg-color;
    border-radius: $radius-xl;
    padding: 0 30rpx;
    font-size: $font-sm;
  }
  
  .send-btn {
    width: 120rpx;
    height: 70rpx;
    line-height: 70rpx;
    background: linear-gradient(135deg, #FF9A9E 0%, #FECFEF 100%);
    color: #fff;
    border-radius: 50rpx;
    border: none;
    font-weight: 500;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 6rpx 16rpx rgba(255, 140, 140, 0.2);
    font-size: $font-sm;
    padding: 0;
    margin: 0;
    
    &[disabled] {
      opacity: 0.5;
    }
  }
}

.loading {
  text-align: center;
  padding: 100rpx 0;
  color: $text-tertiary;
}
</style>
