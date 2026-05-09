<template>
  <view class="container">
    <view class="post-header">
      <text class="title">{{ post.title }}</text>
      <view class="meta">
        <text class="author">{{ post.anonymousId }}</text>
        <text class="category" v-if="post.categoryName">{{ post.categoryName }}</text>
        <text class="time">{{ formatDate(post.createdAt) }}</text>
      </view>
      <!-- 作者操作按钮 -->
      <view class="author-actions" v-if="isPostAuthor">
        <text class="action-btn edit" @click="editPost">编辑</text>
        <text class="action-btn delete" @click="confirmDeletePost">删除</text>
      </view>
    </view>
    
    <view class="post-content">
      <text>{{ post.content }}</text>
    </view>
    
    <view class="post-stats">
      <view class="stat-item" @click="toggleLike">
        <text class="icon">{{ liked ? '❤️' : '🤍' }}</text>
        <text class="count">{{ post.likeCount }}</text>
      </view>
      <view class="stat-item">
        <text class="icon">👁</text>
        <text class="count">{{ post.viewCount }}</text>
      </view>
      <view class="stat-item">
        <text class="icon">💬</text>
        <text class="count">{{ post.commentCount }}</text>
      </view>
    </view>
    
    <view class="comments-section">
      <text class="section-title">评论 ({{ comments.length }})</text>
      <view class="comment-list">
        <view class="comment-item" v-for="comment in comments" :key="comment.id">
          <view class="comment-top">
            <text class="comment-author">{{ comment.anonymousId }}</text>
            <text class="comment-delete" v-if="currentUserId === comment.userId" @click="confirmDeleteComment(comment.id)">删除</text>
          </view>
          <text class="comment-content">{{ comment.content }}</text>
        </view>
        <view class="no-comment" v-if="comments.length === 0">
          <image class="empty-image" src="/static/images/empty-state.png" mode="aspectFit"></image>
          <text>暂无评论，来抢沙发吧~</text>
        </view>
      </view>
    </view>
    
    <view class="comment-input">
      <input class="input" v-model="commentText" placeholder="写下你的评论..." />
      <button class="send-btn" @click="sendComment">发送</button>
    </view>
  </view>
</template>

<script>
import { getPostDetail, createComment, likePost, unlikePost, getComments, deletePost, deleteComment } from '@/api/community'

