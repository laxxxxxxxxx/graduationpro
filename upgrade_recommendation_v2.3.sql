-- ============================================
-- 推荐系统 v2.3 升级 — 数据库迁移脚本
-- 执行前提：已执行过 init_database.sql 和 init_recommendation.sql
-- 说明：清理冗余表 + 增强行为日志表索引
-- ============================================

USE mental_health;

-- ============================================
-- 1. 移除冗余表
-- ============================================

-- user_profile 数据已由 user_psychological_profile + user_interest_tags 覆盖
DROP TABLE IF EXISTS `user_profile`;

-- recommendation_cache 已被 recommendation_result（含过期时间）替代
DROP TABLE IF EXISTS `recommendation_cache`;

-- ============================================
-- 2. 为 user_behavior_log 添加新索引
--    用于支持 interest tag 更新时的聚合查询
-- ============================================

-- 先检查索引是否已存在（MySQL 5.7 无 IF NOT EXISTS 语法，通过存储过程安全添加）
DROP PROCEDURE IF EXISTS `add_index_if_missing`;

DELIMITER $$
CREATE PROCEDURE `add_index_if_missing`()
BEGIN
    DECLARE idx_exists INT DEFAULT 0;
    
    SELECT COUNT(*) INTO idx_exists
    FROM information_schema.statistics
    WHERE table_schema = 'mental_health'
      AND table_name = 'user_behavior_log'
      AND index_name = 'idx_user_behavior';
    
    IF idx_exists = 0 THEN
        ALTER TABLE `user_behavior_log` ADD INDEX `idx_user_behavior` (`user_id`, `behavior_type`, `created_at`);
    END IF;
END$$
DELIMITER ;

CALL `add_index_if_missing`();
DROP PROCEDURE IF EXISTS `add_index_if_missing`;

-- ============================================
-- 3. （可选）插入行为日志测试数据
--    如果已有测试用户和资源数据，取消下面注释执行
-- ============================================

