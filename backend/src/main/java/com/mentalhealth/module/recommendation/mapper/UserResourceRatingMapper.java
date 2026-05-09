package com.mentalhealth.module.recommendation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mentalhealth.module.recommendation.entity.UserResourceRating;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserResourceRatingMapper extends BaseMapper<UserResourceRating> {
    
    /**
     * 获取用户对资源的加权评分（带时间衰减）
     * 时间衰减公式: score * weight / (1 + 0.01 * 距今天数)
     */
    @Select("SELECT resource_id, SUM(score * weight / (1 + 0.01 * DATEDIFF(NOW(), created_at))) as total_score " +
            "FROM user_resource_rating " +
            "WHERE user_id = #{userId} " +
            "GROUP BY resource_id " +
            "ORDER BY total_score DESC")
    List<Map<String, Object>> getUserWeightedRatings(@Param("userId") Long userId);
    
    /**
     * 获取相似用户（基于共同评分资源，带时间衰减的余弦相似度）
     * 时间衰减融入评分计算，使近期行为权重更高
     */
    @Select("SELECT r2_eff.user_id, COUNT(*) as common_resources, " +
            "SUM(r1_eff.score * r2_eff.score) / " +
            "(SQRT(SUM(r1_eff.score * r1_eff.score)) * SQRT(SUM(r2_eff.score * r2_eff.score))) as similarity " +
            "FROM (SELECT user_id, resource_id, " +
            "  SUM(score * weight / (1 + 0.01 * DATEDIFF(NOW(), created_at))) as score " +
            "  FROM user_resource_rating WHERE user_id = #{userId} GROUP BY user_id, resource_id) r1_eff " +
            "JOIN (SELECT user_id, resource_id, " +
            "  SUM(score * weight / (1 + 0.01 * DATEDIFF(NOW(), created_at))) as score " +
            "  FROM user_resource_rating WHERE user_id != #{userId} GROUP BY user_id, resource_id) r2_eff " +
            "ON r1_eff.resource_id = r2_eff.resource_id " +
            "JOIN education_resource er ON r1_eff.resource_id = er.id " +
            "WHERE er.status = 1 " +
            "GROUP BY r2_eff.user_id " +
            "HAVING common_resources >= 2 " +
            "ORDER BY similarity DESC " +
            "LIMIT #{topN}")
    List<Map<String, Object>> findSimilarUsers(@Param("userId") Long userId, @Param("topN") int topN);
    
    /**
     * 获取资源相似度（基于共同评分用户，带时间衰减的余弦相似度）
     */
    @Select("SELECT r2_eff.resource_id, COUNT(*) as common_users, " +
            "SUM(r1_eff.score * r2_eff.score) / " +
            "(SQRT(SUM(r1_eff.score * r1_eff.score)) * SQRT(SUM(r2_eff.score * r2_eff.score))) as similarity " +
            "FROM (SELECT user_id, resource_id, " +
            "  SUM(score * weight / (1 + 0.01 * DATEDIFF(NOW(), created_at))) as score " +
            "  FROM user_resource_rating WHERE resource_id = #{resourceId} GROUP BY user_id, resource_id) r1_eff " +
            "JOIN (SELECT user_id, resource_id, " +
            "  SUM(score * weight / (1 + 0.01 * DATEDIFF(NOW(), created_at))) as score " +
            "  FROM user_resource_rating WHERE resource_id != #{resourceId} GROUP BY user_id, resource_id) r2_eff " +
            "ON r1_eff.user_id = r2_eff.user_id " +
            "GROUP BY r2_eff.resource_id " +
            "HAVING common_users >= 2 " +
            "ORDER BY similarity DESC " +
            "LIMIT #{topN}")
    List<Map<String, Object>> findSimilarResources(@Param("resourceId") Long resourceId, @Param("topN") int topN);
    
    /**
     * 获取用户的兴趣标签
     */
    @Select("SELECT tag_name, interest_score, tag_category, source " +
            "FROM user_interest_tags " +
            "WHERE user_id = #{userId} " +
            "ORDER BY interest_score DESC")
    List<Map<String, Object>> getUserInterestTags(@Param("userId") Long userId);
    
    /**
     * 获取资源的关键词
     */
    @Select("SELECT keyword, tfidf_score " +
            "FROM resource_keywords " +
            "WHERE resource_id = #{resourceId}")
    List<Map<String, Object>> getResourceKeywords(@Param("resourceId") Long resourceId);
    
    /**
     * 基于标签匹配度获取推荐资源（内容推荐）
     */
    @Select("SELECT er.id, er.title, er.type, er.cover_url, er.tags, er.difficulty, " +
            "SUM(uit.interest_score * rk.tfidf_score) as match_score " +
            "FROM education_resource er " +
            "JOIN resource_keywords rk ON er.id = rk.resource_id " +
            "JOIN user_interest_tags uit ON rk.keyword = uit.tag_name " +
            "WHERE uit.user_id = #{userId} " +
            "AND er.status = 1 " +
            "AND er.id NOT IN (SELECT resource_id FROM user_resource_rating WHERE user_id = #{userId}) " +
            "GROUP BY er.id " +
            "ORDER BY match_score DESC " +
            "LIMIT #{topN}")
    List<Map<String, Object>> getContentBasedRecommendations(@Param("userId") Long userId, @Param("topN") int topN);
    
    /**
     * 基于心理画像的推荐（冷启动/补充推荐）
     * 根据用户心理画像中的主要问题和水平匹配相关资源
     */
    @Select("SELECT DISTINCT er.id, er.title, er.type, er.cover_url, er.tags, er.difficulty, " +
            "(CASE " +
            " WHEN upp.dominant_issues LIKE CONCAT('%', rk.keyword, '%') THEN 80 " +
            " WHEN rk.keyword IN ('压力管理','焦虑缓解','情绪调节','放松技巧','正念冥想') " +
            "   AND (upp.stress_level IN ('medium','high') OR upp.anxiety_level IN ('medium','high')) THEN 70 " +
            " WHEN rk.keyword IN ('抑郁干预','情绪调节','积极心理学','自我关怀') " +
            "   AND upp.depression_level IN ('medium','high') THEN 70 " +
            " WHEN rk.keyword IN ('职业规划','就业指导','职业发展') " +
            "   AND upp.dominant_issues LIKE '%就业%' THEN 75 " +
            " WHEN rk.keyword IN ('时间管理','学习效率','考试焦虑') " +
            "   AND upp.dominant_issues LIKE '%学业%' THEN 75 " +
            " ELSE 50 END) as match_score " +
            "FROM user_psychological_profile upp " +
            "CROSS JOIN resource_keywords rk " +
            "JOIN education_resource er ON rk.resource_id = er.id " +
            "WHERE upp.user_id = #{userId} " +
            "AND er.status = 1 " +
            "AND er.id NOT IN (SELECT resource_id FROM user_resource_rating WHERE user_id = #{userId}) " +
            "HAVING match_score >= 50 " +
            "ORDER BY match_score DESC " +
            "LIMIT #{topN}")
    List<Map<String, Object>> getProfileBasedRecommendations(@Param("userId") Long userId, @Param("topN") int topN);
    
    /**
     * 获取用户最近的行为日志（用于兴趣标签提取）
     */
    @Select("SELECT ubl.target_id as resource_id, ubl.behavior_type, ubl.created_at, er.tags " +
            "FROM user_behavior_log ubl " +
            "LEFT JOIN education_resource er ON ubl.target_id = er.id AND ubl.target_type = 'resource' " +
            "WHERE ubl.user_id = #{userId} " +
            "AND ubl.target_type = 'resource' " +
            "AND ubl.created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY) " +
            "ORDER BY ubl.created_at DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> getRecentBehaviorLogs(@Param("userId") Long userId, 
                                                      @Param("days") int days, 
                                                      @Param("limit") int limit);
    
    /**
     * 获取用户行为统计（按资源关键词聚合，用于更新兴趣标签）
     * 时间衰减权重融入聚合计算
     */
    @Select("SELECT rk.keyword, rk.category, " +
            "SUM(urr.score * urr.weight / (1 + 0.01 * DATEDIFF(NOW(), urr.created_at)) * rk.tfidf_score) as aggregated_score, " +
            "COUNT(DISTINCT urr.resource_id) as resource_count " +
            "FROM user_resource_rating urr " +
            "JOIN resource_keywords rk ON urr.resource_id = rk.resource_id " +
            "WHERE urr.user_id = #{userId} " +
            "AND urr.created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY) " +
            "GROUP BY rk.keyword, rk.category " +
            "ORDER BY aggregated_score DESC " +
            "LIMIT #{topN}")
    List<Map<String, Object>> getUserBehaviorKeywordStats(@Param("userId") Long userId,
                                                            @Param("days") int days,
                                                            @Param("topN") int topN);
    
    /**
     * 清理用户过期的兴趣标签（超过指定天数未更新的标签）
     */
    @Delete("DELETE FROM user_interest_tags " +
            "WHERE user_id = #{userId} " +
            "AND source = 'behavior' " +
            "AND updated_at < DATE_SUB(NOW(), INTERVAL #{days} DAY)")
    int deleteStaleInterestTags(@Param("userId") Long userId, @Param("days") int days);
    
    /**
     * 插入或更新用户心理特征
     */
    @Insert("INSERT INTO user_psychological_profile (user_id, stress_level, anxiety_level, depression_level, " +
            "dominant_issues, last_assessment_date, updated_at) " +
            "VALUES (#{userId}, #{stressLevel}, #{anxietyLevel}, #{depressionLevel}, #{dominantIssues}, CURDATE(), NOW()) " +
            "ON DUPLICATE KEY UPDATE stress_level=VALUES(stress_level), anxiety_level=VALUES(anxiety_level), " +
            "depression_level=VALUES(depression_level), dominant_issues=VALUES(dominant_issues), " +
            "last_assessment_date=CURDATE(), updated_at=NOW()")
    void insertOrUpdateProfile(@Param("userId") Long userId, @Param("stressLevel") String stressLevel,
                               @Param("anxietyLevel") String anxietyLevel, @Param("depressionLevel") String depressionLevel,
                               @Param("dominantIssues") String dominantIssues);
    
    /**
     * 插入或更新用户兴趣标签（取最大值保留）
     */
    @Insert("INSERT INTO user_interest_tags (user_id, tag_name, tag_category, interest_score, source, updated_at) " +
            "VALUES (#{userId}, #{tagName}, #{tagCategory}, #{interestScore}, #{source}, NOW()) " +
            "ON DUPLICATE KEY UPDATE interest_score=GREATEST(interest_score, VALUES(interest_score)), " +
            "tag_category=VALUES(tag_category), source=VALUES(source), updated_at=NOW()")
    void insertOrUpdateInterestTag(@Param("userId") Long userId, @Param("tagName") String tagName,
                                   @Param("source") String source, @Param("tagCategory") String tagCategory,
                                   @Param("interestScore") Double interestScore);

    /**
     * 获取用户心理画像
     */
    @Select("SELECT user_id, stress_level, anxiety_level, depression_level, dominant_issues, " +
            "personality_traits, coping_style, last_assessment_date " +
            "FROM user_psychological_profile WHERE user_id = #{userId}")
    Map<String, Object> getUserPsychologicalProfile(@Param("userId") Long userId);
}
