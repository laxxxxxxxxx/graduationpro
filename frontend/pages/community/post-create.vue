<template>
  <view class="container">
    <view class="form">
      <view class="form-item">
        <text class="label">标题</text>
        <input class="input" v-model="form.title" placeholder="请输入标题" />
      </view>
      
      <view class="form-item">
        <text class="label">分类</text>
        <picker mode="selector" :range="categories" range-key="name" @change="onCategoryChange">
          <view class="picker">{{ selectedCategory || '请选择分类' }}</view>
        </picker>
      </view>
      
      <view class="form-item">
        <text class="label">内容</text>
        <textarea class="textarea" v-model="form.content" placeholder="分享你的心情或困惑..." maxlength="500" />
      </view>
    </view>
    
    <button class="submit-btn" @click="submit">{{ isEditMode ? '保存修改' : '发布' }}</button>
  </view>
</template>

<script>
import { createPost, updatePost, getPostDetail, getCategories } from '@/api/community'

export default {
  data() {
    return {
      isEditMode: false,
      editPostId: null,
      form: {
        title: '',
        content: '',
        categoryId: null
      },
      categories: [],
      selectedCategory: ''
    }
  },
  
  onLoad(options) {
    this.loadCategories()
    if (options.mode === 'edit' && options.id) {
      this.isEditMode = true
      this.editPostId = options.id
      this.loadPostData(options.id)
      uni.setNavigationBarTitle({ title: '编辑帖子' })
    }
  },
  
  methods: {
    async loadCategories() {
      try {
        const data = await getCategories()
        this.categories = data || []
      } catch (error) {
        console.error('加载分类失败:', error)
        // 降级使用默认分类
        this.categories = [
          { id: 1, name: '情感倾诉' },
          { id: 2, name: '学业压力' },
          { id: 3, name: '人际关系' },
          { id: 4, name: '职业规划' }
        ]
      }
    },
    
    async loadPostData(postId) {
      try {
        const data = await getPostDetail(postId)
        const post = data.post
        if (post) {
          this.form.title = post.title || ''
          this.form.content = post.content || ''
          this.form.categoryId = post.categoryId || null
          
          // 设置选中的分类名称
          if (post.categoryId) {
            const cat = this.categories.find(c => c.id === post.categoryId)
            if (cat) {
              this.selectedCategory = cat.name
            }
          }
        }
      } catch (error) {
        console.error('加载帖子数据失败:', error)
      }
    },
    
    onCategoryChange(e) {
      const index = e.detail.value
      this.selectedCategory = this.categories[index].name
      this.form.categoryId = this.categories[index].id
    },
    
    async submit() {
      if (!this.form.title || !this.form.content) {
        uni.showToast({ title: '请填写完整', icon: 'none' })
        return
      }
      
      if (!this.form.categoryId) {
        uni.showToast({ title: '请选择分类', icon: 'none' })
        return
      }
      
      try {
        const loadingTitle = this.isEditMode ? '保存中...' : '发布中...'
        uni.showLoading({ title: loadingTitle })
        
        if (this.isEditMode) {
          await updatePost(this.editPostId, this.form)
        } else {
          await createPost(this.form)
        }
        
        uni.hideLoading()
        uni.showToast({ 
          title: this.isEditMode ? '编辑成功' : '发布成功', 
          icon: 'success' 
        })
        
        setTimeout(() => {
          uni.navigateBack()
        }, 1500)
      } catch (error) {
        uni.hideLoading()
        console.error('提交失败:', error)
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

.form {
  @extend %card;
  padding: 40rpx;
  margin-bottom: $spacing-lg;
  
  .form-item {
    margin-bottom: $spacing-lg;
    
    &:last-child {
      margin-bottom: 0;
    }
    
    .label {
      display: block;
      font-size: $font-sm;
      color: $text-secondary;
      margin-bottom: 16rpx;
      font-weight: 500;
      padding-left: 8rpx;
    }
    
    .input, .textarea, .picker {
      width: 100%;
      padding: 24rpx 30rpx;
      background: $bg-color;
      border: 1rpx solid transparent;
      border-radius: $radius-md;
      font-size: $font-md;
      color: $text-primary;
      transition: all 0.3s;
      box-sizing: border-box;
      
      &:focus {
        background: #fff;
        border-color: $primary-color;
        box-shadow: $shadow-sm;
      }
    }
    
    .textarea {
      height: 400rpx;
      line-height: 1.6;
    }
    
    .picker {
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      &::after {
        content: '›';
        font-size: 40rpx;
        color: $text-tertiary;
      }
    }
  }
}

.submit-btn {
  @extend %btn-primary;
  width: 100%;
  height: 100rpx;
  font-size: $font-lg;
  margin-top: $spacing-xl;
}
</style>
