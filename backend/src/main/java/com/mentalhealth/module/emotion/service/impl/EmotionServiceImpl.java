package com.mentalhealth.module.emotion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mentalhealth.common.exception.BusinessException;
import com.mentalhealth.module.emotion.entity.EmotionDiary;
import com.mentalhealth.module.emotion.mapper.EmotionDiaryMapper;
import com.mentalhealth.module.emotion.service.EmotionService;
import com.mentalhealth.module.recommendation.mapper.UserResourceRatingMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EmotionServiceImpl implements EmotionService {
    
    @Autowired
    private EmotionDiaryMapper diaryMapper;

    @Autowired
    private UserResourceRatingMapper ratingMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createDiary(Long userId, EmotionDiary diary) {
        diary.setUserId(userId);
        
        // 检查该用户当天是否已经写过日记 (因为数据库有 UNIQUE KEY `uk_user_date`)
        LambdaQueryWrapper<EmotionDiary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EmotionDiary::getUserId, userId)
               .eq(EmotionDiary::getDiaryDate, diary.getDiaryDate());
        
        EmotionDiary existing = diaryMapper.selectOne(wrapper);
        
        if (existing != null) {
            // 如果已存在，则更新
            diary.setId(existing.getId());
            diary.setUpdatedAt(new Date());
            diaryMapper.updateById(diary);
            log.info("用户{}更新了{}的情绪日记", userId, diary.getDiaryDate());
        } else {
            // 不存在则创建
            diary.setCreatedAt(new Date());
            diaryMapper.insert(diary);
            log.info("用户{}创建了{}的情绪日记", userId, diary.getDiaryDate());
        }

        // 联动推荐系统：根据日记标签更新兴趣偏好
        if (diary.getMoodTags() != null && !diary.getMoodTags().isEmpty()) {
            String[] tags = diary.getMoodTags().split("[,，]");
            for (String tag : tags) {
                String cleanTag = tag.trim();
                if (!cleanTag.isEmpty()) {
                    // 行为来源标签，给予较高的初始兴趣分 (85分)，用于反映近期心境
                    ratingMapper.insertOrUpdateInterestTag(userId, cleanTag, 
                        "diary", "emotion", 85.0);
                }
            }
        }

        // 尝试从内容中提取关键词
        if (diary.getContent() != null && !diary.getContent().isEmpty()) {
            String content = diary.getContent();
            String[] demoKeywords = {"考试", "焦虑", "压力", "沟通", "人际", "社交", "复习", "学业", "室友", "疲惫", "失眠"};
            for (String kw : demoKeywords) {
                if (content.contains(kw)) {
                    ratingMapper.insertOrUpdateInterestTag(userId, kw, "diary", "general", 80.0);
                }
            }
        }
    }
    
    @Override
    public void updateDiary(Long userId, Long id, EmotionDiary diary) {
        EmotionDiary existing = diaryMapper.selectById(id);
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new BusinessException("日记不存在或无权限");
        }
        
        diary.setId(id);
        diary.setUserId(userId);
        diary.setUpdatedAt(new Date());
        diaryMapper.updateById(diary);
    }
    
    @Override
    public void deleteDiary(Long userId, Long id) {
        EmotionDiary existing = diaryMapper.selectById(id);
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new BusinessException("日记不存在或无权限");
        }
        diaryMapper.deleteById(id);
    }
    
    @Override
    public List<EmotionDiary> getDiaryList(Long userId, Integer pageNum, Integer pageSize) {
        Page<EmotionDiary> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<EmotionDiary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EmotionDiary::getUserId, userId);
        wrapper.orderByDesc(EmotionDiary::getDiaryDate);
        
        Page<EmotionDiary> result = diaryMapper.selectPage(page, wrapper);
        return result.getRecords();
    }
    
    @Override
    public EmotionDiary getDiaryDetail(Long userId, Long id) {
        EmotionDiary diary = diaryMapper.selectById(id);
        if (diary == null || !diary.getUserId().equals(userId)) {
            throw new BusinessException("日记不存在或无权限");
        }
        return diary;
    }
    
    @Override
    public Map<String, Object> getStatistics(Long userId, String startDate, String endDate) {
        LambdaQueryWrapper<EmotionDiary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EmotionDiary::getUserId, userId);
        
        if (startDate != null && endDate != null) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                wrapper.ge(EmotionDiary::getDiaryDate, sdf.parse(startDate));
                wrapper.le(EmotionDiary::getDiaryDate, sdf.parse(endDate));
            } catch (Exception e) {
                throw new BusinessException("日期格式错误");
            }
        }
        
        wrapper.orderByAsc(EmotionDiary::getDiaryDate);
        List<EmotionDiary> diaries = diaryMapper.selectList(wrapper);
        
        // 计算统计数据
        Map<String, Object> stats = new HashMap<>();
        
        // 情绪趋势
        List<Map<String, Object>> moodTrend = diaries.stream()
            .map(d -> {
                Map<String, Object> item = new HashMap<>();
                item.put("date", new SimpleDateFormat("yyyy-MM-dd").format(d.getDiaryDate()));
                item.put("score", d.getMoodScore());
                return item;
            })
            .collect(Collectors.toList());
        
        // 情绪分布
        Map<String, Long> emotionDist = new HashMap<>();
        diaries.forEach(d -> {
            if (d.getMoodTags() != null) {
                String[] tags = d.getMoodTags().split("[,，]");
                for (String tag : tags) {
                    emotionDist.merge(tag.trim(), 1L, Long::sum);
                }
            }
        });
        
        List<Map<String, Object>> emotionDistribution = emotionDist.entrySet().stream()
            .map(entry -> {
                Map<String, Object> item = new HashMap<>();
                item.put("emotion", entry.getKey());
                item.put("count", entry.getValue());
                return item;
            })
            .collect(Collectors.toList());
        
        // 平均分
        double avgScore = diaries.stream()
            .filter(d -> d.getMoodScore() != null)
            .mapToInt(EmotionDiary::getMoodScore)
            .average()
            .orElse(0.0);
        
        stats.put("moodTrend", moodTrend);
        stats.put("emotionDistribution", emotionDistribution);
        stats.put("avgScore", Math.round(avgScore * 100.0) / 100.0);
        stats.put("totalDiaries", diaries.size());
        
        return stats;
    }
}
