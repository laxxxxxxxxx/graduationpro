-- ============================================
-- 个性化推荐模块 - 数据库表结构
-- ============================================

USE mental_health;

-- ============================================
-- 1. 用户行为记录表（用于协同过滤）
-- ============================================

-- 用户资源评分表（隐式反馈）
CREATE TABLE IF NOT EXISTS `user_resource_rating` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `resource_id` BIGINT NOT NULL COMMENT '资源ID',
    `rating_type` TINYINT NOT NULL COMMENT '评分类型: 1-浏览 2-点赞 3-收藏 4-完成学习 5-分享',
    `score` DECIMAL(3,2) DEFAULT 0 COMMENT '评分值(0-5): 浏览=1, 点赞=3, 收藏=4, 完成=5, 分享=5',
    `weight` DECIMAL(3,2) DEFAULT 1.0 COMMENT '权重(考虑时间衰减)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '行为时间',
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`resource_id`) REFERENCES `education_resource`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_user_resource_type` (`user_id`, `resource_id`, `rating_type`),
    INDEX `idx_user` (`user_id`),
    INDEX `idx_resource` (`resource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户资源评分表';

-- ============================================
-- 2. 用户画像表
-- ============================================

-- 用户兴趣标签表
CREATE TABLE IF NOT EXISTS `user_interest_tags` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `tag_name` VARCHAR(50) NOT NULL COMMENT '标签名称',
    `tag_category` VARCHAR(50) COMMENT '标签分类: emotion/stress/relationship/career等',
    `interest_score` DECIMAL(5,2) DEFAULT 0 COMMENT '兴趣度(0-100)',
    `source` VARCHAR(50) COMMENT '来源: assessment/diary/behavior',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_user_tag` (`user_id`, `tag_name`),
    INDEX `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户兴趣标签表';

-- 用户心理特征表
CREATE TABLE IF NOT EXISTS `user_psychological_profile` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `stress_level` VARCHAR(20) COMMENT '压力水平: low/medium/high',
    `anxiety_level` VARCHAR(20) COMMENT '焦虑水平',
    `depression_level` VARCHAR(20) COMMENT '抑郁水平',
    `dominant_issues` VARCHAR(200) COMMENT '主要问题: 学业压力/人际困扰/情感问题等',
    `personality_traits` VARCHAR(200) COMMENT '性格特征',
    `coping_style` VARCHAR(50) COMMENT '应对方式: active/passive',
    `last_assessment_date` DATE COMMENT '最后测评日期',
    `profile_version` INT DEFAULT 1 COMMENT '画像版本',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户心理特征表';

-- ============================================
-- 3. 推荐结果表
-- ============================================

