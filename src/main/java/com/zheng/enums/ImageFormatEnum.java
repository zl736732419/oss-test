package com.zheng.enums;

/**
 * 图片格式枚举
 * Created by zhenglian on 2017/6/11.
 */
public enum ImageFormatEnum {
    /**
     * png格式
     */
    PNG("png"),
    /**
     * jpg格式
     */
    JPG("jpg"),
    /**
     * bmp格式
     */
    BMP("bmp"),
    /**
     * webp格式
     */
    WEBP("webp");

    private String value;

    private ImageFormatEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
