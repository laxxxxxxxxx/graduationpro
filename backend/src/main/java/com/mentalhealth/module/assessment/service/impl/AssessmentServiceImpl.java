package com.mentalhealth.module.assessment.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mentalhealth.common.exception.BusinessException;
import com.mentalhealth.module.assessment.entity.AssessmentScale;
import com.mentalhealth.module.assessment.entity.ScaleQuestion;
import com.mentalhealth.module.assessment.entity.UserAssessment;
import com.mentalhealth.module.assessment.mapper.AssessmentScaleMapper;
import com.mentalhealth.module.assessment.mapper.ScaleQuestionMapper;
import com.mentalhealth.module.assessment.mapper.UserAssessmentMapper;
import com.mentalhealth.module.assessment.service.AssessmentService;
import com.mentalhealth.module.recommendation.mapper.UserResourceRatingMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AssessmentServiceImpl implements AssessmentService {
    
    @Autowired
    private AssessmentScaleMapper scaleMapper;
    
    @Autowired
    private ScaleQuestionMapper questionMapper;
    
    @Autowired
    private UserAssessmentMapper userAssessmentMapper;
    
    @Autowired
    private UserResourceRatingMapper ratingMapper;
    
    @Override
    public List<AssessmentScale> getScaleList() {
        LambdaQueryWrapper<AssessmentScale> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssessmentScale::getStatus, 1);
        return scaleMapper.selectList(wrapper);
    }
    
    @Override
    public List<ScaleQuestion> getScaleQuestions(Integer scaleId) {
        LambdaQueryWrapper<ScaleQuestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScaleQuestion::getScaleId, scaleId);
        wrapper.orderByAsc(ScaleQuestion::getQuestionNo);
        return questionMapper.selectList(wrapper);
    }
    
    @Override
    @Transactional
    public Map<String, Object> submitAssessment(Long userId, Integer scaleId, String answersJson, Integer completionTime) {
        AssessmentScale scale = scaleMapper.selectById(scaleId);
        if (scale == null) {
            throw new BusinessException("量表不存在");
        }
        
        JSONArray answers = JSON.parseArray(answersJson);
        String scaleCode = scale.getCode();
        
        // 根据量表类型执行不同的评分逻辑
        Map<String, Object> scoreResult;
        if ("SCL90".equals(scaleCode)) {
            scoreResult = scoreSCL90(answers);
        } else if ("UPI".equals(scaleCode)) {
            scoreResult = scoreUPI(answers);
        } else {
            scoreResult = scoreSDS(answers);
        }
        
        double totalScore = ((Number) scoreResult.get("totalScore")).doubleValue();
        String level = (String) scoreResult.get("level");
        
        // 生成完整报告数据
        Map<String, Object> reportData = generateReportData(scale, scoreResult, answers, completionTime);
        String reportJson = JSON.toJSONString(reportData);
        String dimensionJson = JSON.toJSONString(scoreResult.get("dimensions"));
        
        // 生成权威解读和建议
        String interpretation = generateProfessionalInterpretation(scaleCode, scoreResult);
        String suggestions = generateProfessionalSuggestions(scaleCode, scoreResult);
        
        // 保存测评记录
        UserAssessment userAssessment = new UserAssessment();
        userAssessment.setUserId(userId);
        userAssessment.setScaleId(scaleId);
        userAssessment.setTotalScore(BigDecimal.valueOf(totalScore).setScale(2, RoundingMode.HALF_UP));
        userAssessment.setResultLevel(level);
        userAssessment.setInterpretation(interpretation);
        userAssessment.setSuggestions(suggestions);
        userAssessment.setAnswers(answersJson);
        userAssessment.setCompletionTime(completionTime);
        userAssessment.setDimensionScores(dimensionJson);
        userAssessment.setReportData(reportJson);
        userAssessment.setReportGeneratedAt(LocalDateTime.now());
        userAssessmentMapper.insert(userAssessment);
        
        // 更新推荐系统的用户画像和兴趣标签
        updateRecommendationProfile(userId, scaleCode, scoreResult);
        
        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("assessmentId", userAssessment.getId());
        result.put("scaleId", scaleId);
        result.put("scaleName", scale.getName());
        result.put("scaleCode", scaleCode);
        result.put("totalScore", totalScore);
        result.put("resultLevel", level);
        result.put("levelDescription", scoreResult.get("levelDescription"));
        result.put("interpretation", interpretation);
        result.put("suggestions", suggestions);
        result.put("completionTime", completionTime);
        result.put("dimensions", scoreResult.get("dimensions"));
        result.put("riskFlags", scoreResult.get("riskFlags"));
        result.put("reportData", reportData);
        
        log.info("用户{}完成{}测评,得分{},等级:{}", userId, scaleCode, totalScore, level);
        return result;
    }
    
    @Override
    public List<Map<String, Object>> getAssessmentHistory(Long userId) {
        LambdaQueryWrapper<UserAssessment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAssessment::getUserId, userId);
        wrapper.orderByDesc(UserAssessment::getCreatedAt);
        wrapper.last("LIMIT 50");
        
        List<UserAssessment> records = userAssessmentMapper.selectList(wrapper);
        
        List<Map<String, Object>> history = new ArrayList<>();
        for (UserAssessment record : records) {
            AssessmentScale scale = scaleMapper.selectById(record.getScaleId());
            Map<String, Object> item = new HashMap<>();
            item.put("id", record.getId());
            item.put("scaleId", record.getScaleId());
            item.put("scaleName", scale != null ? scale.getName() : "未知量表");
            item.put("scaleCode", scale != null ? scale.getCode() : "");
            item.put("totalScore", record.getTotalScore());
            item.put("resultLevel", record.getResultLevel());
            item.put("interpretation", record.getInterpretation());
            item.put("completionTime", record.getCompletionTime());
            item.put("createdAt", record.getCreatedAt());
            history.add(item);
        }
        
        return history;
    }
    
    @Override
    public Map<String, Object> getReportDetail(Long assessmentId, Long userId) {
        UserAssessment record = userAssessmentMapper.selectById(assessmentId);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BusinessException("测评记录不存在");
        }
        
        AssessmentScale scale = scaleMapper.selectById(record.getScaleId());
        
        Map<String, Object> report = new HashMap<>();
        report.put("id", record.getId());
        report.put("scaleId", record.getScaleId());
        report.put("scaleName", scale != null ? scale.getName() : "未知");
        report.put("scaleCode", scale != null ? scale.getCode() : "");
        report.put("totalScore", record.getTotalScore());
        report.put("resultLevel", record.getResultLevel());
        report.put("interpretation", record.getInterpretation());
        report.put("suggestions", record.getSuggestions());
        report.put("completionTime", record.getCompletionTime());
        report.put("createdAt", record.getCreatedAt());
        
        // 解析维度得分
        if (record.getDimensionScores() != null) {
            report.put("dimensions", JSON.parseArray(record.getDimensionScores()));
        }
        // 解析完整报告数据
        if (record.getReportData() != null) {
            report.put("reportData", JSON.parseObject(record.getReportData()));
        }
        
        return report;
    }
    
    @Override
    public Map<String, Object> getAssessmentReports(Long userId) {
        LambdaQueryWrapper<UserAssessment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAssessment::getUserId, userId);
        wrapper.orderByDesc(UserAssessment::getCreatedAt);
        
        List<UserAssessment> records = userAssessmentMapper.selectList(wrapper);
        
        // 按量表分组构建趋势数据
        Map<String, List<Map<String, Object>>> scaleTrends = new LinkedHashMap<>();
        List<Map<String, Object>> reportList = new ArrayList<>();
        
        for (UserAssessment record : records) {
            AssessmentScale scale = scaleMapper.selectById(record.getScaleId());
            String scaleCode = scale != null ? scale.getCode() : "unknown";
            
            Map<String, Object> item = new HashMap<>();
            item.put("id", record.getId());
            item.put("scaleId", record.getScaleId());
            item.put("scaleName", scale != null ? scale.getName() : "未知");
            item.put("scaleCode", scaleCode);
            item.put("totalScore", record.getTotalScore());
            item.put("resultLevel", record.getResultLevel());
            item.put("completionTime", record.getCompletionTime());
            item.put("createdAt", record.getCreatedAt());
            reportList.add(item);
            
            // 构建趋势点
            Map<String, Object> trendPoint = new HashMap<>();
            trendPoint.put("date", record.getCreatedAt() != null ? record.getCreatedAt().toLocalDate().toString() : "");
            trendPoint.put("score", record.getTotalScore());
            trendPoint.put("level", record.getResultLevel());
            trendPoint.put("id", record.getId());
            
            scaleTrends.computeIfAbsent(scaleCode, k -> new ArrayList<>()).add(trendPoint);
        }
        
        // 反转趋势列表使时间正序
        Map<String, Object> result = new HashMap<>();
        result.put("reports", reportList);
        result.put("totalCount", records.size());
        
        Map<String, List<Map<String, Object>>> trends = new LinkedHashMap<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : scaleTrends.entrySet()) {
            List<Map<String, Object>> reversed = new ArrayList<>(entry.getValue());
            Collections.reverse(reversed);
            trends.put(entry.getKey(), reversed);
        }
        result.put("trends", trends);
        
        return result;
    }
    
    // ============================================
    // SDS抑郁自评量表评分（20题，标准分=粗分×1.25）
    // ============================================
    private Map<String, Object> scoreSDS(JSONArray answers) {
        // SDS正向题: 1,3,4,7,8,9,10,13,15,19 (1-4分), 反向题: 2,5,6,11,12,14,16,17,18,20 (4-1分)
        Set<Integer> reverseItems = new HashSet<>(Arrays.asList(2, 5, 6, 11, 12, 14, 16, 17, 18, 20));
        
        double rawScore = 0;
        Map<String, Double> dimensionScores = new LinkedHashMap<>();
        
        // SDS四维度: 情感症状(1,3)、躯体症状(4,7,8,9,10)、精神运动症状(12,13)、心理症状(11,14,15,16,17,18,19,20)
        double affectiveScore = 0;  // 情感症状
        double somaticScore = 0;    // 躯体症状
        double psychomotorScore = 0; // 精神运动症状
        double psychologicalScore = 0; // 心理症状
        int affectiveCount = 0, somaticCount = 0, psychomotorCount = 0, psychologicalCount = 0;
        
        for (int i = 0; i < answers.size(); i++) {
            JSONObject answer = answers.getJSONObject(i);
            int itemNo = i + 1;
            int score = answer.getInteger("answer");
            // 反向计分
            if (reverseItems.contains(itemNo)) {
                score = 5 - score;
            }
            rawScore += score;
            
            // 维度归类
            if (itemNo == 1 || itemNo == 3) {
                affectiveScore += score; affectiveCount++;
            } else if (itemNo == 4 || itemNo == 7 || itemNo == 8 || itemNo == 9 || itemNo == 10) {
                somaticScore += score; somaticCount++;
            } else if (itemNo == 12 || itemNo == 13) {
                psychomotorScore += score; psychomotorCount++;
            } else {
                psychologicalScore += score; psychologicalCount++;
            }
        }
        
        double standardScore = rawScore * 1.25;
        
        // 维度平均分(1-4范围)
        dimensionScores.put("情感症状", round(affectiveScore / Math.max(affectiveCount, 1), 2));
        dimensionScores.put("躯体症状", round(somaticScore / Math.max(somaticCount, 1), 2));
        dimensionScores.put("精神运动性症状", round(psychomotorScore / Math.max(psychomotorCount, 1), 2));
        dimensionScores.put("心理症状", round(psychologicalScore / Math.max(psychologicalCount, 1), 2));
        
        String level;
        String levelDesc;
        if (standardScore < 53) {
            level = "正常"; levelDesc = "无抑郁症状，心理健康状况良好";
        } else if (standardScore < 63) {
            level = "轻度抑郁"; levelDesc = "存在轻度抑郁倾向，建议关注情绪变化";
        } else if (standardScore < 73) {
            level = "中度抑郁"; levelDesc = "存在中度抑郁症状，建议及时寻求专业帮助";
        } else {
            level = "重度抑郁"; levelDesc = "存在重度抑郁症状，强烈建议尽快就医";
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalScore", round(standardScore, 2));
        result.put("rawScore", rawScore);
        result.put("level", level);
        result.put("levelDescription", levelDesc);
        result.put("dimensions", buildDimensionList(dimensionScores, 4.0));
        result.put("riskFlags", level.equals("正常") ? new ArrayList<>() : 
            Collections.singletonList(level.contains("重度") ? "⚠️ 存在较高风险，建议尽快就医评估" : "⚡ 建议关注心理健康，必要时寻求专业帮助"));
        return result;
    }
    
    // ============================================
    // SCL-90症状自评量表评分（90题，10因子）
    // ============================================
    private Map<String, Object> scoreSCL90(JSONArray answers) {
        // SCL-90 10个因子维度映射
        Map<String, int[]> factorItems = new LinkedHashMap<>();
        factorItems.put("躯体化", new int[]{1, 4, 12, 27, 40, 42, 48, 49, 52, 53, 56, 58});
        factorItems.put("强迫症状", new int[]{3, 9, 10, 28, 38, 45, 46, 51, 55, 65});
        factorItems.put("人际关系敏感", new int[]{6, 21, 34, 36, 37, 41, 61, 69, 73});
        factorItems.put("抑郁", new int[]{5, 14, 15, 20, 22, 26, 29, 30, 31, 32, 54, 71, 79});
        factorItems.put("焦虑", new int[]{2, 17, 23, 33, 39, 57, 72, 78, 80, 86});
        factorItems.put("敌对", new int[]{11, 24, 63, 67, 74, 81});
        factorItems.put("恐怖", new int[]{13, 25, 47, 50, 70, 75, 82});
        factorItems.put("偏执", new int[]{8, 18, 43, 68, 76, 83});
        factorItems.put("精神病性", new int[]{7, 16, 35, 62, 77, 84, 85, 87, 88, 90});
        factorItems.put("附加项目", new int[]{19, 44, 59, 60, 64, 66, 89});
        
        // 解析答案：建立题号->分数的映射
        Map<Integer, Integer> answerMap = new HashMap<>();
        double totalScore = 0;
        for (int i = 0; i < answers.size(); i++) {
            JSONObject answer = answers.getJSONObject(i);
            int itemNo = answer.getInteger("questionNo") != null ? answer.getInteger("questionNo") : (i + 1);
            int score = answer.getInteger("answer");
            answerMap.put(itemNo, score);
            totalScore += score;
        }
        
        int answeredCount = answers.size();
        
        // 计算各因子分
        Map<String, Double> dimensionScores = new LinkedHashMap<>();
        List<String> highDimensions = new ArrayList<>();
        
        for (Map.Entry<String, int[]> entry : factorItems.entrySet()) {
            String factorName = entry.getKey();
            int[] items = entry.getValue();
            double sum = 0;
            int count = 0;
            for (int item : items) {
                Integer score = answerMap.get(item);
                if (score != null) {
                    sum += score;
                    count++;
                }
            }
            double avg = count > 0 ? sum / count : 0;
            dimensionScores.put(factorName, round(avg, 2));
            if (avg >= 3.0) {
                highDimensions.add(factorName);
            }
        }
        
        // 判断等级
        String level;
        String levelDesc;
        if (totalScore <= 160) {
            level = "正常"; levelDesc = "心理健康状况良好，无明显心理问题";
        } else if (totalScore <= 200) {
            level = "轻度"; levelDesc = "存在轻度心理不适，建议自我调适或寻求心理咨询";
        } else if (totalScore <= 250) {
            level = "中度"; levelDesc = "存在中度心理问题，建议尽快寻求专业心理咨询";
        } else {
            level = "重度"; levelDesc = "存在较严重心理问题，强烈建议尽快就医评估";
        }
        
        List<String> riskFlags = new ArrayList<>();
        if (!highDimensions.isEmpty()) {
            riskFlags.add("⚡ 偏高维度: " + String.join("、", highDimensions) + "，建议重点关注");
        }
        if (totalScore > 200) {
            riskFlags.add("⚠️ 总分偏高，建议尽快进行专业评估");
        }
        if (answerMap.containsKey(15) && answerMap.get(15) >= 4) {
            riskFlags.add("🚨 第15题(想结束生命)得分偏高，需立即关注！危机热线: 400-161-9995");
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalScore", round(totalScore, 2));
        result.put("level", level);
        result.put("levelDescription", levelDesc);
        result.put("dimensions", buildDimensionList(dimensionScores, 5.0));
        result.put("highDimensions", highDimensions);
        result.put("riskFlags", riskFlags);
        return result;
    }
    
    // ============================================
    // UPI大学生人格问卷评分（60题，是=1分/否=0分）
    // ============================================
    private Map<String, Object> scoreUPI(JSONArray answers) {
        // 关键题（需要立即关注的题目）
        Set<Integer> criticalItems = new HashSet<>(Arrays.asList(15, 20, 45, 54));
        // 辅助关键题
        Set<Integer> secondaryItems = new HashSet<>(Arrays.asList(9, 14, 18, 24, 25, 38, 39, 40));
        
        int totalScore = 0;
        int criticalCount = 0;
        List<String> criticalWarnings = new ArrayList<>();
        
        Map<String, Double> dimensionScores = new LinkedHashMap<>();
        // UPI维度: 躯体症状、情绪状态、人际关系、学习适应
        double somaticUPI = 0, emotionalUPI = 0, relationshipUPI = 0, studyUPI = 0;
        int somaticC = 0, emotionalC = 0, relationshipC = 0, studyC = 0;
        
        for (int i = 0; i < answers.size(); i++) {
            JSONObject answer = answers.getJSONObject(i);
            int itemNo = answer.getInteger("questionNo") != null ? answer.getInteger("questionNo") : (i + 1);
            int score = answer.getInteger("answer");
            totalScore += score;
            
            if (criticalItems.contains(itemNo) && score == 1) {
                criticalCount++;
                String warning = getUPICriticalWarning(itemNo);
                if (warning != null) criticalWarnings.add(warning);
            }
            
            // 维度归类
            if (itemNo <= 10) { somaticUPI += score; somaticC++; }
            else if (itemNo <= 20) { emotionalUPI += score; emotionalC++; }
            else if (itemNo <= 30) { relationshipUPI += score; relationshipC++; }
            else { studyUPI += score; studyC++; }
        }
        
        dimensionScores.put("躯体症状", round(somaticUPI / Math.max(somaticC, 1) * 5, 2));
        dimensionScores.put("情绪状态", round(emotionalUPI / Math.max(emotionalC, 1) * 5, 2));
        dimensionScores.put("人际关系", round(relationshipUPI / Math.max(relationshipC, 1) * 5, 2));
        dimensionScores.put("学习适应", round(studyUPI / Math.max(studyC, 1) * 5, 2));
        
        String level;
        String levelDesc;
        List<String> riskFlags = new ArrayList<>();
        
        if (totalScore >= 26 || criticalCount >= 3) {
            level = "三类(需咨询)";
            levelDesc = "存在较明显心理问题，建议尽快进行心理咨询或就医评估";
        } else if (totalScore >= 16 || criticalCount >= 1) {
            level = "二类(需关注)";
            levelDesc = "存在一定心理困扰，建议关注心理状态，必要时寻求咨询";
        } else {
            level = "一类(正常)";
            levelDesc = "心理健康状况良好，无明显心理问题";
        }
        
        if (!criticalWarnings.isEmpty()) {
            riskFlags.addAll(criticalWarnings);
        }
        if (totalScore >= 25) {
            riskFlags.add("⚠️ 总分较高，建议进一步评估");
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalScore", (double) totalScore);
        result.put("level", level);
        result.put("levelDescription", levelDesc);
        result.put("criticalCount", criticalCount);
        result.put("dimensions", buildDimensionList(dimensionScores, 5.0));
        result.put("riskFlags", riskFlags);
        return result;
    }
    
    private String getUPICriticalWarning(int itemNo) {
        switch (itemNo) {
            case 15: return "🚨 情绪起伏过大，需关注情绪调节能力";
            case 20: return "🚨 存在轻生念头，请立即寻求专业帮助！危机热线: 400-161-9995";
            case 45: return "⚠️ 过度担心未来，存在焦虑倾向";
            case 54: return "⚠️ 缺乏自信心，建议加强自我认同建设";
            default: return null;
        }
    }
    
    // ============================================
    // 生成完整报告数据
    // ============================================
    private Map<String, Object> generateReportData(AssessmentScale scale, Map<String, Object> scoreResult, 
                                                     JSONArray answers, Integer completionTime) {
        Map<String, Object> report = new HashMap<>();
        report.put("scaleName", scale.getName());
        report.put("scaleCode", scale.getCode());
        report.put("totalQuestions", answers.size());
        report.put("completionTime", completionTime);
        report.put("completionTimeFormatted", formatTime(completionTime));
        report.put("generatedAt", LocalDateTime.now().toString());
        
        // 雷达图数据
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dimensions = (List<Map<String, Object>>) scoreResult.get("dimensions");
        report.put("radarData", buildRadarData(dimensions));
        
        // 参考范围
        report.put("referenceRanges", getReferenceRanges(scale.getCode()));
        
        // 免责声明
        report.put("disclaimer", "本测评报告仅供参考，不能作为医学诊断依据。" +
                "测评结果受多种因素影响，如有疑虑请咨询专业心理医生或心理咨询师。");
        
        return report;
    }
    
    private List<Map<String, Object>> buildRadarData(List<Map<String, Object>> dimensions) {
        List<Map<String, Object>> radar = new ArrayList<>();
        if (dimensions == null) return radar;
        for (Map<String, Object> dim : dimensions) {
            Map<String, Object> point = new HashMap<>();
            point.put("name", dim.get("name"));
            point.put("value", dim.get("score"));
            point.put("max", dim.get("maxScore"));
            radar.add(point);
        }
        return radar;
    }
    
    private Map<String, Object> getReferenceRanges(String scaleCode) {
        Map<String, Object> ranges = new HashMap<>();
        if ("SDS".equals(scaleCode)) {
            ranges.put("正常", "< 53分");
            ranges.put("轻度抑郁", "53-62分");
            ranges.put("中度抑郁", "63-72分");
            ranges.put("重度抑郁", "> 72分");
        } else if ("SCL90".equals(scaleCode)) {
            ranges.put("正常", "≤ 160分");
            ranges.put("轻度", "161-200分");
            ranges.put("中度", "201-250分");
            ranges.put("重度", "> 250分");
        } else if ("UPI".equals(scaleCode)) {
            ranges.put("一类(正常)", "< 16分");
            ranges.put("二类(需关注)", "16-25分");
            ranges.put("三类(需咨询)", "≥ 26分");
        }
        return ranges;
    }
    
    // ============================================
    // 权威专业解读生成（基于临床心理学标准）
    // ============================================
    private String generateProfessionalInterpretation(String scaleCode, Map<String, Object> scoreResult) {
        String level = (String) scoreResult.get("level");
        double totalScore = ((Number) scoreResult.get("totalScore")).doubleValue();
        
        StringBuilder sb = new StringBuilder();
        
        if ("SDS".equals(scaleCode)) {
            sb.append("【SDS抑郁自评量表解读】\n\n");
            sb.append("SDS（Self-Rating Depression Scale）由Zung于1965年编制，");
            sb.append("是国际上广泛使用的抑郁症状筛查工具。量表包含20个项目，");
            sb.append("采用4级评分，标准分=粗分×1.25。\n\n");
            sb.append(String.format("您的标准分为 %.1f 分，属于「%s」范围。\n\n", totalScore, level));
            
            if ("正常".equals(level)) {
                sb.append("您目前的情绪状态良好，未表现出明显的抑郁症状。");
                sb.append("请继续保持积极健康的生活方式，关注自身情绪变化。");
            } else if (level.contains("轻度")) {
                sb.append("您存在轻度的抑郁倾向，可能在某些方面感到情绪低落、兴趣减退。");
                sb.append("轻度抑郁通过积极的心理调适、规律作息、适度运动等方式通常可以得到改善。");
                sb.append("建议关注情绪变化趋势，如持续2周以上未见好转，建议寻求专业心理咨询。");
            } else if (level.contains("中度")) {
                sb.append("您存在中度的抑郁症状，可能在情绪、认知、行为和生理多方面受到影响。");
                sb.append("中度抑郁通常需要专业干预，建议尽快预约心理咨询或精神科医生进行评估。");
                sb.append("认知行为疗法(CBT)和人际心理治疗(IPT)对中度抑郁有较好的疗效。");
            } else {
                sb.append("您存在重度的抑郁症状，需要引起高度重视。重度抑郁可能严重影响日常生活、");
                sb.append("学习和社交功能，甚至出现自伤风险。强烈建议立即就医，寻求精神科医生的专业评估和治疗。");
                sb.append("请记住，抑郁症是可治疗的，及早就医预后更好。");
            }
        } else if ("SCL90".equals(scaleCode)) {
            sb.append("【SCL-90症状自评量表解读】\n\n");
            sb.append("SCL-90（Symptom Checklist 90）是国际通用的心理健康状况评估工具，");
            sb.append("从感觉、情感、思维、意识、行为、生活习惯、人际关系、饮食睡眠等");
            sb.append("多个维度全面评估心理健康状况。\n\n");
            sb.append(String.format("您的总分为 %.1f 分，属于「%s」范围。\n\n", totalScore, level));
            
            @SuppressWarnings("unchecked")
            List<String> highDimensions = (List<String>) scoreResult.get("highDimensions");
            if (highDimensions != null && !highDimensions.isEmpty()) {
                sb.append("⚠️ 需要关注的维度：").append(String.join("、", highDimensions)).append("\n");
                sb.append("这些维度的得分高于正常范围，建议在以下方面多加注意：\n");
                for (String dim : highDimensions) {
                    sb.append(getDimensionDescription(dim));
                }
            } else if ("正常".equals(level)) {
                sb.append("各维度得分均在正常范围内，心理健康状况良好。");
            }
        } else if ("UPI".equals(scaleCode)) {
            sb.append("【UPI大学生人格问卷解读】\n\n");
            sb.append("UPI（University Personality Inventory）是专门针对大学生群体编制");
            sb.append("的心理健康筛查工具，旨在早期发现有心理问题的学生。\n\n");
            sb.append(String.format("您的总分为 %.0f 分，属于「%s」。\n\n", totalScore, level));
            
            if (level.contains("一类")) {
                sb.append("您的心理健康状况良好，能够较好地适应大学生活。");
            } else if (level.contains("二类")) {
                sb.append("您目前存在一定的心理困扰，虽然不是严重问题，但建议适当关注。");
                sb.append("可以通过自我调适、与朋友交流、参加校园活动等方式改善心理状态。");
            } else {
                sb.append("您在问卷中表现出较明显的心理问题倾向，建议尽快到学校心理咨询中心");
                sb.append("进行面谈评估。早期干预对心理问题的改善非常重要。");
            }
        }
        
        return sb.toString();
    }
    
    private String getDimensionDescription(String dim) {
        switch (dim) {
            case "躯体化": return "  · 躯体化：身体不适感，包括心血管、胃肠道、呼吸等系统的主诉不适\n";
            case "强迫症状": return "  · 强迫症状：明知没有必要但无法摆脱的无意义想法和行为\n";
            case "人际关系敏感": return "  · 人际关系敏感：人际交往中的不自在感、自卑感\n";
            case "抑郁": return "  · 抑郁：苦闷的情感与心境，对生活兴趣减退、缺乏动力\n";
            case "焦虑": return "  · 焦虑：烦躁、紧张、神经过敏等情绪体验\n";
            case "敌对": return "  · 敌对：愤怒、攻击、怨恨等情绪及行为表现\n";
            case "恐怖": return "  · 恐怖：对特定事物或场景的恐惧反应\n";
            case "偏执": return "  · 偏执：猜疑、不信任、敌意等偏执性思维\n";
            case "精神病性": return "  · 精神病性：思维涣散、行为怪异等精神病性症状\n";
            default: return "  · " + dim + "\n";
        }
    }
    
    // ============================================
    // 权威专业建议生成（分层级）
    // ============================================
    private String generateProfessionalSuggestions(String scaleCode, Map<String, Object> scoreResult) {
        String level = (String) scoreResult.get("level");
        
        StringBuilder sb = new StringBuilder();
        sb.append("【分层级干预建议】\n\n");
        
        // 第一层：生活方式建议（适用所有人）
        sb.append("一、健康生活方式\n");
        sb.append("1. 保持规律作息，每天保证7-8小时充足睡眠\n");
        sb.append("2. 每周至少进行3次有氧运动，每次30分钟以上\n");
        sb.append("3. 均衡饮食，多摄入富含Omega-3和维生素B的食物\n");
        sb.append("4. 限制咖啡因和酒精摄入，避免影响情绪稳定\n\n");
        
        // 第二层：心理调适建议
        sb.append("二、心理调适方法\n");
        sb.append("1. 练习正念冥想：每天10-15分钟，使用深呼吸和身体扫描技术\n");
        sb.append("2. 记录情绪日记：梳理每日情绪变化，识别触发因素\n");
        sb.append("3. 学会情绪表达：与信任的人分享感受，不压抑情绪\n");
        sb.append("4. 合理安排时间：使用时间管理矩阵，平衡学习与休息\n");
        sb.append("5. 培养兴趣爱好：通过音乐、绘画、阅读等活动转移注意力\n\n");
        
        // 第三层：社交支持
        sb.append("三、社会支持系统\n");
        sb.append("1. 主动与家人、朋友保持联系，建立支持网络\n");
        sb.append("2. 参加校园社团活动，拓展社交圈\n");
        sb.append("3. 关注学校心理健康中心的活动和资源\n");
        sb.append("4. 避免长期独处，适度参与集体活动\n\n");
        
        // 第四层：专业干预（非正常时触发）
        if (!level.contains("正常") && !level.contains("一类")) {
            sb.append("四、专业干预建议\n");
            
            if (level.contains("重度") || level.contains("三类")) {
                sb.append("1. 【紧急】建议尽快就医，预约精神科医生进行全面评估\n");
                sb.append("2. 考虑药物治疗与心理治疗相结合的综合治疗方案\n");
                sb.append("3. 如出现自伤念头，请立即拨打心理危机干预热线\n");
            } else {
                sb.append("1. 建议预约学校心理咨询中心进行面谈评估\n");
                sb.append("2. 认知行为疗法(CBT)对轻中度心理问题有显著效果\n");
                sb.append("3. 可考虑参加团体心理辅导，与有相似经历的同学交流\n");
            }
            sb.append("4. 心理危机干预24小时热线: 400-161-9995\n");
            sb.append("5. 全国心理援助热线: 010-82951332\n");
        }
        
        return sb.toString();
    }
    
    // ============================================
    // 更新推荐系统的用户画像和兴趣标签
    // ============================================
    private void updateRecommendationProfile(Long userId, String scaleCode, Map<String, Object> scoreResult) {
        try {
            String level = (String) scoreResult.get("level");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> dimensions = (List<Map<String, Object>>) scoreResult.get("dimensions");
            
            // 根据测评结果确定需要关注的领域
            String stressLevel = "low";
            String anxietyLevel = "low";
            String depressionLevel = "low";
            String dominantIssues = "";
            
            if (level.contains("重度") || level.contains("三类")) {
                stressLevel = "high";
                anxietyLevel = level.contains("三类") ? "medium" : "high";
                depressionLevel = "SDS".equals(scaleCode) ? "high" : "medium";
            } else if (level.contains("中度") || level.contains("二类")) {
                stressLevel = "medium";
                anxietyLevel = "medium";
                depressionLevel = "SDS".equals(scaleCode) ? "medium" : "low";
            } else if (level.contains("轻度")) {
                stressLevel = "medium";
                anxietyLevel = "low";
                depressionLevel = "SDS".equals(scaleCode) ? "low" : "low";
            }
            
            // 找出得分最高的维度
            if (dimensions != null && !dimensions.isEmpty()) {
                // 找出相对得分最高的维度（得分/最大值 比例最高）
                Map<String, Object> highestDim = dimensions.stream()
                    .max((a, b) -> {
                        double ratioA = getScoreRatio(a);
                        double ratioB = getScoreRatio(b);
                        return Double.compare(ratioA, ratioB);
                    }).orElse(null);
                
                if (highestDim != null) {
                    dominantIssues = (String) highestDim.get("name");
                }
            }
            
            // 更新或插入 user_psychological_profile
            upsertPsychologicalProfile(userId, stressLevel, anxietyLevel, depressionLevel, dominantIssues);
            
            // 更新或插入 user_interest_tags（基于测评发现的问题推荐相关资源标签）
            upsertInterestTags(userId, scaleCode, level, dominantIssues);
            
            log.info("已为用户{}更新推荐画像: stress={}, anxiety={}, depression={}, issues={}", 
                userId, stressLevel, anxietyLevel, depressionLevel, dominantIssues);
        } catch (Exception e) {
            log.error("更新用户{}推荐画像失败", userId, e);
        }
    }
    
    private double getScoreRatio(Map<String, Object> dim) {
        double score = ((Number) dim.get("score")).doubleValue();
        double max = ((Number) dim.get("maxScore")).doubleValue();
        return max > 0 ? score / max : 0;
    }
    
    private void upsertPsychologicalProfile(Long userId, String stressLevel, String anxietyLevel, 
                                              String depressionLevel, String dominantIssues) {
        try {
            // 使用 INSERT ... ON DUPLICATE KEY UPDATE 语法
            ratingMapper.insertOrUpdateProfile(userId, stressLevel, anxietyLevel, depressionLevel, dominantIssues);
        } catch (Exception e) {
            log.warn("更新心理特征失败(可能表不存在): {}", e.getMessage());
        }
    }
    
    private void upsertInterestTags(Long userId, String scaleCode, String level, String dominantIssues) {
        try {
            // 根据测评结果确定推荐标签
            Map<String, Double> tagsMap = new LinkedHashMap<>();
            
            // 基础标签
            if ("SDS".equals(scaleCode)) {
                tagsMap.put("抑郁调适", 85.0);
                tagsMap.put("情绪管理", 80.0);
                if (!level.contains("正常")) {
                    tagsMap.put("认知疗法", 75.0);
                    tagsMap.put("自我关怀", 78.0);
                }
            } else if ("SCL90".equals(scaleCode)) {
                tagsMap.put("心理健康", 85.0);
                tagsMap.put("压力管理", 80.0);
                if (dominantIssues != null && !dominantIssues.isEmpty()) {
                    tagsMap.put(dominantIssues + "应对", 75.0);
                }
            } else if ("UPI".equals(scaleCode)) {
                tagsMap.put("大学生心理", 85.0);
                tagsMap.put("适应问题", 78.0);
                if (!level.contains("一类")) {
                    tagsMap.put("心理调适", 75.0);
                }
            }
            
            // 严重程度相关标签
            if (level.contains("重度") || level.contains("三类")) {
                tagsMap.put("专业治疗", 90.0);
                tagsMap.put("危机干预", 88.0);
            } else if (level.contains("中度") || level.contains("二类")) {
                tagsMap.put("心理咨询", 82.0);
                tagsMap.put("团体辅导", 78.0);
            } else if (level.contains("轻度")) {
                tagsMap.put("自我调节", 75.0);
                tagsMap.put("放松技巧", 72.0);
            } else {
                tagsMap.put("积极心理学", 70.0);
                tagsMap.put("幸福感", 68.0);
            }
            
            // 批量插入或更新标签
            for (Map.Entry<String, Double> entry : tagsMap.entrySet()) {
                ratingMapper.insertOrUpdateInterestTag(userId, entry.getKey(), 
                    "assessment", "emotion", entry.getValue());
            }
        } catch (Exception e) {
            log.warn("更新兴趣标签失败(可能表不存在): {}", e.getMessage());
        }
    }
    
    // ============================================
    // 工具方法
    // ============================================
    private List<Map<String, Object>> buildDimensionList(Map<String, Double> dimensionScores, double maxScore) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, Double> entry : dimensionScores.entrySet()) {
            Map<String, Object> dim = new HashMap<>();
            dim.put("name", entry.getKey());
            dim.put("score", entry.getValue());
            dim.put("maxScore", maxScore);
            dim.put("ratio", round(entry.getValue() / maxScore, 2));
            list.add(dim);
        }
        return list;
    }
    
    private double round(double value, int places) {
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
    
    private String formatTime(Integer seconds) {
        if (seconds == null) return "未知";
        int min = seconds / 60;
        int sec = seconds % 60;
        return min > 0 ? min + "分" + sec + "秒" : sec + "秒";
    }
}
