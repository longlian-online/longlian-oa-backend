package online.longlian.app.service.resource.impl;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.model.ResponseHeaderOverrides;
import com.qcloud.cos.region.Region;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.pojo.bo.common.ResourceProbeParamsBO;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.stream.Collectors;

/**
 * COS 与历史 OSS 配置都走腾讯云 COS SDK，只是桶和密钥不同。
 */
final class TencentCosBucketClient {

    private final COSClient cosClient;
    private final String bucket;
    private final long presignedUrlTtlMillis;

    TencentCosBucketClient(String secretId, String secretKey, String region, String bucket,
                           long presignedUrlTtlMillis) {
        this.bucket = bucket;
        this.presignedUrlTtlMillis = presignedUrlTtlMillis;
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        this.cosClient = new COSClient(cred, new ClientConfig(new Region(region)));
    }

    String presign(String key, HttpMethodName method, String contentDisposition) {
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, key, method);
        request.setExpiration(expiration());
        if (contentDisposition != null) {
            // content-disposition 属于 COS 默认必签头：签名后前端上传必须逐字携带该头，
            // COS 把它存为对象元数据，读取时经 CDN/预签名透传，浏览器下载名即原文件名。
            request.putCustomRequestHeader("Content-Disposition", contentDisposition);
        }
        return cosClient.generatePresignedUrl(request).toString();
    }

    /** 读取时通过 response-content-disposition 覆盖响应头，让直连预签名下载也还原原文件名。 */
    String readUrl(String key, String fileName) {
        if (fileName == null) {
            return presign(key, HttpMethodName.GET, null);
        }
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, key, HttpMethodName.GET);
        request.setExpiration(expiration());
        request.setResponseHeaders(new ResponseHeaderOverrides().withContentDisposition(fileName));
        return cosClient.generatePresignedUrl(request).toString();
    }

    Map<String, String> readUrls(List<String> keys) {
        return keys.stream().collect(Collectors.toMap(key -> key, key -> readUrl(key, null)));
    }

    void probe(ResourceProbeParamsBO params) {
        try {
            if (cosClient.getObjectMetadata(bucket, params.storageKey()).getContentLength() != params.expectedSize()) {
                throw incompleteFile();
            }
        } catch (AppException e) {
            throw e;
        } catch (RuntimeException e) {
            throw incompleteFile();
        }
    }

    void delete(String key) {
        cosClient.deleteObject(bucket, key);
    }

    OptionalLong findContentLength(String key) {
        try {
            return OptionalLong.of(cosClient.getObjectMetadata(bucket, key).getContentLength());
        } catch (CosServiceException exception) {
            if (exception.getStatusCode() == 404) {
                return OptionalLong.empty();
            }
            throw exception;
        }
    }

    void shutdown() {
        cosClient.shutdown();
    }

    private Date expiration() {
        return new Date(System.currentTimeMillis() + presignedUrlTtlMillis);
    }

    private static AppException incompleteFile() {
        return new AppException(ResultCode.OPERATION_FAIL, "文件未完成上传或内容不匹配");
    }
}
