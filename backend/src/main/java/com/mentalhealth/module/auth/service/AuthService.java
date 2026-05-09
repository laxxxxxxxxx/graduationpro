package com.mentalhealth.module.auth.service;

import com.mentalhealth.module.auth.dto.LoginRequest;
import com.mentalhealth.module.auth.dto.RegisterRequest;
import com.mentalhealth.module.auth.vo.LoginVO;

/**
 * 认证服务接口
 */
public interface AuthService {
    
    /**
     * 用户登录
     */
    LoginVO login(LoginRequest request);
    
    /**
     * 用户注册
     */
    void register(RegisterRequest request);
    
    /**
     * 退出登录
     */
    void logout(Long userId);
}
