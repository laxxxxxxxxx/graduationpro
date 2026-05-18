<template>
  <view class="container">
    <!-- 顶部进度条 -->
    <view class="progress-section">
      <view class="progress-header">
        <text class="progress-text">{{ currentIndex + 1 }}/{{ questions.length }}</text>
        <view class="quit-tag" @click="confirmQuit">
          <text>中途退出</text>
        </view>
      </view>
      <view class="progress-bar">
        <view class="progress-fill" :style="{ width: progress + '%' }"></view>
      </view>
    </view>
    
    <!-- 题目卡片 -->
    <view class="question-card" v-if="currentQuestion">
      <view class="question-header">
        <text class="question-number">第{{ currentIndex + 1 }}题</text>
      </view>
      <text class="question-title">{{ currentQuestion.questionText }}</text>
      
      <!-- 选项列表 -->
      <view class="options-list">
        <view 
          class="option-item" 
          v-for="(option, index) in currentQuestion.options" 
          :key="index"
          :class="{ selected: userAnswers[currentQuestion.id] === option.value }"
          @click="selectOption(option.value)"
        >
          <text class="option-key">{{ getOptionLabel(index) }}</text>
          <text class="option-text">{{ option.text }}</text>
          <view class="check-icon" v-if="userAnswers[currentQuestion.id] === option.value">✓</view>
        </view>
      </view>
    </view>
    
    <!-- 操作按钮 -->
    <view class="footer-btns">
      <button 
        class="nav-btn" 
        @click="prevQuestion" 
        :disabled="currentIndex === 0"
      >上一题</button>
      <button 
        class="nav-btn btn-primary" 
        v-if="currentIndex < questions.length - 1"
        @click="nextQuestion"
        :disabled="currentQuestion && userAnswers[currentQuestion.id] === undefined"
      >下一题</button>
      <button 
        class="nav-btn btn-primary" 
        v-else
        @click="submitTest"
        :disabled="!isAllAnswered"
      >提交测评</button>
    </view>
  </view>
</template>

<script>
import { getScaleQuestions, submitAssessment } from '@/api/assessment'

