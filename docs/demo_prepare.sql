-- ============================================
-- 答辩演示方案 - 综合数据重置与增强脚本 (v5.2 终极稳定版)
-- 目标：保证任何时候推荐结果都不少于 10 条（如果总资源够）
-- ============================================

USE mental_health;

-- 1. 彻底清理演示数据
SET @student_usernames = 'student001,student002,student003,student004,student005,student006,student007,student008';

DELETE FROM recommendation_result WHERE user_id IN (SELECT id FROM user WHERE FIND_IN_SET(username, @student_usernames));
DELETE FROM user_resource_rating WHERE user_id IN (SELECT id FROM user WHERE FIND_IN_SET(username, @student_usernames));
DELETE FROM user_behavior_log WHERE user_id IN (SELECT id FROM user WHERE FIND_IN_SET(username, @student_usernames));
DELETE FROM user_assessment WHERE user_id IN (SELECT id FROM user WHERE FIND_IN_SET(username, @student_usernames));
DELETE FROM user_psychological_profile WHERE user_id IN (SELECT id FROM user WHERE FIND_IN_SET(username, @student_usernames));
DELETE FROM user_interest_tags WHERE user_id IN (SELECT id FROM user WHERE FIND_IN_SET(username, @student_usernames));
DELETE FROM emotion_diary WHERE user_id IN (SELECT id FROM user WHERE FIND_IN_SET(username, @student_usernames));
DELETE FROM user_learning_record WHERE user_id IN (SELECT id FROM user WHERE FIND_IN_SET(username, @student_usernames));
DELETE FROM resource_like WHERE user_id IN (SELECT id FROM user WHERE FIND_IN_SET(username, @student_usernames));
DELETE FROM resource_favorite WHERE user_id IN (SELECT id FROM user WHERE FIND_IN_SET(username, @student_usernames));

-- 2. 预设变量
SELECT id INTO @u1 FROM user WHERE username = 'student001';
SELECT id INTO @u2 FROM user WHERE username = 'student002';
SELECT id INTO @u3 FROM user WHERE username = 'student003';
SELECT id INTO @u4 FROM user WHERE username = 'student004';
SELECT id INTO @u5 FROM user WHERE username = 'student005';
SELECT id INTO @u6 FROM user WHERE username = 'student006';
SELECT id INTO @u7 FROM user WHERE username = 'student007';
SELECT id INTO @u8 FROM user WHERE username = 'student008';

SELECT id INTO @r1 FROM education_resource WHERE title = '如何应对考试焦虑';
SELECT id INTO @r2 FROM education_resource WHERE title = '正念冥想入门指南';
SELECT id INTO @r3 FROM education_resource WHERE title = '人际关系沟通技巧';
SELECT id INTO @r4 FROM education_resource WHERE title = '职业生涯规划指导';
SELECT id INTO @r5 FROM education_resource WHERE title = '情绪管理ABC理论';
SELECT id INTO @r6 FROM education_resource WHERE title = '睡眠改善实用方法';
SELECT id INTO @r7 FROM education_resource WHERE title = '抑郁情绪的识别与应对';
SELECT id INTO @r8 FROM education_resource WHERE title = '时间管理与学习效率';
SELECT id INTO @r9 FROM education_resource WHERE title = '恋爱心理学基础';
SELECT id INTO @r10 FROM education_resource WHERE title = '压力下的自我关怀';
SELECT id INTO @r11 FROM education_resource WHERE title = '社交恐惧的克服方法';
SELECT id INTO @r12 FROM education_resource WHERE title = '积极心理学实践';

-- 确保资源状态全部为 1
UPDATE education_resource SET status = 1;

-- ============================================
-- 账号 1-6 按画像填充基本数据 (不进入 rating 表以避免过滤)
-- ============================================
-- [此处省略重复的 1-6 画像填充，保持 v5.1 逻辑，但确保 interest_score 足够高]
INSERT INTO `user_psychological_profile` (`user_id`, `stress_level`, `anxiety_level`, `depression_level`, `dominant_issues`) VALUES
(@u1, 'medium', 'medium', 'low', '学业压力,考试焦虑,时间管理'),
(@u2, 'medium', 'low', 'medium', '情绪困扰,自我价值,抑郁'),
(@u3, 'high', 'medium', 'low', '就业压力,未来规划,职业规划');

-- ============================================
-- 关键：student005 (超级用户) 提供协同信号
-- ============================================
-- 他完成了所有资源，作为所有算法的锚点
INSERT INTO `user_resource_rating` (`user_id`, `resource_id`, `rating_type`, `score`) VALUES
(@u5, @r1, 4, 5.0), (@u5, @r2, 4, 5.0), (@u5, @r3, 4, 5.0), (@u5, @r4, 4, 5.0), (@u5, @r5, 4, 5.0),
(@u5, @r6, 4, 5.0), (@u5, @r7, 4, 5.0), (@u5, @r8, 4, 5.0), (@u5, @r9, 4, 5.0), (@u5, @r10, 4, 5.0),
(@u5, @r11, 4, 5.0), (@u5, @r12, 4, 5.0);

-- ============================================
-- 关键：student007 (演示账号) 的初始状态
-- ============================================
-- 只给他 2 个评分记录（R10, R12），这样他与 student005 有 2 个共同点，激活 User-CF
-- 剩下的 10 个资源全是"未发现"状态，保证推荐列表有足够数据
INSERT INTO `user_resource_rating` (`user_id`, `resource_id`, `rating_type`, `score`) VALUES
(@u7, @r10, 1, 1.0), (@u7, @r12, 1, 1.0);

-- 补充 UI 展示的学习记录
INSERT INTO `user_learning_record` (`user_id`, `resource_id`, `progress`) VALUES (@u7, @r10, 50), (@u7, @r12, 30);

-- ============================================
-- 兜底：调整热门排名
-- ============================================
UPDATE education_resource SET view_count = 1000 WHERE id = @r9;
UPDATE education_resource SET view_count = 800 WHERE id = @r7;
UPDATE education_resource SET view_count = 500 WHERE id = @r5;

-- 强制清除缓存
DELETE FROM recommendation_result;

SELECT 'Defense preparation data reset successfully (v5.2 Robust).' as result;
