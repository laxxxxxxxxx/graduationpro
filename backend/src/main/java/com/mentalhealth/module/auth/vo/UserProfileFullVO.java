package com.mentalhealth.module.auth.vo;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 用户个人主页完整视图（含测评摘要）
 */
@Data
public class UserProfileFullVO {
    
    /** 用户基本信息 */
    private Long id;
    private String username;
    private String realName;
    private Integer gender;
    private Integer age;
    private String email;
    private String phone;
    private String university;
    private String major;
    private String grade;
    private Integer role;
    private String avatar;
    
    /** 测评摘要统计 */
    private Integer assessmentCount;
    private String lastAssessmentDate;
    private String lastAssessmentScale;
    private String lastAssessmentResult;
    
    /** 测评历史列表（最近5条） */
    private List<Map<String, Object>> assessmentHistory;
    
    /** 其他统计 */
    private Integer diaryCount;
    private Integer learningCount;
    private Integer communityPostCount;
}
