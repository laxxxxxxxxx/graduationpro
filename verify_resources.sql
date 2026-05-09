-- ============================================
-- 验证教育资源数据
-- ============================================

USE mental_health;

-- 1. 查看所有教育资源的数量和状态
SELECT 
    COUNT(*) AS total_resources,
    SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) AS published_resources,
    SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) AS pending_resources,
    SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) AS offline_resources
FROM education_resource;

-- 2. 查看已发布的资源列表
SELECT 
    id,
    title,
    type,
    category_id,
    status,
    view_count,
    like_count,
    tags
FROM education_resource
WHERE status = 1
ORDER BY id;

-- 3. 查看用户行为记录中引用的资源ID是否都存在
SELECT 
    urr.resource_id,
    er.title,
    er.status,
    COUNT(*) AS behavior_count
FROM user_resource_rating urr
LEFT JOIN education_resource er ON urr.resource_id = er.id
WHERE er.id IS NULL OR er.status != 1
GROUP BY urr.resource_id, er.title, er.status;

-- 4. 查看推荐结果表中引用的资源ID是否都存在
SELECT 
    rr.resource_id,
    er.title,
    er.status,
    COUNT(*) AS recommendation_count
FROM recommendation_result rr
LEFT JOIN education_resource er ON rr.resource_id = er.id
WHERE er.id IS NULL OR er.status != 1
GROUP BY rr.resource_id, er.title, er.status;

-- 5. 查看资源关键词表中的资源是否都存在
SELECT 
    rk.resource_id,
    er.title,
    er.status
FROM resource_keywords rk
LEFT JOIN education_resource er ON rk.resource_id = er.id
WHERE er.id IS NULL OR er.status != 1;

-- 6. 测试推荐查询（模拟基于内容的推荐）
SELECT 
    er.id,
    er.title,
    er.type,
    er.tags,
    SUM(uit.interest_score * rk.tfidf_score) as match_score
FROM education_resource er
JOIN resource_keywords rk ON er.id = rk.resource_id
JOIN user_interest_tags uit ON rk.keyword = uit.tag_name
WHERE uit.user_id = (SELECT id FROM user WHERE username = 'student001')
AND er.status = 1
AND er.id NOT IN (SELECT resource_id FROM user_resource_rating WHERE user_id = (SELECT id FROM user WHERE username = 'student001'))
GROUP BY er.id
ORDER BY match_score DESC
LIMIT 10;
