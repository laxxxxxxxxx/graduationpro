package com.mentalhealth.module.recommendation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户资源评分实体（用于协同过滤）
 */
@Data
@TableName("user_resource_rating")
public class UserResourceRating implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private Long resourceId;
    
    /**
     * 评分类型: 1-浏览 2-点赞 3-收藏 4-完成学习 5-分享
     */
    private Integer ratingType;
    
    /**
     * 评分值(0-5)
     */
    private Double score;
    
    /**
     * 权重(考虑时间衰减)
     */
    private Double weight;
    
    private Date createdAt;
}
