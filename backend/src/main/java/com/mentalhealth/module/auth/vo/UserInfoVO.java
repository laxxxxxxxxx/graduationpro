package com.mentalhealth.module.auth.vo;

import lombok.Data;

/**
 * 用户信息VO
 */
@Data
public class UserInfoVO {
    
    private Long id;
    
    private String username;
    
    private String realName;
    
    private Integer gender;
    
    private Integer age;
    
    private String email;
    
    private String phone;
    
    private String university;
    
    private String major;
    
    private String grade;
    
    private Integer role;
    
    private String avatar;
}
