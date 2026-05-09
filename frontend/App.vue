<script>
export default {
  onLaunch: function() {
    console.log('App Launch')
    // 检查登录状态
    this.checkLoginStatus()
  },
  onShow: function() {
    console.log('App Show')
  },
  onHide: function() {
    console.log('App Hide')
  },
  methods: {
    checkLoginStatus() {
      const token = uni.getStorageSync('token')
      if (!token) {
        // 未登录,可以跳转到登录页或保持当前状态
        console.log('用户未登录')
      }
    }
  }
}
</script>

<style lang="scss">
/* 全局样式 */
@import '@/uni.scss';

page {
  background-color: $bg-color;
  font-size: $font-md;
  color: $text-primary;
  line-height: 1.5;
}

/* 通用容器 */
.container {
  padding: $spacing-md;
}

/* 卡片样式 - 柔和圆角与阴影 */
.card {
  background: $card-bg;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  margin-bottom: $spacing-md;
  box-shadow: $shadow-md;
  border: 1px solid rgba(255, 255, 255, 0.8);
}

/* 按钮样式 - 渐变与圆润 */
.btn-primary {
  background: $primary-gradient;
  color: #fff;
  border-radius: $radius-xl;
  border: none;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6rpx 16rpx rgba(255, 140, 140, 0.2);
  
  &:active {
    opacity: 0.9;
    transform: scale(0.98);
  }
}

.btn-secondary {
  background: $primary-light;
  color: $primary-color;
  border-radius: $radius-xl;
  border: none;
}

/* 文本标题 */
.section-title {
  font-size: $font-lg;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: $spacing-md;
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
    background: $primary-color;
    border-radius: 4rpx;
  }
}

/* 文本省略 */
.text-ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.text-ellipsis-2 {
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
</style>
