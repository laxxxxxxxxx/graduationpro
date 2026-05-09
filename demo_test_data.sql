-- ============================================
-- 推荐算法演示 - 补充测试数据
-- 用于答辩演示不同推荐场景
-- 使用前先执行: source init_recommendation.sql
-- ============================================

USE mental_health;

-- ============================================
-- 场景0：纯冷启动用户（无任何数据）
-- ============================================
-- student009 不插入任何测评/评分/标签/画像数据
-- 仅注册账号，推荐系统只能用热门资源兜底
INSERT IGNORE INTO `user` (`username`, `password`, `email`, `real_name`, `gender`, `age`, `university`, `major`, `grade`, `role`, `status`) VALUES
('student009', '$2a$10$sm3dp1Yq8uNE5S/v17FtkuU08NlPihHC9nbl/OwCgMys8qiQl9zQC', 's009@test.com', '冷启动用户', 1, 19, '合肥工业大学', '物理', '大一', 1, 1);

-- ============================================
-- 场景1：刚做完测评的新用户（有画像无行为）
-- ============================================
-- student010 做完SDS测评（重度抑郁）+ 有心理画像，但无任何浏览行为
INSERT IGNORE INTO `user` (`username`, `password`, `email`, `real_name`, `gender`, `age`, `university`, `major`, `grade`, `role`, `status`) VALUES
('student010', '$2a$10$sm3dp1Yq8uNE5S/v17FtkuU08NlPihHC9nbl/OwCgMys8qiQl9zQC', 's010@test.com', '测评新用户', 0, 20, '合肥工业大学', '临床医学', '大二', 1, 1);

-- 测评记录：重度抑郁
INSERT INTO `user_assessment` (`user_id`, `scale_id`, `total_score`, `result_level`, `interpretation`, `suggestions`, `answers`, `completion_time`) VALUES
((SELECT id FROM user WHERE username='student010'), 1, 78.00, '重度', '存在重度抑郁症状，建议尽快就医', '建议立即预约精神科医生评估，考虑药物治疗，寻求专业心理咨询', '[{"questionId":1,"answer":4},{"questionId":2,"answer":4}]', 900);

-- 心理画像
INSERT INTO `user_psychological_profile` (`user_id`, `stress_level`, `anxiety_level`, `depression_level`, `dominant_issues`, `coping_style`, `last_assessment_date`) VALUES
((SELECT id FROM user WHERE username='student010'), 'high', 'medium', 'high', '抑郁情绪,失眠,兴趣丧失', 'passive', CURDATE());

-- 兴趣标签（测评自动写入）
INSERT INTO `user_interest_tags` (`user_id`, `tag_name`, `tag_category`, `interest_score`, `source`) VALUES
((SELECT id FROM user WHERE username='student010'), '抑郁调适', 'depression', 85.00, 'assessment'),
((SELECT id FROM user WHERE username='student010'), '情绪管理', 'emotion', 80.00, 'assessment'),
((SELECT id FROM user WHERE username='student010'), '认知疗法', 'therapy', 75.00, 'assessment'),
((SELECT id FROM user WHERE username='student010'), '自我关怀', 'self_care', 78.00, 'assessment');

-- ============================================
-- 场景2：就业焦虑用户（大四，主要关注就业）
-- ============================================
-- student011 就业焦虑 + 有大量职业规划类浏览
INSERT IGNORE INTO `user` (`username`, `password`, `email`, `real_name`, `gender`, `age`, `university`, `major`, `grade`, `role`, `status`) VALUES
('student011', '$2a$10$sm3dp1Yq8uNE5S/v17FtkuU08NlPihHC9nbl/OwCgMys8qiQl9zQC', 's011@test.com', '就业焦虑用户', 1, 22, '合肥工业大学', '土木工程', '大四', 1, 1);

-- 测评：中高度压力
INSERT INTO `user_assessment` (`user_id`, `scale_id`, `total_score`, `result_level`, `interpretation`, `suggestions`, `answers`, `completion_time`) VALUES
((SELECT id FROM user WHERE username='student011'), 2, 195.00, '中度', '存在中度焦虑症状，以就业焦虑为主', '建议进行职业规划咨询，学习面试技巧，减轻就业压力', '[{"questionId":1,"answer":3}]', 800);

-- 心理画像
INSERT INTO `user_psychological_profile` (`user_id`, `stress_level`, `anxiety_level`, `depression_level`, `dominant_issues`, `coping_style`, `last_assessment_date`) VALUES
((SELECT id FROM user WHERE username='student011'), 'high', 'medium', 'low', '就业压力,未来规划,面试焦虑', 'active', CURDATE());

-- 兴趣标签
INSERT INTO `user_interest_tags` (`user_id`, `tag_name`, `tag_category`, `interest_score`, `source`) VALUES
((SELECT id FROM user WHERE username='student011'), '职业规划', 'career', 92.00, 'assessment'),
((SELECT id FROM user WHERE username='student011'), '就业指导', 'career', 88.00, 'assessment'),
((SELECT id FROM user WHERE username='student011'), '压力管理', 'stress', 75.00, 'assessment'),
((SELECT id FROM user WHERE username='student011'), '职业发展', 'career', 85.00, 'assessment');

