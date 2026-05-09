package com.mentalhealth.module.auth.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 微信服务类
 */
@Slf4j
@Service
public class WechatService {
    
    @Value("${wechat.miniapp.appid}")
    private String appid;
    
    @Value("${wechat.miniapp.secret}")
    private String secret;
    
    private static final String CODE2SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session";
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * 微信登录响应
     */
    @Data
    public static class WxLoginResponse {
        private String openid;
        private String session_key;
        private String unionid;
        private Integer errcode;
        private String errmsg;
    }
    
    /**
     * 通过code获取openid和session_key
     * @param code 微信登录凭证
     * @return 微信登录响应
     */
    public WxLoginResponse code2Session(String code) {
        try {
            // 构建请求URL
            String url = String.format("%s?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                    CODE2SESSION_URL, appid, secret, code);
            
            log.info("调用微信接口: {}", url);
            
            // 调用微信接口
            ResponseEntity<WxLoginResponse> response = restTemplate.getForEntity(url, WxLoginResponse.class);
            WxLoginResponse result = response.getBody();
            
            if (result != null && result.getErrcode() != null && result.getErrcode() != 0) {
                log.error("微信接口返回错误: errcode={}, errmsg={}", result.getErrcode(), result.getErrmsg());
                throw new RuntimeException("微信登录失败: " + result.getErrmsg());
            }
            
            log.info("微信登录成功, openid: {}", result != null ? result.getOpenid() : null);
            return result;
            
        } catch (Exception e) {
            log.error("调用微信接口异常", e);
            throw new RuntimeException("微信登录失败: " + e.getMessage());
        }
    }
}
