-- ============================================
-- 验证和补充教育资源分类数据
-- ============================================

USE mental_health;

-- 1. 查看当前所有分类
SELECT 
    id,
    name,
    parent_id,
    sort_order,
    status
FROM resource_category
ORDER BY parent_id, sort_order;

-- 2. 查看每个分类下的资源数量
SELECT 
    rc.id AS category_id,
    rc.name AS category_name,
    COUNT(er.id) AS resource_count
FROM resource_category rc
LEFT JOIN education_resource er ON rc.id = er.category_id AND er.status = 1
WHERE rc.parent_id = 0
GROUP BY rc.id, rc.name
ORDER BY rc.sort_order;

-- 3. 检查是否有资源使用了不存在的分类ID
SELECT 
    er.id AS resource_id,
    er.title,
    er.category_id
FROM education_resource er
LEFT JOIN resource_category rc ON er.category_id = rc.id
WHERE rc.id IS NULL;

-- 4. 补充缺失的分类（如果需要）
-- 情绪管理 (id=1) - 已存在
-- 压力应对 (id=2) - 已存在
-- 人际交往 (id=3) - 已存在
-- 学业指导 (id=4) - 已存在
-- 职业规划 (id=5) - 已存在
-- 放松训练 (id=6) - 已存在

-- 5. 确保所有分类状态为启用
UPDATE resource_category 
SET status = 1 
WHERE id <= 6;

-- 6. 查看分类及其子分类的完整结构
SELECT 
    p.id AS parent_id,
    p.name AS parent_name,
    c.id AS child_id,
    c.name AS child_name,
    c.sort_order
FROM resource_category p
LEFT JOIN resource_category c ON p.id = c.parent_id
WHERE p.parent_id = 0
ORDER BY p.sort_order, c.sort_order;

-- 7. 测试分类查询（模拟后端API）
SELECT 
    id,
    name,
    parent_id,
    sort_order,
    status
FROM resource_category
WHERE status = 1
ORDER BY sort_order;

-- 8. 测试带分类筛选的资源查询
SELECT 
    er.id,
    er.title,
    er.type,
    er.category_id,
    rc.name AS category_name,
    er.tags,
    er.view_count,
    er.like_count,
    er.status
FROM education_resource er
LEFT JOIN resource_category rc ON er.category_id = rc.id
WHERE er.status = 1
AND er.category_id = 1  -- 测试情绪管理分类
ORDER BY er.created_at DESC
LIMIT 10;
