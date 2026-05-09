package com.mentalhealth.module.resource.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 教育资源实体
 */
@Data
@TableName("education_resource")
public class EducationResource implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String title;
    
    private Integer type;
    
    private Integer categoryId;
    
    private String coverUrl;
    
    private String content;
    
    private Integer duration;
    
    private Integer difficulty;
    
    private String tags;
    
    private String author;
    
    private String source;
    
    private Integer copyrightStatus;
    
    private Integer viewCount;
    
    private Integer likeCount;

    private Integer favoriteCount;

    private Integer commentCount;

    private String description;

    private String mediaUrl;
    
    private Integer status;
    
    private Date publishTime;
    
    private Long createdBy;
    
    private Date createdAt;
    
    private Date updatedAt;
}
