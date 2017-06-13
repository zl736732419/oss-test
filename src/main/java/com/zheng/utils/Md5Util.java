package com.zheng.utils;

import com.google.common.base.Throwables;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * md5工具类
 * Created by zhenglian on 2017/6/10.
 */
public class Md5Util {
    
    private static final String template="0123456789abcdefghijklmnopqrstuvwxyz";
    
    /**
     * 将字符串进行MD5加密
     * @param str
     * @return
     */
    public static String md5(String str) {
        if(StringUtils.isBlank(str)) {
            return null;
        }
        
        String result = null;
        byte[] source = str.getBytes(StandardCharsets.UTF_8);
        try {
            MessageDigest md = MessageDigest.getInstance("md5");
            md.update(source);
            byte[] bytes = md.digest();
            int len = bytes.length;
            char[] chars = new char[len * 2];
            char[] arr = template.toCharArray();
            int k = 0;
            for(int i = 0; i < len; i++) {
                chars[k++] = arr[bytes[i] >>> 4 & 0xff];
                chars[k++] = arr[bytes[i] & 0xff];
            }
            
            result = new String(chars);
        } catch (NoSuchAlgorithmException e) {
            Throwables.propagate(e);
        }

        return result;
    }
    
}
