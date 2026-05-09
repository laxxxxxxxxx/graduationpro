package com.mentalhealth.module.recommendation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 推荐结果实体
 */
@Data
@TableName("recommendation_result")
public class RecommendationResult implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private Long resourceId;
    
    /**
     * 算法类型: collaborative/content_based/hybrid
     */
    private String algorithmType;
    
    /**
     * 置信度(0-100)
     */
    private Double confidenceScore;
    
    /**
     * 推荐理由
     */
    private String reason;
    
    /**
     * 展示顺序
     */
    private Integer displayOrder;
    
    /**
     * 是否点击
     */
    private Integer isClicked;
    
    /**
     * 是否有效推荐
     */
    private Integer isEffective;
    
    private Date createdAt;
    
    private Date expiredAt;
}
