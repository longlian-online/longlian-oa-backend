package online.longlian.app.service.resource.impl;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.region.Region;
import online.longlian.app.common.enumeration.StorageType;
import online.longlian.app.pojo.bo.PresignedUploadUrlParamsBO;
import online.longlian.app.pojo.bo.PresignedUploadUrlResultBO;
import online.longlian.app.service.resource.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.*;

@Service
public class OssStorageService implements StorageService {

    private final COSClient cosClient;
    private final String bucket;

    public OssStorageService(
            @Value("${storage.cos.bucket}") String bucket,
            @Value("${storage.cos.region}") String region,
            @Value("${storage.cos.secret-id}") String secretId,
            @Value("${storage.cos.secret-key}") String secretKey
    ) {
        this.bucket = bucket;
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        this.cosClient = new COSClient(cred, clientConfig);
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.OSS;
    }

    @Override
    public PresignedUploadUrlResultBO generatePresignedUploadUrl(PresignedUploadUrlParamsBO params) {
        Date expiration = new Date(System.currentTimeMillis() + 10 * 60 * 1000);
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                bucket,
                params.getKey(),
                HttpMethodName.PUT
        );
        request.setExpiration(expiration);
        URL url = cosClient.generatePresignedUrl(request);
        return new PresignedUploadUrlResultBO(url.toString(), params.getKey());
    }

    @Override
    public String getFileUrl(Long fileId) {
        return "";
    }

    @Override
    public Map<Long, String> getFileUrls(List<Long> fileIds) {
        return new HashMap<>();
    }
}