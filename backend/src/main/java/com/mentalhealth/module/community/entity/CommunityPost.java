package com.mentalhealth.module.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("community_post")
public class CommunityPost implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String anonymousId;
    private String title;
    private String content;
    private Integer categoryId;
    private String coverImage;
    private Integer viewCount;
    private Integer commentCount;
    private Integer likeCount;
    private Integer status;
    private Integer auditResult;
    private String auditReason;
    private Integer isTop;
    private Integer isEssence;
    private Date createdAt;
    private Date updatedAt;

    @TableField(exist = false)
    private String categoryName;

    @TableField(exist = false)
    private Boolean liked;
}
