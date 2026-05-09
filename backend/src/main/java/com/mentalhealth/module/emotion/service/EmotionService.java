package com.mentalhealth.module.emotion.service;

import com.mentalhealth.module.emotion.entity.EmotionDiary;

import java.util.List;
import java.util.Map;

public interface EmotionService {
    
    void createDiary(Long userId, EmotionDiary diary);
    
    void updateDiary(Long userId, Long id, EmotionDiary diary);
    
    void deleteDiary(Long userId, Long id);
    
    List<EmotionDiary> getDiaryList(Long userId, Integer pageNum, Integer pageSize);
    
    EmotionDiary getDiaryDetail(Long userId, Long id);
    
    Map<String, Object> getStatistics(Long userId, String startDate, String endDate);
}
