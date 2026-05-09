package com.mentalhealth.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload-path:uploads/}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 确保上传目录存在
        File dir = new File(uploadPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        String absolutePath = dir.getAbsolutePath() + File.separator;
        
        // 映射 /uploads/** 到 本地磁盘路径
        // 注意：由于 application.yml 中配置了 context-path: /api
        // 所以实际访问路径是 http://localhost:8080/api/uploads/**
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absolutePath);
                
        // 兼容Knife4j/Swagger
        registry.addResourceHandler("doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
    }
}
