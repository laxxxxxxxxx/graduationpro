/**
 * 情绪日记相关API
 */
import { post, get, put, del } from '@/utils/request'

/**
 * 创建日记
 */
export const createDiary = (data) => {
  return post('/emotion/diary', data)
}

/**
 * 更新日记
 */
export const updateDiary = (id, data) => {
  return put(`/emotion/diary/${id}`, data)
}

/**
 * 删除日记
 */
export const deleteDiary = (id) => {
  return del(`/emotion/diary/${id}`)
}

/**
 * 获取日记列表
 */
export const getDiaryList = (params) => {
  return get('/emotion/diary/list', params)
}

/**
 * 获取日记详情
 */
export const getDiaryDetail = (id) => {
  return get(`/emotion/diary/${id}`)
}

/**
 * 获取情绪统计
 */
export const getEmotionStats = (params) => {
  return get('/emotion/statistics', params)
}
