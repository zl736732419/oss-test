package com.zheng.exception;

/**
 * 抽象异常对象，需要针对不同的场景继承实现不同的具体异常
 * Created by zhenglian on 2017/6/6.
 */
public abstract class AbstractException extends RuntimeException {
    protected String code;
    protected String message;

    public AbstractException(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