-- 个性化推荐结果表
CREATE TABLE IF NOT EXISTS `recommendation_result` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `resource_id` BIGINT NOT NULL COMMENT '推荐资源ID',
    `algorithm_type` VARCHAR(50) NOT NULL COMMENT '算法类型: collaborative/content_based/hybrid',
    `confidence_score` DECIMAL(5,2) COMMENT '置信度(0-100)',
    `reason` VARCHAR(200) COMMENT '推荐理由',
    `display_order` INT COMMENT '展示顺序',
    `is_clicked` TINYINT DEFAULT 0 COMMENT '是否点击: 0-否 1-是',
    `is_effective` TINYINT DEFAULT 0 COMMENT '是否有效推荐: 0-否 1-是',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
    `expired_at` DATETIME COMMENT '过期时间',
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`resource_id`) REFERENCES `education_resource`(`id`) ON DELETE CASCADE,
    INDEX `idx_user_order` (`user_id`, `display_order`),
    INDEX `idx_expired` (`expired_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='个性化推荐结果表';

-- ============================================
-- 4. 资源特征表（用于基于内容推荐）
-- ============================================

-- 资源关键词表
CREATE TABLE IF NOT EXISTS `resource_keywords` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `resource_id` BIGINT NOT NULL COMMENT '资源ID',
    `keyword` VARCHAR(50) NOT NULL COMMENT '关键词',
    `tfidf_score` DECIMAL(5,2) COMMENT 'TF-IDF分数',
    `category` VARCHAR(50) COMMENT '关键词类别: emotion/problem/solution等',
    FOREIGN KEY (`resource_id`) REFERENCES `education_resource`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_resource_keyword` (`resource_id`, `keyword`),
    INDEX `idx_keyword` (`keyword`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源关键词表';

-- ============================================
-- 5. 插入测试数据
-- ============================================

-- 删除旧的测试用户（避免重复）
DELETE FROM user WHERE username LIKE 'student%';

-- 插入更多测试用户（不同专业年级）
-- 注意：需要记录插入后的实际ID，后续使用变量或子查询引用
INSERT INTO `user` (`username`, `password`, `email`, `real_name`, `gender`, `age`, `university`, `major`, `grade`, `role`, `status`) VALUES
('student001', '$2a$10$kLjeA73KcQLFQBgfft8lP.0e01jo1zwtq4r4F.cO9wHugjahmhVce', 's001@test.com', '张三', 1, 20, '合肥工业大学', '计算机科学与技术', '大三', 1, 1),
('student002', '$2a$10$kLjeA73KcQLFQBgfft8lP.0e01jo1zwtq4r4F.cO9wHugjahmhVce', 's002@test.com', '李四', 0, 19, '合肥工业大学', '心理学', '大二', 1, 1),
('student003', '$2a$10$kLjeA73KcQLFQBgfft8lP.0e01jo1zwtq4r4F.cO9wHugjahmhVce', 's003@test.com', '王五', 1, 21, '合肥工业大学', '机械工程', '大四', 1, 1),
('student004', '$2a$10$kLjeA73KcQLFQBgfft8lP.0e01jo1zwtq4r4F.cO9wHugjahmhVce', 's004@test.com', '赵六', 0, 18, '合肥工业大学', '工商管理', '大一', 1, 1),
('student005', '$2a$10$kLjeA73KcQLFQBgfft8lP.0e01jo1zwtq4r4F.cO9wHugjahmhVce', 's005@test.com', '孙七', 1, 22, '合肥工业大学', '电子信息', '大四', 1, 1),
('student006', '$2a$10$kLjeA73KcQLFQBgfft8lP.0e01jo1zwtq4r4F.cO9wHugjahmhVce', 's006@test.com', '周八', 0, 20, '合肥工业大学', '英语', '大三', 1, 1),
('student007', '$2a$10$kLjeA73KcQLFQBgfft8lP.0e01jo1zwtq4r4F.cO9wHugjahmhVce', 's007@test.com', '吴九', 1, 19, '合肥工业大学', '数学', '大二', 1, 1),
('student008', '$2a$10$kLjeA73KcQLFQBgfft8lP.0e01jo1zwtq4r4F.cO9wHugjahmhVce', 's008@test.com', '郑十', 0, 21, '合肥工业大学', '艺术设计', '大三', 1, 1);

-- 重置user表的AUTO_INCREMENT，确保ID从1开始（如果表是空的）
-- 注意：如果表中已有数据（如admin），这不会影响已有数据
SET @new_user_id_start = (SELECT IFNULL(MAX(id), 0) + 1 FROM user);

-- 清空现有资源数据并重置自增
DELETE FROM resource_keywords;
DELETE FROM education_resource;
ALTER TABLE education_resource AUTO_INCREMENT = 1;

-- 插入测试教育资源（不同主题和类型）
-- 注意：这些资源的ID将从1开始连续分配（因为上面重置了AUTO_INCREMENT）
INSERT INTO `education_resource` (`title`, `type`, `category_id`, `cover_url`, `content`, `duration`, `difficulty`, `tags`, `author`, `source`, `copyright_status`, `view_count`, `like_count`, `status`, `publish_time`) VALUES
('如何应对考试焦虑', 1, 1, NULL, '<p>考试焦虑是大学生常见的心理问题...</p>', NULL, 1, '考试焦虑,压力管理,放松技巧', '心理咨询师A', '原创', 1, 150, 45, 1, NOW()),
('正念冥想入门指南', 2, 2, NULL, 'https://video.example.com/mindfulness.mp4', 600, 1, '正念,冥想,放松,减压', '冥想导师B', '合作', 1, 280, 89, 1, NOW()),
('人际关系沟通技巧', 1, 3, NULL, '<p>良好的人际关系对心理健康至关重要...</p>', NULL, 2, '人际交往,沟通技巧,社交', '心理学教授C', '原创', 1, 200, 67, 1, NOW()),
('职业生涯规划指导', 1, 4, NULL, '<p>大学期间如何进行职业规划...</p>', NULL, 2, '职业规划,就业指导,未来发展', '职业咨询师D', '原创', 1, 320, 98, 1, NOW()),
('情绪管理ABC理论', 2, 1, NULL, 'https://video.example.com/emotion-abc.mp4', 900, 2, '情绪管理,认知疗法,ABC理论', '心理学专家E', '合作', 1, 410, 125, 1, NOW()),
('睡眠改善实用方法', 1, 2, NULL, '<p>良好的睡眠对心理健康的影响...</p>', NULL, 1, '睡眠,健康,生活习惯', '睡眠专家F', '原创', 1, 190, 56, 1, NOW()),
('抑郁情绪的识别与应对', 2, 1, NULL, 'https://video.example.com/depression.mp4', 1200, 3, '抑郁,情绪识别,自我调节', '临床心理学家G', '合作', 1, 520, 178, 1, NOW()),
('时间管理与学习效率', 1, 4, NULL, '<p>如何合理安排时间提高学习效率...</p>', NULL, 1, '时间管理,学习效率,自律', '学习顾问H', '原创', 1, 240, 72, 1, NOW()),
('恋爱心理学基础', 2, 3, NULL, 'https://video.example.com/love.mp4', 1500, 2, '恋爱,情感,亲密关系', '情感咨询师I', '合作', 1, 680, 234, 1, NOW()),
('压力下的自我关怀', 1, 2, NULL, '<p>在压力下如何照顾好自己...</p>', NULL, 1, '自我关怀,压力应对,心理健康', '心理咨询师J', '原创', 1, 175, 51, 1, NOW()),
('社交恐惧的克服方法', 2, 3, NULL, 'https://video.example.com/social-anxiety.mp4', 1000, 2, '社交恐惧,焦虑,暴露疗法', '心理治疗师K', '合作', 1, 390, 115, 1, NOW()),
('积极心理学实践', 1, 1, NULL, '<p>培养积极心态的方法...</p>', NULL, 2, '积极心理学,幸福感,乐观', '积极心理学专家L', '原创', 1, 310, 92, 1, NOW());

-- 插入用户测评记录（不同结果）
-- 使用子查询动态获取用户ID，避免硬编码导致的 FK 约束错误
INSERT INTO `user_assessment` (`user_id`, `scale_id`, `total_score`, `result_level`, `interpretation`, `suggestions`, `answers`, `completion_time`) VALUES
((SELECT id FROM user WHERE username='student001'), 1, 45.00, '轻度', '存在轻度抑郁倾向，建议关注情绪变化', '建议进行正念练习，保持规律作息，必要时寻求专业帮助', '[{"questionId":1,"answer":2},{"questionId":2,"answer":3}]', 600),
((SELECT id FROM user WHERE username='student002'), 1, 62.00, '中度', '存在中度抑郁症状，建议及时干预', '建议寻求心理咨询，参加支持小组，学习情绪调节技巧', '[{"questionId":1,"answer":3},{"questionId":2,"answer":4}]', 720),
((SELECT id FROM user WHERE username='student003'), 2, 180.00, '中度', '存在中度焦虑症状', '建议学习放松技巧，进行认知重构，必要时就医', '[{"questionId":1,"answer":3}]', 900),
((SELECT id FROM user WHERE username='student004'), 1, 35.00, '正常', '心理健康状况良好', '继续保持积极的生活方式', '[{"questionId":1,"answer":1}]', 480),
((SELECT id FROM user WHERE username='student005'), 3, 25.00, '轻度', '存在轻度适应问题', '建议加强社交活动，寻求同伴支持', '[{"questionId":1,"answer":2}]', 540),
((SELECT id FROM user WHERE username='student006'), 2, 145.00, '轻度', '存在轻度焦虑', '建议进行放松训练，调整期望值', '[{"questionId":1,"answer":2}]', 660);

-- 插入用户行为记录（浏览、点赞、收藏等）
-- 注意：使用子查询动态获取资源ID，避免硬编码导致的 FK 约束错误
INSERT INTO `user_resource_rating` (`user_id`, `resource_id`, `rating_type`, `score`, `weight`) VALUES
-- 学生1（计算机，关注学习和压力）
((SELECT id FROM user WHERE username='student001'), (SELECT id FROM education_resource WHERE title='如何应对考试焦虑'), 1, 1.00, 1.00),
((SELECT id FROM user WHERE username='student001'), (SELECT id FROM education_resource WHERE title='如何应对考试焦虑'), 2, 3.00, 1.00),
((SELECT id FROM user WHERE username='student001'), (SELECT id FROM education_resource WHERE title='时间管理与学习效率'), 1, 1.00, 1.00),
((SELECT id FROM user WHERE username='student001'), (SELECT id FROM education_resource WHERE title='时间管理与学习效率'), 4, 5.00, 1.00),
((SELECT id FROM user WHERE username='student001'), (SELECT id FROM education_resource WHERE title='情绪管理ABC理论'), 1, 1.00, 0.95),

-- 学生2（心理学，关注专业和情绪）
((SELECT id FROM user WHERE username='student002'), (SELECT id FROM education_resource WHERE title='抑郁情绪的识别与应对'), 1, 1.00, 1.00),
((SELECT id FROM user WHERE username='student002'), (SELECT id FROM education_resource WHERE title='抑郁情绪的识别与应对'), 2, 3.00, 1.00),
((SELECT id FROM user WHERE username='student002'), (SELECT id FROM education_resource WHERE title='抑郁情绪的识别与应对'), 3, 4.00, 1.00),
((SELECT id FROM user WHERE username='student002'), (SELECT id FROM education_resource WHERE title='情绪管理ABC理论'), 1, 1.00, 1.00),
((SELECT id FROM user WHERE username='student002'), (SELECT id FROM education_resource WHERE title='积极心理学实践'), 1, 1.00, 0.98),
((SELECT id FROM user WHERE username='student002'), (SELECT id FROM education_resource WHERE title='积极心理学实践'), 2, 3.00, 0.98),

-- 学生3（机械，关注职业和人际）
((SELECT id FROM user WHERE username='student003'), (SELECT id FROM education_resource WHERE title='职业生涯规划指导'), 1, 1.00, 1.00),
((SELECT id FROM user WHERE username='student003'), (SELECT id FROM education_resource WHERE title='职业生涯规划指导'), 3, 4.00, 1.00),
((SELECT id FROM user WHERE username='student003'), (SELECT id FROM education_resource WHERE title='人际关系沟通技巧'), 1, 1.00, 1.00),
((SELECT id FROM user WHERE username='student003'), (SELECT id FROM education_resource WHERE title='恋爱心理学基础'), 1, 1.00, 0.90),

-- 学生4（工商，大一新生，关注适应）
((SELECT id FROM user WHERE username='student004'), (SELECT id FROM education_resource WHERE title='压力下的自我关怀'), 1, 1.00, 1.00),
((SELECT id FROM user WHERE username='student004'), (SELECT id FROM education_resource WHERE title='时间管理与学习效率'), 1, 1.00, 1.00),
((SELECT id FROM user WHERE username='student004'), (SELECT id FROM education_resource WHERE title='时间管理与学习效率'), 2, 3.00, 1.00),
((SELECT id FROM user WHERE username='student004'), (SELECT id FROM education_resource WHERE title='社交恐惧的克服方法'), 1, 1.00, 0.85),

-- 学生5（电子，大四，关注就业）
((SELECT id FROM user WHERE username='student005'), (SELECT id FROM education_resource WHERE title='职业生涯规划指导'), 1, 1.00, 1.00),
((SELECT id FROM user WHERE username='student005'), (SELECT id FROM education_resource WHERE title='职业生涯规划指导'), 2, 3.00, 1.00),
((SELECT id FROM user WHERE username='student005'), (SELECT id FROM education_resource WHERE title='职业生涯规划指导'), 4, 5.00, 1.00),
((SELECT id FROM user WHERE username='student005'), (SELECT id FROM education_resource WHERE title='如何应对考试焦虑'), 1, 1.00, 0.92),

-- 学生6（英语，关注情感和放松）
((SELECT id FROM user WHERE username='student006'), (SELECT id FROM education_resource WHERE title='恋爱心理学基础'), 1, 1.00, 1.00),
((SELECT id FROM user WHERE username='student006'), (SELECT id FROM education_resource WHERE title='恋爱心理学基础'), 2, 3.00, 1.00),
((SELECT id FROM user WHERE username='student006'), (SELECT id FROM education_resource WHERE title='恋爱心理学基础'), 3, 4.00, 1.00),
((SELECT id FROM user WHERE username='student006'), (SELECT id FROM education_resource WHERE title='正念冥想入门指南'), 1, 1.00, 1.00),
((SELECT id FROM user WHERE username='student006'), (SELECT id FROM education_resource WHERE title='正念冥想入门指南'), 4, 5.00, 1.00),

-- 学生7（数学，关注学习）
((SELECT id FROM user WHERE username='student007'), (SELECT id FROM education_resource WHERE title='时间管理与学习效率'), 1, 1.00, 1.00),
((SELECT id FROM user WHERE username='student007'), (SELECT id FROM education_resource WHERE title='时间管理与学习效率'), 2, 3.00, 1.00),
((SELECT id FROM user WHERE username='student007'), (SELECT id FROM education_resource WHERE title='如何应对考试焦虑'), 1, 1.00, 0.88),
((SELECT id FROM user WHERE username='student007'), (SELECT id FROM education_resource WHERE title='情绪管理ABC理论'), 1, 1.00, 0.85);

-- 插入用户兴趣标签
INSERT INTO `user_interest_tags` (`user_id`, `tag_name`, `tag_category`, `interest_score`, `source`) VALUES
((SELECT id FROM user WHERE username='student001'), '压力管理', 'stress', 85.00, 'assessment'),
((SELECT id FROM user WHERE username='student001'), '学习效率', 'study', 78.00, 'behavior'),
((SELECT id FROM user WHERE username='student001'), '考试焦虑', 'anxiety', 72.00, 'assessment'),
((SELECT id FROM user WHERE username='student002'), '抑郁干预', 'depression', 90.00, 'assessment'),
((SELECT id FROM user WHERE username='student002'), '情绪调节', 'emotion', 82.00, 'behavior'),
((SELECT id FROM user WHERE username='student002'), '积极心理学', 'positive', 75.00, 'behavior'),
((SELECT id FROM user WHERE username='student003'), '职业发展', 'career', 88.00, 'behavior'),
((SELECT id FROM user WHERE username='student003'), '人际关系', 'relationship', 70.00, 'behavior'),
((SELECT id FROM user WHERE username='student004'), '时间管理', 'study', 80.00, 'behavior'),
((SELECT id FROM user WHERE username='student004'), '自我关怀', 'self_care', 76.00, 'behavior'),
((SELECT id FROM user WHERE username='student004'), '社交技能', 'relationship', 65.00, 'assessment'),
((SELECT id FROM user WHERE username='student005'), '职业规划', 'career', 92.00, 'behavior'),
((SELECT id FROM user WHERE username='student005'), '压力应对', 'stress', 68.00, 'assessment'),
((SELECT id FROM user WHERE username='student006'), '亲密关系', 'relationship', 86.00, 'behavior'),
((SELECT id FROM user WHERE username='student006'), '正念冥想', 'relaxation', 81.00, 'behavior');

-- 插入用户心理特征
INSERT INTO `user_psychological_profile` (`user_id`, `stress_level`, `anxiety_level`, `depression_level`, `dominant_issues`, `coping_style`, `last_assessment_date`) VALUES
((SELECT id FROM user WHERE username='student001'), 'medium', 'medium', 'low', '学业压力,考试焦虑', 'active', CURDATE()),
((SELECT id FROM user WHERE username='student002'), 'medium', 'low', 'medium', '情绪困扰,自我价值', 'passive', CURDATE()),
((SELECT id FROM user WHERE username='student003'), 'high', 'medium', 'low', '就业压力,未来规划', 'active', CURDATE()),
((SELECT id FROM user WHERE username='student004'), 'low', 'low', 'low', '适应问题,时间管理', 'active', CURDATE()),
((SELECT id FROM user WHERE username='student005'), 'medium', 'low', 'low', '职业发展,目标设定', 'active', CURDATE()),
((SELECT id FROM user WHERE username='student006'), 'low', 'medium', 'low', '情感困扰,人际关系', 'passive', CURDATE());

-- 插入资源关键词（用于基于内容推荐）
-- 使用子查询动态获取资源ID
INSERT INTO `resource_keywords` (`resource_id`, `keyword`, `tfidf_score`, `category`) VALUES
((SELECT id FROM education_resource WHERE title='如何应对考试焦虑'), '考试焦虑', 0.85, 'problem'),
((SELECT id FROM education_resource WHERE title='如何应对考试焦虑'), '压力管理', 0.78, 'solution'),
((SELECT id FROM education_resource WHERE title='如何应对考试焦虑'), '放松技巧', 0.72, 'solution'),
((SELECT id FROM education_resource WHERE title='正念冥想入门指南'), '正念', 0.90, 'solution'),
((SELECT id FROM education_resource WHERE title='正念冥想入门指南'), '冥想', 0.88, 'solution'),
((SELECT id FROM education_resource WHERE title='正念冥想入门指南'), '减压', 0.75, 'solution'),
((SELECT id FROM education_resource WHERE title='人际关系沟通技巧'), '人际交往', 0.82, 'problem'),
((SELECT id FROM education_resource WHERE title='人际关系沟通技巧'), '沟通技巧', 0.80, 'solution'),
((SELECT id FROM education_resource WHERE title='职业生涯规划指导'), '职业规划', 0.92, 'solution'),
((SELECT id FROM education_resource WHERE title='职业生涯规划指导'), '就业指导', 0.85, 'solution'),
((SELECT id FROM education_resource WHERE title='情绪管理ABC理论'), '情绪管理', 0.88, 'solution'),
((SELECT id FROM education_resource WHERE title='情绪管理ABC理论'), '认知疗法', 0.76, 'solution'),
((SELECT id FROM education_resource WHERE title='抑郁情绪的识别与应对'), '抑郁', 0.95, 'problem'),
((SELECT id FROM education_resource WHERE title='抑郁情绪的识别与应对'), '情绪识别', 0.82, 'solution'),
((SELECT id FROM education_resource WHERE title='时间管理与学习效率'), '时间管理', 0.90, 'solution'),
((SELECT id FROM education_resource WHERE title='时间管理与学习效率'), '学习效率', 0.85, 'solution'),
((SELECT id FROM education_resource WHERE title='恋爱心理学基础'), '恋爱', 0.88, 'problem'),
((SELECT id FROM education_resource WHERE title='恋爱心理学基础'), '亲密关系', 0.85, 'problem'),
((SELECT id FROM education_resource WHERE title='社交恐惧的克服方法'), '社交恐惧', 0.92, 'problem'),
((SELECT id FROM education_resource WHERE title='社交恐惧的克服方法'), '焦虑', 0.80, 'problem'),
((SELECT id FROM education_resource WHERE title='积极心理学实践'), '积极心理学', 0.90, 'solution'),
((SELECT id FROM education_resource WHERE title='积极心理学实践'), '幸福感', 0.82, 'solution');

-- ============================================
-- 6. 插入用户行为日志（用于兴趣标签动态更新）
-- ============================================

INSERT INTO `user_behavior_log` (`user_id`, `behavior_type`, `target_type`, `target_id`, `duration`, `created_at`) VALUES
-- 学生1（计算机，关注学习和压力）
((SELECT id FROM user WHERE username='student001'), 'view', 'resource', (SELECT id FROM education_resource WHERE title='如何应对考试焦虑'), 320, DATE_SUB(NOW(), INTERVAL 3 DAY)),
((SELECT id FROM user WHERE username='student001'), 'click', 'resource', (SELECT id FROM education_resource WHERE title='如何应对考试焦虑'), NULL, DATE_SUB(NOW(), INTERVAL 3 DAY)),
((SELECT id FROM user WHERE username='student001'), 'like', 'resource', (SELECT id FROM education_resource WHERE title='如何应对考试焦虑'), NULL, DATE_SUB(NOW(), INTERVAL 3 DAY)),
((SELECT id FROM user WHERE username='student001'), 'view', 'resource', (SELECT id FROM education_resource WHERE title='时间管理与学习效率'), 480, DATE_SUB(NOW(), INTERVAL 2 DAY)),
((SELECT id FROM user WHERE username='student001'), 'complete', 'resource', (SELECT id FROM education_resource WHERE title='时间管理与学习效率'), NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
((SELECT id FROM user WHERE username='student001'), 'view', 'resource', (SELECT id FROM education_resource WHERE title='正念冥想入门指南'), 180, DATE_SUB(NOW(), INTERVAL 1 DAY)),

-- 学生2（心理学，关注情绪和抑郁）
((SELECT id FROM user WHERE username='student002'), 'view', 'resource', (SELECT id FROM education_resource WHERE title='抑郁情绪的识别与应对'), 600, DATE_SUB(NOW(), INTERVAL 5 DAY)),
((SELECT id FROM user WHERE username='student002'), 'like', 'resource', (SELECT id FROM education_resource WHERE title='抑郁情绪的识别与应对'), NULL, DATE_SUB(NOW(), INTERVAL 5 DAY)),
((SELECT id FROM user WHERE username='student002'), 'favorite', 'resource', (SELECT id FROM education_resource WHERE title='抑郁情绪的识别与应对'), NULL, DATE_SUB(NOW(), INTERVAL 5 DAY)),
((SELECT id FROM user WHERE username='student002'), 'view', 'resource', (SELECT id FROM education_resource WHERE title='积极心理学实践'), 250, DATE_SUB(NOW(), INTERVAL 3 DAY)),
((SELECT id FROM user WHERE username='student002'), 'view', 'resource', (SELECT id FROM education_resource WHERE title='情绪管理ABC理论'), 300, DATE_SUB(NOW(), INTERVAL 2 DAY)),

-- 学生3（机械，关注职业和恋爱）
((SELECT id FROM user WHERE username='student003'), 'view', 'resource', (SELECT id FROM education_resource WHERE title='职业生涯规划指导'), 400, DATE_SUB(NOW(), INTERVAL 4 DAY)),
((SELECT id FROM user WHERE username='student003'), 'favorite', 'resource', (SELECT id FROM education_resource WHERE title='职业生涯规划指导'), NULL, DATE_SUB(NOW(), INTERVAL 4 DAY)),
((SELECT id FROM user WHERE username='student003'), 'view', 'resource', (SELECT id FROM education_resource WHERE title='人际关系沟通技巧'), 200, DATE_SUB(NOW(), INTERVAL 2 DAY)),
((SELECT id FROM user WHERE username='student003'), 'view', 'resource', (SELECT id FROM education_resource WHERE title='恋爱心理学基础'), 350, DATE_SUB(NOW(), INTERVAL 1 DAY)),

-- 学生4（工商，大一新生）
((SELECT id FROM user WHERE username='student004'), 'view', 'resource', (SELECT id FROM education_resource WHERE title='压力下的自我关怀'), 280, DATE_SUB(NOW(), INTERVAL 3 DAY)),
((SELECT id FROM user WHERE username='student004'), 'view', 'resource', (SELECT id FROM education_resource WHERE title='时间管理与学习效率'), 200, DATE_SUB(NOW(), INTERVAL 2 DAY)),
((SELECT id FROM user WHERE username='student004'), 'like', 'resource', (SELECT id FROM education_resource WHERE title='时间管理与学习效率'), NULL, DATE_SUB(NOW(), INTERVAL 2 DAY)),

-- 学生5（电子，大四就业）
((SELECT id FROM user WHERE username='student005'), 'view', 'resource', (SELECT id FROM education_resource WHERE title='职业生涯规划指导'), 500, DATE_SUB(NOW(), INTERVAL 6 DAY)),
((SELECT id FROM user WHERE username='student005'), 'like', 'resource', (SELECT id FROM education_resource WHERE title='职业生涯规划指导'), NULL, DATE_SUB(NOW(), INTERVAL 6 DAY)),
((SELECT id FROM user WHERE username='student005'), 'complete', 'resource', (SELECT id FROM education_resource WHERE title='职业生涯规划指导'), NULL, DATE_SUB(NOW(), INTERVAL 5 DAY)),

-- 学生6（英语，关注情感和冥想）
((SELECT id FROM user WHERE username='student006'), 'view', 'resource', (SELECT id FROM education_resource WHERE title='恋爱心理学基础'), 450, DATE_SUB(NOW(), INTERVAL 4 DAY)),
((SELECT id FROM user WHERE username='student006'), 'like', 'resource', (SELECT id FROM education_resource WHERE title='恋爱心理学基础'), NULL, DATE_SUB(NOW(), INTERVAL 4 DAY)),
((SELECT id FROM user WHERE username='student006'), 'favorite', 'resource', (SELECT id FROM education_resource WHERE title='恋爱心理学基础'), NULL, DATE_SUB(NOW(), INTERVAL 4 DAY)),
((SELECT id FROM user WHERE username='student006'), 'view', 'resource', (SELECT id FROM education_resource WHERE title='正念冥想入门指南'), 200, DATE_SUB(NOW(), INTERVAL 3 DAY)),
((SELECT id FROM user WHERE username='student006'), 'complete', 'resource', (SELECT id FROM education_resource WHERE title='正念冥想入门指南'), NULL, DATE_SUB(NOW(), INTERVAL 2 DAY)),

-- 学生7（数学，关注学习和考试）
((SELECT id FROM user WHERE username='student007'), 'view', 'resource', (SELECT id FROM education_resource WHERE title='时间管理与学习效率'), 350, DATE_SUB(NOW(), INTERVAL 3 DAY)),
((SELECT id FROM user WHERE username='student007'), 'like', 'resource', (SELECT id FROM education_resource WHERE title='时间管理与学习效率'), NULL, DATE_SUB(NOW(), INTERVAL 3 DAY)),
((SELECT id FROM user WHERE username='student007'), 'view', 'resource', (SELECT id FROM education_resource WHERE title='情绪管理ABC理论'), 150, DATE_SUB(NOW(), INTERVAL 1 DAY)),

-- 学生8（艺术，关注人际和社交）
((SELECT id FROM user WHERE username='student008'), 'view', 'resource', (SELECT id FROM education_resource WHERE title='人际关系沟通技巧'), 300, DATE_SUB(NOW(), INTERVAL 2 DAY)),
((SELECT id FROM user WHERE username='student008'), 'view', 'resource', (SELECT id FROM education_resource WHERE title='社交恐惧的克服方法'), 250, DATE_SUB(NOW(), INTERVAL 1 DAY));

-- ============================================
-- 说明
-- ============================================
-- 本SQL文件包含：
-- 1. 推荐系统所需的数据库表结构
-- 2. 测试用户数据（8个不同专业年级的学生）
-- 3. 丰富的教育资源（12个不同主题）
-- 4. 用户测评记录（模拟不同心理状态）
-- 5. 用户行为数据（浏览、点赞、收藏、完成学习）
-- 6. 用户兴趣标签和心理特征
-- 7. 资源关键词（用于基于内容推荐）
-- 8. 用户行为日志（用于兴趣标签动态更新和推荐效果分析）
-- 
-- 这些数据将支持：
-- - 基于用户的协同过滤（User-based CF，带时间衰减）
-- - 基于物品的协同过滤（Item-based CF，带时间衰减）
-- - 基于内容的推荐（Content-based，兴趣标签匹配）
-- - 基于心理画像的推荐（Profile-based，冷启动补充）
-- - 四路混合推荐算法（Hybrid，加权融合）
-- - 兴趣标签动态更新（基于行为日志聚合）
