package com.cn.util;

import com.google.gson.Gson;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传工具类
 */
public class UploadUtil {

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
}
