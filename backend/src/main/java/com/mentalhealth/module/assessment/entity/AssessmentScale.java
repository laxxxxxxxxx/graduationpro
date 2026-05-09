package com.mentalhealth.module.assessment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("assessment_scale")
public class AssessmentScale implements Serializable {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    private String code;
    private String description;
    private Integer totalQuestions;
    private Integer estimatedTime;
    private String scoringRule;
    private String interpretationTemplate;
    private Integer status;
    private Date createdAt;
    private Date updatedAt;
}
