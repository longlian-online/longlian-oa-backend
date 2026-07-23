package online.longlian.app.service.resource;

import online.longlian.app.pojo.bo.common.PresignedUploadUrlParamsBO;
import online.longlian.app.pojo.bo.common.PresignedUploadUrlResultBO;
import online.longlian.common.enumeration.StorageType;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.Map;

/**
 * 文件存储策略接口。
 * <p>
 * 定义文件上传和读取的统一抽象，支持多种存储后端。
 * 各实现通过 {@link StorageServiceFactory} 按配置的存储类型自动路由。
 * <p>
 * 文件上传采用预签名 URL 模式：服务端生成带时效的上传 URL，客户端直传文件到存储后端。
 */
public interface StorageService {

    /**
     * 自标识存储类型，供 {@link StorageServiceFactory} 自动注册和路由。
     *
     * @return 当前策略对应的存储类型枚举
     */
    StorageType getStorageType();

    /**
     * 生成预签名上传 URL。
     *
     * @param params 包含文件名、业务类型等上传参数
     * @return 包含预签名 URL 和文件访问 key 的上传凭证
     */
    PresignedUploadUrlResultBO generatePresignedUploadUrl(PresignedUploadUrlParamsBO params);

    /**
     * 获取单个文件的访问 URL。
     *
     * @param key 文件存储 key
     * @return 可公开访问的文件 URL
     */
    String getResourceReadUrl(String key);

    /**
     * 批量获取文件的访问 URL。
     *
     * @param keys 文件 key 列表
     * @return key 到 URL 的映射
     */
    Map<String, String> getResourceReadUrls(List<String> keys);

    default void upload(String key, byte[] content) {
        throw new UnsupportedOperationException("当前存储不支持服务端上传");
    }

    default Resource getResource(String key) {
        throw new UnsupportedOperationException("当前存储不支持本地文件读取");
    }
}
