package online.longlian.app.service.resource.impl;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.region.Region;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.properties.StorageProperties;
import online.longlian.app.pojo.bo.PresignedUploadUrlParamsBO;
import online.longlian.app.pojo.bo.PresignedUploadUrlResultBO;
import online.longlian.app.service.resource.StorageService;
import online.longlian.common.enumeration.StorageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OssStorageService implements StorageService, DisposableBean {

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

    private String getPresignUrl(String key, HttpMethodName method) {
        int EXPIRE_SECONDS = 10 * 60 * 1000;
        Date expiration = new Date(System.currentTimeMillis() + EXPIRE_SECONDS);
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                ossConfig.getBucket(),
                key,
                method
        );
        request.setExpiration(expiration);
        URL url = cosClient.generatePresignedUrl(request);
        return url.toString();
    }

    @Override
    public PresignedUploadUrlResultBO generatePresignedUploadUrl(PresignedUploadUrlParamsBO params) {
        return new PresignedUploadUrlResultBO(this.getPresignUrl(params.getKey(), HttpMethodName.PUT), params.getKey());
    }

    @Override
    public String getResourceReadUrl(String key) {
        return getPresignUrl(key, HttpMethodName.GET);
    }

    @Override
    public Map<String, String> getResourceReadUrls(List<String> keys) {
        return keys.stream().collect(Collectors.toMap(key -> key, this::getResourceReadUrl));
    }

    @Override
    public void destroy() throws Exception {
        // 关闭 oss 客户端
        this.cosClient.shutdown();
        log.info("oss 客户端已关闭");
    }
}