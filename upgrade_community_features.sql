-- ============================================
-- 匿名社区功能升级脚本（分类 + 编辑/删除）
-- 兼容 MySQL 5.7（使用存储过程安全DDL）
-- ============================================

-- 1. 创建社区分类表
CREATE TABLE IF NOT EXISTS `community_category` (
    `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `icon` VARCHAR(100) COMMENT '图标(emoji)',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 1-启用 0-禁用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社区分类表';

-- 2. 插入默认分类数据
INSERT INTO `community_category` (`id`, `name`, `icon`, `sort_order`) VALUES
(1, '情感倾诉', '💭', 1),
(2, '学业压力', '📚', 2),
(3, '人际关系', '🤝', 3),
(4, '职业规划', '🎯', 4),
(5, '家庭困扰', '🏠', 5),
(6, '自我成长', '🌱', 6)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- 3. 给 community_post 添加外键约束（如果 category 表已存在则关联）
DELIMITER $$
CREATE PROCEDURE add_category_fk_if_not_exists()
BEGIN
    DECLARE fk_count INT DEFAULT 0;
    SELECT COUNT(*) INTO fk_count
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = 'mental_health'
      AND TABLE_NAME = 'community_post'
      AND CONSTRAINT_NAME = 'fk_post_category';

    IF fk_count = 0 THEN
        ALTER TABLE `community_post`
        ADD CONSTRAINT `fk_post_category`
        FOREIGN KEY (`category_id`) REFERENCES `community_category`(`id`)
        ON DELETE SET NULL;
    END IF;
END$$
DELIMITER ;
CALL add_category_fk_if_not_exists();
DROP PROCEDURE IF EXISTS add_category_fk_if_not_exists;