-- INSERT IGNORE INTO `user_behavior_log` (`user_id`, `behavior_type`, `target_type`, `target_id`, `duration`, `created_at`) VALUES
-- -- 学生1（计算机，关注学习和压力）
-- ((SELECT id FROM user WHERE username='student001'), 'view', 'resource', (SELECT id FROM education_resource WHERE title='如何应对考试焦虑'), 320, DATE_SUB(NOW(), INTERVAL 3 DAY)),
-- ((SELECT id FROM user WHERE username='student001'), 'click', 'resource', (SELECT id FROM education_resource WHERE title='如何应对考试焦虑'), NULL, DATE_SUB(NOW(), INTERVAL 3 DAY)),
-- ((SELECT id FROM user WHERE username='student001'), 'like', 'resource', (SELECT id FROM education_resource WHERE title='如何应对考试焦虑'), NULL, DATE_SUB(NOW(), INTERVAL 3 DAY)),
-- ((SELECT id FROM user WHERE username='student001'), 'view', 'resource', (SELECT id FROM education_resource WHERE title='时间管理与学习效率'), 480, DATE_SUB(NOW(), INTERVAL 2 DAY)),
-- ((SELECT id FROM user WHERE username='student001'), 'complete', 'resource', (SELECT id FROM education_resource WHERE title='时间管理与学习效率'), NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
-- ((SELECT id FROM user WHERE username='student001'), 'view', 'resource', (SELECT id FROM education_resource WHERE title='正念冥想入门指南'), 180, DATE_SUB(NOW(), INTERVAL 1 DAY)),
-- -- 学生2（心理学，关注情绪和抑郁）
-- ((SELECT id FROM user WHERE username='student002'), 'view', 'resource', (SELECT id FROM education_resource WHERE title='抑郁情绪的识别与应对'), 600, DATE_SUB(NOW(), INTERVAL 5 DAY)),
-- ((SELECT id FROM user WHERE username='student002'), 'like', 'resource', (SELECT id FROM education_resource WHERE title='抑郁情绪的识别与应对'), NULL, DATE_SUB(NOW(), INTERVAL 5 DAY)),
-- ((SELECT id FROM user WHERE username='student002'), 'favorite', 'resource', (SELECT id FROM education_resource WHERE title='抑郁情绪的识别与应对'), NULL, DATE_SUB(NOW(), INTERVAL 5 DAY)),
-- ((SELECT id FROM user WHERE username='student002'), 'view', 'resource', (SELECT id FROM education_resource WHERE title='积极心理学实践'), 250, DATE_SUB(NOW(), INTERVAL 3 DAY)),
-- ((SELECT id FROM user WHERE username='student002'), 'view', 'resource', (SELECT id FROM education_resource WHERE title='情绪管理ABC理论'), 300, DATE_SUB(NOW(), INTERVAL 2 DAY)),
-- -- 学生3（机械，关注职业和恋爱）
-- ((SELECT id FROM user WHERE username='student003'), 'view', 'resource', (SELECT id FROM education_resource WHERE title='职业生涯规划指导'), 400, DATE_SUB(NOW(), INTERVAL 4 DAY)),
-- ((SELECT id FROM user WHERE username='student003'), 'favorite', 'resource', (SELECT id FROM education_resource WHERE title='职业生涯规划指导'), NULL, DATE_SUB(NOW(), INTERVAL 4 DAY)),
-- ((SELECT id FROM user WHERE username='student003'), 'view', 'resource', (SELECT id FROM education_resource WHERE title='人际关系沟通技巧'), 200, DATE_SUB(NOW(), INTERVAL 2 DAY)),
-- ((SELECT id FROM user WHERE username='student003'), 'view', 'resource', (SELECT id FROM education_resource WHERE title='恋爱心理学基础'), 350, DATE_SUB(NOW(), INTERVAL 1 DAY)),
-- -- 学生4（工商，大一新生）
-- ((SELECT id FROM user WHERE username='student004'), 'view', 'resource', (SELECT id FROM education_resource WHERE title='压力下的自我关怀'), 280, DATE_SUB(NOW(), INTERVAL 3 DAY)),
-- ((SELECT id FROM user WHERE username='student004'), 'view', 'resource', (SELECT id FROM education_resource WHERE title='时间管理与学习效率'), 200, DATE_SUB(NOW(), INTERVAL 2 DAY)),
-- ((SELECT id FROM user WHERE username='student004'), 'like', 'resource', (SELECT id FROM education_resource WHERE title='时间管理与学习效率'), NULL, DATE_SUB(NOW(), INTERVAL 2 DAY)),
-- -- 学生5（电子，大四就业）
-- ((SELECT id FROM user WHERE username='student005'), 'view', 'resource', (SELECT id FROM education_resource WHERE title='职业生涯规划指导'), 500, DATE_SUB(NOW(), INTERVAL 6 DAY)),
-- ((SELECT id FROM user WHERE username='student005'), 'like', 'resource', (SELECT id FROM education_resource WHERE title='职业生涯规划指导'), NULL, DATE_SUB(NOW(), INTERVAL 6 DAY)),
-- ((SELECT id FROM user WHERE username='student005'), 'complete', 'resource', (SELECT id FROM education_resource WHERE title='职业生涯规划指导'), NULL, DATE_SUB(NOW(), INTERVAL 5 DAY)),
-- -- 学生6（英语，关注情感和冥想）
-- ((SELECT id FROM user WHERE username='student006'), 'view', 'resource', (SELECT id FROM education_resource WHERE title='恋爱心理学基础'), 450, DATE_SUB(NOW(), INTERVAL 4 DAY)),
-- ((SELECT id FROM user WHERE username='student006'), 'like', 'resource', (SELECT id FROM education_resource WHERE title='恋爱心理学基础'), NULL, DATE_SUB(NOW(), INTERVAL 4 DAY)),
-- ((SELECT id FROM user WHERE username='student006'), 'favorite', 'resource', (SELECT id FROM education_resource WHERE title='恋爱心理学基础'), NULL, DATE_SUB(NOW(), INTERVAL 4 DAY)),
-- ((SELECT id FROM user WHERE username='student006'), 'view', 'resource', (SELECT id FROM education_resource WHERE title='正念冥想入门指南'), 200, DATE_SUB(NOW(), INTERVAL 3 DAY)),
-- ((SELECT id FROM user WHERE username='student006'), 'complete', 'resource', (SELECT id FROM education_resource WHERE title='正念冥想入门指南'), NULL, DATE_SUB(NOW(), INTERVAL 2 DAY)),
-- -- 学生7（数学，关注学习和考试）
-- ((SELECT id FROM user WHERE username='student007'), 'view', 'resource', (SELECT id FROM education_resource WHERE title='时间管理与学习效率'), 350, DATE_SUB(NOW(), INTERVAL 3 DAY)),
-- ((SELECT id FROM user WHERE username='student007'), 'like', 'resource', (SELECT id FROM education_resource WHERE title='时间管理与学习效率'), NULL, DATE_SUB(NOW(), INTERVAL 3 DAY)),
-- ((SELECT id FROM user WHERE username='student007'), 'view', 'resource', (SELECT id FROM education_resource WHERE title='情绪管理ABC理论'), 150, DATE_SUB(NOW(), INTERVAL 1 DAY)),
-- -- 学生8（艺术，关注人际和社交）
-- ((SELECT id FROM user WHERE username='student008'), 'view', 'resource', (SELECT id FROM education_resource WHERE title='人际关系沟通技巧'), 300, DATE_SUB(NOW(), INTERVAL 2 DAY)),
-- ((SELECT id FROM user WHERE username='student008'), 'view', 'resource', (SELECT id FROM education_resource WHERE title='社交恐惧的克服方法'), 250, DATE_SUB(NOW(), INTERVAL 1 DAY));

-- ============================================
-- 完成
-- ============================================
SELECT '推荐系统 v2.3 数据库迁移完成' AS message;
