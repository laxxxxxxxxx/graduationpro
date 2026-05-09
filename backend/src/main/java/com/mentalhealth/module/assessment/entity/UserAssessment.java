package com.mentalhealth.module.assessment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("user_assessment")
public class UserAssessment implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private Integer scaleId;
    
    private BigDecimal totalScore;
    
    private String resultLevel;
    
    private String interpretation;
    
    private String suggestions;
    
    private String answers;
    
    private Integer completionTime;
    
    /**
     * 维度得分JSON: [{name:"躯体化",score:2.5,maxScore:5},...]
     */
    private String dimensionScores;
    
    /**
     * 完整报告数据JSON(含雷达图、趋势、详细建议)
     */
    private String reportData;
    
    /**
     * 报告生成时间
     */
    private LocalDateTime reportGeneratedAt;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
