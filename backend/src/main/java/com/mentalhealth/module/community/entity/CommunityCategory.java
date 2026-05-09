package com.mentalhealth.module.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("community_category")
public class CommunityCategory implements Serializable {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    private String icon;
    private Integer sortOrder;
    private Integer status;
    private Date createdAt;
}
