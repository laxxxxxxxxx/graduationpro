/**
 * 社区相关API
 */
import { post, get, put, del } from '@/utils/request'

/**
 * 获取分类列表
 */
export const getCategories = () => {
  return get('/community/categories')
}

/**
 * 发布帖子
 */
export const createPost = (data) => {
  return post('/community/posts', data)
}

/**
 * 编辑帖子
 */
export const updatePost = (postId, data) => {
  return put(`/community/posts/${postId}`, data)
}

/**
 * 删除帖子
 */
export const deletePost = (postId) => {
  return del(`/community/posts/${postId}`)
}

/**
 * 获取帖子列表
 */
export const getPostList = (params) => {
  return get('/community/posts', params)
}

/**
 * 获取帖子详情
 */
export const getPostDetail = (id) => {
  return get(`/community/posts/${id}`)
}

/**
 * 发表评论
 */
export const createComment = (postId, data) => {
  return post(`/community/posts/${postId}/comments`, data)
}

/**
 * 获取评论列表
 */
export const getComments = (postId) => {
  return get(`/community/posts/${postId}/comments`)
}

/**
 * 删除评论
 */
export const deleteComment = (commentId) => {
  return del(`/community/comments/${commentId}`)
}

/**
 * 点赞帖子
 */
export const likePost = (postId) => {
  return post(`/community/posts/${postId}/like`)
}

/**
 * 取消点赞
 */
export const unlikePost = (postId) => {
  return del(`/community/posts/${postId}/like`)
}
