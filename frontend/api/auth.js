/**
 * 认证相关API
 */
import { post, get, put } from '@/utils/request'

/**
 * 用户登录
 */
export const login = (data) => {
  return post('/auth/login', data)
}

/**
 * 用户注册
 */
export const register = (data) => {
  return post('/auth/register', data)
}

/**
 * 微信登录
 */
export const wxLogin = (code) => {
  return post('/auth/wx-login', { code })
}

/**
 * 获取用户信息
 */
export const getUserInfo = () => {
  return get('/user/profile')
}

/**
 * 获取个人主页完整信息（含测评摘要）
 */
export const getUserProfileFull = () => {
  return get('/user/profile/full')
}

/**
 * 更新用户信息
 */
export const updateUserInfo = (data) => {
  return put('/user/profile', data)
}

/**
 * 退出登录
 */
export const logout = () => {
  return post('/auth/logout')
}
