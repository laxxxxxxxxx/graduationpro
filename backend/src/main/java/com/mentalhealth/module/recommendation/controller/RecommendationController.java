package com.mentalhealth.module.recommendation.controller;

import com.mentalhealth.common.result.Result;
import com.mentalhealth.common.utils.JwtUtil;
import com.mentalhealth.module.recommendation.service.RecommendationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/recommendation")
@Api(tags = "个性化推荐")
public class RecommendationController {
    
    @Autowired
    private RecommendationService recommendationService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @GetMapping("/personalized")
    @ApiOperation("获取个性化推荐列表")
    public Result<List<Map<String, Object>>> getPersonalizedRecommendations(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "10") Integer topN) {
        
        String actualToken = token.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(actualToken);
        
        List<Map<String, Object>> recommendations = recommendationService.getPersonalizedRecommendations(userId, topN);
        return Result.success(recommendations);
    }
    
    @GetMapping("/user-cf")
    @ApiOperation("基于用户的协同过滤推荐")
    public Result<List<Map<String, Object>>> getUserBasedCF(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "10") Integer topN) {
        
        String actualToken = token.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(actualToken);
        
        List<Map<String, Object>> recommendations = recommendationService.userBasedCollaborativeFiltering(userId, topN);
        return Result.success(recommendations);
    }
    
    @GetMapping("/item-cf")
    @ApiOperation("基于物品的协同过滤推荐")
    public Result<List<Map<String, Object>>> getItemBasedCF(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "10") Integer topN) {
        
        String actualToken = token.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(actualToken);
        
        List<Map<String, Object>> recommendations = recommendationService.itemBasedCollaborativeFiltering(userId, topN);
        return Result.success(recommendations);
    }
    
    @GetMapping("/content-based")
    @ApiOperation("基于内容的推荐")
    public Result<List<Map<String, Object>>> getContentBased(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "10") Integer topN) {
        
        String actualToken = token.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(actualToken);
        
        List<Map<String, Object>> recommendations = recommendationService.contentBasedRecommendation(userId, topN);
        return Result.success(recommendations);
    }
    
    @PostMapping("/click")
    @ApiOperation("记录推荐点击")
    public Result<Void> recordClick(
            @RequestHeader("Authorization") String token,
            @RequestParam Long resourceId) {
        
        String actualToken = token.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(actualToken);
        
        recommendationService.recordClick(userId, resourceId);
        return Result.success("记录成功", null);
    }
    
    @GetMapping("/profile-based")
    @ApiOperation("基于心理画像的推荐")
    public Result<List<Map<String, Object>>> getProfileBased(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "10") Integer topN) {
        
        String actualToken = token.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(actualToken);
        
        List<Map<String, Object>> recommendations = recommendationService.getProfileBasedRecommendations(userId, topN);
        return Result.success(recommendations);
    }
    
    @PostMapping("/refresh-tags")
    @ApiOperation("刷新用户兴趣标签（基于最近行为）")
    public Result<Void> refreshInterestTags(
            @RequestHeader("Authorization") String token) {
        
        String actualToken = token.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(actualToken);
        
        recommendationService.updateUserInterestTags(userId);
        return Result.success("兴趣标签已刷新", null);
    }
}
