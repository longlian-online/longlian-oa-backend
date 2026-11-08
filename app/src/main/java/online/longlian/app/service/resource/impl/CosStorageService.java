package online.longlian.app.service.resource.impl;

import com.qcloud.cos.http.HttpMethodName;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.properties.StorageProperties;
import online.longlian.app.pojo.bo.common.PresignedUploadUrlParamsBO;
import online.longlian.app.pojo.bo.common.PresignedUploadUrlResultBO;
import online.longlian.app.pojo.bo.common.ResourceProbeParamsBO;
import online.longlian.app.service.resource.StorageService;
import online.longlian.common.enumeration.StorageType;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

@Slf4j
@Service
public class CosStorageService implements StorageService, DisposableBean {

    private final TencentCosBucketClient client;

    public CosStorageService(StorageProperties storageProperties) {
        StorageProperties.CosConfig config = storageProperties.getCos();
        this.client = new TencentCosBucketClient(
                config.getSecretId(),
                config.getSecretKey(),
                config.getRegion(),
                config.getBucket(),
                Math.multiplyExact(storageProperties.getPresignedUrlTtlSeconds(), 1000L));
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.COS;
    }

    @Override
    public PresignedUploadUrlResultBO generatePresignedUploadUrl(PresignedUploadUrlParamsBO params) {
        return new PresignedUploadUrlResultBO(
                client.presign(params.getKey(), HttpMethodName.PUT, params.getContentDisposition()),
                params.getKey(), params.getContentDisposition());
    }

    @Override
    public String getResourceReadUrl(String key) {
        return client.readUrl(key, null);
    }

    @Override
    public String getResourceReadUrl(String key, String fileName) {
        return client.readUrl(key, fileName);
    }

    @Override
    public Map<String, String> getResourceReadUrls(List<String> keys) {
        return client.readUrls(keys);
    }

    @Override
    public void probe(ResourceProbeParamsBO params) {
        client.probe(params);
    }

    @Override
    public void delete(String key) {
        client.delete(key);
    }

    @Override
    public OptionalLong findContentLength(String key) {
        return client.findContentLength(key);
    }

    @Override
    public void destroy() {
        client.shutdown();
        log.info("COS 客户端已关闭");
    }
}
