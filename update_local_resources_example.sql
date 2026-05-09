-- ============================================
-- 本地资源路径更新示例脚本
-- 使用此脚本前，请确保已将对应的视频/音频文件放入 backend/uploads/ 对应的子目录中
-- ============================================

USE mental_health;

-- 1. 更新视频资源示例 (请确保 backend/uploads/videos/mindfulness.mp4 文件存在)
UPDATE education_resource SET 
media_url = '/uploads/videos/mindfulness.mp4',
cover_url = '/uploads/images/mindfulness_cover.jpg'
WHERE title = '正念冥想入门指南';

-- 2. 更新音频资源示例 (请确保 backend/uploads/audios/breathing.mp3 文件存在)
UPDATE education_resource SET 
media_url = '/uploads/audios/breathing.mp3',
cover_url = '/uploads/images/breathing_cover.jpg'
WHERE title = '深呼吸放松技巧';

-- 3. 更新课程资源示例 (课程通常包含视频和文章内容)
UPDATE education_resource SET 
media_url = '/uploads/videos/emotion_abc.mp4',
cover_url = '/uploads/images/emotion_cover.jpg'
WHERE title = '情绪管理ABC理论';

-- 4. 确认更新
SELECT id, title, type, media_url FROM education_resource WHERE media_url LIKE '/uploads/%';
