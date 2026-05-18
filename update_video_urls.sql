-- ============================================
-- 修复视频资源路径 SQL 脚本 (最终版)
-- 同时更新 media_url 和 content 字段，确保前端兼容性
-- ============================================

USE mental_health;

-- 1. 正念冥想入门指南
UPDATE education_resource SET 
media_url = '/uploads/videos/mindfulness.mp4',
content = '正念冥想入门指南视频'
WHERE title = '正念冥想入门指南';

-- 2. 情绪管理ABC理论
UPDATE education_resource SET 
media_url = '/uploads/videos/emotion_abc.mp4',
content = '情绪管理ABC理论视频'
WHERE title = '情绪管理ABC理论';

-- 3. 抑郁情绪的识别与应对
UPDATE education_resource SET 
media_url = '/uploads/videos/breathing.mp4',
content = '抑郁情绪的识别与应对视频'
WHERE title = '抑郁情绪的识别与应对';

-- 4. 恋爱心理学基础
UPDATE education_resource SET 
media_url = '/uploads/videos/mindfulness.mp4',
content = '恋爱心理学基础视频'
WHERE title = '恋爱心理学基础';

-- 5. 社交恐惧的克服方法
UPDATE education_resource SET 
media_url = '/uploads/videos/breathing.mp4',
content = '社交恐惧的克服方法视频'
WHERE title = '社交恐惧的克服方法';

SELECT '视频路径修复成功！' AS message;
