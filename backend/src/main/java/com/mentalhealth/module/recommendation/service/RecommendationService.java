package com.mentalhealth.module.recommendation.service;

import java.util.List;
import java.util.Map;

/**
 * 推荐服务接口
 */
public interface RecommendationService {
    
    /**
     * 获取个性化推荐列表（四路混合推荐）
     * @param userId 用户ID
     * @param topN 返回数量
     * @return 推荐资源列表
     */
    List<Map<String, Object>> getPersonalizedRecommendations(Long userId, int topN);
    
    /**
     * 基于用户的协同过滤推荐（带时间衰减）
     */
    List<Map<String, Object>> userBasedCollaborativeFiltering(Long userId, int topN);
    
    /**
     * 基于物品的协同过滤推荐（带时间衰减）
     */
    List<Map<String, Object>> itemBasedCollaborativeFiltering(Long userId, int topN);
    
    /**
     * 基于内容的推荐（兴趣标签匹配）
     */
    List<Map<String, Object>> contentBasedRecommendation(Long userId, int topN);
    
    /**
     * 基于心理画像的推荐（冷启动/补充推荐）
     */
    List<Map<String, Object>> getProfileBasedRecommendations(Long userId, int topN);
    
    /**
     * 混合推荐算法（四路加权融合）
     */
    List<Map<String, Object>> hybridRecommendation(Long userId, int topN);
    
    /**
     * 记录用户对推荐的点击行为
     */
    void recordClick(Long userId, Long resourceId);
    
    /**
     * 更新用户兴趣标签（基于最近行为动态提取）
     */
    void updateUserInterestTags(Long userId);
}
