package com.mentalhealth.common.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业务异常
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BusinessException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 默认错误码
     */
    public static final Integer DEFAULT_CODE = 500;
    
    /**
     * 错误码
     */
    private Integer code;
    
    public BusinessException(String message) {
        super(message);
        this.code = DEFAULT_CODE;
    }
    
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
    
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = DEFAULT_CODE;
    }
    
    public BusinessException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
