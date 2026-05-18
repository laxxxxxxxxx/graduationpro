/**
 * 教育资源API
 */
import { get, post, del } from '@/utils/request'

/**
 * 获取资源列表
 */
export const getResourceList = (params) => {
  return get('/resource/list', params)
}

/**
 * 获取资源详情（含点赞/收藏状态和学习记录）
 */
export const getResourceDetail = (id) => {
  return get(`/resource/${id}`)
}

/**
 * 搜索资源
 */
export const searchResource = (keyword) => {
  return get('/resource/search', { keyword })
}

/**
 * 记录学习进度
 */
export const recordStudy = (resourceId, data) => {
  return post(`/resource/${resourceId}/study`, data)
}

/**
 * 获取学习记录
 */
export const getLearningRecords = () => {
  return get('/user/learning-records')
}

/**
 * 获取分类列表
 */
export const getCategories = () => {
  return get('/resource/categories')
}

/**
 * 点赞/取消点赞
 */
export const toggleLike = (resourceId) => {
  return post(`/resource/${resourceId}/like`)
}

/**
 * 收藏/取消收藏
 */
export const toggleFavorite = (resourceId) => {
  return post(`/resource/${resourceId}/favorite`)
}

/**
 * 删除资源 (管理员)
 */
export const deleteResource = (id) => {
  return del(`/resource/${id}`)
}

/**
 * 获取资源评论列表
 */
export const getComments = (resourceId) => {
  return get(`/resource/${resourceId}/comments`)
}

/**
 * 发表评论
 */
export const addComment = (resourceId, data) => {
  return post(`/resource/${resourceId}/comment`, data)
}

/**
 * 删除评论
 */
export const deleteComment = (commentId) => {
  return del(`/resource/comment/${commentId}`)
}

/**
 * 上传资源 (管理员)
 */
export const createResource = (data) => {
  return post('/resource', data)
}

/**
 * 获取用户收藏列表
 */
export const getUserFavorites = () => {
  return get('/resource/favorites')
}

/**
 * 获取用户点赞列表
 */
export const getUserLikes = () => {
  return get('/resource/likes')
}
