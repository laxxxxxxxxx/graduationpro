package com.mentalhealth.module.emotion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("emotion_diary")
public class EmotionDiary implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Date diaryDate;
    private Integer moodScore;
    private String moodTags;
    private String content;
    private String weather;
    private String events;
    private String gratitude;
    private Integer energyLevel;
    private Integer sleepQuality;
    private Date createdAt;
    private Date updatedAt;
}
