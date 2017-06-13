package com.zheng;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.event.ProgressEventType;
import com.aliyun.oss.model.*;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zheng.constant.OSSConfig;
import com.zheng.domain.OssProgressor;
import com.zheng.enums.ImageFormatEnum;
import com.zheng.exception.BucketNotExistException;
import com.zheng.utils.DateUtil;
import com.zheng.utils.FileUtil;
import com.zheng.utils.PropertiesUtil;
import com.zheng.utils.UUIDUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


/**
 * oss中心，用于上传下载文件操作
 * 需要说明的是：
 * 在上传文件时，如果指定文件的key中包含/，在oss中将会当成文件夹
 * 可以在oss控制台上看到
 * Created by zhenglian on 2017/6/10.
 */
public class OSSCenter {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    
    private OSSCenter() {
    }
    
    private static class Inner {
        public static OSSCenter instance = new OSSCenter();
    }
    
    public static OSSCenter newCenter() {
        return Inner.instance;
    }
    
    /**
     * 获取连接oss客户端工具
     * 所有的文件操作都需要使用client来进行
     * @return
     */
    private OSSClient getOSSClient() {
        OSSClient client = new OSSClient(OSSConfig.ENDPOINT, OSSConfig.ACCESS_KEY_ID, OSSConfig.ACCESS_KEY_SECRET);
        
        if(!client.doesBucketExist(OSSConfig.BUCKET_NAME)) {
            createBucket(client);
        }
        
        return client;
    }

    /**
     * bucket不存在，通过client进行创建
     */
    private void createBucket(OSSClient client) {
        if (StringUtils.isBlank(OSSConfig.BUCKET_NAME)) {
            Throwables.propagate(new BucketNotExistException("bucket名称没有配置"));
        }
        
        logger.debug("创建bucket: {}", OSSConfig.BUCKET_NAME);
        CreateBucketRequest request = new CreateBucketRequest(OSSConfig.BUCKET_NAME);
        request.setCannedACL(CannedAccessControlList.PublicRead);
        client.createBucket(request);
    }

    /**
     * 获取oss中已经存在的bucket列表
     * @param prefix 指定查找的的前缀
     * @param marker 从指定的marker object处开始向后查找
     * @param maxKeys 如果不指定，默认返回100条
     * @return
     */
    public List<Bucket> getExistBuckets(String prefix, String marker, Integer maxKeys) {
        OSSClient client = getOSSClient();
        ListBucketsRequest request = getListBucketsRequest(prefix, marker, maxKeys);
        BucketList bucketList = client.listBuckets(request);
        List<Bucket> list = bucketList.getBucketList();
        client.shutdown();
        
        return list;
    }

    private ListBucketsRequest getListBucketsRequest(String prefix, String marker, Integer maxKeys) {
        ListBucketsRequest request = new ListBucketsRequest();
        if(!StringUtils.isBlank(prefix)) {
            request.withPrefix(prefix);
        }
        if(!StringUtils.isBlank(marker)) {
            request.withMarker(marker);
        }

        if(null != maxKeys && maxKeys > 0) {
            request.withMaxKeys(maxKeys);
        }
        
        return request;
    }
    
    /**
     * 打印bucket信息
     */
    public void showBucketInfo() {
        OSSClient client = getOSSClient();
        
        //输出bucket信息
        BucketInfo bucketInfo = client.getBucketInfo(OSSConfig.BUCKET_NAME);
        logger.debug("bucket{}的信息如下：", bucketInfo.getBucket().getName());
        logger.debug("\t数据中心：{}", bucketInfo.getBucket().getLocation());
        logger.debug("\t创建时间：{}", bucketInfo.getBucket().getCreationDate());
        
        client.shutdown();
    }

    /**
     * 上传文件
     * @param file
     * @return
     */
    public boolean uploadFile(File file) {
        if(null == file || !file.exists()) {
            return false;
        }
        
        OSSClient client = getOSSClient();
        String key = getKey(file);
        client.putObject(new PutObjectRequest(OSSConfig.BUCKET_NAME, key, file));
        client.setObjectAcl(OSSConfig.BUCKET_NAME, key, CannedAccessControlList.PublicRead);
        
        client.shutdown();
        return client.doesObjectExist(OSSConfig.BUCKET_NAME, key);
    }

