package com.zheng.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日期工具类
 * Created by zhenglian on 2017/6/10.
 */
public class DateUtil {

    /**
     * 获取yyyy-MM-dd日期格式字符串
     * @param dateTime
     * @return
     */
    public static String getYYYYMMDDStr(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    /**
     * 获取yyyy-MM-dd HH:mm:ss 格式字符串
     * @param dateTime 
     * @return
     */
    public static String getYYYYMMDDHHMMSSStr(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
}
