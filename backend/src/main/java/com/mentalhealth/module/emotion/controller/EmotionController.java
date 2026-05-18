package com.mentalhealth.module.emotion.controller;

import com.mentalhealth.common.result.Result;
import com.mentalhealth.common.utils.JwtUtil;
import com.mentalhealth.module.emotion.entity.EmotionDiary;
import com.mentalhealth.module.emotion.service.EmotionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/emotion")
@Api(tags = "情绪日记")
public class EmotionController {
    
    @Autowired
    private EmotionService emotionService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @PostMapping("/diary")
    @ApiOperation("创建日记")
    public Result<Void> createDiary(
            @RequestHeader("Authorization") String token,
            @RequestBody EmotionDiary diary) {
        
        try {
            String actualToken = token.replace("Bearer ", "");
            Long userId = jwtUtil.getUserIdFromToken(actualToken);
            
            emotionService.createDiary(userId, diary);
            return Result.success("保存成功", null);
        } catch (Exception e) {
            log.error("创建日记失败: {}", e.getMessage(), e);
            return Result.error("保存失败: " + e.getMessage());
        }
    }
    
    @PutMapping("/diary/{id}")
    @ApiOperation("更新日记")
    public Result<Void> updateDiary(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id,
            @RequestBody EmotionDiary diary) {
        
        try {
            String actualToken = token.replace("Bearer ", "");
            Long userId = jwtUtil.getUserIdFromToken(actualToken);
            
            emotionService.updateDiary(userId, id, diary);
            return Result.success("更新成功", null);
        } catch (Exception e) {
            log.error("更新日记失败: {}", e.getMessage(), e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/diary/{id}")
    @ApiOperation("删除日记")
    public Result<Void> deleteDiary(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        
        String actualToken = token.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(actualToken);
        
        emotionService.deleteDiary(userId, id);
        return Result.success("删除成功", null);
    }
    
    @GetMapping("/diary/list")
    @ApiOperation("获取日记列表")
    public Result<List<EmotionDiary>> getDiaryList(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        
        String actualToken = token.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(actualToken);
        
        List<EmotionDiary> list = emotionService.getDiaryList(userId, pageNum, pageSize);
        return Result.success(list);
    }
    
    @GetMapping("/diary/{id}")
    @ApiOperation("获取日记详情")
    public Result<EmotionDiary> getDiaryDetail(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        
        String actualToken = token.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(actualToken);
        
        EmotionDiary diary = emotionService.getDiaryDetail(userId, id);
        return Result.success(diary);
    }
    
    @GetMapping("/statistics")
    @ApiOperation("获取情绪统计")
    public Result<Map<String, Object>> getStatistics(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        String actualToken = token.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(actualToken);
        
        Map<String, Object> stats = emotionService.getStatistics(userId, startDate, endDate);
        return Result.success(stats);
    }
}
