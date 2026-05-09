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
    
    // 推荐算法权重（四路融合）
    private static final double USER_CF_WEIGHT = 0.30;
    private static final double ITEM_CF_WEIGHT = 0.25;
    private static final double CONTENT_WEIGHT = 0.25;
    private static final double PROFILE_WEIGHT = 0.20;
    
    // 多样性参数：每种类型最多推荐的比例
    private static final double MAX_SAME_TYPE_RATIO = 0.4;
    
    // 兴趣标签时间窗口（天）
    private static final int INTEREST_DAYS = 30;
    
    @Override
    public List<Map<String, Object>> getPersonalizedRecommendations(Long userId, int topN) {
        log.info("为用户{}生成个性化推荐，数量: {}", userId, topN);
        
        // 使用四路混合推荐算法，获取更多候选资源
        List<Map<String, Object>> recommendations = hybridRecommendation(userId, topN * 3);
        
        // 过滤掉不存在或未发布的资源
        enrichWithResourceDetails(recommendations);
        
        // 多样性过滤：避免同类型资源过多
        recommendations = applyDiversityFilter(recommendations);
        
        // 如果推荐数量不足，先用心理画像推荐补充
        if (recommendations.size() < topN) {
            log.info("推荐数量不足({}<{})，使用心理画像推荐补充", recommendations.size(), topN);
            List<Map<String, Object>> profileRecs = getProfileBasedRecommendations(userId, topN - recommendations.size());
            enrichWithResourceDetails(profileRecs);
            recommendations.addAll(profileRecs);
        }
        
        // 仍然不足，使用热门资源补充
        if (recommendations.size() < topN) {
            log.info("推荐数量仍不足，使用热门资源补充");
            List<Map<String, Object>> hotResources = getHotResources(userId, topN - recommendations.size());
            recommendations.addAll(hotResources);
        }
        
        // 保存推荐结果到数据库
        saveRecommendationResults(userId, recommendations.stream().limit(topN).collect(Collectors.toList()));
        
        return recommendations.stream().limit(topN).collect(Collectors.toList());
    }
    
    /**
     * 获取热门资源作为补充推荐（冷启动兜底）
     */
    private List<Map<String, Object>> getHotResources(Long userId, int count) {
        LambdaQueryWrapper<EducationResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EducationResource::getStatus, 1)
               .notInSql(EducationResource::getId, 
                   "SELECT resource_id FROM user_resource_rating WHERE user_id = " + userId)
               .orderByDesc(EducationResource::getViewCount)
               .last("LIMIT " + count);
        
        List<EducationResource> resources = resourceMapper.selectList(wrapper);
        
        return resources.stream().map(resource -> {
            Map<String, Object> rec = new HashMap<>();
            rec.put("resourceId", resource.getId());
            rec.put("title", resource.getTitle());
            rec.put("type", resource.getType());
            rec.put("coverUrl", resource.getCoverUrl());
            rec.put("tags", resource.getTags());
            rec.put("difficulty", resource.getDifficulty());
            rec.put("finalScore", 0.3);
            rec.put("reasons", new ArrayList<>(Collections.singletonList("热门资源推荐")));
            rec.put("algorithm", "hot");
            return rec;
        }).collect(Collectors.toList());
    }
    
    @Override
    public List<Map<String, Object>> userBasedCollaborativeFiltering(Long userId, int topN) {
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
        
        return resourceScores.entrySet().stream()
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
    }
    
    @Override
    public List<Map<String, Object>> itemBasedCollaborativeFiltering(Long userId, int topN) {
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
        
        return resourceScores.entrySet().stream()
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
    }
    
    @Override
    public List<Map<String, Object>> contentBasedRecommendation(Long userId, int topN) {
        log.info("执行基于内容的推荐");
        
        List<Map<String, Object>> recommendations = ratingMapper.getContentBasedRecommendations(userId, topN);
        
        return recommendations.stream()
                .map(rec -> {
                    Map<String, Object> result = new HashMap<>();
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
        log.info("执行基于心理画像的推荐");
        
        List<Map<String, Object>> recommendations = ratingMapper.getProfileBasedRecommendations(userId, topN);
        
        if (recommendations.isEmpty()) {
            log.warn("用户{}无心理画像数据，跳过画像推荐", userId);
            return Collections.emptyList();
        }
        
        return recommendations.stream()
                .map(rec -> {
                    Map<String, Object> result = new HashMap<>();
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
    }
    
    @Override
    public void updateUserInterestTags(Long userId) {
        log.info("开始更新用户{}的兴趣标签", userId);
        
        // 1. 获取用户最近30天的行为关键词统计
        List<Map<String, Object>> behaviorStats = ratingMapper.getUserBehaviorKeywordStats(
                userId, INTEREST_DAYS, 20);
        
        if (behaviorStats.isEmpty()) {
            log.info("用户{}最近{}天无行为数据，跳过标签更新", userId, INTEREST_DAYS);
            return;
        }
        
        // 2. 归一化聚合分数到0-100范围
        double maxScore = behaviorStats.stream()
                .mapToDouble(s -> ((Number) s.get("aggregated_score")).doubleValue())
                .max().orElse(1.0);
        
        int updatedCount = 0;
        for (Map<String, Object> stat : behaviorStats) {
            String keyword = (String) stat.get("keyword");
            String category = (String) stat.get("category");
            double rawScore = ((Number) stat.get("aggregated_score")).doubleValue();
            double normalizedScore = Math.min((rawScore / maxScore) * 100, 100.0);
            
            // 至少浏览过1个资源才保留
            int resourceCount = ((Number) stat.get("resource_count")).intValue();
            if (resourceCount < 1) continue;
            
            ratingMapper.insertOrUpdateInterestTag(userId, keyword, "behavior", 
                    category != null ? category : "general", normalizedScore);
            updatedCount++;
        }
        
        // 3. 清理过期的行为标签（超过60天未更新）
        int deletedCount = ratingMapper.deleteStaleInterestTags(userId, 60);
        
        log.info("用户{}兴趣标签更新完成: 新增/更新{}个, 清理过期{}个", userId, updatedCount, deletedCount);
    }
    
    /**
     * Min-Max归一化评分到0-1范围
     * 比原来的 Max-only 归一化更能区分低分项
     */
    private void normalizeScores(List<Map<String, Object>> recommendations) {
        if (recommendations.isEmpty()) return;
        
        double maxScore = recommendations.stream()
                .mapToDouble(r -> ((Number) r.get("predictedScore")).doubleValue())
                .max().orElse(1.0);
        double minScore = recommendations.stream()
                .mapToDouble(r -> ((Number) r.get("predictedScore")).doubleValue())
                .min().orElse(0.0);
        
        double range = maxScore - minScore;
        if (range == 0) {
            // 所有分数相同，统一设为0.5
            for (Map<String, Object> rec : recommendations) {
                rec.put("predictedScore", 0.5);
            }
        } else {
            for (Map<String, Object> rec : recommendations) {
                double score = ((Number) rec.get("predictedScore")).doubleValue();
                rec.put("predictedScore", (score - minScore) / range);
            }
        }
    }
    
    /**
     * 多样性过滤：避免同一类型资源过多
     */
    private List<Map<String, Object>> applyDiversityFilter(List<Map<String, Object>> recommendations) {
        if (recommendations.size() <= 3) return recommendations;
        
        int maxSameType = (int) Math.ceil(recommendations.size() * MAX_SAME_TYPE_RATIO);
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
        
        // 如果过滤后太少，从溢出列表中补充
        if (filtered.size() < 3 && !overflow.isEmpty()) {
            filtered.addAll(overflow.subList(0, Math.min(overflow.size(), 3 - filtered.size())));
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
            Long resourceId = ((Number) rec.get("resourceId")).longValue();
            EducationResource resource = resourceMapper.selectById(resourceId);
            
            if (resource == null || resource.getStatus() != 1) {
                iterator.remove();
                log.warn("推荐资源ID{}不存在或未发布，已移除", resourceId);
            } else {
                rec.putIfAbsent("title", resource.getTitle());
                rec.putIfAbsent("type", resource.getType());
                rec.putIfAbsent("coverUrl", resource.getCoverUrl());
                rec.putIfAbsent("tags", resource.getTags());
                rec.putIfAbsent("difficulty", resource.getDifficulty());
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
}
