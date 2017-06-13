package com.zheng.utils;

import com.google.common.base.Throwables;

import java.io.File;
import java.io.IOException;

/**
 * 文件操作工具类
 * Created by zhenglian on 2017/6/10.
 */
public class FileUtil {

    /**
     * 获取文件名后缀
     * 如xxxx/xxx/test.jpg返回jpg
     * @param file
     * @return
     */
    public static String getExtention(File file) {
        if(null == file || !file.exists()) {
            return null;
        }
        
        String fileName = file.getName();
        int index = fileName.lastIndexOf(".");
        if(-1 == index) {
            return null;
        }
        
        return fileName.substring(index+1);
    }

    /**
     * 获取指定文件后缀
     * 如xxxx/xxx/test.jpg返回.jpg
     * @param file
     * @return
     */
    public static String getExtentionWithDot(File file) {
        if(null == file || !file.exists()) {
            return null;
        }
        
        String fileName = file.getName();
        int index = fileName.lastIndexOf(".");
        return fileName.substring(index);
    }

    /**
     * 创建文件或目录
     * @param file
     */
    public static void createFile(File file) {
        if(null == file) {
            return;
        }
        
        if(!file.exists()) {
            if(file.isDirectory()) {
                file.mkdirs();
            }else {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    Throwables.propagate(e);
                }
            }
        }
    }
}
