package com.mentalhealth.module.auth.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mentalhealth.common.exception.BusinessException;
import com.mentalhealth.common.utils.JwtUtil;
import com.mentalhealth.module.auth.dto.LoginRequest;
import com.mentalhealth.module.auth.dto.RegisterRequest;
import com.mentalhealth.module.auth.entity.User;
import com.mentalhealth.module.auth.mapper.UserMapper;
import com.mentalhealth.module.auth.service.AuthService;
import com.mentalhealth.module.auth.vo.LoginVO;
import com.mentalhealth.module.auth.vo.UserInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证服务实现类（简化版）
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Override
    public LoginVO login(LoginRequest request) {
        // 查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        User user = userMapper.selectOne(wrapper);
        
        if (user == null) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        
        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        
        // 检查用户状态
        if (user.getStatus() == 0) {
            throw new BusinessException(403, "账号已被禁用");
        }
        
        // 生成Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        
        // 构建返回结果
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setUserInfo(convertToUserInfoVO(user));
        
        log.info("用户登录成功: {}", user.getUsername());
        return loginVO;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterRequest request) {
        // 检查用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("用户名已存在");
        }
        
        // 检查邮箱是否已存在
        if (request.getEmail() != null) {
            wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getEmail, request.getEmail());
            if (userMapper.selectCount(wrapper) > 0) {
                throw new BusinessException("邮箱已被注册");
            }
        }
        
        // 创建用户
        User user = new User();
        BeanUtil.copyProperties(request, user);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(1); // 默认学生角色
        user.setStatus(1);
        
        userMapper.insert(user);
        log.info("用户注册成功: {}", user.getUsername());
    }
    
    @Override
    public void logout(Long userId) {
        // JWT无状态,前端删除token即可
        // 如果需要服务端维护黑名单,可以在这里将token加入Redis黑名单
        log.info("用户退出登录: {}", userId);
    }
    
    /**
     * 转换为UserInfoVO
     */
    private UserInfoVO convertToUserInfoVO(User user) {
        UserInfoVO vo = new UserInfoVO();
        BeanUtil.copyProperties(user, vo);
        return vo;
    }
}
