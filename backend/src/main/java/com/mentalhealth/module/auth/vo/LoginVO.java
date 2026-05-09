package com.mentalhealth.module.auth.vo;

import lombok.Data;

/**
 * 登录响应VO
 */
@Data
public class LoginVO {
    
    private String token;
    
    private UserInfoVO userInfo;
}
