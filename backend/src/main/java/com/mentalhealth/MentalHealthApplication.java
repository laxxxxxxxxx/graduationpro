package com.mentalhealth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 大学生心理健康教育与自测平台 - 启动类
 */
@SpringBootApplication
@MapperScan("com.mentalhealth.module.**.mapper")
public class MentalHealthApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(MentalHealthApplication.class, args);
        System.out.println("========================================");
        System.out.println("   大学生心理健康平台启动成功!");
        System.out.println("   API文档: http://localhost:8080/api/doc.html");
        System.out.println("========================================");
    }
}
