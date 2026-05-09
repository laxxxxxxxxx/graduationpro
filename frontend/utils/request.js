/**
 * HTTP请求封装
 */

// API基础URL - 开发环境。可通过 VUE_APP_API_BASE_URL 或本地 storage(apiBaseUrl) 覆盖。
const DEFAULT_BASE_URL = (
  typeof process !== 'undefined' &&
  process.env &&
  process.env.VUE_APP_API_BASE_URL
) || 'http://localhost:8080/api'

// 生产环境请改为实际域名
// const BASE_URL = 'https://your-api-domain.com/api'

export const getApiBaseUrl = () => {
  return uni.getStorageSync('apiBaseUrl') || DEFAULT_BASE_URL
}

const serializeParams = (params = {}) => {
  return Object.keys(params)
    .filter((key) => params[key] !== undefined && params[key] !== null && params[key] !== '')
    .map((key) => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
    .join('&')
}

const buildUrl = (url, params) => {
  const query = serializeParams(params)
  const baseUrl = getApiBaseUrl()
  if (!query) {
    return baseUrl + url
  }

  const separator = url.includes('?') ? '&' : '?'
  return baseUrl + url + separator + query
}

/**
 * 请求方法
 */
const request = (options) => {
  return new Promise((resolve, reject) => {
    // 获取token
    const token = uni.getStorageSync('token')
    
    // 显示加载提示
    if (options.loading !== false) {
      uni.showLoading({
        title: options.loadingText || '加载中...',
        mask: true
      })
    }
    
    uni.request({
      url: buildUrl(options.url, options.params),
      method: options.method || 'GET',
      data: options.data || {},
      header: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
      },
      timeout: 15000,
      success: (res) => {
        // 隐藏加载提示
        if (options.loading !== false) {
          uni.hideLoading()
        }
        
        // HTTP状态码判断
        if (res.statusCode === 200) {
          // 业务状态码判断
          if (res.data.code === 200) {
            resolve(res.data.data)
          } else if (res.data.code === 401) {
            // Token过期或未登录
            uni.removeStorageSync('token')
            uni.removeStorageSync('userInfo')
            uni.showToast({
              title: '请先登录',
              icon: 'none'
            })
            setTimeout(() => {
              uni.reLaunch({
                url: '/pages/login/login'
              })
            }, 1500)
            reject(res.data)
          } else {
            // 业务错误
            uni.showToast({
              title: res.data.message || '请求失败',
              icon: 'none'
            })
            reject(res.data)
          }
        } else {
          // HTTP错误
          uni.showToast({
            title: `网络错误(${res.statusCode})`,
            icon: 'none'
          })
          reject(res)
        }
      },
      fail: (err) => {
        // 隐藏加载提示
        if (options.loading !== false) {
          uni.hideLoading()
        }
        
        // 请求失败
        uni.showToast({
          title: '网络异常,请检查网络连接',
          icon: 'none'
        })
        reject(err)
      }
    })
  })
}

/**
 * GET请求
 */
export const get = (url, params = {}, options = {}) => {
  return request({
    url,
    method: 'GET',
    params,
    ...options
  })
}

/**
 * POST请求
 */
export const post = (url, data = {}, options = {}) => {
  return request({
    url,
    method: 'POST',
    data,
    ...options
  })
}

/**
 * PUT请求
 */
export const put = (url, data = {}, options = {}) => {
  return request({
    url,
    method: 'PUT',
    data,
    ...options
  })
}

/**
 * DELETE请求
 */
export const del = (url, data = {}, options = {}) => {
  return request({
    url,
    method: 'DELETE',
    data,
    ...options
  })
}

/**
 * 文件上传
 */
export const upload = (filePath, options = {}) => {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')
    
    uni.uploadFile({
      url: buildUrl(options.url, options.params),
      filePath,
      name: options.name || 'file',
      formData: options.formData || {},
      header: {
        'Authorization': token ? `Bearer ${token}` : ''
      },
      success: (res) => {
        const data = JSON.parse(res.data)
        if (data.code === 200) {
          resolve(data.data)
        } else {
          uni.showToast({
            title: data.message || '上传失败',
            icon: 'none'
          })
          reject(data)
        }
      },
      fail: (err) => {
        uni.showToast({
          title: '上传失败',
          icon: 'none'
        })
        reject(err)
      }
    })
  })
}

export default {
  get,
  post,
  put,
  del,
  upload
}
