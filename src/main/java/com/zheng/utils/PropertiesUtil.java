package com.zheng.utils;

import com.google.common.base.Throwables;

import java.io.IOException;
import java.util.Properties;

/**
 * 读取系统配置属性工具类
 * Created by zhenglian on 2017/6/10.
 */
public class PropertiesUtil {
    
    private static Properties properties;
    
    static {
        properties = new Properties();
        try {
            properties.load(PropertiesUtil.class.getClassLoader().getResourceAsStream("config.properties"));
        } catch (IOException e) {
            Throwables.propagate(e);
        }
    }

    /**
     * 获取配置文件中指定key属性值
     * @param key
     * @return
     */
    public static String getProperty(String key) {
        return properties.getProperty(key);
    }
    
}
