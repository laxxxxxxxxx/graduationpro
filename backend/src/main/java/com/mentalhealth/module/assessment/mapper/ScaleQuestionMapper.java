package com.mentalhealth.module.assessment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mentalhealth.module.assessment.entity.ScaleQuestion;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface ScaleQuestionMapper extends BaseMapper<ScaleQuestion> {
    
    List<ScaleQuestion> selectByScaleId(Integer scaleId);
}
