-- ============================================
-- 大学生心理健康教育与自测平台 - 数据库初始化脚本
-- 数据库: mental_health
-- 字符集: utf8mb4
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS mental_health DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE mental_health;

-- ============================================
-- 1. 用户相关表
-- ============================================

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) UNIQUE NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '加密密码',
    `email` VARCHAR(100) UNIQUE COMMENT '邮箱',
    `phone` VARCHAR(20) COMMENT '手机号',
    `real_name` VARCHAR(255) COMMENT '真实姓名(AES加密存储)',
    `gender` TINYINT COMMENT '性别: 0-女 1-男 2-其他',
    `age` INT COMMENT '年龄',
    `university` VARCHAR(100) COMMENT '学校',
    `major` VARCHAR(100) COMMENT '专业',
    `grade` VARCHAR(20) COMMENT '年级',
    `role` TINYINT DEFAULT 1 COMMENT '角色: 1-学生 2-咨询师 3-管理员',
    `avatar` VARCHAR(255) COMMENT '头像URL',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_username` (`username`),
    INDEX `idx_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 用户隐私设置表
CREATE TABLE IF NOT EXISTS `user_privacy` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `show_real_name` TINYINT DEFAULT 0 COMMENT '是否显示真实姓名: 0-否 1-是',
    `show_university` TINYINT DEFAULT 0 COMMENT '是否显示学校: 0-否 1-是',
    `allow_recommend` TINYINT DEFAULT 1 COMMENT '是否允许个性化推荐: 0-否 1-是',
    `data_retention_days` INT DEFAULT 365 COMMENT '数据保留天数',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户隐私设置表';

-- ============================================
-- 2. 教育资源相关表
-- ============================================

