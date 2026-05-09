<template>
  <view class="register-page">
    <view class="header">
      <text class="title">注册账号</text>
      <text class="subtitle">创建您的心理健康平台账户</text>
    </view>
    
    <view class="form-section card">
      <view class="form-item">
        <view class="label">用户名</view>
        <view class="input-wrapper">
          <text class="icon">👤</text>
          <input 
            v-model="form.username" 
            placeholder="请输入用户名(4-20位字母数字)" 
            class="input"
            maxlength="20"
          />
        </view>
      </view>
      
      <view class="form-item">
        <view class="label">密码</view>
        <view class="input-wrapper">
          <text class="icon">🔒</text>
          <input 
            v-model="form.password" 
            type="password"
            placeholder="请输入密码(6-20位)" 
            class="input"
            maxlength="20"
          />
        </view>
      </view>
      
      <view class="form-item">
        <view class="label">确认密码</view>
        <view class="input-wrapper">
          <text class="icon">🔒</text>
          <input 
            v-model="form.confirmPassword" 
            type="password"
            placeholder="请再次输入密码" 
            class="input"
            maxlength="20"
          />
        </view>
      </view>
      
      <view class="form-item">
        <view class="label">邮箱（选填）</view>
        <view class="input-wrapper">
          <text class="icon">📧</text>
          <input 
            v-model="form.email" 
            type="email"
            placeholder="请输入邮箱地址" 
            class="input"
          />
        </view>
      </view>
      
      <view class="form-item">
        <view class="label">手机号（选填）</view>
        <view class="input-wrapper">
          <text class="icon">📱</text>
          <input 
            v-model="form.phone" 
            type="number"
            placeholder="请输入手机号" 
            class="input"
            maxlength="11"
          />
        </view>
      </view>
      
      <view class="form-item">
        <view class="label">学校（选填）</view>
        <view class="input-wrapper">
          <text class="icon">🏫</text>
          <input 
            v-model="form.university" 
            placeholder="请输入学校名称" 
            class="input"
          />
        </view>
      </view>
      
      <button 
        @click="handleRegister" 
        :loading="loading"
        class="register-btn"
      >
        注册
      </button>
      
      <view class="actions">
        <text class="link" @click="navigateTo('/pages/login/login')">已有账号？立即登录</text>
      </view>
    </view>
  </view>
</template>

<script>
import { register } from '@/api/auth'

export default {
  data() {
    return {
      form: {
        username: '',
        password: '',
        confirmPassword: '',
        email: '',
        phone: '',
        university: '',
        major: '',
        grade: ''
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
      if (this.form.username.length < 4 || this.form.username.length > 20) {
        uni.showToast({ title: '用户名长度为4-20位', icon: 'none' })
        return false
      }
      if (!/^[a-zA-Z0-9_]+$/.test(this.form.username)) {
        uni.showToast({ title: '用户名只能包含字母、数字和下划线', icon: 'none' })
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
      
      // 验证确认密码
      if (!this.form.confirmPassword) {
        uni.showToast({ title: '请确认密码', icon: 'none' })
        return false
      }
      if (this.form.password !== this.form.confirmPassword) {
        uni.showToast({ title: '两次输入的密码不一致', icon: 'none' })
        return false
      }
      
      // 验证邮箱格式（如果填写了）
      if (this.form.email) {
        const emailReg = /^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/
        if (!emailReg.test(this.form.email)) {
          uni.showToast({ title: '邮箱格式不正确', icon: 'none' })
          return false
        }
      }
      
      // 验证手机号格式（如果填写了）
      if (this.form.phone) {
        const phoneReg = /^1[3-9]\d{9}$/
        if (!phoneReg.test(this.form.phone)) {
          uni.showToast({ title: '手机号格式不正确', icon: 'none' })
          return false
        }
      }
      
      return true
    },
    
    async handleRegister() {
      if (!this.validate()) return
      
      this.loading = true
      try {
        // 准备注册数据
        const registerData = {
          username: this.form.username,
          password: this.form.password,
          email: this.form.email || null,
          phone: this.form.phone || null,
          university: this.form.university || null,
          major: this.form.major || null,
          grade: this.form.grade || null
        }
        
        await register(registerData)
        
        uni.showToast({ 
          title: '注册成功', 
          icon: 'success' 
        })
        
        setTimeout(() => {
          uni.navigateTo({
            url: '/pages/login/login'
          })
        }, 1500)
      } catch (error) {
        console.error('注册失败', error)
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
.register-page {
  min-height: 100vh;
  background: $primary-gradient;
  padding: 60rpx 40rpx;
}

.header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 60rpx;
  
  .title {
    font-size: 48rpx;
    font-weight: 700;
    color: #fff;
    margin-bottom: 16rpx;
    letter-spacing: 2rpx;
  }
  
  .subtitle {
    font-size: $font-sm;
    color: rgba(255, 255, 255, 0.95);
  }
}

.form-section {
  padding: 50rpx 40rpx;
  box-shadow: $shadow-lg;
  
  .form-item {
    margin-bottom: $spacing-lg;
    
    .label {
      font-size: $font-sm;
      color: $text-secondary;
      margin-bottom: 12rpx;
      font-weight: 500;
      padding-left: 10rpx;
    }
    
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
  
  .register-btn {
    @extend %btn-primary;
    height: 100rpx;
    font-size: $font-lg;
    margin-top: $spacing-xl;
    width: 100%;
  }
  
  .actions {
    display: flex;
    justify-content: center;
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
