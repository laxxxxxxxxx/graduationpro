<template>
  <view class="login-page">
    <view class="logo-section">
      <view class="logo">🧠</view>
      <text class="app-name">心理健康平台</text>
      <text class="slogan">关注心理健康,拥抱美好生活</text>
    </view>
    
    <view class="form-section card">
      <view class="form-item">
        <view class="input-wrapper">
          <text class="icon">👤</text>
          <input 
            v-model="form.username" 
            placeholder="请输入用户名" 
            class="input"
          />
        </view>
      </view>
      
      <view class="form-item">
        <view class="input-wrapper">
          <text class="icon">🔒</text>
          <input 
            v-model="form.password" 
            type="password"
            placeholder="请输入密码" 
            class="input"
          />
        </view>
      </view>
      
      <button 
        @click="handleLogin" 
        :loading="loading"
        class="login-btn"
      >
        登录
      </button>
      
      <view class="actions">
        <text class="link" @click="navigateTo('/pages/register/register')">注册账号</text>
        <text class="link" @click="navigateTo('/pages/forgot-password/forgot-password')">忘记密码?</text>
      </view>
    </view>
  </view>
</template>

<script>
import { login } from '@/api/auth'

export default {
  data() {
    return {
      form: {
        username: '',
        password: ''
      },
      loading: false
    }
  },
  
  methods: {
    validate() {
      // 验证用户名
      if (!this.form.username) {
        uni.showToast({ title: '请输入用户名', icon: 'none' })
        return false
      }
      
      // 验证密码
      if (!this.form.password) {
        uni.showToast({ title: '请输入密码', icon: 'none' })
        return false
      }
      if (this.form.password.length < 6 || this.form.password.length > 20) {
        uni.showToast({ title: '密码长度为6-20位', icon: 'none' })
        return false
      }
      
      return true
    },
    
    async handleLogin() {
      if (!this.validate()) return
      
      this.loading = true
      try {
        const res = await login(this.form)
        
        // 保存token和用户信息
        uni.setStorageSync('token', res.token)
        uni.setStorageSync('userInfo', res.userInfo)
        
        uni.showToast({ 
          title: '登录成功', 
          icon: 'success' 
        })
        
        setTimeout(() => {
          uni.switchTab({
            url: '/pages/index/index'
          })
        }, 1500)
      } catch (error) {
        console.error('登录失败', error)
        // 错误提示已由request.js统一处理
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
.login-page {
  min-height: 100vh;
  background: $primary-gradient;
  padding: 100rpx 40rpx;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.logo-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 100rpx;
  
  .logo {
    font-size: 140rpx;
    margin-bottom: 30rpx;
    filter: drop-shadow(0 10rpx 20rpx rgba(0,0,0,0.1));
  }
  
  .app-name {
    font-size: 48rpx;
    font-weight: 700;
    color: #fff;
    margin-bottom: 16rpx;
    letter-spacing: 2rpx;
  }
  
  .slogan {
    font-size: $font-sm;
    color: rgba(255, 255, 255, 0.95);
  }
}

.form-section {
  padding: 60rpx 40rpx;
  box-shadow: $shadow-lg;
  
  .form-item {
    margin-bottom: $spacing-lg;
    
    .input-wrapper {
      display: flex;
      align-items: center;
      background: $bg-color;
      border-radius: $radius-md;
      padding: 24rpx 30rpx;
      transition: all 0.3s;
      border: 1rpx solid transparent;
      
      &:focus-within {
        border-color: $primary-color;
        background: #fff;
        box-shadow: $shadow-sm;
      }
      
      .icon {
        font-size: $font-lg;
        margin-right: $spacing-md;
      }
      
      .input {
        flex: 1;
        font-size: $font-md;
        color: $text-primary;
      }
    }
  }
  
  .login-btn {
    @extend %btn-primary;
    height: 100rpx;
    font-size: $font-lg;
    margin-top: $spacing-lg;
    width: 100%;
  }
  
  .actions {
    display: flex;
    justify-content: space-between;
    margin-top: $spacing-xl;
    
    .link {
      font-size: $font-sm;
      color: $primary-color;
      font-weight: 500;
      
      &:active {
        opacity: 0.7;
      }
    }
  }
}
</style>
