package com.mentalhealth.module.resource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mentalhealth.module.resource.entity.UserLearningRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LearningRecordMapper extends BaseMapper<UserLearningRecord> {
}