    /**
     * 文件断点续传
     * @param file
     * @return
     */
    public void uploadFileCheckpoint(File file) {
        if(null == file || !file.exists()) {
            return;
        }
        
        OSSClient client = getOSSClient();
        String key = getKey(file);
        UploadFileRequest request = new UploadFileRequest(OSSConfig.BUCKET_NAME, key);
        // 带上传的本地文件
        request.setUploadFile(file.getAbsolutePath());
        // 设置并发下载数
        request.setTaskNum(5);
        // 设置分片大小，默认是100KB，这里设置为1M
        request.setPartSize(1024*1024*1);
        // 开启断点续传功能，默认是关闭的
        request.setEnableCheckpoint(true);

        UploadFileResult result = null;
        try {
            result = client.uploadFile(request);
        } catch (Throwable throwable) {
            Throwables.propagate(throwable);
        }
        CompleteMultipartUploadResult uploadResult = result.getMultipartUploadResult();
        logger.info("checkpoint upload file result: {}", uploadResult.getETag());
        
        client.shutdown();
    }

    /**
     * 断点下载文件
     * @param key
     * @param localFile 下载后保存的本地文件
     * @return
     */
    public void downloadFileCheckpoint(String key, File localFile) {
        if(StringUtils.isBlank(key) || null == localFile) {
            return ;
        }
        
        FileUtil.createFile(localFile);
        
        DownloadFileRequest request = new DownloadFileRequest(OSSConfig.BUCKET_NAME, key);
        // 设置下载后存放的文件
        request.setDownloadFile(localFile.getAbsolutePath());
        // 设置并发下载数默认为1
        request.setTaskNum(5);
        // 设置下载的分片大小，默认是100KB
        request.setPartSize(1024*1024*1);
        // 开启断点下载，默认关闭
        request.setEnableCheckpoint(true);
        
        OSSClient client = getOSSClient();
        DownloadFileResult result = null;
        try {
            result = client.downloadFile(request);
        } catch (Throwable throwable) {
            Throwables.propagate(throwable);
        }

        ObjectMetadata metadata = result.getObjectMetadata();
       logger.debug("ETag: {}", metadata.getETag());
       logger.debug("Lastmodified: {}", metadata.getLastModified());

        client.shutdown();
    }
    

    /**
     * 内容追加到同一个object上
     * @param key
     * @param input
     * @return 下一次追加内容的位置position 出现错误时返回-1
     */
    public long appendFile(String key, InputStream input, long position) {
        if(StringUtils.isBlank(key) || null == input) {
            return -1;
        }
        
        OSSClient client = getOSSClient();
        AppendObjectResult result = client.appendObject(new AppendObjectRequest(OSSConfig.BUCKET_NAME, key, input).withPosition(position));
        long nextPosition = result.getNextPosition();
        
        client.shutdown();
        return nextPosition;
    }

    /**
     * append方式上传多个文件到同一个object中
     * @param key
     * @param files
     * @return
     */
    public boolean appendFiles(String key, List<File> files) {
        if(StringUtils.isBlank(key) || null == files || files.isEmpty()) {
            return false;
        }

        OSSClient client = getOSSClient();
        Long nextPosition = 0L;
        int count = 0;
        for(File file : files) {
            if(null==file || !file.exists()) {
                continue;
            }

            InputStream input = null;
            try {
                input = FileUtils.openInputStream(file);
            } catch (IOException e) {
                logger.error(e.getLocalizedMessage());
            }
            if(null == input) {
                continue;
            }

            AppendObjectRequest request = new AppendObjectRequest(OSSConfig.BUCKET_NAME, key, input)
                    .withPosition(nextPosition);
            AppendObjectResult result = client.appendObject(request);
            nextPosition = result.getNextPosition();
            count++;
        }
        logger.debug("需要追加{}个文件，实际只追加了{}个文件", files.size(), count);

        client.shutdown();
        return count > 0;
    }
    
    /**
     * 下载文件
     * @param key
     * @return
     */
    public InputStream downloadFile(String key) {
        if(StringUtils.isBlank(key)) {
            return null;
        }
        
        OSSClient client = getOSSClient();
        OSSObject object = client.getObject(OSSConfig.BUCKET_NAME, key);
        
        client.shutdown();
        return object.getObjectContent();
    }
    
