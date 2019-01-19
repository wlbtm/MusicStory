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
import java.net.URL;
import java.util.Date;
import java.util.Random;

/**
 * 文件上传工具类
 */
public class UploadUtil {

    /**
     * 云基本信息
     */
    private static String endpoint = "https://oss-cn-hongkong-internal.aliyuncs.com";
    private static String accessKey = "LTAI6bpHNc1Fjfb7";
    private static String secretKey = "KBH0GfDdJAhTyMOZ3kCXqBmVtNmueb";
    private static String bucketName = "music-story";
    private static String returnPath = "oss.ngcly.cn";

    /**
     * 七牛云上传（由于七牛云取消了测试域名，所以用阿里云）
     * @param file
     * @return
     * @throws Exception
     */
    public static String uploadFile(MultipartFile file) throws Exception {
        //构造一个带指定Zone对象的配置类 七牛上传
        Configuration cfg = new Configuration(Zone.zone2());
        UploadManager uploadManager = new UploadManager(cfg);
        //默认不指定key的情况下，以文件内容的hash值作为文件名
        String key = null;
        byte[] uploadBytes = file.getBytes();
        Auth auth = Auth.create(accessKey, secretKey);
        String upToken = auth.uploadToken(bucketName);

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
        OSSClient ossClient = new OSSClient(endpoint, accessKey, secretKey);
        // 上传内容到指定的存储空间（bucketName）并保存为指定的文件名称（objectName）。
        String suffix = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".")).toLowerCase();
        Random random = new Random();
        String fileName = random.nextInt(10000) + System.currentTimeMillis() + suffix;
        String fileKey = cloudDir+"/"+fileName;
        ossClient.putObject(bucketName, fileKey, new ByteArrayInputStream(file.getBytes()));

        // 设置URL过期时间为10年 3600l* 1000*24*365*10
        Date expiration = new Date(System.currentTimeMillis() + 3600L * 1000 * 24 * 365 * 10);
        // 生成URL
        URL url = ossClient.generatePresignedUrl(bucketName, fileKey, expiration);
        // 关闭OSSClient
        ossClient.shutdown();
        if (url != null) {
            return url.toString().replace("music-story.oss-cn-hongkong-internal.aliyuncs.com",returnPath);
        }
        return "";
    }

    /**
     * 阿里云删除文件
     * @param url 图片地址
     */
    public static void deleteFileByAli(String url){
        // 创建OSSClient实例。
        OSSClient ossClient = new OSSClient(endpoint, accessKey, secretKey);
        // 删除文件。
        String fileName = url.replace(returnPath,"");
        ossClient.deleteObject(bucketName, fileName.substring(fileName.lastIndexOf(":/"),fileName.indexOf("?")));
        // 关闭OSSClient。
        ossClient.shutdown();
    }
}