export default {
  data() {
    return {
      postId: null,
      post: {},
      comments: [],
      liked: false,
      currentUserId: null,
      commentText: ''
    }
  },
  
  computed: {
    isPostAuthor() {
      return this.currentUserId && this.post.userId && this.currentUserId === this.post.userId
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
    
    editPost() {
      uni.navigateTo({
        url: `/pages/community/post-create?id=${this.postId}&mode=edit`
      })
    },
    
    confirmDeletePost() {
      uni.showModal({
        title: '确认删除',
        content: '删除后无法恢复，确定要删除这个帖子吗？',
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
        uni.showToast({ title: '删除成功', icon: 'success' })
        setTimeout(() => {
          uni.navigateBack()
        }, 1500)
      } catch (error) {
        console.error('删除帖子失败:', error)
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
        uni.showToast({ title: '删除成功', icon: 'success' })
        this.loadComments()
        if (this.post.commentCount > 0) {
          this.post.commentCount--
        }
      } catch (error) {
        console.error('删除评论失败:', error)
      }
    },
    
    async toggleLike() {
      try {
        if (this.liked) {
          await unlikePost(this.postId)
          this.liked = false
          this.post.likeCount--
        } else {
          await likePost(this.postId)
          this.liked = true
          this.post.likeCount++
        }
      } catch (error) {
        console.error('点赞操作失败:', error)
      }
    },
    
    async sendComment() {
      if (!this.commentText.trim()) {
        uni.showToast({ title: '请输入评论内容', icon: 'none' })
        return
      }
      
      try {
        await createComment(this.postId, {
          content: this.commentText
        })
        
        uni.showToast({ title: '评论成功', icon: 'success' })
        this.commentText = ''
        
        // 重新加载评论
        this.loadComments()
        
        // 更新评论数
        this.post.commentCount++
      } catch (error) {
        console.error('评论失败:', error)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.container {
  padding: $spacing-md;
  padding-bottom: 160rpx;
}

.post-header {
  @extend %card;
  padding: 40rpx;
  border-radius: $radius-lg $radius-lg 0 0;
  margin-bottom: 0;
  box-shadow: none;
  border-bottom: 1rpx solid $border-color;
  
  .title {
    display: block;
    font-size: 40rpx;
    font-weight: 700;
    color: $text-primary;
    margin-bottom: $spacing-md;
    line-height: 1.4;
  }
  
  .meta {
    display: flex;
    gap: 30rpx;
    font-size: $font-xs;
    color: $text-tertiary;
    font-weight: 500;
  }
  
  .author-actions {
    margin-top: 24rpx;
    display: flex;
    gap: 24rpx;
    
    .action-btn {
      font-size: $font-xs;
      padding: 8rpx 24rpx;
      border-radius: $radius-sm;
      font-weight: 500;
      
      &.edit {
        color: $primary-color;
        background: $primary-light;
      }
      
      &.delete {
        color: $error-color;
        background: #FFF5F5;
      }
    }
  }
}

.post-content {
  @extend %card;
  padding: 40rpx;
  border-radius: 0 0 $radius-lg $radius-lg;
  margin-top: 0;
  box-shadow: $shadow-sm;
  font-size: $font-md;
  color: $text-secondary;
  line-height: 1.8;
  letter-spacing: 0.5rpx;
}

.post-stats {
  @extend %card;
  padding: 30rpx 40rpx;
  display: flex;
  justify-content: space-around;
  margin-top: $spacing-md;
  
  .stat-item {
    display: flex;
    align-items: center;
    gap: 12rpx;
    padding: 10rpx 30rpx;
    border-radius: $radius-xl;
    transition: all 0.2s;
    
    &:active {
      background: $bg-color;
    }
    
    .icon {
      font-size: 40rpx;
    }
    
    .count {
      font-size: $font-sm;
      color: $text-secondary;
      font-weight: 600;
    }
  }
}

.comments-section {
  @extend %card;
  padding: 40rpx;
  margin-top: $spacing-lg;
  
  .section-title {
    @extend %section-title;
    margin-bottom: 40rpx;
  }
  
  .comment-item {
    padding: 30rpx 0;
    border-bottom: 1rpx solid $border-color;
    
    &:last-child {
      border-bottom: none;
    }
    
    .comment-top {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12rpx;
      
      .comment-author {
        font-size: $font-sm;
        color: $text-primary;
        font-weight: 600;
      }
      
      .comment-delete {
        font-size: $font-xs;
        color: $error-color;
        padding: 6rpx 16rpx;
        background: #FFF5F5;
        border-radius: $radius-sm;
        font-weight: 500;
      }
    }
    
    .comment-content {
      font-size: $font-md;
      color: $text-secondary;
      line-height: 1.6;
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
}

.comment-input {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  padding: 24rpx 40rpx;
  padding-bottom: calc(24rpx + env(safe-area-inset-bottom));
  display: flex;
  gap: 20rpx;
  box-shadow: 0 -10rpx 30rpx rgba(0,0,0,0.05);
  z-index: 100;
  
  .input {
    flex: 1;
    height: 84rpx;
    padding: 0 32rpx;
    background: $bg-color;
    border-radius: $radius-xl;
    font-size: $font-md;
    box-shadow: inset 0 2rpx 4rpx rgba(0,0,0,0.03);
  }
  
  .send-btn {
    width: 140rpx;
    height: 84rpx;
    line-height: 84rpx;
    @extend %btn-primary;
    border-radius: $radius-xl;
    font-size: $font-md;
    padding: 0;
    margin: 0;
  }
}
</style>
);
  }
  
  .send-btn {
    width: 140rpx;
    height: 84rpx;
    line-height: 84rpx;
    @extend %btn-primary;
    border-radius: $radius-xl;
    font-size: $font-md;
    padding: 0;
    margin: 0;
  }
}
</style>