-- 资源分类表
CREATE TABLE IF NOT EXISTS `resource_category` (
    `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `parent_id` INT DEFAULT 0 COMMENT '父分类ID',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源分类表';

-- 教育资源表
CREATE TABLE IF NOT EXISTS `education_resource` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '资源ID',
    `title` VARCHAR(200) NOT NULL COMMENT '资源标题',
    `type` TINYINT NOT NULL COMMENT '类型: 1-文章 2-视频 3-音频 4-课程',
    `category_id` INT NOT NULL COMMENT '分类ID',
    `cover_url` VARCHAR(255) COMMENT '封面图URL',
    `content` LONGTEXT COMMENT '内容(文章HTML/视频链接/音频链接)',
    `duration` INT COMMENT '时长(秒,视频/音频)',
    `difficulty` TINYINT COMMENT '难度: 1-入门 2-进阶 3-高级',
    `tags` VARCHAR(255) COMMENT '标签,逗号分隔',
    `author` VARCHAR(50) COMMENT '作者',
    `source` VARCHAR(100) COMMENT '来源',
    `copyright_status` TINYINT DEFAULT 1 COMMENT '版权状态: 1-已授权',
    `view_count` INT DEFAULT 0 COMMENT '浏览量',
    `like_count` INT DEFAULT 0 COMMENT '点赞数',
    `favorite_count` INT DEFAULT 0 COMMENT '收藏数',
    `comment_count` INT DEFAULT 0 COMMENT '评论数',
    `description` TEXT COMMENT '资源简介/描述',
    `media_url` VARCHAR(500) COMMENT '媒体资源URL(视频/音频链接)',
    `status` TINYINT DEFAULT 0 COMMENT '状态: 0-待审核 1-已发布 2-下架',
    `publish_time` DATETIME COMMENT '发布时间',
    `created_by` BIGINT COMMENT '创建者ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_category` (`category_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_type` (`type`),
    FULLTEXT INDEX `ft_title_content` (`title`, `content`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教育资源表';

-- 用户学习记录表
CREATE TABLE IF NOT EXISTS `user_learning_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `resource_id` BIGINT NOT NULL COMMENT '资源ID',
    `progress` INT DEFAULT 0 COMMENT '学习进度百分比',
    `completed` TINYINT DEFAULT 0 COMMENT '是否完成: 0-否 1-是',
    `study_duration` INT DEFAULT 0 COMMENT '学习时长(秒)',
    `last_study_time` DATETIME COMMENT '最后学习时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`resource_id`) REFERENCES `education_resource`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_user_resource` (`user_id`, `resource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户学习记录表';

-- 资源点赞表
CREATE TABLE IF NOT EXISTS `resource_like` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `resource_id` BIGINT NOT NULL COMMENT '资源ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`resource_id`) REFERENCES `education_resource`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_user_resource` (`user_id`, `resource_id`),
    INDEX `idx_resource` (`resource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源点赞表';

-- 资源收藏表
CREATE TABLE IF NOT EXISTS `resource_favorite` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `resource_id` BIGINT NOT NULL COMMENT '资源ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`resource_id`) REFERENCES `education_resource`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_user_resource` (`user_id`, `resource_id`),
    INDEX `idx_user` (`user_id`),
    INDEX `idx_resource` (`resource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源收藏表';

-- 资源评论表
CREATE TABLE IF NOT EXISTS `resource_comment` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评论ID',
    `resource_id` BIGINT NOT NULL COMMENT '资源ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父评论ID(0为顶级评论)',
    `content` TEXT NOT NULL COMMENT '评论内容',
    `like_count` INT DEFAULT 0 COMMENT '评论点赞数',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-隐藏 1-正常 2-删除',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (`resource_id`) REFERENCES `education_resource`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    INDEX `idx_resource_created` (`resource_id`, `created_at` DESC),
    INDEX `idx_parent` (`parent_id`),
    INDEX `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源评论表';

-- ============================================
-- 3. 自测评估相关表
-- ============================================

-- 测评量表表
CREATE TABLE IF NOT EXISTS `assessment_scale` (
    `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '量表ID',
    `name` VARCHAR(100) NOT NULL COMMENT '量表名称',
    `code` VARCHAR(50) UNIQUE NOT NULL COMMENT '量表编码(SDS/SCL90/UPI)',
    `description` TEXT COMMENT '量表描述',
    `total_questions` INT NOT NULL COMMENT '总题数',
    `estimated_time` INT COMMENT '预计用时(分钟)',
    `scoring_rule` TEXT COMMENT '评分规则JSON',
    `interpretation_template` TEXT COMMENT '结果解读模板',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测评量表表';

-- 量表题目表
CREATE TABLE IF NOT EXISTS `scale_question` (
    `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '题目ID',
    `scale_id` INT NOT NULL COMMENT '量表ID',
    `question_no` INT NOT NULL COMMENT '题目序号',
    `question_text` TEXT NOT NULL COMMENT '题目内容',
    `question_type` TINYINT DEFAULT 1 COMMENT '题型: 1-单选 2-多选',
    `options` JSON NOT NULL COMMENT '选项JSON [{value:1,text:"几乎没有"},...]',
    `score_mapping` JSON COMMENT '计分映射JSON',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (`scale_id`) REFERENCES `assessment_scale`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_scale_question` (`scale_id`, `question_no`),
    INDEX `idx_scale` (`scale_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='量表题目表';

-- 用户测评记录表
CREATE TABLE IF NOT EXISTS `user_assessment` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `scale_id` INT NOT NULL COMMENT '量表ID',
    `total_score` DECIMAL(10,2) COMMENT '总分',
    `result_level` VARCHAR(20) COMMENT '结果等级: 正常/轻度/中度/重度',
    `interpretation` TEXT COMMENT '专业解读',
    `suggestions` TEXT COMMENT '干预建议',
    `answers` JSON NOT NULL COMMENT '答题详情JSON',
    `completion_time` INT COMMENT '完成用时(秒)',
    `dimension_scores` JSON COMMENT '维度得分JSON',
    `report_data` JSON COMMENT '完整报告数据JSON',
    `report_generated_at` DATETIME COMMENT '报告生成时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`scale_id`) REFERENCES `assessment_scale`(`id`),
    INDEX `idx_user_created` (`user_id`, `created_at`),
    INDEX `idx_user_scale_created` (`user_id`, `scale_id`, `created_at`),
    INDEX `idx_scale` (`scale_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户测评记录表';

-- ============================================
-- 4. 情绪日记相关表
-- ============================================

-- 情绪日记表
CREATE TABLE IF NOT EXISTS `emotion_diary` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日记ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `diary_date` DATE NOT NULL COMMENT '日记日期',
    `mood_score` INT COMMENT '情绪评分1-10',
    `mood_tags` VARCHAR(100) COMMENT '情绪标签,逗号分隔(开心/焦虑/平静等)',
    `content` TEXT COMMENT '日记内容',
    `weather` VARCHAR(20) COMMENT '天气',
    `events` TEXT COMMENT '今日事件',
    `gratitude` TEXT COMMENT '感恩事项',
    `energy_level` INT COMMENT '精力水平1-10',
    `sleep_quality` INT COMMENT '睡眠质量1-10',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_user_date` (`user_id`, `diary_date`),
    INDEX `idx_user_date` (`user_id`, `diary_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='情绪日记表';

-- 情绪统计分析表(每日聚合)
CREATE TABLE IF NOT EXISTS `emotion_statistics` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `stat_date` DATE NOT NULL COMMENT '统计日期',
    `avg_mood_score` DECIMAL(5,2) COMMENT '平均情绪分',
    `dominant_emotion` VARCHAR(20) COMMENT '主导情绪',
    `diary_count` INT DEFAULT 0 COMMENT '日记数量',
    `weekly_trend` VARCHAR(50) COMMENT '周趋势',
    `monthly_trend` VARCHAR(50) COMMENT '月趋势',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_user_date` (`user_id`, `stat_date`),
    INDEX `idx_user_date` (`user_id`, `stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='情绪统计分析表';

-- ============================================
-- 5. 匿名社区相关表
-- ============================================

-- 社区分类表
CREATE TABLE IF NOT EXISTS `community_category` (
    `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `icon` VARCHAR(100) COMMENT '图标',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 1-启用 0-禁用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社区分类表';

-- 社区帖子表
CREATE TABLE IF NOT EXISTS `community_post` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '帖子ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `anonymous_id` VARCHAR(50) COMMENT '匿名ID(脱敏)',
    `title` VARCHAR(200) NOT NULL COMMENT '标题',
    `content` TEXT NOT NULL COMMENT '内容',
    `category_id` INT COMMENT '分类ID',
    `cover_image` VARCHAR(255) COMMENT '封面图',
    `view_count` INT DEFAULT 0 COMMENT '浏览量',
    `comment_count` INT DEFAULT 0 COMMENT '评论数',
    `like_count` INT DEFAULT 0 COMMENT '点赞数',
    `status` TINYINT DEFAULT 0 COMMENT '状态: 0-待审核 1-已发布 2-隐藏 3-删除',
    `audit_result` TINYINT COMMENT '审核结果: 0-待审 1-通过 2-拒绝',
    `audit_reason` VARCHAR(255) COMMENT '审核原因',
    `is_top` TINYINT DEFAULT 0 COMMENT '是否置顶: 0-否 1-是',
    `is_essence` TINYINT DEFAULT 0 COMMENT '是否精华: 0-否 1-是',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`category_id`) REFERENCES `community_category`(`id`) ON DELETE SET NULL,
    INDEX `idx_status_created` (`status`, `created_at` DESC),
    INDEX `idx_user` (`user_id`),
    INDEX `idx_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社区帖子表';

-- 社区评论表
CREATE TABLE IF NOT EXISTS `community_comment` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评论ID',
    `post_id` BIGINT NOT NULL COMMENT '帖子ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `anonymous_id` VARCHAR(50) COMMENT '匿名ID',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父评论ID',
    `content` TEXT NOT NULL COMMENT '评论内容',
    `like_count` INT DEFAULT 0 COMMENT '点赞数',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-待审 1-正常 2-删除',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (`post_id`) REFERENCES `community_post`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    INDEX `idx_post_created` (`post_id`, `created_at`),
    INDEX `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社区评论表';

-- 帖子点赞表
CREATE TABLE IF NOT EXISTS `post_like` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `post_id` BIGINT NOT NULL COMMENT '帖子ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`post_id`) REFERENCES `community_post`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_user_post` (`user_id`, `post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子点赞表';

-- ============================================
-- 6. 推荐系统相关表
-- ============================================

-- 用户行为日志表（用于记录用户操作明细，支持兴趣标签动态更新和推荐效果分析）
CREATE TABLE IF NOT EXISTS `user_behavior_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `behavior_type` VARCHAR(20) NOT NULL COMMENT '行为类型:view/click/like/favorite/complete/share',
    `target_type` VARCHAR(20) NOT NULL COMMENT '目标类型:resource/post/scale',
    `target_id` BIGINT NOT NULL COMMENT '目标ID',
    `duration` INT COMMENT '停留时长(秒)',
    `extra_data` JSON COMMENT '额外数据(如浏览的资源标签、停留时间等)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_user_created` (`user_id`, `created_at`),
    INDEX `idx_target` (`target_type`, `target_id`),
    INDEX `idx_user_behavior` (`user_id`, `behavior_type`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户行为日志表(推荐系统核心数据源)';

-- ============================================
-- 7. 系统管理相关表
-- ============================================

-- 管理员操作日志
CREATE TABLE IF NOT EXISTS `admin_operation_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `admin_id` BIGINT NOT NULL COMMENT '管理员ID',
    `operation_type` VARCHAR(50) COMMENT '操作类型',
    `target_type` VARCHAR(50) COMMENT '目标类型',
    `target_id` BIGINT COMMENT '目标ID',
    `operation_desc` TEXT COMMENT '操作描述',
    `ip_address` VARCHAR(50) COMMENT 'IP地址',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (`admin_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员操作日志表';

-- 系统配置表
CREATE TABLE IF NOT EXISTS `system_config` (
    `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `config_key` VARCHAR(100) UNIQUE NOT NULL COMMENT '配置键',
    `config_value` TEXT COMMENT '配置值',
    `description` VARCHAR(255) COMMENT '描述',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- ============================================
-- 8. 插入初始数据
-- ============================================

-- 插入默认管理员账号 (密码: admin123, 需要后端BCrypt加密后替换)
INSERT INTO `user` (`username`, `password`, `role`, `status`) 
VALUES ('admin', '$2a$10$1QWsFcfhksrzoWDeQ3gR.uFrvYaHD5iiFO6JLbFxvQ.NmDJPqsK1e', 3, 1);

-- 插入资源分类
INSERT INTO `resource_category` (`name`, `parent_id`, `sort_order`) VALUES
('情绪管理', 0, 1),
('压力应对', 0, 2),
('人际交往', 0, 3),
('学业指导', 0, 4),
('职业规划', 0, 5),
('放松训练', 0, 6);

-- 插入子分类
INSERT INTO `resource_category` (`name`, `parent_id`, `sort_order`) VALUES
('焦虑缓解', 1, 1),
('抑郁调适', 1, 2),
('考试压力', 2, 1),
('时间管理', 2, 2),
('沟通技巧', 3, 1),
('恋爱心理', 3, 2);

-- 插入社区分类
INSERT INTO `community_category` (`id`, `name`, `icon`, `sort_order`) VALUES
(1, '情感倾诉', '💭', 1),
(2, '学业压力', '📚', 2),
(3, '人际关系', '🤝', 3),
(4, '职业规划', '🎯', 4),
(5, '家庭困扰', '🏠', 5),
(6, '自我成长', '🌱', 6)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- 插入测评量表
INSERT INTO `assessment_scale` (`name`, `code`, `description`, `total_questions`, `estimated_time`, `scoring_rule`, `interpretation_template`, `status`) VALUES
('抑郁自评量表', 'SDS', 'SDS抑郁自评量表由Zung于1965年编制,含有20个项目,分为4级评分的自评量表。', 20, 10, 
'{"rule": "standard_score = raw_score * 1.25", "levels": [{"min": 0, "max": 52, "level": "正常"}, {"min": 53, "max": 62, "level": "轻度抑郁"}, {"min": 63, "max": 72, "level": "中度抑郁"}, {"min": 73, "max": 100, "level": "重度抑郁"}]}',
'您的SDS标准分为{score}分,属于{level}。建议您{advice}', 1),

('症状自评量表', 'SCL90', 'SCL-90症状自评量表包含90个项目,从感觉、情感、思维、意识、行为直至生活习惯、人际关系、饮食睡眠等多种角度评定。', 90, 20,
'{"rule": "计算各因子分和总分", "levels": [{"min": 0, "max": 160, "level": "正常"}, {"min": 161, "max": 200, "level": "轻度"}, {"min": 201, "max": 250, "level": "中度"}, {"min": 251, "max": 450, "level": "重度"}]}',
'您的SCL-90总分为{score}分,整体心理健康状况{level}。', 1),

('大学生人格问卷', 'UPI', 'UPI是大学生人格问卷的简称,是为了早期发现、早期治疗有心理问题的学生而编制的大学生精神健康调查表。', 60, 15,
'{"rule": "根据关键题和总分综合判断", "levels": [{"min": 0, "max": 15, "level": "一类(正常)"}, {"min": 16, "max": 25, "level": "二类(需关注)"}, {"min": 26, "max": 60, "level": "三类(需咨询)"}]}',
'您的UPI得分为{score}分,属于{level}。', 1);

-- 插入系统配置
INSERT INTO `system_config` (`config_key`, `config_value`, `description`) VALUES
('app.name', '大学生心理健康平台', '应用名称'),
('app.version', '1.0.0', '应用版本'),
('privacy.policy.url', '', '隐私政策链接'),
('crisis.hotline', '400-161-9995', '心理危机干预热线'),
('max.diary.per.day', '5', '每日最大日记数量');

-- ============================================
-- 完成提示
-- ============================================
SELECT '数据库初始化完成!' AS message;
SELECT COUNT(*) AS table_count FROM information_schema.tables WHERE table_schema = 'mental_health';
