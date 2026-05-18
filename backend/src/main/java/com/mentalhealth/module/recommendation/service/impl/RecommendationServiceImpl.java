package com.mentalhealth.module.recommendation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mentalhealth.module.recommendation.entity.RecommendationResult;
import com.mentalhealth.module.recommendation.entity.UserResourceRating;
import com.mentalhealth.module.recommendation.mapper.RecommendationResultMapper;
import com.mentalhealth.module.recommendation.mapper.UserResourceRatingMapper;
import com.mentalhealth.module.recommendation.service.RecommendationService;
import com.mentalhealth.module.resource.entity.EducationResource;
import com.mentalhealth.module.resource.mapper.ResourceMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RecommendationServiceImpl implements RecommendationService {
    
    @Autowired
    private UserResourceRatingMapper ratingMapper;
    
    @Autowired
    private ResourceMapper resourceMapper;
    
    @Autowired
    private RecommendationResultMapper recommendationResultMapper;
    
    // 推荐算法权重（四路融合）- 大幅提升内容权重以响应实时心境
    private static final double USER_CF_WEIGHT = 0.10;
    private static final double ITEM_CF_WEIGHT = 0.10;
    private static final double CONTENT_WEIGHT = 0.70;
    private static final double PROFILE_WEIGHT = 0.10;
    
    // 多样性参数：每种类型最多推荐的比例
    private static final double MAX_SAME_TYPE_RATIO = 0.4;
    
    // 兴趣标签时间窗口（天）
    private static final int INTEREST_DAYS = 30;

    private static final int MAX_TOP_N = 50;
    private static final int RATING_TYPE_VIEW = 1;
    private static final double SCORE_VIEW = 1.0;
    private static final double DEFAULT_RATING_WEIGHT = 1.0;
    
    @Override
    public List<Map<String, Object>> getPersonalizedRecommendations(Long userId, int topN) {
        // 强制设定推荐数量为 3
        final int TARGET_COUNT = 3;
        log.info("为用户{}生成个性化推荐，固定数量: {}", userId, TARGET_COUNT);
        
        // 演示环境优化：在获取推荐前，先强制刷新一次兴趣标签，确保日记标签立即生效
        try {
            updateUserInterestTags(userId);
        } catch (Exception e) {
            log.warn("静默刷新用户{}兴趣标签失败: {}", userId, e.getMessage());
        }
        
        // 1. 获取四路混合推荐结果 (多取一些用于过滤)
        List<Map<String, Object>> recommendations = hybridRecommendation(userId, TARGET_COUNT * 5);
        
        // 2. 补全资源详情
        enrichWithResourceDetails(recommendations);
        
        // 3. 多样性过滤 (尝试保留不同类型)
        recommendations = applyDiversityFilter(recommendations, TARGET_COUNT);
        
        // 4. 多级补偿，确保刚好达到 3 条
        if (recommendations.size() < TARGET_COUNT) {
            log.info("混合推荐不足 3 条，开始补偿");
            
            // 补偿 A: 心理画像
            List<Map<String, Object>> profileRecs = getProfileBasedRecommendations(userId, TARGET_COUNT);
            enrichWithResourceDetails(profileRecs);
            appendMissingRecommendations(recommendations, profileRecs, TARGET_COUNT);
            
            // 补偿 B: 内容推荐 (显式补充标签匹配)
            if (recommendations.size() < TARGET_COUNT) {
                List<Map<String, Object>> contentRecs = contentBasedRecommendation(userId, TARGET_COUNT);
                appendMissingRecommendations(recommendations, contentRecs, TARGET_COUNT);
            }
            
            // 补偿 C: 热门资源 (保底填充)
            if (recommendations.size() < TARGET_COUNT) {
                List<Map<String, Object>> hotResources = getHotResources(userId, TARGET_COUNT);
                appendMissingRecommendations(recommendations, hotResources, TARGET_COUNT);
            }
        }
        
        // 最终裁剪，确保只有 3 条
        List<Map<String, Object>> finalResult = recommendations.stream()
                .limit(TARGET_COUNT)
                .collect(Collectors.toList());
        
        // 保存推荐结果到数据库
        saveRecommendationResults(userId, finalResult);
        
        return finalResult;
    }
    
    /**
     * 获取热门资源作为补充推荐（冷启动兜底）
     */
    private List<Map<String, Object>> getHotResources(Long userId, int count) {
        if (count <= 0) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<EducationResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EducationResource::getStatus, 1)
               .notInSql(EducationResource::getId, 
                   "SELECT resource_id FROM user_resource_rating WHERE user_id = " + userId)
               .orderByDesc(EducationResource::getViewCount)
               .last("LIMIT " + count);
        
        List<EducationResource> resources = resourceMapper.selectList(wrapper);
        
        return resources.stream().map(resource -> {
            Map<String, Object> rec = new HashMap<>();
            rec.put("id", resource.getId());
            rec.put("resourceId", resource.getId());
            rec.put("title", resource.getTitle());
            rec.put("type", resource.getType());
            rec.put("coverUrl", resource.getCoverUrl());
            rec.put("tags", resource.getTags());
            rec.put("difficulty", resource.getDifficulty());
            rec.put("viewCount", resource.getViewCount());
            rec.put("finalScore", 0.3);
            rec.put("reasons", new ArrayList<>(Collections.singletonList("热门资源推荐")));
            rec.put("algorithm", "hot");
            return rec;
        }).collect(Collectors.toList());
    }
    
    @Override
    public List<Map<String, Object>> userBasedCollaborativeFiltering(Long userId, int topN) {
        topN = normalizeTopN(topN);
        if (topN == 0) {
            return Collections.emptyList();
        }
        log.info("执行基于用户的协同过滤推荐（带时间衰减）");
        
        List<Map<String, Object>> similarUsers = ratingMapper.findSimilarUsers(userId, 10);
        
        if (similarUsers.isEmpty()) {
            log.warn("未找到与用户{}相似的用户", userId);
            return Collections.emptyList();
        }
        
        // 获取相似用户喜欢但目标用户未评分的资源
        Map<Long, Double> resourceScores = new HashMap<>();
        
        for (Map<String, Object> similarUser : similarUsers) {
            Long similarUserId = ((Number) similarUser.get("user_id")).longValue();
            Double similarity = ((Number) similarUser.get("similarity")).doubleValue();
            
            List<Map<String, Object>> ratings = ratingMapper.getUserWeightedRatings(similarUserId);
            
            for (Map<String, Object> rating : ratings) {
                Long resourceId = ((Number) rating.get("resource_id")).longValue();
                Double score = ((Number) rating.get("total_score")).doubleValue();
                
                LambdaQueryWrapper<UserResourceRating> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(UserResourceRating::getUserId, userId)
                       .eq(UserResourceRating::getResourceId, resourceId);
                
                if (ratingMapper.selectCount(wrapper) == 0) {
                    resourceScores.merge(resourceId, similarity * score, Double::sum);
                }
            }
        }
        
        List<Map<String, Object>> results = resourceScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(topN)
                .map(entry -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("resourceId", entry.getKey());
                    result.put("predictedScore", entry.getValue());
                    result.put("algorithm", "user_cf");
                    result.put("reason", "与您兴趣相似的用户也喜欢");
                    return result;
                })
                .collect(Collectors.toList());
        enrichWithResourceDetails(results);
        return results;
    }
    
    @Override
    public List<Map<String, Object>> itemBasedCollaborativeFiltering(Long userId, int topN) {
        topN = normalizeTopN(topN);
        if (topN == 0) {
            return Collections.emptyList();
        }
        log.info("执行基于物品的协同过滤推荐（带时间衰减）");
        
        List<Map<String, Object>> userRatings = ratingMapper.getUserWeightedRatings(userId);
        
        if (userRatings.isEmpty()) {
            log.warn("用户{}暂无评分记录", userId);
            return Collections.emptyList();
        }
        
        Map<Long, Double> resourceScores = new HashMap<>();
        
        for (Map<String, Object> rating : userRatings) {
            Long resourceId = ((Number) rating.get("resource_id")).longValue();
            Double userScore = ((Number) rating.get("total_score")).doubleValue();
            
            List<Map<String, Object>> similarResources = ratingMapper.findSimilarResources(resourceId, 5);
            
            for (Map<String, Object> similar : similarResources) {
                Long similarResourceId = ((Number) similar.get("resource_id")).longValue();
                Double similarity = ((Number) similar.get("similarity")).doubleValue();
                
                LambdaQueryWrapper<UserResourceRating> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(UserResourceRating::getUserId, userId)
                       .eq(UserResourceRating::getResourceId, similarResourceId);
                
                if (ratingMapper.selectCount(wrapper) == 0) {
                    resourceScores.merge(similarResourceId, userScore * similarity, Double::sum);
                }
            }
        }
        
        List<Map<String, Object>> results = resourceScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(topN)
                .map(entry -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("resourceId", entry.getKey());
                    result.put("predictedScore", entry.getValue());
                    result.put("algorithm", "item_cf");
                    result.put("reason", "与您喜欢的资源相似");
                    return result;
                })
                .collect(Collectors.toList());
        enrichWithResourceDetails(results);
        return results;
    }
    
    @Override
    public List<Map<String, Object>> contentBasedRecommendation(Long userId, int topN) {
        topN = normalizeTopN(topN);
        if (topN == 0) {
            return Collections.emptyList();
        }
        log.info("执行基于内容的推荐");
        
        List<Map<String, Object>> recommendations = ratingMapper.getContentBasedRecommendations(userId, topN);
        
        return recommendations.stream()
                .map(rec -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("id", rec.get("id"));
                    result.put("resourceId", rec.get("id"));
                    result.put("title", rec.get("title"));
                    result.put("type", rec.get("type"));
                    result.put("coverUrl", rec.get("cover_url"));
                    result.put("tags", rec.get("tags"));
                    result.put("difficulty", rec.get("difficulty"));
                    result.put("predictedScore", rec.get("match_score"));
                    result.put("algorithm", "content_based");
                    result.put("reason", "符合您的兴趣标签");
                    return result;
                })
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Map<String, Object>> getProfileBasedRecommendations(Long userId, int topN) {
        topN = normalizeTopN(topN);
        if (topN == 0) {
            return Collections.emptyList();
        }
        log.info("执行基于心理画像的推荐");
        
        List<Map<String, Object>> recommendations = ratingMapper.getProfileBasedRecommendations(userId, topN);
        
        if (recommendations.isEmpty()) {
            log.warn("用户{}无心理画像数据，跳过画像推荐", userId);
            return Collections.emptyList();
        }
        
        return recommendations.stream()
                .map(rec -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("id", rec.get("id"));
                    result.put("resourceId", rec.get("id"));
                    result.put("title", rec.get("title"));
                    result.put("type", rec.get("type"));
                    result.put("coverUrl", rec.get("cover_url"));
                    result.put("tags", rec.get("tags"));
                    result.put("difficulty", rec.get("difficulty"));
                    result.put("predictedScore", rec.get("match_score"));
                    result.put("algorithm", "profile_based");
                    result.put("reason", "根据您的心理测评结果推荐");
                    return result;
                })
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Map<String, Object>> hybridRecommendation(Long userId, int topN) {
        topN = normalizeTopN(topN);
        if (topN == 0) {
            return Collections.emptyList();
        }
        log.info("执行四路混合推荐算法");
        
        // 1. 分别获取四种算法的推荐结果
        List<Map<String, Object>> userCfRecs = userBasedCollaborativeFiltering(userId, topN);
        List<Map<String, Object>> itemCfRecs = itemBasedCollaborativeFiltering(userId, topN);
        List<Map<String, Object>> contentRecs = contentBasedRecommendation(userId, topN);
        List<Map<String, Object>> profileRecs = getProfileBasedRecommendations(userId, topN);
        
        // 2. Min-Max归一化评分到0-1范围
        normalizeScores(userCfRecs);
        normalizeScores(itemCfRecs);
        normalizeScores(contentRecs);
        normalizeScores(profileRecs);
        
        // 3. 加权融合四种算法结果
        Map<Long, Map<String, Object>> mergedRecs = new LinkedHashMap<>();
        
        mergeAlgorithmResults(mergedRecs, userCfRecs, USER_CF_WEIGHT);
        mergeAlgorithmResults(mergedRecs, itemCfRecs, ITEM_CF_WEIGHT);
        mergeAlgorithmResults(mergedRecs, contentRecs, CONTENT_WEIGHT);
        mergeAlgorithmResults(mergedRecs, profileRecs, PROFILE_WEIGHT);
        
        // 4. 按最终评分排序
        List<Map<String, Object>> sortedRecs = mergedRecs.values().stream()
                .sorted((a, b) -> Double.compare(
                        ((Number) b.get("finalScore")).doubleValue(),
                        ((Number) a.get("finalScore")).doubleValue()))
                .limit(topN)
                .collect(Collectors.toList());
        
        // 5. 补充资源详细信息
        enrichWithResourceDetails(sortedRecs);
        
        log.info("四路混合推荐完成，生成{}条推荐", sortedRecs.size());
        return sortedRecs;
    }
    
    /**
     * 合并单个算法结果到融合Map
     */
    private void mergeAlgorithmResults(Map<Long, Map<String, Object>> mergedRecs,
                                        List<Map<String, Object>> recs, double weight) {
        for (Map<String, Object> rec : recs) {
            Long resourceId = ((Number) rec.get("resourceId")).longValue();
            Double score = ((Number) rec.get("predictedScore")).doubleValue();
            String reason = (String) rec.get("reason");
            String algorithm = (String) rec.get("algorithm");
            
            if (mergedRecs.containsKey(resourceId)) {
                Map<String, Object> merged = mergedRecs.get(resourceId);
                double currentScore = ((Number) merged.get("finalScore")).doubleValue();
                merged.put("finalScore", currentScore + score * weight);
                @SuppressWarnings("unchecked")
                List<String> reasons = (List<String>) merged.get("reasons");
                if (!reasons.contains(reason)) {
                    reasons.add(reason);
                }
                merged.put("algorithm", "hybrid");
            } else {
                Map<String, Object> merged = new HashMap<>();
                merged.put("resourceId", resourceId);
                merged.put("finalScore", score * weight);
                merged.put("algorithm", algorithm);
                merged.put("reasons", new ArrayList<>(Collections.singletonList(reason)));
                mergedRecs.put(resourceId, merged);
            }
        }
    }
    
    @Override
    public void recordClick(Long userId, Long resourceId) {
        log.info("记录用户{}点击资源{}", userId, resourceId);
        
        // 更新推荐结果的点击状态
        LambdaQueryWrapper<RecommendationResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RecommendationResult::getUserId, userId)
               .eq(RecommendationResult::getResourceId, resourceId)
               .orderByDesc(RecommendationResult::getCreatedAt)
               .last("LIMIT 1");
        
        RecommendationResult result = recommendationResultMapper.selectOne(wrapper);
        if (result != null) {
            result.setIsClicked(1);
            recommendationResultMapper.updateById(result);
        }

        try {
            ratingMapper.upsertResourceRating(userId, resourceId, RATING_TYPE_VIEW, SCORE_VIEW, DEFAULT_RATING_WEIGHT);
            ratingMapper.insertResourceBehaviorLog(userId, "click", resourceId, null);
        } catch (Exception e) {
            log.warn("记录推荐点击反馈失败: userId={}, resourceId={}, reason={}",
                    userId, resourceId, e.getMessage());
        }
    }
    
    @Override
    public void updateUserInterestTags(Long userId) {
        log.info("开始更新用户{}的兴趣标签", userId);
        
        // 1. 基于用户行为提取兴趣标签
        List<Map<String, Object>> behaviorStats = ratingMapper.getUserBehaviorKeywordStats(
                userId, INTEREST_DAYS, 20);
        
        int updatedCount = 0;
        if (!behaviorStats.isEmpty()) {
            double maxScore = behaviorStats.stream()
                    .mapToDouble(s -> ((Number) s.get("aggregated_score")).doubleValue())
                    .max().orElse(1.0);
            
            for (Map<String, Object> stat : behaviorStats) {
                String keyword = (String) stat.get("keyword");
                String category = (String) stat.get("category");
                double rawScore = ((Number) stat.get("aggregated_score")).doubleValue();
                double normalizedScore = Math.min((rawScore / maxScore) * 100, 100.0);
                
                int resourceCount = ((Number) stat.get("resource_count")).intValue();
                if (resourceCount < 1) continue;
                
                ratingMapper.insertOrUpdateInterestTag(userId, keyword, "behavior", 
                        category != null ? category : "general", normalizedScore);
                updatedCount++;
            }
        }
        
        // 2. 基于情绪日记提取兴趣标签
        List<Map<String, Object>> diaries = ratingMapper.getRecentDiaries(userId, INTEREST_DAYS);
        for (Map<String, Object> diary : diaries) {
            String moodTags = (String) diary.get("mood_tags");
            String content = (String) diary.get("content");
            
            // 处理情绪标签
            if (moodTags != null && !moodTags.isEmpty()) {
                String[] tags = moodTags.split("[,，]");
                for (String tag : tags) {
                    tag = tag.trim();
                    if (!tag.isEmpty()) {
                        // 日记标签给予较高的初始兴趣分 (85分)
                        ratingMapper.insertOrUpdateInterestTag(userId, tag, "diary", "emotion", 85.0);
                        updatedCount++;
                    }
                }
            }
            
            // 简单的关键词提取
            if (content != null) {
                String[] demoKeywords = {"考试", "焦虑", "压力", "沟通", "人际", "社交", "复习", "学业", "室友", "疲惫", "失眠"};
                for (String kw : demoKeywords) {
                    if (content.contains(kw)) {
                        // 匹配到的关键词给予 80分
                        ratingMapper.insertOrUpdateInterestTag(userId, kw, "diary", "general", 80.0);
                        updatedCount++;
                    }
                }
            }
        }
        
        // 3. 清理过期的行为标签（超过60天未更新）
        int deletedCount = ratingMapper.deleteStaleInterestTags(userId, 60);
        
        log.info("用户{}兴趣标签更新完成: 新增/更新{}个, 清理过期{}个", userId, updatedCount, deletedCount);
    }
    
    /**
     * Min-Max归一化评分到0-1范围
     */
    private void normalizeScores(List<Map<String, Object>> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) return;
        
        if (recommendations.size() == 1) {
            recommendations.get(0).put("predictedScore", 1.0);
            return;
        }

        double maxScore = -1.0;
        double minScore = Double.MAX_VALUE;
        
        for (Map<String, Object> rec : recommendations) {
            double score = ((Number) rec.get("predictedScore")).doubleValue();
            if (score > maxScore) maxScore = score;
            if (score < minScore) minScore = score;
        }
        
        double range = maxScore - minScore;
        if (range < 0.0001) {
            for (Map<String, Object> rec : recommendations) {
                rec.put("predictedScore", 1.0);
            }
        } else {
            for (Map<String, Object> rec : recommendations) {
                double score = ((Number) rec.get("predictedScore")).doubleValue();
                // 使用平滑归一化，确保最小值也有 0.2 的基础分
                double normalized = 0.2 + 0.8 * (score - minScore) / range;
                rec.put("predictedScore", normalized);
            }
        }
    }
    
    /**
     * 多样性过滤：避免同一类型资源过多
     */
    private List<Map<String, Object>> applyDiversityFilter(List<Map<String, Object>> recommendations, int targetSize) {
        if (recommendations.size() <= 1 || targetSize <= 0) return recommendations;
        
        int maxSameType = Math.max(1, (int) Math.ceil(targetSize * MAX_SAME_TYPE_RATIO));
        Map<Integer, Integer> typeCount = new HashMap<>();
        List<Map<String, Object>> filtered = new ArrayList<>();
        List<Map<String, Object>> overflow = new ArrayList<>();
        
        for (Map<String, Object> rec : recommendations) {
            Integer type = rec.get("type") != null ? 
                    ((Number) rec.get("type")).intValue() : 0;
            int count = typeCount.getOrDefault(type, 0);
            
            if (count < maxSameType) {
                filtered.add(rec);
                typeCount.put(type, count + 1);
            } else {
                overflow.add(rec);
            }
        }
        
        // 类型不够丰富时，用溢出项补足目标数量，避免为了多样性牺牲可用推荐数量。
        int expectedSize = Math.min(targetSize, recommendations.size());
        for (Map<String, Object> rec : overflow) {
            if (filtered.size() >= expectedSize) {
                break;
            }
            filtered.add(rec);
        }
        
        log.debug("多样性过滤: {}/{} 条保留", filtered.size(), recommendations.size());
        return filtered;
    }
    
    /**
     * 补充资源详细信息并过滤无效资源
     */
    private void enrichWithResourceDetails(List<Map<String, Object>> recommendations) {
        Iterator<Map<String, Object>> iterator = recommendations.iterator();
        while (iterator.hasNext()) {
            Map<String, Object> rec = iterator.next();
            Long resourceId = extractResourceId(rec);
            
            if (resourceId == null) {
                iterator.remove();
                continue;
            }
            
            EducationResource resource = resourceMapper.selectById(resourceId);
            
            if (resource == null || resource.getStatus() != 1) {
                iterator.remove();
                log.warn("推荐资源ID{}不存在或未发布，已移除", resourceId);
            } else {
                rec.putIfAbsent("id", resource.getId());
                rec.putIfAbsent("title", resource.getTitle());
                rec.putIfAbsent("type", resource.getType());
                rec.putIfAbsent("coverUrl", resource.getCoverUrl());
                rec.putIfAbsent("tags", resource.getTags());
                rec.putIfAbsent("difficulty", resource.getDifficulty());
                rec.putIfAbsent("viewCount", resource.getViewCount());
            }
        }
    }
    
    /**
     * 保存推荐结果到数据库
     */
    private void saveRecommendationResults(Long userId, List<Map<String, Object>> recommendations) {
        // 删除该用户旧的推荐结果
        LambdaQueryWrapper<RecommendationResult> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(RecommendationResult::getUserId, userId);
        recommendationResultMapper.delete(deleteWrapper);
        
        int order = 1;
        for (Map<String, Object> rec : recommendations) {
            RecommendationResult result = new RecommendationResult();
            result.setUserId(userId);
            result.setResourceId(((Number) rec.get("resourceId")).longValue());
            result.setAlgorithmType(rec.get("algorithm") != null ? 
                    rec.get("algorithm").toString() : "hybrid");
            
            Object finalScore = rec.get("finalScore");
            if (finalScore != null) {
                result.setConfidenceScore(((Number) finalScore).doubleValue() * 100);
            } else {
                result.setConfidenceScore(50.0);
            }
            
            @SuppressWarnings("unchecked")
            List<String> reasons = (List<String>) rec.get("reasons");
            if (reasons != null) {
                result.setReason(String.join(", ", reasons));
            } else {
                result.setReason("智能推荐");
            }
            
            result.setDisplayOrder(order++);
            result.setIsClicked(0);
            result.setIsEffective(0);
            result.setCreatedAt(new Date());
            
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, 7);
            result.setExpiredAt(cal.getTime());
            
            recommendationResultMapper.insert(result);
        }
        
        log.info("为用户{}保存了{}条推荐结果", userId, recommendations.size());
    }

    private int normalizeTopN(int topN) {
        if (topN <= 0) {
            return 0;
        }
        return Math.min(topN, MAX_TOP_N);
    }

    private void appendMissingRecommendations(List<Map<String, Object>> target,
                                              List<Map<String, Object>> additions,
                                              int maxSize) {
        if (target.size() >= maxSize || additions == null || additions.isEmpty()) {
            return;
        }
        Set<Long> existingResourceIds = target.stream()
                .map(this::extractResourceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (Map<String, Object> rec : additions) {
            Long resourceId = extractResourceId(rec);
            if (resourceId == null || existingResourceIds.contains(resourceId)) {
                continue;
            }
            target.add(rec);
            existingResourceIds.add(resourceId);
            if (target.size() >= maxSize) {
                break;
            }
        }
    }

    private Long extractResourceId(Map<String, Object> rec) {
        Object resourceId = rec.get("resourceId");
        if (resourceId == null) {
            resourceId = rec.get("id");
        }
        return resourceId instanceof Number ? ((Number) resourceId).longValue() : null;
    }
}
