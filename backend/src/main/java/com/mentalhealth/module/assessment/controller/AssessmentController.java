package com.mentalhealth.module.assessment.controller;

import com.mentalhealth.common.result.Result;
import com.mentalhealth.common.utils.JwtUtil;
import com.mentalhealth.module.assessment.entity.AssessmentScale;
import com.mentalhealth.module.assessment.entity.ScaleQuestion;
import com.mentalhealth.module.assessment.service.AssessmentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/assessment")
@Api(tags = "测评评估")
public class AssessmentController {
    
    @Autowired
    private AssessmentService assessmentService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @GetMapping("/scales")
    @ApiOperation("获取量表列表")
    public Result<List<AssessmentScale>> getScaleList() {
        List<AssessmentScale> list = assessmentService.getScaleList();
        return Result.success(list);
    }
    
    @GetMapping("/scales/{scaleId}/questions")
    @ApiOperation("获取量表题目")
    public Result<List<ScaleQuestion>> getQuestions(@PathVariable Integer scaleId) {
        List<ScaleQuestion> questions = assessmentService.getScaleQuestions(scaleId);
        return Result.success(questions);
    }
    
    @PostMapping("/submit")
    @ApiOperation("提交答卷")
    public Result<Map<String, Object>> submit(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request) {
        
        try {
            String actualToken = token.replace("Bearer ", "");
            Long userId = jwtUtil.getUserIdFromToken(actualToken);
            
            // 安全的类型转换
            Integer scaleId = request.get("scaleId") instanceof Integer 
                ? (Integer) request.get("scaleId") 
                : Integer.parseInt(request.get("scaleId").toString());
            
            String answers = request.get("answers").toString();
            
            Integer completionTime = request.get("completionTime") instanceof Integer 
                ? (Integer) request.get("completionTime") 
                : Integer.parseInt(request.get("completionTime").toString());
            
            Map<String, Object> result = assessmentService.submitAssessment(userId, scaleId, answers, completionTime);
            return Result.success(result);
        } catch (Exception e) {
            log.error("提交测评失败", e);
            return Result.error("提交失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/history")
    @ApiOperation("获取测评历史")
    public Result<List<Map<String, Object>>> getHistory(@RequestHeader("Authorization") String token) {
        String actualToken = token.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(actualToken);
        
        List<Map<String, Object>> history = assessmentService.getAssessmentHistory(userId);
        return Result.success(history);
    }
    
    @GetMapping("/report/{id}")
    @ApiOperation("获取单次测评完整报告")
    public Result<Map<String, Object>> getReportDetail(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        String actualToken = token.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(actualToken);
        
        Map<String, Object> report = assessmentService.getReportDetail(id, userId);
        return Result.success(report);
    }
    
    @GetMapping("/reports")
    @ApiOperation("获取测评档案(含趋势分析)")
    public Result<Map<String, Object>> getAssessmentReports(@RequestHeader("Authorization") String token) {
        String actualToken = token.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(actualToken);
        
        Map<String, Object> reports = assessmentService.getAssessmentReports(userId);
        return Result.success(reports);
    }
}
