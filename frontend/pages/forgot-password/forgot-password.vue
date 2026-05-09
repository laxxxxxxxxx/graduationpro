<template>
  <view class="forgot-password-page">
    <view class="header">
      <text class="title">找回密码</text>
      <text class="subtitle">通过邮箱重置您的密码</text>
    </view>
    
    <view class="form-section card">
      <view class="tips">
        <text class="tips-icon">💡</text>
        <text class="tips-text">请输入您注册时使用的邮箱地址，我们将发送密码重置链接到您的邮箱。</text>
      </view>
      
      <view class="form-item">
        <view class="label">邮箱地址</view>
        <view class="input-wrapper">
          <text class="icon">📧</text>
          <input 
            v-model="email" 
            type="email"
            placeholder="请输入注册邮箱" 
            class="input"
          />
        </view>
      </view>
      
      <button 
        @click="handleSendResetLink" 
        :loading="loading"
        class="submit-btn"
      >
        发送重置链接
      </button>
      
      <view class="actions">
        <text class="link" @click="navigateTo('/pages/login/login')">返回登录</text>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      email: '',
      loading: false
    }
  },
  
  methods: {
    validate() {
      if (!this.email) {
        uni.showToast({ title: '请输入邮箱地址', icon: 'none' })
        return false
      }
      
      const emailReg = /^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/
      if (!emailReg.test(this.email)) {
        uni.showToast({ title: '邮箱格式不正确', icon: 'none' })
        return false
      }
      
      return true
    },
    
    async handleSendResetLink() {
      if (!this.validate()) return
      
      this.loading = true
      try {
        // TODO: 调用后端发送重置邮件接口
        // await sendResetEmail(this.email)
        
        // 模拟成功
        uni.showToast({ 
          title: '重置链接已发送', 
          icon: 'success' 
        })
        
        setTimeout(() => {
          uni.navigateTo({
            url: '/pages/login/login'
          })
        }, 2000)
      } catch (error) {
        console.error('发送失败', error)
      } finally {
        this.loading = false
      }
    },
    
    navigateTo(url) {
      uni.navigateTo({ url })
    }
  }
}
</script>

<style lang="scss" scoped>
.forgot-password-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40rpx 30rpx;
}

.header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 60rpx;
  
  .title {
    font-size: 44rpx;
    font-weight: bold;
    color: #fff;
    margin-bottom: 10rpx;
  }
  
  .subtitle {
    font-size: 26rpx;
    color: rgba(255, 255, 255, 0.9);
  }
}

.form-section {
  background: #fff;
  border-radius: 24rpx;
  padding: 40rpx 30rpx;
  
  .tips {
    display: flex;
    align-items: flex-start;
    background: #f0f7ff;
    border-left: 4rpx solid #667eea;
    padding: 20rpx;
    border-radius: 8rpx;
    margin-bottom: 30rpx;
    
    .tips-icon {
      font-size: 32rpx;
      margin-right: 10rpx;
      flex-shrink: 0;
    }
    
    .tips-text {
      font-size: 24rpx;
      color: #666;
      line-height: 1.6;
    }
  }
  
  .form-item {
    margin-bottom: 30rpx;
    
    .label {
      font-size: 28rpx;
      color: #333;
      margin-bottom: 12rpx;
      font-weight: 500;
    }
    
    .input-wrapper {
      display: flex;
      align-items: center;
      background: #f5f5f5;
      border-radius: 12rpx;
      padding: 20rpx 24rpx;
      
      .icon {
        font-size: 36rpx;
        margin-right: 15rpx;
      }
      
      .input {
        flex: 1;
        font-size: 28rpx;
      }
    }
  }
  
  .submit-btn {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #fff;
    border-radius: 12rpx;
    font-size: 32rpx;
    padding: 24rpx 0;
    margin-top: 20rpx;
  }
  
  .actions {
    display: flex;
    justify-content: center;
    margin-top: 30rpx;
    
    .link {
      font-size: 26rpx;
      color: #667eea;
    }
  }
}
</style>
