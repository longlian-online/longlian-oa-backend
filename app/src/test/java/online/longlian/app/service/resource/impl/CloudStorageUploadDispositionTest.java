package online.longlian.app.service.resource.impl;

import online.longlian.app.common.properties.StorageProperties;
import online.longlian.app.pojo.bo.common.PresignedUploadUrlParamsBO;
import online.longlian.app.service.resource.StorageService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** 离线断言 COS SDK 生成的预签名 URL 真实包含 disposition 头，保证前端 PUT 头被纳入签名与对象元数据。 */
class CloudStorageUploadDispositionTest {

    static Stream<StorageService> cloudStorageServices() {
        StorageProperties cosProperties = new StorageProperties();
        cosProperties.setPresignedUrlTtlSeconds(120);
        StorageProperties.CosConfig cosConfig = new StorageProperties.CosConfig();
        cosConfig.setBucket("cos-bucket");
        cosConfig.setRegion("ap-guangzhou");
        cosConfig.setSecretId("test-secret-id");
        cosConfig.setSecretKey("test-secret-key");
        cosProperties.setCos(cosConfig);

        StorageProperties ossProperties = new StorageProperties();
        ossProperties.setPresignedUrlTtlSeconds(120);
        StorageProperties.OssConfig ossConfig = new StorageProperties.OssConfig();
        ossConfig.setBucket("oss-bucket");
        ossConfig.setRegion("ap-guangzhou");
        ossConfig.setSecretId("test-secret-id");
        ossConfig.setSecretKey("test-secret-key");
        ossProperties.setOss(ossConfig);

        return Stream.of(new CosStorageService(cosProperties), new OssStorageService(ossProperties));
    }

    @ParameterizedTest
    @MethodSource("cloudStorageServices")
    void shouldPutContentDispositionIntoSignedHeaderList(StorageService storage) {
        String disposition = "attachment; filename=\"file.docx\"; filename*=UTF-8''%E6%88%91.docx";
        String uploadUrl = storage.generatePresignedUploadUrl(
                new PresignedUploadUrlParamsBO("task_submit/1.docx", disposition)).getUploadUrl();

        // SDK 以 sign= 包裹预签名参数，头列表经 URL 编码呈现。
        assertThat(uploadUrl).contains("q-header-list%3Dcontent-disposition");
    }

    @ParameterizedTest
    @MethodSource("cloudStorageServices")
    void shouldOmitHeaderListEntryWithoutDisposition(StorageService storage) {
        String uploadUrl = storage.generatePresignedUploadUrl(
                new PresignedUploadUrlParamsBO("avatar/1.png", null)).getUploadUrl();

        assertThat(uploadUrl).doesNotContain("content-disposition");
    }
}