    /**
     * 上传流
     * 可以文件流之外的其他流资源，比如ByteArrayInputStream
     * @param key
     * @param input
     * @return
     */
    public boolean uploadStream(String key, InputStream input) {
        if(null == input) {
            return false;
        }
        
        OSSClient client = getOSSClient();
        client.putObject(OSSConfig.BUCKET_NAME, key, input);
        client.setObjectAcl(OSSConfig.BUCKET_NAME, key, CannedAccessControlList.PublicRead);
        
        client.shutdown();
        return client.doesObjectExist(OSSConfig.BUCKET_NAME, key);
    }
    
    /**
     * 按照前缀列出当前bucket中的object
     * @param prefix 搜索前缀
     * @param marker 指定从marker对应的object开始向后查找 可以用来做分页
     * @param maxKeys 返回个数，最少为10
     * @return 返回列表，包含objectkey:objectsize键值对
     */
    public List<Map<String, Object>> listObjects(String prefix, String marker, Integer maxKeys) {
        if(null != maxKeys && maxKeys < 0) {
            maxKeys = 10; //默认列举10个
        }
        
        OSSClient client = getOSSClient();

        List<Map<String, Object>> list = Lists.newArrayList();
        ObjectListing listing;
        do {
            ListObjectsRequest request = getListObjectsRequest(prefix, marker, maxKeys);
            listing = client.listObjects(request);
            
            listing.getObjectSummaries().stream().forEach(summary -> {
                Map<String, Object> map = Maps.newHashMap();
                map.put(summary.getKey(), summary.getSize());
                list.add(map);
            });
            marker = listing.getNextMarker(); //跳向下一页
        }while (listing.isTruncated());
        
        client.shutdown();
        return list;
    }

    /**
     * 获取列表object对象
     * @param prefix
     * @param marker
     * @param maxKeys
     * @return
     */
    private ListObjectsRequest getListObjectsRequest(String prefix, String marker, Integer maxKeys) {
        ListObjectsRequest request = new ListObjectsRequest(OSSConfig.BUCKET_NAME);
        if(!StringUtils.isBlank(prefix)) {
            request.withPrefix(prefix);
        }
        
        if(!StringUtils.isBlank(marker)) {
            request.withMarker(marker);
        }

        if(null != maxKeys && maxKeys > 0) {
            request.withMaxKeys(maxKeys);
        }

        return request;
    }

    /**
     * 删除bucket
     * @return
     */
    public boolean deleteBucket() {
        String bucketName = OSSConfig.BUCKET_NAME;
        OSSClient client = getOSSClient();
        client.deleteBucket(bucketName);
        boolean exist = client.doesBucketExist(bucketName);
        
        client.shutdown();
        return !exist;
    }

    /**
     * 删除key指定对象
     * @param key
     * @return
     */
    public boolean deleteObject(String key) {
        OSSClient client = getOSSClient();
        client.deleteObject(OSSConfig.BUCKET_NAME, key);
        boolean exist = client.doesObjectExist(OSSConfig.BUCKET_NAME, key);
        
        client.shutdown();
        return !exist;
    }

    /**
     * 批量删除存储对象
     * @param keys
     * @return
     */
    public boolean deleteObjects(List<String> keys) {
        OSSClient client = getOSSClient();
        DeleteObjectsResult result = client.deleteObjects(new DeleteObjectsRequest(OSSConfig.BUCKET_NAME).withKeys(keys));
        
        List<String> deleteObjects = result.getDeletedObjects();
        
        client.shutdown();
        return keys.size() == deleteObjects.size();
    }
    
    /**
     * 创建文件路径
     * 也就是oss上对应的key
     * @param file
     * @return
     */
    public String getKey(File file) {
        StringBuilder builder = new StringBuilder();
        builder.append(PropertiesUtil.getProperty("oss.upload.root")).append("/")
                .append(DateUtil.getYYYYMMDDStr(LocalDateTime.now())).append("/")
                .append(UUIDUtil.getFormatedUUID()).append(FileUtil.getExtentionWithDot(file));
        
        return builder.toString();
        
    }

    /**
     * 生成图片处理中心
     * @param key
     * @param localFile
     * @return
     */
    public ImageCenter newImageCenter(String key, File localFile) {
        return new ImageCenter(key, localFile);
    }
    
    /**
     * 图片处理中心
     * 可以链式编程
     */
    class ImageCenter {
        private StringBuilder styleBuilder = new StringBuilder();
        /**
         * 需要转变的图片文件对应key
         */
        private String key;
        /**
         * 转换后的图片存放的位置
         */
        private File localFile;

