package online.longlian.app.service.resource;

import online.longlian.app.common.properties.StorageProperties;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.pojo.bo.common.PresignedUploadUrlResultBO;
import online.longlian.app.mapper.ResourceMapper;
import online.longlian.app.pojo.bo.common.PresignedUploadUrlParamsBO;
import online.longlian.app.pojo.bo.common.ResourceCreateParamsBO;
import online.longlian.app.pojo.entity.Resource;
import online.longlian.app.pojo.vo.common.ResourceCreateVO;
import online.longlian.app.service.resource.impl.OssStorageService;
import org.mockito.ArgumentCaptor;
import online.longlian.common.enumeration.StorageType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class ResourceServiceTest {
    @Mock
    private  ResourceMapper resourceMapper;

    @Mock
    private StorageServiceFactory storageServiceFactory;

    @Mock
    private OssStorageService ossStorageService;

    @InjectMocks
    private ResourceService resourceService;

    @BeforeEach
    void setUp() {
        StorageProperties storageProperties = new StorageProperties();
        storageProperties.setType(StorageType.OSS);
        storageProperties.setOss(new StorageProperties.OssConfig());
        resourceService = new ResourceService(resourceMapper, storageServiceFactory,
                new CdnUrlSigner("https://cdn.example", "test-secret", Clock.systemUTC()),
                new LocalFileUrlSigner("test-local-signing-secret-32-bytes", storageProperties, Clock.systemUTC()),
                storageProperties, Clock.systemUTC());
    }

    @Test
    void testCreate() {
        ResourceCreateParamsBO params = new ResourceCreateParamsBO(1L, 1L, "test_file", "txt", 100L, "", "biz");
        PresignedUploadUrlResultBO presignedUploadUrlResult = PresignedUploadUrlResultBO.builder()
                .uploadUrl("xxxxxx").key("xxxxxx").contentDisposition("attachment; filename=\"file.txt\"").build();
        Mockito.when(resourceMapper.insert(Mockito.any(Resource.class))).thenReturn(1);
        Mockito.when(storageServiceFactory.get(Mockito.any())).thenReturn(ossStorageService);
        Mockito.when(ossStorageService.generatePresignedUploadUrl(Mockito.any())).thenReturn(presignedUploadUrlResult);

        Assertions.assertDoesNotThrow(() -> resourceService.create(params));
    }

    /** 只有任务提交附件携带 disposition；头像封面保持原行为。 */
    @Test
    void testCreateTaskSubmitCarriesDisposition() {
        ResourceCreateParamsBO params = new ResourceCreateParamsBO(1L, 1L, "我的文件.docx", "docx", 100L,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "task_submit");
        Mockito.when(resourceMapper.insert(Mockito.any(Resource.class))).thenReturn(1);
        Mockito.when(storageServiceFactory.get(Mockito.any())).thenReturn(ossStorageService);
        Mockito.when(ossStorageService.generatePresignedUploadUrl(Mockito.any()))
                .thenAnswer(invocation -> {
                    PresignedUploadUrlParamsBO uploadParams = invocation.getArgument(0);
                    return PresignedUploadUrlResultBO.builder()
                            .uploadUrl("xxxxxx").key(uploadParams.getKey())
                            .contentDisposition(uploadParams.getContentDisposition())
                            .build();
                });

        ResourceCreateVO vo = resourceService.create(params);

        ArgumentCaptor<PresignedUploadUrlParamsBO> uploadParams =
                ArgumentCaptor.forClass(PresignedUploadUrlParamsBO.class);
        Mockito.verify(ossStorageService).generatePresignedUploadUrl(uploadParams.capture());
        assertThat(uploadParams.getValue().getContentDisposition())
                .startsWith("attachment; filename=\"____.docx\"; filename*=UTF-8''%E6%88%91")
                .endsWith(".docx");
        assertThat(vo.getContentDisposition()).isEqualTo(uploadParams.getValue().getContentDisposition());
    }

    /** 非任务附件业务不生成 disposition。 */
    @Test
    void testCreateAvatarOmitsDisposition() {
        ResourceCreateParamsBO params = new ResourceCreateParamsBO(1L, 1L, "avatar.png", "png", 100L,
                "image/png", "avatar");
        Mockito.when(resourceMapper.insert(Mockito.any(Resource.class))).thenReturn(1);
        Mockito.when(storageServiceFactory.get(Mockito.any())).thenReturn(ossStorageService);
        Mockito.when(ossStorageService.generatePresignedUploadUrl(Mockito.any()))
                .thenAnswer(invocation -> {
                    PresignedUploadUrlParamsBO uploadParams = invocation.getArgument(0);
                    return PresignedUploadUrlResultBO.builder()
                            .uploadUrl("xxxxxx").key(uploadParams.getKey())
                            .contentDisposition(uploadParams.getContentDisposition())
                            .build();
                });

        ResourceCreateVO vo = resourceService.create(params);

        assertThat(vo.getContentDisposition()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"cover", "thumbnail"})
    void create_projectImageLargerThanTenMegabytes_rejectedBeforeInsert(String bizType) {
        assertThatThrownBy(() -> resourceService.create(new ResourceCreateParamsBO(
                1L, 1L, "image.webp", "webp", 10L * 1024 * 1024 + 1, "image/webp", bizType)))
                .isInstanceOfSatisfying(AppException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode());
                });
        Mockito.verify(resourceMapper, Mockito.never()).insert(Mockito.any(Resource.class));
    }

    @Test
    void create_thumbnailNonImage_rejected() {
        assertThatThrownBy(() -> resourceService.create(new ResourceCreateParamsBO(
                1L, 1L, "thumb.pdf", "pdf", 100L, "application/pdf", "thumbnail")))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("只能上传图片");
        Mockito.verify(resourceMapper, Mockito.never()).insert(Mockito.any(Resource.class));
    }
}
