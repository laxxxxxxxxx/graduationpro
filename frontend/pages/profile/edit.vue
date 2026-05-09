<template>
  <view class="container">
    <view class="header">
      <text class="title">编辑资料</text>
      <text class="subtitle">完善个人信息，获得更精准的推荐</text>
    </view>

    <view class="form">
      <view class="form-item">
        <text class="label">真实姓名</text>
        <input class="input" v-model="form.realName" placeholder="请输入真实姓名" />
      </view>

      <view class="form-row">
        <view class="form-item half">
          <text class="label">性别</text>
          <picker mode="selector" :range="genderOptions" @change="onGenderChange">
            <view class="picker">{{ genderText || '请选择' }}</view>
          </picker>
        </view>
        <view class="form-item half">
          <text class="label">年龄</text>
          <input class="input" type="number" v-model="form.age" placeholder="年龄" />
        </view>
      </view>

      <view class="form-item">
        <text class="label">学校</text>
        <input class="input" v-model="form.university" placeholder="请输入学校名称" />
      </view>

      <view class="form-item">
        <text class="label">专业</text>
        <input class="input" v-model="form.major" placeholder="请输入专业" />
      </view>

      <view class="form-item">
        <text class="label">年级</text>
        <picker mode="selector" :range="gradeOptions" @change="onGradeChange">
          <view class="picker">{{ form.grade || '请选择年级' }}</view>
        </picker>
      </view>

      <view class="form-item">
        <text class="label">邮箱</text>
        <input class="input" v-model="form.email" placeholder="请输入邮箱" />
      </view>

      <view class="form-item">
        <text class="label">手机号</text>
        <input class="input" type="number" v-model="form.phone" placeholder="请输入手机号" maxlength="11" />
      </view>
    </view>

    <button class="submit-btn" @click="handleSave">保存</button>
  </view>
</template>

<script>
import { getUserInfo, updateUserInfo } from '@/api/auth'

export default {
  data() {
    return {
      form: {
        realName: '',
        gender: null,
        age: null,
        university: '',
        major: '',
        grade: '',
        email: '',
        phone: ''
      },
      genderOptions: ['女', '男', '其他'],
      genderText: '',
      gradeOptions: ['大一', '大二', '大三', '大四', '研一', '研二', '研三', '博士']
    }
  },

  onLoad() {
    this.loadUserInfo()
  },

  methods: {
    async loadUserInfo() {
      try {
        const data = await getUserInfo()
        if (data) {
          this.form.realName = data.realName || ''
          this.form.gender = data.gender
          this.form.age = data.age
          this.form.university = data.university || ''
          this.form.major = data.major || ''
          this.form.grade = data.grade || ''
          this.form.email = data.email || ''
          this.form.phone = data.phone || ''

          if (data.gender === 0) this.genderText = '女'
          else if (data.gender === 1) this.genderText = '男'
          else if (data.gender === 2) this.genderText = '其他'
        }
      } catch (error) {
        console.error('加载用户信息失败:', error)
      }
    },

    onGenderChange(e) {
      const index = e.detail.value
      this.form.gender = index
      this.genderText = this.genderOptions[index]
    },

    onGradeChange(e) {
      this.form.grade = this.gradeOptions[e.detail.value]
    },

    async handleSave() {
      try {
        // 清除空字符串
        const submitData = {}
        for (const key in this.form) {
          if (this.form[key] !== null && this.form[key] !== undefined && this.form[key] !== '') {
            submitData[key] = this.form[key]
          }
        }

        uni.showLoading({ title: '保存中...' })
        await updateUserInfo(submitData)
        uni.hideLoading()
        
        uni.showToast({ title: '保存成功', icon: 'success' })
        setTimeout(() => {
          uni.navigateBack()
        }, 1500)
      } catch (error) {
        uni.hideLoading()
        console.error('保存失败:', error)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.container {
  padding: 30rpx;
  background: #f5f7fa;
  min-height: 100vh;
}

.header {
  text-align: center;
  margin-bottom: 40rpx;

  .title {
    display: block;
    font-size: 40rpx;
    font-weight: bold;
    color: #333;
    margin-bottom: 10rpx;
  }

  .subtitle {
    font-size: 26rpx;
    color: #999;
  }
}

.form {
  background: #fff;
  border-radius: 20rpx;
  padding: 30rpx;
  margin-bottom: 40rpx;

  .form-item {
    margin-bottom: 30rpx;

    &.half {
      flex: 1;
      margin-bottom: 0;
    }

    .label {
      display: block;
      font-size: 28rpx;
      color: #333;
      margin-bottom: 14rpx;
      font-weight: 500;
    }

    .input, .picker {
      width: 100%;
      padding: 20rpx;
      border: 2rpx solid #e8e8e8;
      border-radius: 12rpx;
      font-size: 28rpx;
      background: #fafafa;
    }

    .picker {
      color: #333;
    }
  }

  .form-row {
    display: flex;
    gap: 20rpx;
    margin-bottom: 30rpx;
  }
}

.submit-btn {
  width: 100%;
  height: 90rpx;
  line-height: 90rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-radius: 45rpx;
  font-size: 32rpx;
  font-weight: bold;
}
</style>
