<template>
  <view class="container">
    <view class="header">
      <text class="title">情绪趋势</text>
    </view>
    
    <view class="chart-card">
      <canvas canvas-id="moodChart" id="moodChart" class="chart"></canvas>
    </view>
    
    <view class="stats-card">
      <view class="stat-item">
        <text class="label">平均心情</text>
        <text class="value">7.2</text>
      </view>
      <view class="stat-item">
        <text class="label">记录天数</text>
        <text class="value">15</text>
      </view>
      <view class="stat-item">
        <text class="label">最高分</text>
        <text class="value">9</text>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      chartData: []
    }
  },
  
  onReady() {
    this.drawChart()
  },
  
  methods: {
    drawChart() {
      // TODO: 使用echarts绘制图表
      console.log('绘制情绪趋势图')
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
  margin-bottom: $spacing-xl;
  padding: $spacing-lg 0;
  
  .title {
    font-size: $font-xxl;
    font-weight: 700;
    color: $text-primary;
    letter-spacing: 2rpx;
  }
}

.chart-card {
  @extend %card;
  padding: 40rpx;
  margin-bottom: $spacing-lg;
  
  .chart {
    width: 100%;
    height: 450rpx;
  }
}

.stats-card {
  @extend %card;
  padding: 40rpx 20rpx;
  display: flex;
  justify-content: space-around;
  
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
      background: $border-color;
    }
    
    .label {
      display: block;
      font-size: $font-xs;
      color: $text-secondary;
      margin-bottom: 12rpx;
    }
    
    .value {
      font-size: 52rpx;
      font-weight: 700;
      color: $primary-color;
    }
  }
}
</style>
