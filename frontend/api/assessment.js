/**
 * 测评评估API
 */
import { get, post } from '@/utils/request'

/**
 * 获取量表列表
 */
export const getScaleList = () => {
  return get('/assessment/scales')
}

/**
 * 获取量表题目
 */
export const getScaleQuestions = (scaleId) => {
  return get(`/assessment/scales/${scaleId}/questions`)
}

/**
 * 提交答卷
 */
export const submitAssessment = (data) => {
  return post('/assessment/submit', data)
}

/**
 * 获取测评历史
 */
export const getAssessmentHistory = () => {
  return get('/assessment/history')
}

/**
 * 获取单次测评完整报告
 */
export const getReportDetail = (id) => {
  return get(`/assessment/report/${id}`)
}

/**
 * 获取测评档案(含趋势分析)
 */
export const getAssessmentReports = () => {
  return get('/assessment/reports')
}
