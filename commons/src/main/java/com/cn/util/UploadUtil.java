package com.cn.util;

import com.aliyun.oss.OSSClient;
import com.google.gson.Gson;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * 文件上传工具类
 */
public class UploadUtil {

    /**
     * 阿里云基本信息
     */
    private static String endpoint = "http://oss-cn-hongkong-internal.aliyuncs.com";
    private static String accessKeyId = "<yourAccessKeyId>";
    private static String accessKeySecret = "<yourAccessKeySecret>";
    private static String bucketName = "music-story";

    /**
     * 七牛云上传
     * @param file
     * @return
     * @throws Exception
     */
    public static String uploadFile(MultipartFile file) throws Exception {
        //构造一个带指定Zone对象的配置类 七牛上传
        Configuration cfg = new Configuration(Zone.zone2());
        UploadManager uploadManager = new UploadManager(cfg);
        //...生成上传凭证，然后准备上传
        String accessKey = "yAbBqIQ633g5U4BtZtEJtGkic8l6ALkngpfNeKX_";
        String secretKey = "ry0PDc4GZk9Gd6sYpOYg0EGchKdo3S1aPhJJbAPP";
        String bucket = "music-story";
        String returnPath="http://p6wg9ob78.bkt.clouddn.com/";
        //默认不指定key的情况下，以文件内容的hash值作为文件名
        String key = null;
        byte[] uploadBytes = file.getBytes();
        Auth auth = Auth.create(accessKey, secretKey);
        String upToken = auth.uploadToken(bucket);

        Response response = uploadManager.put(uploadBytes, key, upToken);
        //解析上传成功的结果
        DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
        returnPath+=putRet.hash;
        return returnPath;
    }

    /**
     * 阿里云上传
     * @param file
     * @param cloudDir 上传至服务器目录
     * @return
     * @throws IOException
     */
    public static String uploadFileByAli(MultipartFile file,String cloudDir) throws IOException {
        OSSClient ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret);
        // 上传内容到指定的存储空间（bucketName）并保存为指定的文件名称（objectName）。
        String fileKey = cloudDir+"/"+file.getName();
        ossClient.putObject(bucketName, fileKey, new ByteArrayInputStream(file.getBytes()));
        String url = "http://music-story.oss-cn-hongkong-internal.aliyuncs.com/"+file.getName();
        // 关闭OSSClient。
        ossClient.shutdown();
        return url;
    }

    /**
     * 阿里云删除文件
     * @param fileName
     */
    public static void deleteFileByAli(String fileName){
        // 创建OSSClient实例。
        OSSClient ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret);
        // 删除文件。
        ossClient.deleteObject(bucketName, fileName);
        // 关闭OSSClient。
        ossClient.shutdown();
    }
}
