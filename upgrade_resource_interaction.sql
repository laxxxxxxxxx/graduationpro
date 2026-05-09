-- ============================================
-- 教育资源互动功能升级脚本 (MySQL 5.7 兼容)
-- 新增: 点赞表、收藏表、评论表
-- 新增: education_resource 增加 media_url/description/comment_count/favorite_count 字段
-- ============================================

USE mental_health;

-- ============================================
-- 1. 使用存储过程安全添加 education_resource 新字段
-- ============================================

DELIMITER $$

-- 添加 media_url 字段（视频/音频链接，与 content 文章内容分离）
CREATE PROCEDURE add_media_url_if_not_exists()
BEGIN
    DECLARE col_count INT DEFAULT 0;
    SELECT COUNT(*) INTO col_count
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'mental_health'
      AND TABLE_NAME = 'education_resource'
      AND COLUMN_NAME = 'media_url';
    IF col_count = 0 THEN
        ALTER TABLE `education_resource` ADD COLUMN `media_url` VARCHAR(500) COMMENT '媒体资源URL(视频/音频链接，文章类型为NULL)';
    END IF;
END$$

-- 添加 description 字段（资源简介，文章为摘要，视频/音频为内容描述）
CREATE PROCEDURE add_description_if_not_exists()
BEGIN
    DECLARE col_count INT DEFAULT 0;
    SELECT COUNT(*) INTO col_count
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'mental_health'
      AND TABLE_NAME = 'education_resource'
      AND COLUMN_NAME = 'description';
    IF col_count = 0 THEN
        ALTER TABLE `education_resource` ADD COLUMN `description` TEXT COMMENT '资源简介/描述';
    END IF;
END$$

-- 添加 comment_count 字段
CREATE PROCEDURE add_comment_count_if_not_exists()
BEGIN
    DECLARE col_count INT DEFAULT 0;
    SELECT COUNT(*) INTO col_count
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'mental_health'
      AND TABLE_NAME = 'education_resource'
      AND COLUMN_NAME = 'comment_count';
    IF col_count = 0 THEN
        ALTER TABLE `education_resource` ADD COLUMN `comment_count` INT DEFAULT 0 COMMENT '评论数';
    END IF;
END$$

-- 添加 favorite_count 字段
CREATE PROCEDURE add_favorite_count_if_not_exists()
BEGIN
    DECLARE col_count INT DEFAULT 0;
    SELECT COUNT(*) INTO col_count
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'mental_health'
      AND TABLE_NAME = 'education_resource'
      AND COLUMN_NAME = 'favorite_count';
    IF col_count = 0 THEN
        ALTER TABLE `education_resource` ADD COLUMN `favorite_count` INT DEFAULT 0 COMMENT '收藏数';
    END IF;
END$$

DELIMITER ;

-- 执行存储过程
CALL add_media_url_if_not_exists();
CALL add_description_if_not_exists();
CALL add_comment_count_if_not_exists();
CALL add_favorite_count_if_not_exists();

-- 清理存储过程
DROP PROCEDURE IF EXISTS add_media_url_if_not_exists;
DROP PROCEDURE IF EXISTS add_description_if_not_exists;
DROP PROCEDURE IF EXISTS add_comment_count_if_not_exists;
DROP PROCEDURE IF EXISTS add_favorite_count_if_not_exists;

-- ============================================
-- 2. 创建资源点赞表
-- ============================================

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

-- ============================================
-- 3. 创建资源收藏表
-- ============================================

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

-- ============================================
-- 4. 创建资源评论表
-- ============================================

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
-- 完成
-- ============================================
SELECT '资源互动功能数据库升级完成!' AS message;
SELECT 
    (SELECT COUNT(*) FROM resource_like) AS like_count,
    (SELECT COUNT(*) FROM resource_favorite) AS favorite_count,
    (SELECT COUNT(*) FROM resource_comment) AS comment_count;
