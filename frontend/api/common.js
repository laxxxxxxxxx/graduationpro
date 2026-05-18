/**
 * 公共API
 */
import { upload } from '@/utils/request'

/**
 * 上传文件
 * @param {String} filePath 本地文件路径
 * @param {String} type 类型: image/video/audio/common
 */
export const uploadFile = (filePath, type = 'common') => {
  return upload('/file/upload', filePath, { type })
}
