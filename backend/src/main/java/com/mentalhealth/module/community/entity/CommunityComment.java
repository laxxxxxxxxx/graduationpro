package com.mentalhealth.module.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("community_comment")
public class CommunityComment implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long postId;
    private Long userId;
    private String anonymousId;
    private Long parentId;
    private String content;
    private Integer likeCount;
    private Integer status;
    private Date createdAt;
}
