package com.mentalhealth.module.assessment.service;

import com.mentalhealth.module.assessment.entity.AssessmentScale;
import com.mentalhealth.module.assessment.entity.ScaleQuestion;

import java.util.List;
import java.util.Map;

public interface AssessmentService {
    
    List<AssessmentScale> getScaleList();
    
    List<ScaleQuestion> getScaleQuestions(Integer scaleId);
    
    Map<String, Object> submitAssessment(Long userId, Integer scaleId, String answersJson, Integer completionTime);
    
    List<Map<String, Object>> getAssessmentHistory(Long userId);
    
    /**
     * 获取单次测评完整报告
     */
    Map<String, Object> getReportDetail(Long assessmentId, Long userId);
    
    /**
     * 获取用户所有测评报告（含趋势分析）
     */
    Map<String, Object> getAssessmentReports(Long userId);
}
