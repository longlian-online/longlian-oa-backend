package online.longlian.app.service.resource;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.properties.StorageProperties;
import online.longlian.app.mapper.ResourceMapper;
import online.longlian.app.pojo.bo.PresignedUploadUrlResultBO;
import online.longlian.app.pojo.bo.ResourceCreateParamsBO;
import online.longlian.app.pojo.entity.Resource;
import online.longlian.app.service.resource.impl.OssStorageService;
import online.longlian.common.enumeration.FileProcessStatus;
import online.longlian.common.enumeration.StorageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ResourceServiceTest {
    @Mock
    private  ResourceMapper resourceMapper;

    @Mock
    private StorageServiceFactory storageServiceFactory;

    @Mock
    private OssStorageService ossStorageService;

    private ResourceService resourceService;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Resource.class);
        StorageProperties storageProperties = new StorageProperties();
        storageProperties.setType(StorageType.OSS);
        storageProperties.setOss(new StorageProperties.OssConfig());
        resourceService = new ResourceService(resourceMapper, storageServiceFactory, storageProperties);
    }

    @Test
    public void shouldCreatePendingResourceWithUploadUrl() {
        ResourceCreateParamsBO params = new ResourceCreateParamsBO(1L, 1L, StorageType.OSS, "test_file", "txt", 100L, "", "biz", 1L);
        PresignedUploadUrlResultBO presignedUploadUrlResult = PresignedUploadUrlResultBO.builder()
                .uploadUrl("https://storage.example.test/upload")
                .key("biz/1.txt")
                .build();
        when(resourceMapper.insert(org.mockito.ArgumentMatchers.any(Resource.class))).thenReturn(1);
        when(storageServiceFactory.get(StorageType.OSS)).thenReturn(ossStorageService);
        when(ossStorageService.generatePresignedUploadUrl(org.mockito.ArgumentMatchers.any())).thenReturn(presignedUploadUrlResult);
        ArgumentCaptor<Resource> resource = ArgumentCaptor.forClass(Resource.class);

        var result = resourceService.create(params);

        verify(resourceMapper).insert(resource.capture());
        assertThat(result.getUploadUrl()).isEqualTo("https://storage.example.test/upload");
        assertThat(resource.getValue().getProcessStatus()).isEqualTo(FileProcessStatus.Pending);
        assertThat(resource.getValue().getStorageType()).isEqualTo(StorageType.OSS);
    }

    @Test
    public void shouldReturnReadUrlForActivatedResource() {
        Resource resource = Resource.builder()
                .id(1L)
                .orgId(2L)
                .storageType(StorageType.OSS)
                .storageKey("avatar/1.png")
                .processStatus(FileProcessStatus.Activated)
                .build();
        when(resourceMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(resource));
        when(storageServiceFactory.get(StorageType.OSS)).thenReturn(ossStorageService);
        when(ossStorageService.getResourceReadUrl("avatar/1.png")).thenReturn("https://storage.example.test/avatar/1.png");

        String readUrl = resourceService.getResourceReadUrl(1L);

        assertThat(readUrl).isEqualTo("https://storage.example.test/avatar/1.png");
    }

    @Test
    public void shouldRejectReadUrlForMissingOrInactiveResource() {
        when(resourceMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());

        assertThatThrownBy(() -> resourceService.getResourceReadUrl(1L))
                .isInstanceOf(AppException.class);
    }
}
