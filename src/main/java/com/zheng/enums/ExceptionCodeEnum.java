package com.zheng.enums;

/**
 * Created by zhenglian on 2017/6/10.
 */
public enum ExceptionCodeEnum {

    /**
     * bucket没有创建异常
     */
    BUCKET_NOT_EXIST_ERROR("2000", "bucket不存在"),
    /**
     * 进度条发生错误
     */
    PROGRESS_ERROR("2001", "进度条异常");
    
    private String code;
    private String name;

    private ExceptionCodeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
