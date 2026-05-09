<template>
  <view class="container">
    <view class="progress-bar">
      <view class="progress" :style="progressStyle"></view>
      <text class="progress-text">{{ currentIndex + 1 }}/{{ questions.length }}</text>
    </view>
    
    <view class="question-card">
      <view class="question-header">
        <text class="question-number">第 {{ currentIndex + 1 }} 题</text>
        <text class="question-type" v-if="currentQuestion.questionType === 1">单选题</text>
      </view>
      <text class="question-text">{{ currentQuestion.questionText || currentQuestion.content }}</text>
      
      <view class="options">
        <view 
          class="option" 
          v-for="(option, index) in currentOptions"
          :key="index"
          :class="{active: selectedIndex === index}"
          @click="selectOption(index, option)"
        >
          <view class="option-label">{{ ['A', 'B', 'C', 'D', 'E'][index] }}</view>
          <text class="option-text">{{ option.text }}</text>
        </view>
      </view>
    </view>
    
    <view class="actions">
      <button 
        class="btn" 
        :disabled="currentIndex === 0"
        @click="prevQuestion"
      >上一题</button>
      
      <button 
        class="btn btn-primary" 
        :disabled="selectedIndex === -1"
        @click="nextQuestion"
      >{{ isLast ? '提交' : '下一题' }}</button>
    </view>
  </view>
</template>

<script>
import { getScaleQuestions, submitAssessment } from '@/api/assessment.js'

