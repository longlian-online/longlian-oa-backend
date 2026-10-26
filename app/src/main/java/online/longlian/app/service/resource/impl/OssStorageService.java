package online.longlian.app.service.resource.impl;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.model.ResponseHeaderOverrides;
import com.qcloud.cos.region.Region;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.properties.StorageProperties;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.pojo.bo.common.PresignedUploadUrlParamsBO;
import online.longlian.app.pojo.bo.common.PresignedUploadUrlResultBO;
import online.longlian.app.pojo.bo.common.ResourceProbeParamsBO;
import online.longlian.app.service.resource.StorageService;
import online.longlian.common.enumeration.StorageType;
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
    private final long presignedUrlTtlMillis;

    public OssStorageService(StorageProperties storageProperties) {
        ossConfig = storageProperties.getOss();
        presignedUrlTtlMillis = Math.multiplyExact(storageProperties.getPresignedUrlTtlSeconds(), 1000L);
        COSCredentials cred = new BasicCOSCredentials(ossConfig.getSecretId(), ossConfig.getSecretKey());
        ClientConfig clientConfig = new ClientConfig(new Region(ossConfig.getRegion()));
        this.cosClient = new COSClient(cred, clientConfig);
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.OSS;
    }

    private String getPresignUrl(String key, HttpMethodName method, String contentDisposition) {
        Date expiration = new Date(System.currentTimeMillis() + presignedUrlTtlMillis);
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                ossConfig.getBucket(),
                key,
                method
        );
        request.setExpiration(expiration);
        if (contentDisposition != null) {
            // content-disposition 属于 COS 默认必签头：签名后前端上传必须逐字携带该头，
            // 对象元数据保存原文件名，读取时经 CDN/预签名透传，浏览器下载名即原文件名。
            request.putCustomRequestHeader("Content-Disposition", contentDisposition);
        }
        URL url = cosClient.generatePresignedUrl(request);
        return url.toString();
    }

    @Override
    public PresignedUploadUrlResultBO generatePresignedUploadUrl(PresignedUploadUrlParamsBO params) {
        return new PresignedUploadUrlResultBO(
                getPresignUrl(params.getKey(), HttpMethodName.PUT, params.getContentDisposition()),
                params.getKey(), params.getContentDisposition());
    }

    @Override
    public String getResourceReadUrl(String key) {
        return getPresignUrl(key, HttpMethodName.GET, null);
    }

    /** 读取时通过 response-content-disposition 覆盖响应头，让直连预签名下载也还原原文件名。 */
    @Override
    public String getResourceReadUrl(String key, String fileName) {
        if (fileName == null) {
            return getPresignUrl(key, HttpMethodName.GET, null);
        }
        Date expiration = new Date(System.currentTimeMillis() + presignedUrlTtlMillis);
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                ossConfig.getBucket(), key, HttpMethodName.GET);
        request.setExpiration(expiration);
        request.setResponseHeaders(new ResponseHeaderOverrides().withContentDisposition(fileName));
        return cosClient.generatePresignedUrl(request).toString();
    }

    @Override
    public Map<String, String> getResourceReadUrls(List<String> keys) {
        return keys.stream().collect(Collectors.toMap(key -> key, this::getResourceReadUrl));
    }

    @Override
    public void probe(ResourceProbeParamsBO params) {
        try {
            if (cosClient.getObjectMetadata(ossConfig.getBucket(), params.storageKey()).getContentLength()
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
        cosClient.deleteObject(ossConfig.getBucket(), key);
    }

    @Override
    public OptionalLong findContentLength(String key) {
        try {
            return OptionalLong.of(cosClient.getObjectMetadata(ossConfig.getBucket(), key).getContentLength());
        } catch (CosServiceException exception) {
            if (exception.getStatusCode() == 404) {
                return OptionalLong.empty();
            }
            throw exception;
        }
    }

    private AppException incompleteFile() {
        return new AppException(ResultCode.OPERATION_FAIL, "文件未完成上传或内容不匹配");
    }

    @Override
    public void destroy() throws Exception {
        // 关闭 oss 客户端
        this.cosClient.shutdown();
        log.info("oss 客户端已关闭");
    }
}
