<template>
  <view class="container">
    <!-- 资源主体内容 -->
    <view class="resource-content" v-if="resource">
      <!-- 资源标题 -->
      <text class="title">{{ resource.title }}</text>
      
      <!-- 资源元信息 -->
      <view class="meta-info">
        <view class="meta-row">
          <text class="type-badge type-1" v-if="resource.type === 1">{{ getTypeName(resource.type) }}</text>
          <text class="type-badge type-2" v-else-if="resource.type === 2">{{ getTypeName(resource.type) }}</text>
          <text class="type-badge type-3" v-else-if="resource.type === 3">{{ getTypeName(resource.type) }}</text>
          <text class="type-badge type-4" v-else>{{ getTypeName(resource.type) }}</text>
          <text class="difficulty" v-if="resource.difficulty">{{ getDifficultyName(resource.difficulty) }}</text>
          <text class="views">👁 {{ resource.viewCount || 0 }}</text>
          <text class="likes">❤️ {{ resource.likeCount || 0 }}</text>
          <text class="comments-count">💬 {{ resource.commentCount || 0 }}</text>
        </view>
        
        <!-- 标签 -->
        <view class="tags" v-if="resource.tags">
          <text class="tag" v-for="tag in resource.tags.split(',')" :key="tag">
            {{ tag }}
          </text>
        </view>
        
        <!-- 作者和来源 -->
        <view class="author-info" v-if="resource.author || resource.source">
          <text class="author" v-if="resource.author">作者: {{ resource.author }}</text>
          <text class="source" v-if="resource.source">来源: {{ resource.source }}</text>
        </view>
      </view>

      <!-- 资源描述（简介） -->
      <view class="description-box" v-if="resource.description">
        <text class="description-text">{{ resource.description }}</text>
      </view>
      
      <!-- ========== 视频播放区域 ========== -->
      <view class="video-section" v-if="resource.type === 2">
        <!-- 有真实视频链接时显示播放器 -->
        <view class="video-player" v-if="resource.mediaUrl">
          <video 
            class="video"
            :src="resolveUrl(resource.mediaUrl)" 
            :poster="resolveCoverUrl(resource.coverUrl)"
            controls
            show-center-play-btn
            object-fit="contain"
            :title="resource.title"
          ></video>
        </view>
        <!-- 无视频链接时显示占位提示 -->
        <view class="video-placeholder" v-else>
          <text class="placeholder-icon">🎥</text>
          <text class="placeholder-text">视频资源加载中...</text>
          <text class="placeholder-hint">该视频暂未上传或链接失效</text>
        </view>
        <!-- 视频信息 -->
        <view class="media-info" v-if="resource.duration">
          <text class="duration">⏱ {{ formatDuration(resource.duration) }}</text>
        </view>
      </view>

      <!-- ========== 音频播放区域 ========== -->
      <view class="audio-section" v-if="resource.type === 3">
        <view class="audio-player" v-if="resource.mediaUrl">
          <view class="audio-card">
            <image class="audio-cover" :src="resolveCoverUrl(resource.coverUrl)" mode="aspectFill"></image>
            <view class="audio-info">
              <text class="audio-title">{{ resource.title }}</text>
              <text class="audio-duration" v-if="resource.duration">时长: {{ formatDuration(resource.duration) }}</text>
              <audio 
                v-if="resource.mediaUrl"
                style="width: 100%; margin-top: 10px;"
                :src="resolveUrl(resource.mediaUrl)"
                :name="resource.title"
                :poster="resolveCoverUrl(resource.coverUrl)"
                controls
              ></audio>
            </view>
          </view>
        </view>
        <view class="video-placeholder" v-else>
          <text class="placeholder-icon">🎵</text>
          <text class="placeholder-text">音频资源加载中...</text>
        </view>
      </view>
      
      <!-- ========== 文章内容区域 ========== -->
      <view class="article-section" v-if="resource.type === 1 && resource.content">
        <view class="article-body">
          <rich-text :nodes="formatArticleContent(resource.content)"></rich-text>
        </view>
      </view>
      
      <!-- ========== 课程类型 ========== -->
      <view class="course-section" v-if="resource.type === 4">
        <view class="video-player" v-if="resource.mediaUrl">
          <video 
            class="video"
            :src="resolveUrl(resource.mediaUrl)" 
            :poster="resolveCoverUrl(resource.coverUrl)"
            controls
            show-center-play-btn
            object-fit="contain"
          ></video>
        </view>
        <view class="article-body" v-if="resource.content">
          <rich-text :nodes="formatArticleContent(resource.content)"></rich-text>
        </view>
      </view>
      
      <!-- 学习进度 -->
      <view class="study-progress" v-if="studyRecord">
        <view class="progress-header">
          <text class="progress-title">学习进度</text>
          <text class="progress-percent">{{ formatPercent(studyRecord.progress) }}</text>
        </view>
        <view class="progress-bar">
          <view class="progress-fill" :style="progressStyle"></view>
        </view>
        <text class="study-duration">已学习 {{ formatDuration(studyRecord.studyDuration) }}</text>
      </view>
    </view>

    <!-- ========== 评论区域 ========== -->
    <view class="comment-section" v-if="resource">
      <view class="comment-header">
        <text class="comment-title">评论 ({{ comments.length }})</text>
      </view>

      <!-- 评论列表 -->
      <view class="comment-list" v-if="comments.length > 0">
        <view class="comment-item" v-for="comment in comments" :key="comment.id">
          <view class="comment-avatar">
            <text class="avatar-text">{{ (comment.username || '用户')[0] }}</text>
          </view>
          <view class="comment-body">
            <view class="comment-user">
              <text class="comment-username">{{ comment.username || '匿名用户' }}</text>
              <text class="comment-time">{{ formatTime(comment.createdAt) }}</text>
            </view>
            <text class="comment-content">{{ comment.content }}</text>
            <view class="comment-actions">
              <text class="reply-btn" @click="startReply(comment)">回复</text>
              <text class="delete-btn" v-if="isMyComment(comment)" @click="handleDeleteComment(comment.id)">删除</text>
            </view>
            <!-- 回复列表 -->
            <view class="replies" v-if="comment.replies && comment.replies.length > 0">
              <view class="reply-item" v-for="reply in comment.replies" :key="reply.id">
                <text class="reply-user">{{ reply.username || '匿名用户' }}</text>
                <text class="reply-content">{{ reply.content }}</text>
                <text class="reply-time">{{ formatTime(reply.createdAt) }}</text>
                <text class="delete-btn" v-if="isMyComment(reply)" @click="handleDeleteComment(reply.id)">删除</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <!-- 空评论 -->
      <view class="comment-empty" v-else>
        <image class="empty-image" src="/static/images/empty-state.png" mode="aspectFit"></image>
        <text class="empty-text">暂无评论，来发表第一条评论吧~</text>
      </view>

      <!-- 评论输入框 -->
      <view class="comment-input-box">
        <view class="reply-hint" v-if="replyingTo">
          <text>回复 @{{ replyingTo.username || '用户' }}:</text>
          <text class="cancel-reply" @click="cancelReply">取消</text>
        </view>
        <view class="input-row">
          <input 
            class="comment-input" 
            v-model="commentText"
            :placeholder="replyingTo ? '输入回复...' : '写下你的评论...'"
            @confirm="handleAddComment"
            confirm-type="send"
          />
          <button class="send-btn" @click="handleAddComment" :disabled="!commentText.trim()">发送</button>
        </view>
      </view>
    </view>
    
    <!-- 加载状态 -->
    <view class="loading" v-if="!resource">
      <text>加载中...</text>
    </view>
    
    <!-- 底部操作栏 -->
    <view class="bottom-bar" v-if="resource">
      <view class="action-item" @click="handleToggleLike">
        <text class="action-icon" :class="{active: isLiked}">{{ isLiked ? '❤️' : '🤍' }}</text>
        <text class="action-text" :class="{active: isLiked}">{{ resource.likeCount || 0 }}</text>
      </view>
      <view class="action-item" @click="handleToggleFavorite">
        <text class="action-icon" :class="{active: isFavorited}">{{ isFavorited ? '⭐' : '☆' }}</text>
        <text class="action-text" :class="{active: isFavorited}">{{ isFavorited ? '已收藏' : '收藏' }}</text>
      </view>
      <view class="action-item" @click="focusComment">
        <text class="action-icon">💬</text>
        <text class="action-text">评论</text>
      </view>
      <button class="study-btn" @click="markCompleted">
        <text>{{ studyRecord && studyRecord.completed ? '✓ 已完成' : '标记完成' }}</text>
      </button>
    </view>
  </view>
