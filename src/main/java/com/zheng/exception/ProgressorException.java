package com.zheng.exception;

import com.zheng.enums.ExceptionCodeEnum;

/**
 * 进度条异常
 * Created by zhenglian on 2017/6/12.
 */
public class ProgressorException extends AbstractException {

    public ProgressorException() {
        super(ExceptionCodeEnum.PROGRESS_ERROR.getCode(), ExceptionCodeEnum.PROGRESS_ERROR.getName());
    }
    
    public ProgressorException(String message) {
        super(ExceptionCodeEnum.PROGRESS_ERROR.getCode(), message);
    }
}
