-- ============================================
-- 场景演示脚本：账号 007 情绪联动推荐演示
-- 目标：
-- 1. 运行此脚本后，007 的初始推荐应为“职业规划”和“积极心理”相关（中性兴趣）。
-- 2. 用户写完带“焦虑、疲惫”标签的日记后，刷新首页，推荐将变为“睡眠改善”和“社交恐惧应对”。
-- ============================================

USE mental_health;

-- 1. 获取用户 ID
SET @u001 = (SELECT id FROM user WHERE username = 'student001');
SET @u002 = (SELECT id FROM user WHERE username = 'student002');
SET @u003 = (SELECT id FROM user WHERE username = 'student003');
SET @u007 = (SELECT id FROM user WHERE username = 'student007');

-- 2. 获取资源 ID
SET @res_career = (SELECT id FROM education_resource WHERE title = '大学生职业生涯规划');
SET @res_positive = (SELECT id FROM education_resource WHERE title = '积极心理学：发现生活之美');
SET @res_anxiety = (SELECT id FROM education_resource WHERE title = '社交恐惧的克服方法');
SET @res_sleep = (SELECT id FROM education_resource WHERE title = '睡眠改善实用方法');
SET @res_care = (SELECT id FROM education_resource WHERE title = '压力下的自我关怀');
SET @res_time = (SELECT id FROM education_resource WHERE title = '高效时间管理法则');

-- 3. 清理 007 的历史数据，确保实验纯净
DELETE FROM user_resource_rating WHERE user_id = @u007;
DELETE FROM user_interest_tags WHERE user_id = @u007;
DELETE FROM user_behavior_log WHERE user_id = @u007;
DELETE FROM recommendation_result WHERE user_id = @u007;
DELETE FROM emotion_diary WHERE user_id = @u007;

-- 4. 为 001, 002, 003 设置一些基础数据，模拟活跃用户环境
-- 他们都喜欢职业规划和积极心理学
INSERT INTO user_resource_rating (user_id, resource_id, rating_type, score, weight, created_at) VALUES 
(@u001, @res_career, 1, 1.0, 1.0, NOW()), (@u001, @res_positive, 1, 1.0, 1.0, NOW()),
(@u002, @res_career, 1, 1.0, 1.0, NOW()), (@u002, @res_positive, 1, 1.0, 1.0, NOW()),
(@u003, @res_career, 1, 1.0, 1.0, NOW()), (@u003, @res_positive, 1, 1.0, 1.0, NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 5. 设置 007 的“初始兴趣”：模拟他刚看了职业规划和时间管理
INSERT INTO user_resource_rating (user_id, resource_id, rating_type, score, weight, created_at) VALUES 
(@u007, @res_career, 1, 1.0, 1.0, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(@u007, @res_time, 1, 1.0, 1.0, DATE_SUB(NOW(), INTERVAL 1 DAY));

-- 6. 确保资源关键词库中有“焦虑”和“疲惫”
-- 这一步是为了防止 init_recommendation.sql 没执行全
INSERT INTO resource_keywords (resource_id, keyword, tfidf_score, category) VALUES
(@res_anxiety, '焦虑', 0.95, 'problem'),
(@res_anxiety, '社交', 0.85, 'general'),
(@res_sleep, '疲惫', 0.90, 'problem'),
(@res_sleep, '睡眠', 0.95, 'general'),
(@res_care, '疲惫', 0.85, 'problem'),
(@res_care, '自我关怀', 0.90, 'solution')
ON DUPLICATE KEY UPDATE tfidf_score = VALUES(tfidf_score);

-- 7. 强制刷新 007 的初始兴趣标签
-- 这会让他在没写日记前，标签里主要是“职业规划”和“时间管理”
DELETE FROM user_interest_tags WHERE user_id = @u007;
INSERT INTO user_interest_tags (user_id, tag_name, tag_category, interest_score, source, updated_at) VALUES
(@u007, '职业规划', 'general', 90, 'behavior', NOW()),
(@u007, '时间管理', 'general', 85, 'behavior', NOW());

SELECT 'Scenario for 007 initialized. Now 007 should see Career/Positive/Time resources.' as message;
