/**
 * 推荐相关API
 */
import { get, post } from '@/utils/request'

/**
 * 获取个性化推荐列表（四路混合推荐）
 */
export const getPersonalizedRecommendations = (topN = 10) => {
  return get('/recommendation/personalized', { topN })
}

/**
 * 基于用户的协同过滤推荐
 */
export const getUserBasedCF = (topN = 10) => {
  return get('/recommendation/user-cf', { topN })
}

/**
 * 基于物品的协同过滤推荐
 */
export const getItemBasedCF = (topN = 10) => {
  return get('/recommendation/item-cf', { topN })
}

/**
 * 基于内容的推荐
 */
export const getContentBased = (topN = 10) => {
  return get('/recommendation/content-based', { topN })
}

/**
 * 基于心理画像的推荐
 */
export const getProfileBased = (topN = 10) => {
  return get('/recommendation/profile-based', { topN })
}

/**
 * 记录推荐点击
 */
export const recordClick = (resourceId) => {
  return post('/recommendation/click', null, {
    params: { resourceId }
  })
}

/**
 * 刷新用户兴趣标签
 */
export const refreshInterestTags = () => {
  return post('/recommendation/refresh-tags')
}
