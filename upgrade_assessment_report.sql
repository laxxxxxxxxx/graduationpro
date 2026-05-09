-- ============================================
-- 测评报告增强 - 数据库结构升级 (MySQL 5.7兼容)
-- ============================================

USE mental_health;

-- 使用存储过程安全添加字段（兼容MySQL 5.7）
DROP PROCEDURE IF EXISTS add_column_if_not_exists;

DELIMITER $$
CREATE PROCEDURE add_column_if_not_exists(
    IN tableName VARCHAR(128),
    IN columnName VARCHAR(128),
    IN columnDef VARCHAR(256)
)
BEGIN
    DECLARE col_count INT DEFAULT 0;
    SELECT COUNT(*) INTO col_count
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'mental_health'
      AND TABLE_NAME = tableName
      AND COLUMN_NAME = columnName;
    
    IF col_count = 0 THEN
        SET @sql = CONCAT('ALTER TABLE `', tableName, '` ADD COLUMN `', columnName, '` ', columnDef);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        SELECT CONCAT('Column ', columnName, ' added successfully.') AS result;
    ELSE
        SELECT CONCAT('Column ', columnName, ' already exists, skipped.') AS result;
    END IF;
END$$
DELIMITER ;

-- 添加 dimension_scores 字段
CALL add_column_if_not_exists('user_assessment', 'dimension_scores', 
    "JSON COMMENT '维度得分JSON'") ;

-- 添加 report_data 字段
CALL add_column_if_not_exists('user_assessment', 'report_data', 
    "JSON COMMENT '完整报告数据JSON'") ;

-- 添加 report_generated_at 字段
CALL add_column_if_not_exists('user_assessment', 'report_generated_at', 
    "DATETIME COMMENT '报告生成时间'") ;

-- 清理存储过程
DROP PROCEDURE IF EXISTS add_column_if_not_exists;

-- 安全添加索引（MySQL 5.7兼容）
DROP PROCEDURE IF EXISTS add_index_if_not_exists;

DELIMITER $$
CREATE PROCEDURE add_index_if_not_exists(
    IN tableName VARCHAR(128),
    IN indexName VARCHAR(128),
    IN indexDef VARCHAR(256)
)
BEGIN
    DECLARE idx_count INT DEFAULT 0;
    SELECT COUNT(*) INTO idx_count
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'mental_health'
      AND TABLE_NAME = tableName
      AND INDEX_NAME = indexName;
    
    IF idx_count = 0 THEN
        SET @sql = CONCAT('ALTER TABLE `', tableName, '` ADD INDEX `', indexName, '` ', indexDef);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        SELECT CONCAT('Index ', indexName, ' added successfully.') AS result;
    ELSE
        SELECT CONCAT('Index ', indexName, ' already exists, skipped.') AS result;
    END IF;
END$$
DELIMITER ;

-- 添加复合索引
CALL add_index_if_not_exists('user_assessment', 'idx_user_scale_created', 
    '(user_id, scale_id, created_at)') ;

-- 清理存储过程
DROP PROCEDURE IF EXISTS add_index_if_not_exists;
