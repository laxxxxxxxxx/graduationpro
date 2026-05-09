package com.mentalhealth.module.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户实体类
 */
@Data
@TableName("user")
public class User implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String username;
    
    private String password;
    
    private String email;
    
    private String phone;
    
    private String realName;
    
    private Integer gender;
    
    private Integer age;
    
    private String university;
    
    private String major;
    
    private String grade;
    
    private Integer role;
    
    private String avatar;
    
    private Integer status;
    
    private String openid;
    
    private String sessionKey;
    
    private Date createdAt;
    
    private Date updatedAt;
}
