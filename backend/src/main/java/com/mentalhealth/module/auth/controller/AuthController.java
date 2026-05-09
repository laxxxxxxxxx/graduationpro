package com.mentalhealth.module.auth.controller;

import com.mentalhealth.common.result.Result;
import com.mentalhealth.common.utils.JwtUtil;
import com.mentalhealth.module.auth.dto.LoginRequest;
import com.mentalhealth.module.auth.dto.RegisterRequest;
import com.mentalhealth.module.auth.service.AuthService;
import com.mentalhealth.module.auth.vo.LoginVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@Api(tags = "认证管理")
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @PostMapping("/login")
    @ApiOperation("用户登录")
    public Result<LoginVO> login(@Validated @RequestBody LoginRequest request) {
        LoginVO loginVO = authService.login(request);
        return Result.success(loginVO);
    }
    
    @PostMapping("/register")
    @ApiOperation("用户注册")
    public Result<Void> register(@Validated @RequestBody RegisterRequest request) {
        authService.register(request);
        return Result.success("注册成功", null);
    }
    
    @PostMapping("/wx-login")
    @ApiOperation("微信登录")
    public Result<LoginVO> wxLogin(@RequestParam String code) {
        LoginVO loginVO = authService.wxLogin(code);
        return Result.success(loginVO);
    }
    
    @PostMapping("/logout")
    @ApiOperation("退出登录")
    public Result<Void> logout(@RequestHeader("Authorization") String token) {
        // 从token中获取userId
        String actualToken = token.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(actualToken);
        authService.logout(userId);
        return Result.success("退出成功", null);
    }
}