-- 大量浏览职业相关资源（丰富的行为数据）
INSERT INTO `user_resource_rating` (`user_id`, `resource_id`, `rating_type`, `score`, `weight`) VALUES
((SELECT id FROM user WHERE username='student011'), (SELECT id FROM education_resource WHERE title='职业生涯规划指导'), 1, 1.00, 1.00),
((SELECT id FROM user WHERE username='student011'), (SELECT id FROM education_resource WHERE title='职业生涯规划指导'), 2, 3.00, 1.00),
((SELECT id FROM user WHERE username='student011'), (SELECT id FROM education_resource WHERE title='职业生涯规划指导'), 3, 4.00, 1.00),
((SELECT id FROM user WHERE username='student011'), (SELECT id FROM education_resource WHERE title='职业生涯规划指导'), 4, 5.00, 1.00),
((SELECT id FROM user WHERE username='student011'), (SELECT id FROM education_resource WHERE title='人际关系沟通技巧'), 1, 1.00, 0.95),
((SELECT id FROM user WHERE username='student011'), (SELECT id FROM education_resource WHERE title='人际关系沟通技巧'), 2, 3.00, 0.95),
((SELECT id FROM user WHERE username='student011'), (SELECT id FROM education_resource WHERE title='如何应对考试焦虑'), 1, 1.00, 0.88),
((SELECT id FROM user WHERE username='student011'), (SELECT id FROM education_resource WHERE title='情绪管理ABC理论'), 1, 1.00, 0.85),
((SELECT id FROM user WHERE username='student011'), (SELECT id FROM education_resource WHERE title='时间管理与学习效率'), 1, 1.00, 0.80);

-- ============================================
-- 场景3：有行为但没做测评的用户
-- ============================================
-- student012 只浏览资源，没有心理测评
INSERT IGNORE INTO `user` (`username`, `password`, `email`, `real_name`, `gender`, `age`, `university`, `major`, `grade`, `role`, `status`) VALUES
('student012', '$2a$10$sm3dp1Yq8uNE5S/v17FtkuU08NlPihHC9nbl/OwCgMys8qiQl9zQC', 's012@test.com', '无测评用户', 0, 20, '合肥工业大学', '新闻传播', '大三', 1, 1);

-- 只有浏览行为，无测评数据
INSERT INTO `user_resource_rating` (`user_id`, `resource_id`, `rating_type`, `score`, `weight`) VALUES
((SELECT id FROM user WHERE username='student012'), (SELECT id FROM education_resource WHERE title='正念冥想入门指南'), 1, 1.00, 1.00),
((SELECT id FROM user WHERE username='student012'), (SELECT id FROM education_resource WHERE title='正念冥想入门指南'), 2, 3.00, 1.00),
((SELECT id FROM user WHERE username='student012'), (SELECT id FROM education_resource WHERE title='恋爱心理学基础'), 1, 1.00, 0.95),
((SELECT id FROM user WHERE username='student012'), (SELECT id FROM education_resource WHERE title='恋爱心理学基础'), 2, 3.00, 0.95),
((SELECT id FROM user WHERE username='student012'), (SELECT id FROM education_resource WHERE title='恋爱心理学基础'), 3, 4.00, 0.95),
((SELECT id FROM user WHERE username='student012'), (SELECT id FROM education_resource WHERE title='积极心理学实践'), 1, 1.00, 0.90),
((SELECT id FROM user WHERE username='student012'), (SELECT id FROM education_resource WHERE title='社交恐惧的克服方法'), 1, 1.00, 0.85);

-- 手工刷新后的兴趣标签
INSERT INTO `user_interest_tags` (`user_id`, `tag_name`, `tag_category`, `interest_score`, `source`) VALUES
((SELECT id FROM user WHERE username='student012'), '正念冥想', 'relaxation', 78.00, 'behavior'),
((SELECT id FROM user WHERE username='student012'), '亲密关系', 'relationship', 82.00, 'behavior');

-- ============================================
-- 补充：更新已有资源关键词，增加就业/学业/压力关键词覆盖
-- ============================================
INSERT IGNORE INTO `resource_keywords` (`resource_id`, `keyword`, `tfidf_score`, `category`) VALUES
-- 时间管理这个资源也关联"学业压力"
((SELECT id FROM education_resource WHERE title='时间管理与学习效率'), '学业压力', 0.70, 'problem'),
-- 社交恐惧也关联"人际交往"
((SELECT id FROM education_resource WHERE title='社交恐惧的克服方法'), '人际交往', 0.75, 'problem'),
-- 睡眠改善也关联"放松"
((SELECT id FROM education_resource WHERE title='睡眠改善实用方法'), '放松', 0.72, 'solution'),
-- 压力下的自我关怀链接就业相关（对就业压力者）
((SELECT id FROM education_resource WHERE title='压力下的自我关怀'), '就业', 0.45, 'problem');

-- ============================================
-- 清空所有推荐缓存（让每次演示都是实时计算）
-- ============================================
DELETE FROM recommendation_result;

-- ============================================
-- 验证数据
-- ============================================
SELECT '========== 用户统计 ==========' AS '';
SELECT 
    u.username,
    u.real_name,
    CASE WHEN upp.user_id IS NOT NULL THEN 'YES' ELSE 'NO' END AS has_profile,
    COUNT(DISTINCT urr.id) AS behavior_count,
    COUNT(DISTINCT uit.id) AS tag_count
FROM user u
LEFT JOIN user_psychological_profile upp ON u.id = upp.user_id
LEFT JOIN user_resource_rating urr ON u.id = urr.user_id
LEFT JOIN user_interest_tags uit ON u.id = uit.user_id
WHERE u.username LIKE 'student%'
GROUP BY u.id, u.username, u.real_name, upp.user_id
ORDER BY u.username;
