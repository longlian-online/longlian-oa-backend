package online.longlian.app.service.resource;

import online.longlian.app.common.properties.StorageProperties;
import online.longlian.app.mapper.ResourceMapper;
import online.longlian.app.pojo.bo.PresignedUploadUrlResultBO;
import online.longlian.app.pojo.bo.ResourceCreateParamsBO;
import online.longlian.app.pojo.entity.Resource;
import online.longlian.app.service.resource.impl.OssStorageService;
import online.longlian.generator.enumeration.StorageType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

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
        resourceService = new ResourceService(resourceMapper, storageServiceFactory, storageProperties);
    }

    @Test
    void testCreate() {
        ResourceCreateParamsBO params = new ResourceCreateParamsBO(1L, 1L, StorageType.OSS, "test_file", "txt", 100L, "", "biz", 1L);
        PresignedUploadUrlResultBO presignedUploadUrlResult= PresignedUploadUrlResultBO.builder().uploadUrl("xxxxxx").key("xxxxxx").build();
        Mockito.when(resourceMapper.insert(Mockito.any(Resource.class))).thenReturn(1);
        Mockito.when(storageServiceFactory.get(Mockito.any())).thenReturn(ossStorageService);
        Mockito.when(ossStorageService.generatePresignedUploadUrl(Mockito.any())).thenReturn(presignedUploadUrlResult);

        Assertions.assertDoesNotThrow(()->{
            resourceService.create(params);
        });
    }
}
