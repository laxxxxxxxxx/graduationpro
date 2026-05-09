package com.mentalhealth.module.resource.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 资源评论实体
 */
@Data
@TableName("resource_comment")
public class ResourceComment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long resourceId;

    private Long userId;

    private Long parentId;

    private String content;

    private Integer likeCount;

    private Integer status;

    private Date createdAt;

    /** 非数据库字段 - 用户名 */
    @TableField(exist = false)
    private String username;

    /** 非数据库字段 - 用户头像 */
    @TableField(exist = false)
    private String avatar;

    /** 非数据库字段 - 回复列表 */
    @TableField(exist = false)
    private java.util.List<ResourceComment> replies;
}
