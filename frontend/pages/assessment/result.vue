<template>
  <view class="container">
    <!-- 报告头部 -->
    <view class="report-header">
      <text class="report-title">{{ report.scaleName || '测评报告' }}</text>
      <text class="report-subtitle">专业心理测评 · 结果仅供参考</text>
      <text class="report-date" v-if="reportDate">测评日期：{{ reportDate }}</text>
    </view>
    
    <!-- Tab切换 -->
    <view class="tabs">
      <view class="tab" :class="{active: currentTab === 'overview'}" @click="switchTab('overview')">
        综合概览
      </view>
      <view class="tab" :class="{active: currentTab === 'dimension'}" @click="switchTab('dimension')">
        维度分析
      </view>
      <view class="tab" :class="{active: currentTab === 'interpretation'}" @click="switchTab('interpretation')">
        专业解读
      </view>
      <view class="tab" :class="{active: currentTab === 'suggestions'}" @click="switchTab('suggestions')">
        干预建议
      </view>
    </view>
    
    <!-- 综合概览 -->
    <view class="tab-content" v-if="currentTab === 'overview'">
      <!-- 结论卡片 -->
      <view class="conclusion-card" :class="levelClass">
        <view class="conclusion-icon">{{ levelIcon }}</view>
        <text class="conclusion-text">{{ report.resultLevel || '待评估' }}</text>
        <text class="conclusion-desc" v-if="report.levelDescription">{{ report.levelDescription }}</text>
      </view>
      
      <!-- 分数信息 -->
      <view class="info-grid">
        <view class="info-item">
          <text class="info-label">测评得分</text>
          <text class="info-value score-value">{{ formatScore(report.totalScore) }}</text>
        </view>
        <view class="info-item">
          <text class="info-label">答题用时</text>
          <text class="info-value">{{ formatTime(report.completionTime) }}</text>
        </view>
        <view class="info-item">
          <text class="info-label">量表类型</text>
          <text class="info-value type-value">{{ report.scaleCode || '未知' }}</text>
        </view>
        <view class="info-item" v-if="report.totalQuestions">
          <text class="info-label">题目数量</text>
          <text class="info-value">{{ report.totalQuestions }}题</text>
        </view>
      </view>
      
      <!-- 风险提示 -->
      <view class="risk-section" v-if="riskFlags.length > 0">
        <text class="section-title">⚠️ 风险提示</text>
        <view class="risk-item" v-for="(flag, idx) in riskFlags" :key="idx">
          <text class="risk-text">{{ flag }}</text>
        </view>
      </view>
      
      <!-- 参考范围 -->
      <view class="reference-section" v-if="referenceRanges">
        <text class="section-title">📊 参考范围</text>
        <view class="ref-item" v-for="(desc, key) in referenceRanges" :key="key">
          <text class="ref-label">{{ key }}</text>
          <text class="ref-value">{{ desc }}</text>
        </view>
      </view>
    </view>
    
    <!-- 维度分析 -->
    <view class="tab-content" v-if="currentTab === 'dimension'">
      <view class="dimension-section">
        <text class="section-title">📈 维度雷达图</text>
        <text class="section-hint">各维度得分分布，帮助了解具体问题领域</text>
        
        <!-- 简易雷达图（柱状图替代） -->
        <view class="radar-chart" v-if="dimensions.length > 0">
          <view class="bar-item" v-for="(dim, idx) in enrichedDimensions" :key="idx">
            <view class="bar-header">
              <text class="bar-name">{{ dim.name }}</text>
              <text class="bar-score">{{ dim.score }} / {{ dim.maxScore }}</text>
            </view>
            <view class="bar-track">
              <view 
                class="bar-fill" 
                :class="dim.barClass"
                :style="dim.barStyle"
              ></view>
            </view>
            <text class="bar-ratio">{{ formatPercent(dim.ratio) }}</text>
          </view>
        </view>
        
        <view class="empty-dim" v-else>
          <text>暂无维度数据</text>
        </view>
        
        <view class="dim-legend">
          <view class="legend-item">
            <view class="legend-color normal"></view>
            <text>正常范围（低于 60%）</text>
          </view>
          <view class="legend-item">
            <view class="legend-color mild"></view>
            <text>轻度偏高 (60-80%)</text>
          </view>
          <view class="legend-item">
            <view class="legend-color severe"></view>
            <text>明显偏高（高于 80%）</text>
          </view>
        </view>
      </view>
    </view>
    
    <!-- 专业解读 -->
    <view class="tab-content" v-if="currentTab === 'interpretation'">
      <view class="interpretation-card">
        <text class="section-title">📋 专业测评解读</text>
        
        <view class="interp-content">
          <text class="interp-text" v-for="(line, idx) in interpretationLines" :key="idx">{{ line }}</text>
        </view>
        
        <view class="disclaimer">
          <text class="disclaimer-title">⚠️ 免责声明</text>
          <text class="disclaimer-text">本测评报告仅供参考，不能作为医学诊断依据。测评结果受多种因素影响，如有疑虑请咨询专业心理医生或心理咨询师。</text>
        </view>
      </view>
    </view>
    
    <!-- 干预建议 -->
    <view class="tab-content" v-if="currentTab === 'suggestions'">
      <view class="suggestions-card">
        <text class="section-title">💡 分层级干预建议</text>
        
        <view class="suggestion-group" v-for="(group, gIdx) in suggestionGroups" :key="gIdx">
          <text class="group-title">{{ group.title }}</text>
          <view class="sug-item" v-for="(item, sIdx) in group.items" :key="sIdx">
            <text class="sug-text">{{ item }}</text>
          </view>
        </view>
        
        <!-- 紧急热线 -->
        <view class="hotline-card" v-if="!isNormal">
          <text class="hotline-title">🆘 心理援助热线</text>
          <text class="hotline-number">400-161-9995</text>
          <text class="hotline-desc">24小时心理危机干预热线</text>
          <text class="hotline-number">010-82951332</text>
          <text class="hotline-desc">全国心理援助热线</text>
        </view>
      </view>
    </view>
    
    <!-- 底部操作 -->
    <view class="actions">
      <button class="btn btn-outline" @click="viewHistory">测评档案</button>
      <button class="btn btn-primary" @click="retake">重新测评</button>
    </view>
  </view>
