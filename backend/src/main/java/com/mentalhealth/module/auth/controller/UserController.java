package com.mentalhealth.module.auth.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mentalhealth.common.exception.BusinessException;
import com.mentalhealth.common.result.Result;
import com.mentalhealth.common.utils.JwtUtil;
import com.mentalhealth.module.assessment.entity.UserAssessment;
import com.mentalhealth.module.assessment.mapper.UserAssessmentMapper;
import com.mentalhealth.module.auth.entity.User;
import com.mentalhealth.module.auth.mapper.UserMapper;
import com.mentalhealth.module.auth.vo.UserInfoVO;
import com.mentalhealth.module.auth.vo.UserProfileFullVO;
import com.mentalhealth.module.community.entity.CommunityPost;
import com.mentalhealth.module.community.mapper.CommunityPostMapper;
import com.mentalhealth.module.emotion.entity.EmotionDiary;
import com.mentalhealth.module.emotion.mapper.EmotionDiaryMapper;
import com.mentalhealth.module.resource.entity.EducationResource;
import com.mentalhealth.module.resource.entity.UserLearningRecord;
import com.mentalhealth.module.resource.mapper.LearningRecordMapper;
import com.mentalhealth.module.resource.mapper.ResourceMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户信息控制器
 */
@Slf4j
@RestController
@RequestMapping("/user")
@Api(tags = "用户管理")
public class UserController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserAssessmentMapper assessmentMapper;

    @Autowired
    private EmotionDiaryMapper diaryMapper;

    @Autowired
    private CommunityPostMapper communityPostMapper;

    @Autowired
    private LearningRecordMapper learningRecordMapper;

    @Autowired
    private ResourceMapper resourceMapper;

    @GetMapping("/profile")
    @ApiOperation("获取用户信息")
    public Result<UserInfoVO> getUserInfo(@RequestHeader("Authorization") String token) {
        String actualToken = token.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(actualToken);

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        UserInfoVO vo = new UserInfoVO();
        BeanUtil.copyProperties(user, vo);

        return Result.success(vo);
    }

    @PutMapping("/profile")
    @ApiOperation("更新用户信息")
    public Result<Void> updateUserInfo(
            @RequestHeader("Authorization") String token,
            @RequestBody UserInfoVO userInfo) {

        String actualToken = token.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(actualToken);

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 更新允许修改的字段
        if (userInfo.getRealName() != null) user.setRealName(userInfo.getRealName());
        if (userInfo.getGender() != null) user.setGender(userInfo.getGender());
        if (userInfo.getAge() != null) user.setAge(userInfo.getAge());
        if (userInfo.getEmail() != null) user.setEmail(userInfo.getEmail());
        if (userInfo.getPhone() != null) user.setPhone(userInfo.getPhone());
        if (userInfo.getUniversity() != null) user.setUniversity(userInfo.getUniversity());
        if (userInfo.getMajor() != null) user.setMajor(userInfo.getMajor());
        if (userInfo.getGrade() != null) user.setGrade(userInfo.getGrade());
        if (userInfo.getAvatar() != null) user.setAvatar(userInfo.getAvatar());

        userMapper.updateById(user);

        return Result.success("更新成功", null);
    }

    @GetMapping("/profile/full")
    @ApiOperation("获取个人主页完整信息(含测评摘要)")
    public Result<UserProfileFullVO> getUserProfileFull(@RequestHeader("Authorization") String token) {
        String actualToken = token.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(actualToken);

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        UserProfileFullVO vo = new UserProfileFullVO();
        BeanUtil.copyProperties(user, vo);

        // 1. 测评摘要
        LambdaQueryWrapper<UserAssessment> assessmentWrapper = new LambdaQueryWrapper<>();
        assessmentWrapper.eq(UserAssessment::getUserId, userId)
                .orderByDesc(UserAssessment::getCreatedAt);
        List<UserAssessment> assessments = assessmentMapper.selectList(assessmentWrapper);

        vo.setAssessmentCount(assessments.size());
        if (!assessments.isEmpty()) {
            UserAssessment latest = assessments.get(0);
            vo.setLastAssessmentDate(latest.getCreatedAt() != null ? latest.getCreatedAt().toString().substring(0, 10) : null);
            vo.setLastAssessmentScale(latest.getScaleId() != null ? getScaleName(latest.getScaleId()) : null);
            vo.setLastAssessmentResult(latest.getResultLevel());
        }

        // 最近5条测评历史
        List<Map<String, Object>> history = assessments.stream()
                .limit(5)
                .map(a -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", a.getId());
                    m.put("scaleName", getScaleName(a.getScaleId()));
                    m.put("totalScore", a.getTotalScore());
                    m.put("resultLevel", a.getResultLevel());
                    m.put("createdAt", a.getCreatedAt());
                    return m;
                })
                .collect(Collectors.toList());
        vo.setAssessmentHistory(history);

        // 2. 情绪日记数量
        LambdaQueryWrapper<EmotionDiary> diaryWrapper = new LambdaQueryWrapper<>();
        diaryWrapper.eq(EmotionDiary::getUserId, userId);
        vo.setDiaryCount(Math.toIntExact(diaryMapper.selectCount(diaryWrapper)));

        // 3. 学习记录数量
        LambdaQueryWrapper<UserLearningRecord> learnWrapper = new LambdaQueryWrapper<>();
        learnWrapper.eq(UserLearningRecord::getUserId, userId);
        vo.setLearningCount(Math.toIntExact(learningRecordMapper.selectCount(learnWrapper)));

        // 4. 社区帖子数量（只统计未删除的）
        LambdaQueryWrapper<CommunityPost> postWrapper = new LambdaQueryWrapper<>();
        postWrapper.eq(CommunityPost::getUserId, userId)
                .ne(CommunityPost::getStatus, 3);
        vo.setCommunityPostCount(Math.toIntExact(communityPostMapper.selectCount(postWrapper)));

        return Result.success(vo);
    }

    @GetMapping("/learning-records")
    @ApiOperation("获取学习记录")
    public Result<List<Map<String, Object>>> getLearningRecords(@RequestHeader("Authorization") String token) {
        String actualToken = token.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(actualToken);

        LambdaQueryWrapper<UserLearningRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(UserLearningRecord::getUserId, userId)
                .orderByDesc(UserLearningRecord::getLastStudyTime);
        List<UserLearningRecord> records = learningRecordMapper.selectList(recordWrapper);

        if (records.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        List<Long> resourceIds = records.stream()
                .map(UserLearningRecord::getResourceId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, EducationResource> resourceMap = resourceIds.isEmpty()
                ? Collections.emptyMap()
                : resourceMapper.selectBatchIds(resourceIds).stream()
                        .collect(Collectors.toMap(EducationResource::getId, resource -> resource));

        List<Map<String, Object>> result = records.stream()
                .map(record -> {
                    EducationResource resource = resourceMap.get(record.getResourceId());
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", record.getId());
                    item.put("resourceId", record.getResourceId());
                    item.put("resourceName", resource != null ? resource.getTitle() : "未知资源");
                    item.put("coverUrl", resource != null ? resource.getCoverUrl() : null);
                    item.put("type", resource != null ? resource.getType() : null);
                    item.put("progress", record.getProgress());
                    item.put("completed", Objects.equals(record.getCompleted(), 1));
                    item.put("studyDuration", record.getStudyDuration());
                    item.put("lastStudyTime", record.getLastStudyTime());
                    return item;
                })
                .collect(Collectors.toList());

        return Result.success(result);
    }

    /**
     * 根据scaleId返回量表名称
     */
    private String getScaleName(Integer scaleId) {
        if (scaleId == null) return null;
        switch (scaleId) {
            case 1: return "SDS抑郁自评量表";
            case 2: return "SCL-90症状自评量表";
            case 3: return "UPI大学生人格问卷";
            default: return "未知量表";
        }
    }
}