export default {
  data() {
    return {
      scaleId: null,
      scaleName: '',
      questions: [],
      currentIndex: 0,
      userAnswers: {}, // questionId: optionValue
      startTime: 0,
      loading: false,
      jumpTimer: null // 新增定时器引用
    }
  },
  
  computed: {
    currentQuestion() {
      return this.questions[this.currentIndex] || null
    },
    progress() {
      if (this.questions.length === 0) return 0
      return ((this.currentIndex + 1) / this.questions.length) * 100
    },
    isAllAnswered() {
      if (this.questions.length === 0) return false
      // 确保每一个题目都有答案（排除 undefined）
      return this.questions.every(q => this.userAnswers[q.id] !== undefined)
    }
  },
  
  onLoad(options) {
    this.scaleId = options.id
    this.scaleName = options.name
    uni.setNavigationBarTitle({ title: this.scaleName || '在线测评' })
    this.loadQuestions()
    this.startTime = Date.now()
  },
  
  onUnload() {
    if (this.jumpTimer) {
      clearTimeout(this.jumpTimer)
    }
  },
  
  methods: {
    async loadQuestions() {
      try {
        this.loading = true
        const data = await getScaleQuestions(this.scaleId)
        const questions = data || []
        
        const initialAnswers = {}
        questions.forEach(q => {
          if (typeof q.options === 'string') {
            try {
              q.options = JSON.parse(q.options)
            } catch (e) {
              q.options = []
            }
          }
          // 预先初始化所有题目ID，确保 Vue 能够完美追踪响应式变化
          initialAnswers[q.id] = undefined
        })
        
        this.questions = questions
        this.userAnswers = initialAnswers
      } catch (error) {
        console.error('加载题目失败:', error)
        uni.showToast({ title: '加载题目失败', icon: 'none' })
      } finally {
        this.loading = false
      }
    },
    
    getOptionLabel(index) {
      return String.fromCharCode(65 + index) // A, B, C, D...
    },
    
    selectOption(value) {
      const qid = this.currentQuestion.id
      this.$set(this.userAnswers, qid, value)
      
      // 强制触发对象更新，确保 isAllAnswered 计算属性即时响应
      this.userAnswers = { ...this.userAnswers }
      
      // 增加防抖和延时，让用户看清自己的选择，并防止快速点击导致跳题
      if (this.currentIndex < this.questions.length - 1) {
        if (this.jumpTimer) clearTimeout(this.jumpTimer)
        this.jumpTimer = setTimeout(() => {
          this.currentIndex++
          this.jumpTimer = null
        }, 800) // 增加到800ms，更加平滑
      }
    },
    
    prevQuestion() {
      if (this.jumpTimer) clearTimeout(this.jumpTimer)
      if (this.currentIndex > 0) {
        this.currentIndex--
      }
    },
    
    nextQuestion() {
      if (this.jumpTimer) clearTimeout(this.jumpTimer)
      if (this.currentIndex < this.questions.length - 1) {
        this.currentIndex++
      }
    },
    
    confirmQuit() {
      uni.showModal({
        title: '确认退出',
        content: '退出后当前进度将不会保存，确定退出吗？',
        cancelText: '继续测评',
        confirmText: '确定退出',
        confirmColor: '#FF8C8C',
        success: (res) => {
          if (res.confirm) {
            uni.navigateBack()
          }
        }
      })
    },
    
    async submitTest() {
      if (!this.isAllAnswered) {
        const unansweredCount = this.questions.filter(q => this.userAnswers[q.id] === undefined).length
        uni.showToast({ title: `还有${unansweredCount}道题未完成`, icon: 'none' })
        return
      }
      
      try {
        uni.showLoading({ title: '计算结果中...', mask: true })
        const duration = Math.floor((Date.now() - this.startTime) / 1000)
        
        const answers = this.questions.map(q => ({
          questionId: q.id,
          questionNo: q.questionNo,
          answer: this.userAnswers[q.id]
        }))
        
        const res = await submitAssessment({
          scaleId: parseInt(this.scaleId),
          answers: answers,
          completionTime: duration
        })
        
        uni.hideLoading()
        
        // 核心修复：后端返回的是 assessmentId，之前前端使用的是 res.id 导致跳转失败
        if (res && (res.assessmentId || res.id)) {
          uni.redirectTo({
            url: `/pages/assessment/result?id=${res.assessmentId || res.id}`
          })
        } else {
          throw new Error('未获取到测评ID')
        }
      } catch (error) {
        uni.hideLoading()
        console.error('提交失败:', error)
        // 核心：优先显示后端返回的错误消息，如果没有则显示默认提示
        let errMsg = '提交失败，请稍后重试'
        if (typeof error === 'string') {
          errMsg = error
        } else if (error && error.message) {
          errMsg = error.message
        }
        uni.showToast({ 
          title: errMsg, 
          icon: 'none',
          duration: 3000
        })
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.container {
  padding: $spacing-md;
  background-color: $bg-color;
  min-height: 100vh;
}

.progress-section {
  padding: 40rpx 10rpx;
  
  .progress-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20rpx;
    
    .progress-text {
      font-size: $font-sm;
      color: $text-tertiary;
      font-weight: 600;
    }
    
    .quit-tag {
      font-size: 22rpx;
      color: #999;
      background: #f0f0f0;
      padding: 6rpx 20rpx;
      border-radius: 30rpx;
      &:active {
        opacity: 0.7;
      }
    }
  }
  
  .progress-bar {
    height: 12rpx;
    background: #eef2f5;
    border-radius: 6rpx;
    overflow: hidden;
    margin-bottom: 20rpx;
    
    .progress-fill {
      height: 100%;
      background: $primary-gradient;
      transition: width 0.3s;
    }
  }
}

.question-card {
  background: #ffffff;
  border-radius: 32rpx;
  padding: 50rpx 40rpx;
  margin-bottom: $spacing-lg;
  box-shadow: 0 8rpx 24rpx rgba(255, 140, 140, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.8);
  
  .question-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: $spacing-md;
    
    .question-number {
      font-size: $font-sm;
      color: $primary-color;
      font-weight: 600;
    }
  }
  
  .question-title {
    display: block;
    font-size: $font-lg;
    color: $text-primary;
    font-weight: 600;
    line-height: 1.6;
    margin-bottom: 60rpx;
  }
}

.options-list {
  .option-item {
    display: flex;
    align-items: center;
    padding: 36rpx 40rpx;
    background: $bg-color;
    border-radius: $radius-md;
    margin-bottom: $spacing-md;
    transition: all 0.2s;
    border: 2rpx solid transparent;
    
    &.selected {
      background: $primary-light;
      border-color: $primary-color;
      
      .option-key {
        background: $primary-color;
        color: #fff;
      }
      
      .option-text {
        color: $primary-color;
        font-weight: 600;
      }
    }
    
    .option-key {
      width: 50rpx;
      height: 50rpx;
      line-height: 50rpx;
      text-align: center;
      background: #fff;
      color: $text-secondary;
      border-radius: $radius-round;
      font-size: $font-sm;
      margin-right: 30rpx;
      box-shadow: $shadow-sm;
    }
    
    .option-text {
      flex: 1;
      font-size: $font-md;
      color: $text-primary;
    }
    
    .check-icon {
      color: $primary-color;
      font-weight: bold;
    }
  }
}

.footer-btns {
  display: flex;
  justify-content: space-between;
  gap: $spacing-md;
  margin-top: 60rpx;
  padding: 0 10rpx;
  
  .nav-btn {
    flex: 1;
    height: 100rpx;
    line-height: 100rpx;
    border-radius: $radius-xl;
    font-size: $font-md;
    font-weight: 600;
    background: #fff;
    color: $text-secondary;
    border: 2rpx solid $border-color;
    box-shadow: $shadow-sm;
    
    &:active {
      background: $bg-color;
    }
    
    &[disabled] {
      opacity: 0.3;
      box-shadow: none;
    }
    
    &.btn-primary {
      background: linear-gradient(135deg, #FF9A9E 0%, #FECFEF 100%);
      color: #fff;
      border-radius: 50rpx;
      border: none;
      font-weight: 500;
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 6rpx 16rpx rgba(255, 140, 140, 0.2);
    }
  }
}
</style>
