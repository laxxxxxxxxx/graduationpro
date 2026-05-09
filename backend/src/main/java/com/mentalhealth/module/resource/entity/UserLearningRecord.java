package com.mentalhealth.module.resource.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("user_learning_record")
public class UserLearningRecord implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long resourceId;
    private Integer progress;
    private Integer completed;
    private Integer studyDuration;
    private Date lastStudyTime;
    private Date createdAt;
    private Date updatedAt;
}
