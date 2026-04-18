package online.longlian.app.service.resource.impl;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.region.Region;
import online.longlian.app.common.enumeration.StorageType;
import online.longlian.app.common.properties.StorageProperties;
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
    private final StorageProperties.OssConfig ossConfig;

    public OssStorageService(StorageProperties storageProperties) {
        ossConfig = storageProperties.getOss();
        COSCredentials cred = new BasicCOSCredentials(ossConfig.getSecretId(), ossConfig.getSecretKey());
        ClientConfig clientConfig = new ClientConfig(new Region(ossConfig.getRegion()));
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
                ossConfig.getBucket(),
                params.getKey(),
                HttpMethodName.PUT
        );
        request.setExpiration(expiration);
        URL url = cosClient.generatePresignedUrl(request);
        return new PresignedUploadUrlResultBO(url.toString(), params.getKey());
    }

    @Override
    public String getResourceReadUrl(Long fileId) {
        return "";
    }

    @Override
    public Map<Long, String> getResourceReadUrls(List<Long> fileIds) {
        return new HashMap<>();
    }
}