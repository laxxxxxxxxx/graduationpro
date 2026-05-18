package com.mentalhealth.module.assessment.controller;

import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentalhealth.common.exception.BusinessException;
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

    @Autowired
    private ObjectMapper objectMapper;
    
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
            if (token == null || !token.startsWith("Bearer ")) {
                return Result.error("未登录或Token无效");
            }
            String actualToken = token.replace("Bearer ", "");
            Long userId = jwtUtil.getUserIdFromToken(actualToken);
            if (userId == null) {
                return Result.error("登录状态失效，请重新登录");
            }
            
            Object scaleIdRaw = request.get("scaleId");
            if (scaleIdRaw == null) return Result.error("缺少量表ID");
            Integer scaleId = scaleIdRaw instanceof Integer 
                ? (Integer) scaleIdRaw 
                : Integer.parseInt(scaleIdRaw.toString().split("\\.")[0]);
            
            Object answersRaw = request.get("answers");
            if (answersRaw == null) return Result.error("答题内容不能为空");
            
            // 使用 Jackson 强制序列化为标准 JSON 字符串，彻底解决 FastJSON 有时会变成 toString() 的问题
            String answers = objectMapper.writeValueAsString(answersRaw);
            
            Object timeRaw = request.get("completionTime");
            Integer completionTime = 0;
            if (timeRaw != null) {
                if (timeRaw instanceof Integer) {
                    completionTime = (Integer) timeRaw;
                } else {
                    completionTime = Integer.parseInt(timeRaw.toString().split("\\.")[0]);
                }
            }
            
            Map<String, Object> result = assessmentService.submitAssessment(userId, scaleId, answers, completionTime);
            return Result.success(result);
        } catch (BusinessException e) {
            log.error("提交测评业务异常: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("提交测评系统异常", e);
            return Result.error("提交失败: " + (e.getMessage() != null ? e.getMessage() : "内部错误"));
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
