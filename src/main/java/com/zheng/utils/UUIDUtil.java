package com.zheng.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

/**
 * 获取uuid值工具
 * Created by zhenglian on 2017/6/10.
 */
public class UUIDUtil {
    /**
     * 获取去掉-的uuid字符串
     * @return
     */
    public static String getFormatedUUID() {
        String uuid = UUID.randomUUID().toString();
        return formatStr(uuid);
    }

    /**
     * 去掉字符串中间的-
     * @param str
     * @return
     */
    public static String formatStr(String str) {
        if(StringUtils.isBlank(str)) {
            return null;
        }
        
        return str.replaceAll("\\-", "");
    }
}