</template>

<script>
import { getReportDetail } from '@/api/assessment.js'

export default {
  data() {
    return {
      report: {},
      currentTab: 'overview',
      assessmentId: null,
      fromHistory: false
    }
  },
  
  computed: {
    reportDate() {
      if (this.report.createdAt) {
        return this.report.createdAt.substring(0, 10)
      }
      return ''
    },
    
    levelClass() {
      const level = this.report.resultLevel || ''
      if (level.includes('正常') || level.includes('一类')) return 'normal'
      if (level.includes('轻度') || level.includes('二类')) return 'mild'
      if (level.includes('中度')) return 'moderate'
      if (level.includes('重度') || level.includes('三类')) return 'severe'
      return 'normal'
    },
    
    levelIcon() {
      const level = this.report.resultLevel || ''
      if (level.includes('正常') || level.includes('一类')) return '😊'
      if (level.includes('轻度') || level.includes('二类')) return '😐'
      if (level.includes('中度')) return '😟'
      if (level.includes('重度') || level.includes('三类')) return '😰'
      return '😊'
    },
    
    isNormal() {
      const level = this.report.resultLevel || ''
      return level.includes('正常') || level.includes('一类')
    },
    
    dimensions() {
      if (this.report.dimensions && Array.isArray(this.report.dimensions)) {
        return this.report.dimensions
      }
      if (this.report.reportData && this.report.reportData.radarData) {
        return this.report.reportData.radarData
      }
      return []
    },
    
    // 预计算 barClass 和 barStyle，避免模板中复杂表达式
    enrichedDimensions() {
      return this.dimensions.map(dim => ({
        ...dim,
        barClass: dim.ratio < 0.6 ? 'bar-normal' : dim.ratio < 0.8 ? 'bar-mild' : 'bar-severe',
        barStyle: 'width:' + (dim.ratio * 100) + '%'
      }))
    },
    
    riskFlags() {
      if (this.report.riskFlags && Array.isArray(this.report.riskFlags)) {
        return this.report.riskFlags
      }
      return []
    },
    
    referenceRanges() {
      if (this.report.reportData && this.report.reportData.referenceRanges) {
        return this.report.reportData.referenceRanges
      }
      return null
    },
    
    interpretationLines() {
      const text = this.report.interpretation || ''
      return text.split('\n').filter(s => s.trim())
    },
    
    suggestionGroups() {
      const text = this.report.suggestions || ''
      const lines = text.split('\n').filter(s => s.trim())
      const groups = []
      let currentGroup = null
      
      for (const line of lines) {
        if (line.startsWith('一、') || line.startsWith('二、') || 
            line.startsWith('三、') || line.startsWith('四、')) {
          if (currentGroup) groups.push(currentGroup)
          currentGroup = { title: line, items: [] }
        } else if (currentGroup) {
          currentGroup.items.push(line)
        }
      }
      if (currentGroup) groups.push(currentGroup)
      
      return groups
    }
  },
  
  onLoad(options) {
    if (options.data) {
      // 从提交页面直接跳转（新测评结果）
      try {
        this.report = JSON.parse(decodeURIComponent(options.data))
        this.assessmentId = this.report.assessmentId
      } catch (e) {
        console.error('解析结果失败', e)
        uni.showToast({ title: '数据错误', icon: 'none' })
      }
    } else if (options.id) {
      // 从档案页跳转（查看历史报告）
      this.assessmentId = options.id
      this.fromHistory = true
      this.loadReport()
    }
  },
  
  methods: {
    async loadReport() {
      try {
        uni.showLoading({ title: '加载报告...' })
        const res = await getReportDetail(this.assessmentId)
        this.report = res || {}
        uni.hideLoading()
      } catch (err) {
        uni.hideLoading()
        console.error('加载报告失败', err)
        uni.showToast({ title: '加载失败', icon: 'none' })
      }
    },
    
    switchTab(tab) {
      this.currentTab = tab
    },
    
    formatScore(score) {
      if (score === null || score === undefined) return '--'
      return parseFloat(score).toFixed(1) + ' 分'
    },
    
    formatTime(seconds) {
      if (!seconds) return '--'
      const min = Math.floor(seconds / 60)
      const sec = seconds % 60
      return min > 0 ? `${min}分${sec}秒` : `${sec}秒`
    },
    
    formatPercent(ratio) {
      if (ratio === null || ratio === undefined) return '0%'
      return Math.round(ratio * 100) + '%'
    },
    
    getBarClass(ratio) {
      if (ratio < 0.6) return 'bar-normal'
      if (ratio < 0.8) return 'bar-mild'
      return 'bar-severe'
    },
    
    viewHistory() {
      uni.navigateTo({
        url: '/pages/assessment/history'
      })
    },
    
    retake() {
      uni.navigateBack()
    }
  }
}
</script>

