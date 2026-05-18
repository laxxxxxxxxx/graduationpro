<template>
  <view class="container">
    <view class="header">
      <text class="title">测评档案</text>
      <text class="subtitle">我的专属心理健康档案</text>
    </view>

    <!-- 统计概览 -->
    <view class="stats-card" v-if="totalCount > 0">
      <view class="stat-item">
        <text class="stat-num">{{ totalCount }}</text>
        <text class="stat-label">总测评次数</text>
      </view>
      <view class="stat-item">
        <text class="stat-num">{{ scaleTypes }}</text>
        <text class="stat-label">量表类型</text>
      </view>
      <view class="stat-item">
        <text class="stat-num">{{ latestLevel }}</text>
        <text class="stat-label">最近结果</text>
      </view>
    </view>

    <!-- 趋势图（简易版） -->
    <view class="trend-section" v-if="hasTrendData">
      <text class="section-title">📈 测评趋势</text>
      <view class="trend-chart">
        <view 
          class="trend-group" 
          v-for="(points, scaleCode) in enrichedTrends" 
          :key="scaleCode"
        >
          <text class="trend-label">{{ getScaleName(scaleCode) }}</text>
          <view class="trend-line">
            <view 
              class="trend-dot" 
              v-for="(point, idx) in points" 
              :key="idx"
              :class="point.levelClass"
              @click="viewReport(point.id)"
            >
              <text class="dot-score">{{ formatTrendScore(point.score) }}</text>
              <text class="dot-date">{{ formatTrendDate(point.date) }}</text>
            </view>
            <view class="trend-connector" v-if="points.length > 1"></view>
          </view>
        </view>
      </view>
    </view>

    <!-- 测评记录列表 -->
    <view class="history-section">
      <text class="section-title">📋 测评记录</text>
      
      <view class="timeline">
        <view 
          class="timeline-item" 
          v-for="record in enrichedReports" 
          :key="record.id"
          @click="viewReport(record.id)"
        >
          <view class="timeline-marker" :class="record.levelClass"></view>
          
          <view class="timeline-content">
            <view class="tl-header">
              <text class="tl-name">{{ record.scaleName }}</text>
              <text class="tl-tag" :class="record.levelClass">
                {{ record.resultLevel || '未知' }}
              </text>
            </view>
            
            <view class="tl-info">
              <text class="tl-score">得分: {{ formatScore(record.totalScore) }}</text>
              <text class="tl-time" v-if="record.completionTime">
                用时: {{ formatTime(record.completionTime) }}
              </text>
            </view>
            
            <text class="tl-date">{{ formatDate(record.createdAt) }}</text>
          </view>
          
          <text class="timeline-arrow">›</text>
        </view>
      </view>
      
      <view class="empty" v-if="reports.length === 0 && !loading">
        <image class="empty-image" src="/static/images/empty-state.png" mode="aspectFit"></image>
        <text class="empty-text">暂无测评记录</text>
        <button class="go-test-btn" @click="goTest">去完成第一次测评</button>
      </view>
    </view>

    <!-- 加载 -->
    <view class="loading" v-if="loading">
      <text>加载中...</text>
    </view>
  </view>
</template>

<script>
import { getAssessmentReports } from '@/api/assessment.js'

