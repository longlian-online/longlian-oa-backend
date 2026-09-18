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
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.pojo.bo.common.PresignedUploadUrlParamsBO;
import online.longlian.app.pojo.bo.common.PresignedUploadUrlResultBO;
import online.longlian.app.pojo.bo.common.ResourceProbeParamsBO;
import online.longlian.app.service.resource.StorageService;
import online.longlian.app.service.resource.EdgeOneUrlSigner;
import online.longlian.common.enumeration.StorageType;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.Clock;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CosStorageService implements StorageService, DisposableBean {


    private final COSClient cosClient;
    private final StorageProperties.CosConfig cosConfig;
    private final EdgeOneUrlSigner edgeOneUrlSigner;
    private final long presignedUrlTtlMillis;

    public CosStorageService(StorageProperties storageProperties, Clock clock) {
        cosConfig = storageProperties.getCos();
        presignedUrlTtlMillis = Math.multiplyExact(storageProperties.getPresignedUrlTtlSeconds(), 1000L);
        COSCredentials cred = new BasicCOSCredentials(cosConfig.getSecretId(), cosConfig.getSecretKey());
        ClientConfig clientConfig = new ClientConfig(new Region(cosConfig.getRegion()));
        this.cosClient = new COSClient(cred, clientConfig);
        edgeOneUrlSigner = new EdgeOneUrlSigner(cosConfig.getUrlPrefix(), cosConfig.getEdgeOneAuthKey(), clock);
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.COS;
    }

    private String getPresignUploadUrl(String key) {
        Date expiration = new Date(System.currentTimeMillis() + presignedUrlTtlMillis);
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                cosConfig.getBucket(),
                key,
                HttpMethodName.PUT
        );
        request.setExpiration(expiration);
        URL url = cosClient.generatePresignedUrl(request);
        return url.toString();
    }

    @Override
    public PresignedUploadUrlResultBO generatePresignedUploadUrl(PresignedUploadUrlParamsBO params) {
        return new PresignedUploadUrlResultBO(getPresignUploadUrl(params.getKey()), params.getKey());
    }

    @Override
    public String getResourceReadUrl(String key) {
        return edgeOneUrlSigner.sign(key);
    }

    @Override
    public Map<String, String> getResourceReadUrls(List<String> keys) {
        return keys.stream().collect(Collectors.toMap(key -> key, this::getResourceReadUrl));
    }

    @Override
    public void probe(ResourceProbeParamsBO params) {
        try {
            if (cosClient.getObjectMetadata(cosConfig.getBucket(), params.storageKey()).getContentLength()
                    != params.expectedSize()) {
                throw incompleteFile();
            }
        } catch (AppException e) {
            throw e;
        } catch (RuntimeException e) {
            throw incompleteFile();
        }
    }

    @Override
    public void delete(String key) {
        cosClient.deleteObject(cosConfig.getBucket(), key);
    }

    private AppException incompleteFile() {
        return new AppException(ResultCode.OPERATION_FAIL, "文件未完成上传或内容不匹配");
    }

    @Override
    public void destroy() {
        this.cosClient.shutdown();
        log.info("COS 客户端已关闭");
    }
}
