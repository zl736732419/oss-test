package com.zheng;

import com.aliyun.oss.model.Bucket;
import com.google.common.collect.Lists;
import com.zheng.domain.OssProgressor;
import com.zheng.enums.ImageFormatEnum;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Created by zhenglian on 2017/6/10.
 */
public class OSSCenterTest {
    
    private OSSCenter center;
    
    @Before
    public void init() {
        center = OSSCenter.newCenter();
    }
    
    @Test
    public void testUpoad() {
        File file = new File("D:\\西冲\\微信图片_20170501124819.jpg");
        center.uploadFile(file);
    }
    
    @Test
    public void testShowBuckets() {
        List<Bucket> list = center.getExistBuckets(null, null, null);
        list.stream().forEach(bucket->System.out.println(bucket.getName()));
    }
    
    @Test
    public void testListObjects() {
        List<Map<String, Object>> list = center.listObjects(null, null, null);
        list.stream().forEach(map->System.out.println(map));
    }
    
    @Test
    public void testDownload() throws Exception {
        InputStream input = center.downloadFile("Upload/2017-06-10/9679a248b96a45fdac73a532f1c034c8.jpg");
        FileUtils.copyInputStreamToFile(input, new File("C:\\Users\\Administrator\\Desktop\\test.jpg"));
    }
    
    @Test
    public void testBucketInfo() {
        center.showBucketInfo();
    }
 
    @Test
    public void getFileKey() {
        File file = new File("D:\\学习笔记\\css技巧.txt");
        System.out.println(center.getKey(file));
    }
    
    @Test
    public void testAppendFile() throws Exception {
        File file1 = new File("D:\\学习笔记\\css技巧.txt");
        File file2 = new File("D:\\学习笔记\\email.txt");
        List<File> files = Lists.newArrayList();
        files.add(file1);
        files.add(file2);
        
        String key = center.getKey(file1);
        center.appendFiles(key, files);
        
        InputStream input = center.downloadFile(key);
        FileUtils.copyInputStreamToFile(input, new File("C:\\Users\\Administrator\\Desktop\\key.txt"));
    }
    
    @Test
    public void testUploadCheckpoint() {
        File file = new File("D:\\学习笔记\\Activiti工作流课程.doc");
        center.uploadFileCheckpoint(file);
    }
    
    @Test
    public void testDownloadFileCheckpoint() {
        String key = "Upload/2017-06-11/17a54c25e8cc4325ae731be758ab3e97.doc";
        File localFile = new File("C:\\Users\\Administrator\\Desktop\\test.doc");
        center.downloadFileCheckpoint(key, localFile);
    }
    
    @Test
    public void testImage() {
        String key = "Upload/2017-06-10/9679a248b96a45fdac73a532f1c034c8.jpg";
        File file = new File("C:\\Users\\Administrator\\Desktop\\test.png");
        File infoFile = new File("C:\\Users\\Administrator\\Desktop\\info.txt");
        OSSCenter.ImageCenter imageCenter = center.newImageCenter(key, file);
        
        imageCenter
//                .rotate(180)
//                .crop(10,10,200,200)
                .getImageInfo(infoFile)
//                .sharpen(200)
                .watermark("5LqU5LiA6KW_5Yay5ri4")
                .format(ImageFormatEnum.PNG)
                .transform();
    }
    
    
    @Test
    public void testUploadFileWithProgress() throws Exception {
        File file = new File("D:\\学习笔记\\easyext.doc");
        String key = center.getKey(file);
        
        OssProgressor progressor = center.uploadFileWithProgress(key, file);
        while(!progressor.isFinished()) {
            Thread.sleep(1000);
            System.out.println(progressor);
        }
        System.out.println("上传文件成功");
        
    }
    
    @Test
    public void testDownloadFileWithProgress() throws Exception {
        File file = new File("C:\\Users\\Administrator\\Desktop\\test.doc");
        String key = "Upload/2017-06-12/f76f11ab902147a9b49c312cff1ff0e9.doc";
        OssProgressor progressor = center.downloadFileWithProgress(key, file);
        while(!progressor.isFinished()) {
            Thread.sleep(1000);
            System.out.println(progressor);
        }
        System.out.println("文件下载成功");
    }
}