<style lang="scss" scoped>
.container {
  padding: $spacing-md;
  padding-bottom: $spacing-xl;
}

.report-header {
  text-align: center;
  padding: $spacing-xl 0;
  
  .report-title {
    display: block;
    font-size: $font-xxl;
    font-weight: 700;
    color: $text-primary;
    margin-bottom: 12rpx;
  }
  
  .report-subtitle {
    font-size: $font-sm;
    color: $text-secondary;
    display: block;
    margin-bottom: 8rpx;
  }
  
  .report-date {
    font-size: $font-xs;
    color: $text-tertiary;
  }
}

.tabs {
  display: flex;
  gap: 12rpx;
  margin-bottom: $spacing-lg;
  background: #fff;
  border-radius: $radius-xl;
  padding: 10rpx;
  box-shadow: $shadow-sm;
  
  .tab {
    flex: 1;
    height: 70rpx;
    line-height: 70rpx;
    text-align: center;
    border-radius: $radius-xl;
    font-size: $font-sm;
    color: $text-secondary;
    transition: all 0.3s;
    
    &.active {
      background: $primary-gradient;
      color: #fff;
      font-weight: 600;
      box-shadow: $shadow-sm;
    }
  }
}

.tab-content {
  animation: fadeIn 0.4s ease-out;
}

