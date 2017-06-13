package com.zheng.exception;

import com.zheng.enums.ExceptionCodeEnum;

/**
 * 
 * Created by zhenglian on 2017/6/10.
 */
public class BucketNotExistException extends AbstractException {
    public BucketNotExistException() {
        super(ExceptionCodeEnum.BUCKET_NOT_EXIST_ERROR.getCode(), ExceptionCodeEnum.BUCKET_NOT_EXIST_ERROR.getName());
    }
    
    public BucketNotExistException(String message) {
        super(ExceptionCodeEnum.BUCKET_NOT_EXIST_ERROR.getCode(), message);
    }
    
}