export default {
  data() {
    return {
      scaleId: null,
      scaleInfo: {},
      questions: [],
      currentIndex: 0,
      answers: [],
      selectedIndex: -1,
      startTime: Date.now()
    }
  },
  
  computed: {
    currentQuestion() {
      return this.questions[this.currentIndex] || {}
    },
    
    currentOptions() {
      // 从题目数据中获取选项
      if (this.currentQuestion.options) {
        try {
          return typeof this.currentQuestion.options === 'string' 
            ? JSON.parse(this.currentQuestion.options) 
            : this.currentQuestion.options
        } catch (e) {
          return []
        }
      }
      return []
    },
    
    progress() {
      return ((this.currentIndex + 1) / this.questions.length) * 100
    },
    
    progressStyle() {
      return 'width:' + this.progress + '%'
    },
    
    isLast() {
      return this.currentIndex === this.questions.length - 1
    }
  },
  
  onLoad(options) {
    this.scaleId = options.id
    this.loadQuestions()
  },
  
  methods: {
    async loadQuestions() {
      try {
        uni.showLoading({ title: '加载中...' })
        const res = await getScaleQuestions(this.scaleId)
        this.questions = res || []
        this.answers = new Array(this.questions.length).fill(null)
        uni.hideLoading()
        
        if (this.questions.length === 0) {
          uni.showToast({ title: '暂无题目', icon: 'none' })
          setTimeout(() => {
            uni.navigateBack()
          }, 1500)
        }
      } catch (err) {
        uni.hideLoading()
        console.error('加载题目失败', err)
        uni.showToast({ title: '加载失败', icon: 'none' })
      }
    },
    
    selectOption(index, option) {
      this.selectedIndex = index
      this.answers[this.currentIndex] = {
        questionId: this.currentQuestion.id,
        questionNo: this.currentIndex + 1,
        answer: option.value,
        score: option.value  // 直接使用选项的value作为分数
      }
    },
    
    prevQuestion() {
      if (this.currentIndex > 0) {
        this.currentIndex--
        // 恢复之前的选择
        const prevAnswer = this.answers[this.currentIndex]
        if (prevAnswer) {
          const optionIndex = this.currentOptions.findIndex(opt => opt.value === prevAnswer.answer)
          this.selectedIndex = optionIndex >= 0 ? optionIndex : -1
        } else {
          this.selectedIndex = -1
        }
      }
    },
    
    async nextQuestion() {
      if (this.selectedIndex === -1) {
        uni.showToast({ title: '请选择答案', icon: 'none' })
        return
      }
      
      if (this.isLast) {
        await this.submit()
      } else {
        this.currentIndex++
        // 恢复之前的选择
        const nextAnswer = this.answers[this.currentIndex]
        if (nextAnswer) {
          const optionIndex = this.currentOptions.findIndex(opt => opt.value === nextAnswer.answer)
          this.selectedIndex = optionIndex >= 0 ? optionIndex : -1
        } else {
          this.selectedIndex = -1
        }
      }
    },
    
    async submit() {
      // 检查是否所有题目都已作答
      const unanswered = this.answers.filter(a => a === null).length
      if (unanswered > 0) {
        uni.showModal({
          title: '提示',
          content: `还有 ${unanswered} 道题未作答，确定要提交吗？`,
          success: async (res) => {
            if (res.confirm) {
              await this.doSubmit()
            }
          }
        })
      } else {
        uni.showModal({
          title: '确认提交',
          content: '确定要提交答卷吗？提交后将无法修改。',
          success: async (res) => {
            if (res.confirm) {
              await this.doSubmit()
            }
          }
        })
      }
    },
    
    async doSubmit() {
      try {
        uni.showLoading({ title: '提交中...' })
        
        const completionTime = Math.floor((Date.now() - this.startTime) / 1000)
        
        const res = await submitAssessment({
          scaleId: this.scaleId,
          answers: JSON.stringify(this.answers.filter(a => a !== null)),
          completionTime: completionTime
        })
        
        uni.hideLoading()
        
        // 跳转到结果页，只传记录 ID，避免完整报告塞进 URL 后超长
        uni.redirectTo({
          url: `/pages/assessment/result?id=${res.assessmentId}`
        })
      } catch (err) {
        uni.hideLoading()
        console.error('提交失败', err)
        
        // 显示详细错误信息
        const errorMsg = err.message || err.msg || '提交失败，请重试'
        uni.showModal({
          title: '提交失败',
          content: errorMsg,
          showCancel: false
        })
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

.progress-bar {
  position: relative;
  height: 12rpx;
  background: $border-color;
  border-radius: $radius-xl;
  margin-bottom: 60rpx;
  margin-top: $spacing-lg;
  
  .progress {
    height: 100%;
    background: $primary-gradient;
    border-radius: $radius-xl;
    transition: width 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  }
  
  .progress-text {
    position: absolute;
    right: 0;
    top: -44rpx;
    font-size: $font-xs;
    color: $text-tertiary;
    font-weight: 600;
  }
}

.question-card {
  @extend %card;
  padding: 50rpx 40rpx;
  margin-bottom: $spacing-lg;
  
  .question-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: $spacing-md;
    
    .question-number {
      font-size: $font-sm;
      color: $primary-color;
      font-weight: 700;
      letter-spacing: 1rpx;
    }
    
    .question-type {
      font-size: $font-xs;
      color: $text-tertiary;
      background: $bg-color;
      padding: 6rpx 20rpx;
      border-radius: $radius-sm;
    }
  }
  
  .question-text {
    display: block;
    font-size: $font-lg;
    color: $text-primary;
    line-height: 1.6;
    margin-bottom: 50rpx;
    font-weight: 600;
  }
  
  .options {
    .option {
      display: flex;
      align-items: center;
      padding: 32rpx;
      margin-bottom: $spacing-md;
      background: $bg-color;
      border: 2rpx solid transparent;
      border-radius: $radius-md;
      transition: all 0.3s;
      
      &:last-child {
        margin-bottom: 0;
      }
      
      &.active {
        border-color: $primary-color;
        background: $primary-light;
        box-shadow: $shadow-sm;
        
        .option-label {
          background: $primary-gradient;
          color: #fff;
          font-weight: 700;
        }
        
        .option-text {
          color: $primary-color;
          font-weight: 600;
        }
      }
      
      .option-label {
        width: 60rpx;
        height: 60rpx;
        line-height: 60rpx;
        text-align: center;
        background: #fff;
        border-radius: $radius-round;
        font-size: $font-sm;
        color: $text-tertiary;
        margin-right: 24rpx;
        flex-shrink: 0;
        box-shadow: $shadow-sm;
      }
      
      .option-text {
        flex: 1;
        font-size: $font-md;
        color: $text-secondary;
        line-height: 1.4;
      }
    }
  }
}

.actions {
  display: flex;
  gap: $spacing-md;
  margin-top: $spacing-xl;
  
  .btn {
    flex: 1;
    height: 100rpx;
    line-height: 100rpx;
    border-radius: $radius-xl;
    font-size: $font-lg;
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
      @extend %btn-primary;
    }
  }
}
</style>