export default {
  data() {
    return {
      reports: [],
      trends: {},
      totalCount: 0,
      loading: false
    }
  },
  
  computed: {
    scaleTypes() {
      const codes = new Set(this.reports.map(r => r.scaleCode).filter(Boolean))
      return codes.size
    },
    
    latestLevel() {
      if (this.reports.length === 0) return '--'
      const level = this.reports[0].resultLevel || ''
      if (level.includes('正常') || level.includes('一类')) return '正常'
      if (level.includes('轻度') || level.includes('二类')) return '需关注'
      return '需关注'
    },
    
    hasTrendData() {
      return Object.keys(this.trends).length > 0
    },
    
    enrichedReports() {
      return this.reports.map(r => ({
        ...r,
        levelClass: this.computeLevelClass(r.resultLevel)
      }))
    },
    
    enrichedTrends() {
      const result = {}
      for (const [scaleCode, points] of Object.entries(this.trends)) {
        result[scaleCode] = points.map(p => ({
          ...p,
          levelClass: this.computeLevelClass(p.level)
        }))
      }
      return result
    }
  },
  
  onShow() {
    this.loadReports()
  },
  
  methods: {
    async loadReports() {
      try {
        this.loading = true
        const res = await getAssessmentReports()
        this.reports = res.reports || []
        this.trends = res.trends || {}
        this.totalCount = res.totalCount || 0
      } catch (err) {
        console.error('加载测评记录失败', err)
        uni.showToast({ title: '加载失败', icon: 'none' })
      } finally {
        this.loading = false
      }
    },
    
    getScaleName(code) {
      const map = {
        'SDS': '抑郁自评(SDS)',
        'SCL90': '症状自评(SCL-90)',
        'UPI': '人格问卷(UPI)'
      }
      return map[code] || code
    },
    
    computeLevelClass(level) {
      if (!level) return 'normal'
      if (level.includes('正常') || level.includes('一类')) return 'normal'
      if (level.includes('轻度') || level.includes('二类')) return 'mild'
      if (level.includes('中度')) return 'moderate'
      if (level.includes('重度') || level.includes('三类')) return 'severe'
      return 'normal'
    },
    
    formatScore(score) {
      if (score === null || score === undefined) return '--'
      return parseFloat(score).toFixed(1)
    },
    
    formatTime(seconds) {
      if (!seconds) return '--'
      const min = Math.floor(seconds / 60)
      const sec = seconds % 60
      return min > 0 ? `${min}分${sec}秒` : `${sec}秒`
    },
    
    formatDate(dateStr) {
      if (!dateStr) return ''
      return dateStr.substring(0, 10)
    },
    
    formatTrendScore(score) {
      if (score === null || score === undefined) return '--'
      return parseFloat(score).toFixed(0)
    },
    
    formatTrendDate(dateStr) {
      if (!dateStr) return ''
      const parts = dateStr.split('-')
      if (parts.length >= 3) {
        return `${parseInt(parts[1])}/${parseInt(parts[2])}`
      }
      return dateStr
    },
    
    viewReport(id) {
      uni.navigateTo({
        url: `/pages/assessment/result?id=${id}`
      })
    },
    
    goTest() {
      uni.navigateTo({
        url: '/pages/assessment/scale-list'
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.container {
  padding: $spacing-md;
  padding-bottom: $spacing-xl;
}

.header {
  text-align: center;
  padding: $spacing-xl 0;
  
  .title {
    display: block;
    font-size: $font-xxl;
    font-weight: 700;
    color: $text-primary;
    margin-bottom: 12rpx;
  }
  
  .subtitle {
    font-size: $font-sm;
    color: $text-secondary;
  }
}

.stats-card {
  display: flex;
  background: $primary-gradient;
  border-radius: $radius-lg;
  padding: 40rpx 20rpx;
  margin-bottom: $spacing-lg;
  box-shadow: $shadow-lg;
  
  .stat-item {
    flex: 1;
    text-align: center;
    position: relative;
    
    &:not(:last-child)::after {
      content: '';
      position: absolute;
      right: 0;
      top: 20%;
      height: 60%;
      width: 1rpx;
      background: rgba(255,255,255,0.2);
    }
    
    .stat-num {
      display: block;
      font-size: $font-xl;
      font-weight: 700;
      color: #fff;
      margin-bottom: 6rpx;
    }
    
    .stat-label {
      font-size: $font-xs;
      color: rgba(255, 255, 255, 0.9);
      font-weight: 500;
    }
  }
}

.trend-section {
  background: #ffffff;
  border-radius: 32rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 8rpx 24rpx rgba(255, 140, 140, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.8);
  padding: 40rpx;
  
  .trend-group {
    margin-bottom: 40rpx;
    
    &:last-child {
      margin-bottom: 0;
    }
    
    .trend-label {
      display: block;
      font-size: $font-md;
      font-weight: 600;
      color: $text-primary;
      margin-bottom: 24rpx;
    }
    
    .trend-line {
      display: flex;
      align-items: center;
      position: relative;
      padding: 20rpx 0;
      
      .trend-connector {
        position: absolute;
        left: 5%;
        right: 5%;
        top: 50%;
        height: 4rpx;
        background: $border-color;
        z-index: 0;
      }
      
      .trend-dot {
        flex: 1;
        text-align: center;
        position: relative;
        z-index: 1;
        
        .dot-score {
          display: block;
          width: 72rpx;
          height: 72rpx;
          line-height: 72rpx;
          margin: 0 auto 12rpx;
          border-radius: $radius-round;
          font-size: $font-xs;
          font-weight: 700;
          color: #fff;
          box-shadow: $shadow-sm;
          transition: transform 0.2s;
          
          &:active {
            transform: scale(1.1);
          }
        }
        
        .dot-date {
          font-size: $font-xs;
          color: $text-tertiary;
        }
        
        &.normal .dot-score { background: linear-gradient(135deg, #A8E6CF, #52C41A); }
        &.mild .dot-score { background: linear-gradient(135deg, #FFD3B6, #FAAD14); }
        &.moderate .dot-score { background: linear-gradient(135deg, #FFE0B2, #FB8C00); }
        &.severe .dot-score { background: linear-gradient(135deg, #FF8B94, #FF4D4F); }
      }
    }
  }
}

.history-section {
  .timeline-item {
    background: #ffffff;
    border-radius: 32rpx;
    padding: 32rpx;
    margin-bottom: 24rpx;
    box-shadow: 0 8rpx 24rpx rgba(255, 140, 140, 0.08);
    border: 1px solid rgba(255, 255, 255, 0.8);
    display: flex;
    align-items: flex-start;
    padding: $spacing-lg;
    margin-bottom: $spacing-md;
    transition: transform 0.2s;
    
    &:active {
      transform: scale(0.99);
    }
    
    .timeline-marker {
      width: 20rpx;
      height: 20rpx;
      border-radius: $radius-round;
      margin-right: 24rpx;
      margin-top: 12rpx;
      flex-shrink: 0;
      box-shadow: 0 0 0 6rpx #fff, $shadow-sm;
      
      &.normal { background: #52C41A; }
      &.mild { background: #FAAD14; }
      &.moderate { background: #FB8C00; }
      &.severe { background: #FF4D4F; }
    }
    
    .timeline-content {
      flex: 1;
      
      .tl-header {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        margin-bottom: 12rpx;
        
        .tl-name {
          font-size: $font-md;
          font-weight: 600;
          color: $text-primary;
          flex: 1;
          margin-right: $spacing-md;
        }
        
        .tl-tag {
          font-size: $font-xs;
          padding: 6rpx 20rpx;
          border-radius: $radius-xl;
          white-space: nowrap;
          font-weight: 500;
          
          &.normal { background: #E8F5E9; color: #2E7D32; }
          &.mild { background: #FFFDE7; color: #FBC02D; }
          &.moderate { background: #FFF3E0; color: #FB8C00; }
          &.severe { background: #FFEBEE; color: #D32F2F; }
        }
      }
      
      .tl-info {
        display: flex;
        gap: 40rpx;
        margin-bottom: 10rpx;
        
        .tl-score, .tl-time {
          font-size: $font-sm;
          color: $text-secondary;
          font-weight: 500;
        }
      }
      
      .tl-date {
        font-size: $font-xs;
        color: $text-tertiary;
      }
    }
    
    .timeline-arrow {
      font-size: 40rpx;
      color: $text-tertiary;
      align-self: center;
      margin-left: $spacing-sm;
    }
  }
}

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
}

.empty {
  text-align: center;
  padding: 120rpx 40rpx;
  
  .empty-image {
    width: 280rpx;
    height: 220rpx;
    margin-bottom: $spacing-lg;
  }
  
  .empty-text {
    display: block;
    font-size: $font-lg;
    color: $text-tertiary;
    margin-bottom: 60rpx;
  }
  
  .go-test-btn {
    background: linear-gradient(135deg, #FF9A9E 0%, #FECFEF 100%);
    color: #fff;
    border-radius: 50rpx;
    border: none;
    font-weight: 500;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 6rpx 16rpx rgba(255, 140, 140, 0.2);
    height: 90rpx;
    width: 360rpx;
    font-size: $font-md;
  }
}

.loading {
  text-align: center;
  padding: 50rpx 0;
  color: $text-tertiary;
  font-size: $font-md;
}
</style>