</template>

<script>
import { getResourceDetail, recordStudy, toggleLike, toggleFavorite, getComments, addComment, deleteComment } from '@/api/resource'
import { getApiBaseUrl } from '@/utils/request'

const DEFAULT_COVER = '/static/images/resource-default-cover.png'

export default {
  data() {
    return {
      resource: null,
      isLiked: false,
      isFavorited: false,
      studyRecord: null,
      progressStyle: '',
      comments: [],
      commentText: '',
      replyingTo: null,
      startTime: 0,
      currentUserId: null
    }
  },
  
  computed: {
  },
  
  onLoad(options) {
    this.startTime = Date.now()
    // 获取当前用户ID
    const userInfo = uni.getStorageSync('userInfo')
    if (userInfo) {
      this.currentUserId = userInfo.id
    }
    this.loadResource(options.id)
    this.loadComments(options.id)
  },
  
  onUnload() {
    this.recordStudyProgress()
  },
  
  methods: {
    // 加载资源详情
    async loadResource(id) {
      try {
        const data = await getResourceDetail(id)
        this.resource = data.resource
        this.isLiked = data.isLiked || false
        this.isFavorited = data.isFavorited || false
        this.studyRecord = data.studyRecord || null
        if (this.studyRecord && this.studyRecord.progress != null) {
          this.progressStyle = 'width:' + this.studyRecord.progress + '%;'
        }
      } catch (error) {
        console.error('加载资源失败:', error)
        uni.showToast({ title: '加载失败', icon: 'none' })
      }
    },

    // 加载评论
    async loadComments(resourceId) {
      try {
        const data = await getComments(resourceId)
        this.comments = data || []
      } catch (error) {
        console.error('加载评论失败:', error)
      }
    },
    
    // 记录学习进度
    async recordStudyProgress() {
      if (!this.resource || !uni.getStorageSync('token')) return
      const duration = Math.floor((Date.now() - this.startTime) / 1000)
      try {
        await recordStudy(this.resource.id, {
          progress: this.calculateProgress(duration),
          duration: duration
        })
      } catch (error) {
        console.error('记录学习进度失败:', error)
      }
    },
    
    calculateProgress(duration) {
      return Math.min(100, Math.floor(duration / 60) * 10)
    },
    
    // 标记完成
    async markCompleted() {
      if (!this.checkLogin()) return
      try {
        await recordStudy(this.resource.id, {
          progress: 100,
          duration: Math.floor((Date.now() - this.startTime) / 1000)
        })
        this.studyRecord = { progress: 100, completed: 1, studyDuration: Math.floor((Date.now() - this.startTime) / 1000) }
        uni.showToast({ title: '恭喜完成学习！', icon: 'success' })
      } catch (error) {
        console.error('标记完成失败:', error)
      }
    },
    
    // 点赞/取消点赞
    async handleToggleLike() {
      if (!this.checkLogin()) return
      try {
        const res = await toggleLike(this.resource.id)
        this.isLiked = res.liked
        this.resource.likeCount = res.likeCount
        uni.showToast({ title: this.isLiked ? '已点赞' : '已取消点赞', icon: 'none' })
      } catch (error) {
        console.error('点赞操作失败:', error)
      }
    },

    // 收藏/取消收藏
    async handleToggleFavorite() {
      if (!this.checkLogin()) return
      try {
        const res = await toggleFavorite(this.resource.id)
        this.isFavorited = res.favorited
        this.resource.favoriteCount = res.favoriteCount
        uni.showToast({ title: this.isFavorited ? '已收藏' : '已取消收藏', icon: 'none' })
      } catch (error) {
        console.error('收藏操作失败:', error)
      }
    },

    // 聚焦评论输入框
    focusComment() {
      if (!this.checkLogin()) return
      // 滚动到评论输入框
      uni.pageScrollTo({ selector: '.comment-input-box', duration: 300 })
    },

    // 发表评论
    async handleAddComment() {
      if (!this.checkLogin()) return
      if (!this.commentText.trim()) return
      
      try {
        const data = {
          content: this.commentText.trim(),
          parentId: this.replyingTo ? this.replyingTo.id : null
        }
        const comment = await addComment(this.resource.id, data)
        
        if (this.replyingTo) {
          // 回复：添加到对应评论的replies中
          const target = this.comments.find(c => c.id === this.replyingTo.id)
          if (target) {
            if (!target.replies) target.replies = []
            target.replies.push(comment)
          }
          this.replyingTo = null
        } else {
          // 顶级评论
          this.comments.unshift(comment)
        }
        
        this.resource.commentCount = (this.resource.commentCount || 0) + 1
        this.commentText = ''
        uni.showToast({ title: '评论成功', icon: 'success' })
      } catch (error) {
        console.error('评论失败:', error)
      }
    },

    // 开始回复
    startReply(comment) {
      if (!this.checkLogin()) return
      this.replyingTo = comment
      this.commentText = ''
    },

    // 取消回复
    cancelReply() {
      this.replyingTo = null
      this.commentText = ''
    },

    // 删除评论
    async handleDeleteComment(commentId) {
      try {
        await deleteComment(commentId)
        // 从列表中移除
        this.comments = this.comments.filter(c => c.id !== commentId)
        this.comments.forEach(c => {
          if (c.replies) {
            c.replies = c.replies.filter(r => r.id !== commentId)
          }
        })
        this.resource.commentCount = Math.max(0, (this.resource.commentCount || 1) - 1)
        uni.showToast({ title: '已删除', icon: 'none' })
      } catch (error) {
        console.error('删除评论失败:', error)
      }
    },

    // 判断评论是否属于当前用户
    isMyComment(comment) {
      return this.currentUserId && comment.userId === this.currentUserId
    },

    // 检查登录
    checkLogin() {
      const token = uni.getStorageSync('token')
      if (!token) {
        uni.showToast({ title: '请先登录', icon: 'none' })
        setTimeout(() => {
          uni.navigateTo({ url: '/pages/login/login' })
        }, 1000)
        return false
      }
      return true
    },

    resolveCoverUrl(url) {
      if (!url) return DEFAULT_COVER
      return this.resolveUrl(url)
    },

    // 解析URL
    resolveUrl(url) {
      if (!url) return ''
      if (url.startsWith('http') || url.startsWith('https') || url.startsWith('data:')) {
        return url
      }
      if (url.startsWith('/')) {
        return getApiBaseUrl() + url
      }
      return getApiBaseUrl() + '/' + url
    },

    // 视频开始播放
    onVideoPlay() {
      console.log('视频开始播放')
    },

    // 视频播放结束
    onVideoEnded() {
      // 视频看完自动标记完成
      if (this.resource.type === 2) {
        this.markCompleted()
      }
    },

    // 格式化文章内容（处理可能存在的HTML标签问题）
    formatArticleContent(content) {
      if (!content) return ''
      // 确保内容适合 rich-text 渲染
      return content.replace(/\n/g, '<br/>')
    },
    
    formatDuration(seconds) {
      if (!seconds) return '0秒'
      const minutes = Math.floor(seconds / 60)
      const hours = Math.floor(minutes / 60)
      if (hours > 0) return `${hours}小时${minutes % 60}分钟`
      if (minutes > 0) return `${minutes}分钟`
      return `${seconds}秒`
    },
    
    formatPercent(val) {
      if (val === null || val === undefined) return '0%'
      return Number(val).toFixed(0) + '%'
    },
    
    formatTime(dateStr) {
      if (!dateStr) return ''
      const date = new Date(dateStr)
      const now = new Date()
      const diff = now - date
      if (diff < 60000) return '刚刚'
      if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
      if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
      return date.toLocaleDateString()
    },
    
    getTypeName(type) {
      return { 1: '文章', 2: '视频', 3: '音频', 4: '课程' }[type] || '其他'
    },
    
    getDifficultyName(difficulty) {
      return { 1: '入门', 2: '进阶', 3: '高级' }[difficulty] || ''
    }
  }
}
</script>

