package com.mentalhealth.module.assessment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("scale_question")
public class ScaleQuestion implements Serializable {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer scaleId;
    private Integer questionNo;
    private String questionText;
    private Integer questionType;
    private String options; // JSON字符串
    private String scoreMapping; // JSON字符串
    private Date createdAt;
}