        public ImageCenter(String key, File localFile) {
            this.key = key;
            this.localFile = localFile;
        }

        /**
         * 缩放图片
         * @param width 缩放后的宽度
         * @param height 缩放后的高度
         * @return
         */
        public ImageCenter resize(int width, int height) {
            logger.debug("===============处理图片缩放");
            if(width < 0 || height < 0) {
                logger.warn("图片缩放处理失败，参数错误");
                return this;
            }
            String style = "resize,m_fixed,w_"+width+",h_"+height;
            addStyle(style);
            
            return this;
        }

        /**
         * 裁剪图片
         */
        public ImageCenter crop(int x, int y, int width, int height) {
            logger.debug("===============处理图片裁剪");
            if(x < 0 || y < 0 || width < 0 || height < 0) {
                logger.warn("图片裁剪处理失败，参数错误");
                return this;
            }
            
            String style = "crop,w_"+width+",h_"+height+",x_"+x+",y_"+y+",r_1";
            addStyle(style);
            
            return this;
        }

        /**
         * 进行图片旋转
         * @param angle 范围[0, 360]
         * @return
         */
        public ImageCenter rotate(int angle) {
            logger.debug("===============处理图片旋转");
            if(angle < 0 || angle > 360) {
                logger.warn("图片旋转处理失败，旋转角度只能为0~360(包含边界值)");
                return this;
            }
            
            String style="rotate," + angle;
            addStyle(style);
            
            return this;
        }

        /**
         * 图片锐化
         * @param value 锐化值只能在0~400之间(包括边界值)
         * @return
         */
        public ImageCenter sharpen(int value) {
            logger.debug("===============处理图片锐化");
            if(value < 0 || value > 400) {
                logger.warn("图片锐化处理失败，锐化值只能为0~400(包含边界值)");
                return this;
            }
            
            String style = "sharpen," + value;
            addStyle(style);
            
            return this;
        }

        /**
         * 添加水印
         * @param secretMark 被加密的水印字符串信息，建议在oss console中生成后复制过来
         *                   请直接复制text_之后的字符串作为该方法的参数(不包含text_)
         * @return
         */
        public ImageCenter watermark(String secretMark) {
            logger.debug("===============处理图片水印");
            if(StringUtils.isBlank(secretMark)) {
                logger.warn("图片水印处理失败，水印参数错误，水印加密字符串建议在oss控制台生成");
                return this;
            }
            
            String style = "watermark,text_" + secretMark;
            addStyle(style);
            
            return this;
        }

        /**
         * 图片格式转换，支持jpg, png, webp, bmp方式
         * @param format
         * @return
         */
        public ImageCenter format(ImageFormatEnum format) {
            logger.debug("===============处理图片格式转换");
            if(null == format) {
                logger.warn("图片格式转化错误，格式不能为空");
                return this;
            }
            
            String style = "format," + format.getValue();
            addStyle(style);
            
            return this;
        }

        /**
         * 获取图片信息
         * @return
         */
        public ImageCenter getImageInfo(File infoFile) {
            logger.debug("===============获取图片信息");
            if(null == infoFile) {
                logger.warn("获取图片信息失败，图片信息保存文件参数不能为空");
                return this;
            }
            
            if(StringUtils.isBlank(key) || null == infoFile) {
                logger.warn("获取图片信息失败，图片文件key或者存放图片目标文件localFile参数有误");
                return this;
            }

            if(0 == styleBuilder.length()) {
                logger.warn("获取图片信息失败，您还没有指定具体的图片处理样式");
                return this;
            }

            FileUtil.createFile(infoFile);
            GetObjectRequest request = new GetObjectRequest(OSSConfig.BUCKET_NAME, key);
            request.setProcess("image/info");

            OSSClient client = getOSSClient();
            client.getObject(request, infoFile);

            client.shutdown();
            
            return this;
        }
        
        /**
         * 添加目标样式到样式集合中，用于处理图片级联操作
         * @param style
         */
        private void addStyle(String style) {
            if(StringUtils.isBlank(style)) {
                return;
            }

            String prefix = getStylePrefix();
            styleBuilder.append(prefix).append(style);
        }

        /**
         * 获取样式的前缀，如果styleBuilder中已经存在image，则后面的样式不能再添加image/
         * 这里是为了构造级联处理图片的样式
         * @return
         */
        private String getStylePrefix() {
            String style = styleBuilder.toString();
            String prefix = "image/";
            if(style.contains(prefix)) {
                return "/";
            }

            return prefix;
        }
        