<style lang="scss" scoped>
.container {
  padding: $spacing-md;
  padding-bottom: 200rpx;
}

.resource-content {
  @extend %card;
  padding: 50rpx 40rpx;
  
  .title {
    display: block;
    font-size: 44rpx;
    font-weight: 700;
    color: $text-primary;
    margin-bottom: $spacing-lg;
    line-height: 1.4;
  }
  
  .meta-info {
    margin-bottom: $spacing-lg;
    
    .meta-row {
      display: flex;
      gap: 16rpx;
      align-items: center;
      margin-bottom: $spacing-sm;
      flex-wrap: wrap;
      
      .type-badge {
        font-size: $font-xs;
        padding: 6rpx 20rpx;
        border-radius: $radius-sm;
        font-weight: 500;
        
        &.type-1 { color: #1890ff; background: #e6f7ff; }
        &.type-2 { color: #722ed1; background: #f9f0ff; }
        &.type-3 { color: #13c2c2; background: #e6fffb; }
        &.type-4 { color: #52c41a; background: #f6ffed; }
      }
      
      .difficulty {
        font-size: $font-xs;
        color: $warning-color;
        background: #FFF9E6;
        padding: 6rpx 20rpx;
        border-radius: $radius-sm;
      }
      
      .views, .likes, .comments-count {
        font-size: $font-xs;
        color: $text-tertiary;
      }
    }
    
    .tags {
      display: flex;
      gap: 12rpx;
      flex-wrap: wrap;
      margin-bottom: $spacing-sm;
      
      .tag {
        font-size: $font-xs;
        color: $primary-color;
        background: $primary-light;
        padding: 6rpx 20rpx;
        border-radius: $radius-sm;
      }
    }
    
    .author-info {
      display: flex;
      gap: 40rpx;
      font-size: $font-xs;
      color: $text-tertiary;
      margin-top: 10rpx;
    }
  }

  .description-box {
    background: $bg-color;
    border-radius: $radius-md;
    padding: 30rpx;
    margin-bottom: $spacing-lg;
    border-left: 8rpx solid $primary-color;
    box-shadow: $shadow-sm;

    .description-text {
      font-size: $font-md;
      color: $text-secondary;
      line-height: 1.7;
    }
  }
  
  .video-section {
    margin: $spacing-lg 0;
    
    .video-player {
      .video {
        width: 100%;
        height: 420rpx;
        border-radius: $radius-md;
        background: #000;
        box-shadow: $shadow-md;
      }
    }
    
    .media-info {
      margin-top: 20rpx;
      .duration {
        font-size: $font-xs;
        color: $text-tertiary;
      }
    }
  }

  .video-placeholder {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 350rpx;
    background: $bg-color;
    border-radius: $radius-md;
    border: 2rpx dashed $border-color;
    
    .placeholder-icon {
      font-size: 100rpx;
      margin-bottom: 20rpx;
    }
    
    .placeholder-text {
      font-size: $font-md;
      color: $primary-color;
      margin-bottom: 10rpx;
      font-weight: 500;
    }
    
    .placeholder-hint {
      font-size: $font-xs;
      color: $text-tertiary;
    }
  }
  
  .audio-section {
    margin: $spacing-lg 0;
    
    .audio-player {
      .audio-card {
        display: flex;
        align-items: center;
        padding: 30rpx;
        background: $bg-color;
        border-radius: $radius-md;
        gap: 24rpx;
        box-shadow: $shadow-sm;
        
        .audio-cover {
          width: 120rpx;
          height: 120rpx;
          border-radius: $radius-sm;
          box-shadow: $shadow-sm;
        }
        
        .audio-cover-placeholder {
          width: 120rpx;
          height: 120rpx;
          border-radius: $radius-sm;
          background: $primary-gradient;
          display: flex;
          align-items: center;
          justify-content: center;
          
          .audio-icon {
            font-size: 60rpx;
          }
        }
        
        .audio-info {
          flex: 1;
          
          .audio-title {
            display: block;
            font-size: $font-md;
            font-weight: 600;
            color: $text-primary;
            margin-bottom: 8rpx;
          }
          
          .audio-duration {
            font-size: $font-xs;
            color: $text-tertiary;
          }
        }
      }
    }
  }
  
  .article-section {
    .article-body {
      font-size: $font-md;
      color: $text-primary;
      line-height: 1.8;
      letter-spacing: 0.5rpx;
      
      ::v-deep p {
        margin-bottom: 30rpx;
      }
      
      ::v-deep h2, ::v-deep h3 {
        font-weight: 700;
        margin: 40rpx 0 20rpx;
        color: $text-primary;
      }
      
      ::v-deep h2 { font-size: $font-xl; }
      ::v-deep h3 { font-size: $font-lg; }
      
      ::v-deep img {
        max-width: 100%;
        height: auto;
        border-radius: $radius-md;
        margin: 30rpx 0;
        box-shadow: $shadow-sm;
      }

      ::v-deep ul, ::v-deep ol {
        padding-left: 40rpx;
        margin-bottom: 30rpx;
      }

      ::v-deep strong {
        color: $primary-color;
        font-weight: 700;
      }
    }
  }

  .course-section {
    .video-player {
      margin-bottom: $spacing-lg;
      .video {
        width: 100%;
        height: 420rpx;
        border-radius: $radius-md;
        background: #000;
        box-shadow: $shadow-md;
      }
    }
  }
  
  .study-progress {
    margin-top: 60rpx;
    padding: 40rpx;
    background: $bg-color;
    border-radius: $radius-md;
    box-shadow: $shadow-sm;
    
    .progress-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20rpx;
      
      .progress-title {
        font-size: $font-md;
        font-weight: 700;
        color: $text-primary;
      }
      
      .progress-percent {
        font-size: $font-md;
        font-weight: 700;
        color: $primary-color;
      }
    }
    
    .progress-bar {
      height: 16rpx;
      background: $border-color;
      border-radius: $radius-xl;
      overflow: hidden;
      margin-bottom: 16rpx;
      
      .progress-fill {
        height: 100%;
        background: $primary-gradient;
        border-radius: $radius-xl;
        transition: width 0.3s;
      }
    }
    
    .study-duration {
      font-size: $font-xs;
      color: $text-tertiary;
    }
  }
}

/* ========== 评论区域 ========== */
.comment-section {
  @extend %card;
  padding: 40rpx;

  .comment-header {
    margin-bottom: 40rpx;
    
    .comment-title {
      @extend %section-title;
      margin-bottom: 0;
    }
  }

  .comment-list {
    .comment-item {
      display: flex;
      gap: 20rpx;
      margin-bottom: 40rpx;
      padding-bottom: 30rpx;
      border-bottom: 1rpx solid $border-color;

      &:last-child {
        border-bottom: none;
      }

      .comment-avatar {
        width: 80rpx;
        height: 80rpx;
        border-radius: $radius-round;
        background: $primary-gradient;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
        box-shadow: $shadow-sm;

        .avatar-text {
          font-size: $font-lg;
          color: #fff;
          font-weight: 700;
        }
      }

      .comment-body {
        flex: 1;
        min-width: 0;

        .comment-user {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 12rpx;

          .comment-username {
            font-size: $font-sm;
            font-weight: 600;
            color: $text-primary;
          }

          .comment-time {
            font-size: $font-xs;
            color: $text-tertiary;
          }
        }

        .comment-content {
          display: block;
          font-size: $font-md;
          color: $text-secondary;
          line-height: 1.6;
          margin-bottom: 16rpx;
        }

        .comment-actions {
          display: flex;
          gap: 30rpx;

          .reply-btn {
            font-size: $font-xs;
            color: $primary-color;
            font-weight: 500;
          }

          .delete-btn {
            font-size: $font-xs;
            color: $error-color;
            font-weight: 500;
          }
        }

        .replies {
          margin-top: 20rpx;
          padding: 24rpx;
          background: $bg-color;
          border-radius: $radius-md;

          .reply-item {
            padding: 16rpx 0;
            border-bottom: 1rpx solid rgba(0,0,0,0.03);

            &:last-child {
              border-bottom: none;
            }

            .reply-user {
              font-size: $font-sm;
              color: $primary-color;
              font-weight: 600;
              margin-right: 12rpx;
            }

            .reply-content {
              font-size: $font-sm;
              color: $text-secondary;
            }

            .reply-time {
              display: block;
              font-size: $font-xs;
              color: $text-tertiary;
              margin-top: 8rpx;
            }

            .delete-btn {
              font-size: $font-xs;
              color: $error-color;
              margin-left: 20rpx;
            }
          }
        }
      }
    }
  }

  .comment-empty {
    text-align: center;
    padding: 60rpx 0;

    .empty-image {
      width: 220rpx;
      height: 170rpx;
      margin-bottom: $spacing-sm;
    }
    
    .empty-text {
      display: block;
      font-size: $font-sm;
      color: $text-tertiary;
    }
  }

  .comment-input-box {
    margin-top: 40rpx;
    padding-top: 30rpx;
    border-top: 1rpx solid $border-color;

    .reply-hint {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20rpx;
      padding: 16rpx 24rpx;
      background: $primary-light;
      border-radius: $radius-sm;
      font-size: $font-sm;
      color: $primary-color;

      .cancel-reply {
        color: $text-tertiary;
      }
    }

    .input-row {
      display: flex;
      gap: 20rpx;
      align-items: center;

      .comment-input {
        flex: 1;
        height: 84rpx;
        padding: 0 32rpx;
        background: $bg-color;
        border-radius: $radius-xl;
        font-size: $font-md;
        box-shadow: inset $shadow-sm;
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

        &[disabled] {
          opacity: 0.5;
          box-shadow: none;
        }
      }
    }
  }
}

.loading {
  text-align: center;
  padding: 100rpx 0;
  color: $text-tertiary;
  font-size: $font-md;
}

/* ========== 底部操作栏 ========== */
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  padding: 20rpx 40rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  box-shadow: 0 -10rpx 30rpx rgba(0, 0, 0, 0.05);
  z-index: 100;

  .action-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6rpx;
    padding: 0 24rpx;

    .action-icon {
      font-size: 44rpx;
      transition: transform 0.2s;

      &.active {
        transform: scale(1.2);
        color: $primary-color;
      }
    }

    .action-text {
      font-size: $font-xs;
      color: $text-tertiary;
      font-weight: 500;

      &.active {
        color: $primary-color;
      }
    }
  }

  .study-btn {
    flex: 1;
    height: 90rpx;
    line-height: 90rpx;
    margin-left: 24rpx;
    @extend %btn-primary;
    border-radius: $radius-xl;
    font-size: $font-md;
    padding: 0;
  }
}
</style>