// 结论卡片
.conclusion-card {
  text-align: center;
  padding: 60rpx 40rpx;
  border-radius: $radius-lg;
  margin-bottom: $spacing-lg;
  box-shadow: $shadow-md;
  
  &.normal { background: linear-gradient(135deg, #E8F5E9 0%, #C8E6C9 100%); }
  &.mild { background: linear-gradient(135deg, #FFFDE7 0%, #FFF9C4 100%); }
  &.moderate { background: linear-gradient(135deg, #FFF3E0 0%, #FFE0B2 100%); }
  &.severe { background: linear-gradient(135deg, #FFEBEE 0%, #FFCDD2 100%); }
  
  .conclusion-icon {
    font-size: 100rpx;
    margin-bottom: 24rpx;
    filter: drop-shadow(0 4rpx 10rpx rgba(0,0,0,0.1));
  }
  
  .conclusion-text {
    display: block;
    font-size: 52rpx;
    font-weight: 700;
    margin-bottom: 16rpx;
    
    .normal & { color: #2E7D32; }
    .mild & { color: #FBC02D; }
    .moderate & { color: #FB8C00; }
    .severe & { color: #D32F2F; }
  }
  
  .conclusion-desc {
    font-size: $font-sm;
    color: $text-secondary;
    line-height: 1.5;
  }
}

// 分数网格
.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: $spacing-md;
  margin-bottom: $spacing-lg;
  
  .info-item {
    @extend %card;
    padding: 30rpx;
    margin-bottom: 0;
    
    .info-label {
      display: block;
      font-size: $font-xs;
      color: $text-tertiary;
      margin-bottom: 10rpx;
    }
    
    .info-value {
      font-size: $font-lg;
      font-weight: 700;
      color: $text-primary;
      
      &.score-value { color: $primary-color; font-size: $font-xl; }
      &.type-value { color: $info-color; }
    }
  }
}

// 风险提示
.risk-section {
  @extend %card;
  padding: 40rpx;
  border-left: 10rpx solid $error-color;
  
  .section-title {
    color: $error-color;
  }
  
  .risk-item {
    margin-top: 20rpx;
    padding: 20rpx 30rpx;
    background: #FFF5F5;
    border-radius: $radius-md;
    
    .risk-text {
      font-size: $font-sm;
      color: #C53030;
      line-height: 1.6;
    }
  }
}

// 参考范围
.reference-section {
  @extend %card;
  padding: 40rpx;
  
  .ref-item {
    display: flex;
    justify-content: space-between;
    padding: 24rpx 0;
    border-bottom: 1rpx solid $border-color;
    
    &:last-child {
      border-bottom: none;
    }
    
    .ref-label {
      font-size: $font-md;
      color: $text-secondary;
    }
    
    .ref-value {
      font-size: $font-md;
      color: $primary-color;
      font-weight: 700;
    }
  }
}

// 维度分析
.dimension-section {
  @extend %card;
  padding: 40rpx;
  
  .section-hint {
    display: block;
    font-size: $font-xs;
    color: $text-tertiary;
    margin-bottom: 40rpx;
  }
  
  .bar-item {
    margin-bottom: 30rpx;
    
    .bar-header {
      display: flex;
      justify-content: space-between;
      margin-bottom: 12rpx;
      
      .bar-name {
        font-size: $font-md;
        font-weight: 600;
        color: $text-primary;
      }
      
      .bar-score {
        font-size: $font-xs;
        color: $text-tertiary;
      }
    }
    
    .bar-track {
      height: 20rpx;
      background: $bg-color;
      border-radius: $radius-xl;
      overflow: hidden;
      box-shadow: inset 0 2rpx 4rpx rgba(0,0,0,0.05);
      
      .bar-fill {
        height: 100%;
        border-radius: $radius-xl;
        transition: width 1s cubic-bezier(0.19, 1, 0.22, 1);
        
        &.bar-normal { background: linear-gradient(90deg, #A8E6CF, #52C41A); }
        &.bar-mild { background: linear-gradient(90deg, #FFD3B6, #FAAD14); }
        &.bar-severe { background: linear-gradient(90deg, #FF8B94, #FF4D4F); }
      }
    }
    
    .bar-ratio {
      display: block;
      text-align: right;
      font-size: $font-xs;
      color: $text-tertiary;
      margin-top: 8rpx;
    }
  }
  
  .dim-legend {
    display: flex;
    justify-content: space-around;
    margin-top: 40rpx;
    padding-top: 30rpx;
    border-top: 1rpx solid $border-color;
    
    .legend-item {
      display: flex;
      align-items: center;
      gap: 12rpx;
      font-size: $font-xs;
      color: $text-secondary;
      
      .legend-color {
        width: 24rpx;
        height: 24rpx;
        border-radius: $radius-sm;
        
        &.normal { background: #52C41A; }
        &.mild { background: #FAAD14; }
        &.severe { background: #FF4D4F; }
      }
    }
  }
}

// 专业解读
.interpretation-card {
  @extend %card;
  padding: 40rpx;
  
  .interp-content {
    margin-top: 24rpx;
    
    .interp-text {
      display: block;
      font-size: $font-md;
      color: $text-secondary;
      line-height: 1.8;
      margin-bottom: 20rpx;
    }
  }
  
  .disclaimer {
    margin-top: 40rpx;
    padding: 30rpx;
    background: #FFF9E6;
    border-radius: $radius-md;
    border-left: 8rpx solid $warning-color;
    
    .disclaimer-title {
      display: block;
      font-size: $font-sm;
      font-weight: 700;
      color: #B7791F;
      margin-bottom: 10rpx;
    }
    
    .disclaimer-text {
      font-size: $font-xs;
      color: #744210;
      line-height: 1.6;
    }
  }
}

// 干预建议
.suggestions-card {
  @extend %card;
  padding: 40rpx;
  
  .suggestion-group {
    margin-top: 30rpx;
    
    .group-title {
      display: block;
      font-size: $font-md;
      font-weight: 700;
      color: $text-primary;
      margin-bottom: 16rpx;
      padding-left: 16rpx;
      border-left: 6rpx solid $primary-color;
    }
    
    .sug-item {
      padding: 12rpx 0 12rpx 22rpx;
      
      .sug-text {
        font-size: $font-sm;
        color: $text-secondary;
        line-height: 1.6;
      }
    }
  }
  
  .hotline-card {
    margin-top: 50rpx;
    padding: 40rpx;
    background: $primary-gradient;
    border-radius: $radius-lg;
    text-align: center;
    box-shadow: $shadow-lg;
    
    .hotline-title {
      display: block;
      font-size: $font-md;
      font-weight: 700;
      color: #fff;
      margin-bottom: 20rpx;
    }
    
    .hotline-number {
      display: block;
      font-size: 52rpx;
      font-weight: 800;
      color: #fff;
      letter-spacing: 4rpx;
      margin-bottom: 8rpx;
      text-shadow: 0 4rpx 8rpx rgba(0,0,0,0.1);
    }
    
    .hotline-desc {
      font-size: $font-xs;
      color: rgba(255,255,255,0.9);
    }
  }
}

// 底部操作
.actions {
  display: flex;
  gap: $spacing-md;
  margin-top: $spacing-xl;
  padding-bottom: $spacing-xl;
  
  .btn {
    flex: 1;
    height: 100rpx;
    line-height: 100rpx;
    border-radius: $radius-xl;
    font-size: $font-lg;
    font-weight: 600;
    text-align: center;
    
    &.btn-primary {
      @extend %btn-primary;
    }
    
    &.btn-outline {
      background: #fff;
      color: $primary-color;
      border: 2rpx solid $primary-color;
      box-shadow: $shadow-sm;
      
      &:active {
        background: $bg-color;
      }
    }
  }
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20rpx); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