        /**
         * 根据给定样式集合处理并生成目标图片
         */
        public void transform() {
            if(StringUtils.isBlank(key) || null == localFile) {
                logger.warn("图片无法进行处理，图片文件key或者存放图片目标文件localFile参数有误");
                return;
            }
            
            if(0 == styleBuilder.length()) {
                logger.warn("图片无法进行处理，您还没有指定具体的图片处理样式");
                return;
            }
            
            FileUtil.createFile(localFile);
            GetObjectRequest request = new GetObjectRequest(OSSConfig.BUCKET_NAME, key);
            request.setProcess(styleBuilder.toString());

            OSSClient client = getOSSClient();
            client.getObject(request, localFile);

            client.shutdown();
        }

    }

    /**
     * 带进度的文件上传
     * @param key
     * @param file
     * @return
     */
    public OssProgressor uploadFileWithProgress(String key, File file) {
        if(StringUtils.isBlank(key) || null == file || !file.exists()) {
            return null;
        }
        
        final OssProgressor progressor = new OssProgressor();
        OSSClient client = getOSSClient();
        
        new Thread(() -> {
            PutObjectRequest request = new PutObjectRequest(OSSConfig.BUCKET_NAME, key, file)
                    .withProgressListener((event) -> {
                        long bytes = event.getBytes();
                        ProgressEventType type = event.getEventType();
                        switch(type) {
                            case TRANSFER_STARTED_EVENT:
                                logger.debug("==========开始上传文件{}", key);
                                break;
    
                            case REQUEST_CONTENT_LENGTH_EVENT:
                                logger.debug("==========文件大小为{}", bytes);
                                progressor.setTotal(bytes);
                                break;
    
                            case REQUEST_BYTE_TRANSFER_EVENT:
                                if(!progressor.isError()) { // 如果发生了错误就不在继续执行逻辑
                                    progressor.setCur(progressor.getCur() + bytes);
                                    logger.debug("===========已经上传了{}, 还剩{}字节", progressor.getCur(), 
                                            progressor.getTotal() - progressor.getCur());
                                }
                                break;
    
                            case TRANSFER_COMPLETED_EVENT:
                                logger.debug("上传文件{}成功", key);
                                progressor.setCur(progressor.getTotal());
                                break;
    
                            case TRANSFER_FAILED_EVENT:
                                logger.debug("上传文件{}失败", key);
                                progressor.setErrorMessage("上传文件" + key + "失败");
                                break;
    
                            default:
                                break;
                        }
                    });
            
            client.putObject(request);
        }).start();
        return progressor;
    }

    /**
     * 带进度条的文件下载
     * @param key
     * @param localFile
     * @return
     */
    public OssProgressor downloadFileWithProgress(String key, File localFile) {
        final OssProgressor progressor = new OssProgressor();
        OSSClient client = this.getOSSClient();
        new Thread(() -> {
            GetObjectRequest request = new GetObjectRequest(OSSConfig.BUCKET_NAME, key)
                    .withProgressListener(event -> {
                        long bytes = event.getBytes();
                        ProgressEventType eventType = event.getEventType();
                        switch (eventType) {
                            case TRANSFER_STARTED_EVENT:
                                logger.debug("===============开始下载文件{}", key);
                                break;

                            case RESPONSE_CONTENT_LENGTH_EVENT:
                                logger.debug("==========文件大小为{}", bytes);
                                progressor.setTotal(bytes);
                                break;

                            case RESPONSE_BYTE_TRANSFER_EVENT:
                                if(!progressor.isError()) { // 如果发生了错误就不在继续执行逻辑
                                    progressor.setCur(progressor.getCur() + bytes);
                                    logger.debug("===========已经下载了{}, 还剩{}字节", progressor.getCur(),
                                            progressor.getTotal() - progressor.getCur());
                                }
                                break;

                            case TRANSFER_COMPLETED_EVENT:
                                logger.debug("=============文件下载成功!");
                                progressor.setTotal(progressor.getCur());
                                break;

                            case TRANSFER_FAILED_EVENT:
                                logger.debug("下载文件{}失败", key);
                                progressor.setErrorMessage("下载文件"+key+"失败");
                                break;

                            default:
                                break;
                        }
                    });
            client.getObject(request, localFile);
        }).start();
        
        return progressor;
    }
    
    
}
