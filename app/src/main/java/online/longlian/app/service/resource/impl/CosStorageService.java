package online.longlian.app.service.resource.impl;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.region.Region;
import online.longlian.app.pojo.entity.Resource;
import online.longlian.app.pojo.bo.PresignedUpload;
import online.longlian.app.service.resource.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.Date;

@Service
public class CosStorageService implements StorageService {

    private final COSClient cosClient;
    private final String bucket;

    // 构造函数中初始化 COSClient
    public CosStorageService(
            @Value("${storage.cos.bucket}") String bucket,
            @Value("${storage.cos.region}") String region,
            @Value("${storage.cos.secret-id}") String secretId,
            @Value("${storage.cos.secret-key}") String secretKey
    ) {
        this.bucket = bucket;
        // 使用密钥创建 COS 凭证对象
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        // 指定 COS 所在地域
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        // 创建 COS 客户端（线程安全、可复用）
        this.cosClient = new COSClient(cred, clientConfig);
    }

    @Override
    public PresignedUpload generatePresignedUpload(Resource resource) {

        // 设置预签名 URL 过期时间（当前时间 + 10 分钟）
        Date expiration = new Date(System.currentTimeMillis() + 10 * 60 * 1000);

        GeneratePresignedUrlRequest request =
                new GeneratePresignedUrlRequest(
                        bucket,
                        resource.getKey(),
                        HttpMethodName.PUT
                );

        // 设置 URL 的过期时间
        request.setExpiration(expiration);

        URL url = cosClient.generatePresignedUrl(request);

        return new PresignedUpload(url.toString(), resource.getKey());
    }

}

