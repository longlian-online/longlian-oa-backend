package online.longlian.app.service.resource;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.properties.StorageProperties;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.ResourceMapper;
import online.longlian.app.pojo.bo.common.ResourceBindParamsBO;
import online.longlian.app.pojo.entity.Resource;
import online.longlian.app.pojo.vo.app.TaskAttachmentVO;
import online.longlian.app.service.app.impl.taskinstance.TaskAttachmentPresenter;
import online.longlian.common.enumeration.FileProcessStatus;
import online.longlian.common.enumeration.StorageType;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskResourceServiceTest {
    private static final long TASK_ID = 91L;
    private static final long ORG_ID = 12L;
    private static final long CREATOR_ID = 23L;

    @Mock
    private ResourceMapper resourceMapper;
    @Mock
    private StorageServiceFactory storageFactory;
    @Mock
    private StorageService storageService;

    private StorageProperties properties;
    private ResourceService service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Resource.class);
        properties = new StorageProperties();
        StorageProperties.CdnConfig cdn = new StorageProperties.CdnConfig();
        cdn.setEnabled(true);
        cdn.setUrlPrefix("https://cdn.example");
        cdn.setAuthKey("test-secret");
        properties.setCdn(cdn);
        service = serviceAt(1_000L);
    }


    @Test
    void probeFailureDoesNotChangePendingResource() {
        Resource pending = uploaded(1L);
        pending.setProcessStatus(FileProcessStatus.Pending);
        when(resourceMapper.selectList(any())).thenReturn(List.of(pending));
        when(storageFactory.get(StorageType.OSS)).thenReturn(storageService);
        doThrow(new AppException(ResultCode.OPERATION_FAIL)).when(storageService).probe(any());

        assertThatThrownBy(() -> bind(List.of(1L)))
                .isInstanceOf(AppException.class);
        verify(resourceMapper, never()).update(isNull(), any());
    }

    @Test
    void failedPendingCompareAndSetDoesNotActivate() {
        Resource pending = uploaded(1L);
        pending.setProcessStatus(FileProcessStatus.Pending);
        when(resourceMapper.selectList(any())).thenReturn(List.of(pending));
        when(storageFactory.get(StorageType.OSS)).thenReturn(storageService);
        when(resourceMapper.update(isNull(), any())).thenReturn(0);

        assertThatThrownBy(() -> bind(List.of(1L)))
                .isInstanceOf(AppException.class);
        verify(resourceMapper).update(isNull(), any());
    }

    @Test
    void failedActivationCompareAndSetIsRejected() {
        when(resourceMapper.selectList(any())).thenReturn(List.of(uploaded(1L)));
        when(resourceMapper.update(isNull(), any())).thenReturn(0);

        assertThatThrownBy(() -> bind(List.of(1L)))
                .isInstanceOf(AppException.class);
    }


    @Test
    void allResourcesAreValidatedBeforeAnyUploadTransition() {
        List<Resource> invalidResources = new ArrayList<>();
        Resource foreignOrg = uploaded(2L);
        foreignOrg.setOrgId(99L);
        invalidResources.add(foreignOrg);
        Resource foreignCreator = uploaded(2L);
        foreignCreator.setCreatorId(99L);
        invalidResources.add(foreignCreator);
        Resource avatar = uploaded(2L);
        avatar.setBizType("avatar");
        invalidResources.add(avatar);
        Resource deprecated = uploaded(2L);
        deprecated.setProcessStatus(FileProcessStatus.Deprecated);
        invalidResources.add(deprecated);
        Resource uploadedBound = uploaded(2L);
        uploadedBound.setBizId(TASK_ID);
        invalidResources.add(uploadedBound);
        Resource crossTask = activated(2L);
        crossTask.setBizId(99L);
        invalidResources.add(crossTask);
        Resource activeUnbound = activated(2L);
        activeUnbound.setBizId(0L);
        invalidResources.add(activeUnbound);
        Resource pendingBound = uploaded(2L);
        pendingBound.setProcessStatus(FileProcessStatus.Pending);
        pendingBound.setBizId(99L);
        invalidResources.add(pendingBound);
        Resource activeForeignCreator = activated(2L);
        activeForeignCreator.setCreatorId(99L);
        invalidResources.add(activeForeignCreator);

        Resource validPending = uploaded(1L);
        validPending.setProcessStatus(FileProcessStatus.Pending);
        for (Resource invalid : invalidResources) {
            when(resourceMapper.selectList(any())).thenReturn(List.of(validPending, invalid));
            assertThatThrownBy(() -> bind(List.of(1L, 2L)))
                    .isInstanceOf(AppException.class).hasMessageContaining("无权使用该文件");
        }
        when(resourceMapper.selectList(any())).thenReturn(List.of(validPending));
        assertThatThrownBy(() -> bind(List.of(1L, 2L)))
                .isInstanceOf(AppException.class);
        verify(resourceMapper, never()).update(isNull(), any());
        verifyNoInteractions(storageFactory);
    }

    @Test
    void readsBatchDatabaseMetadataWithRealCdnReuseExpiry() {
        Resource pdf = activated(1L);
        pdf.setFileName("database-name.pdf");
        Resource image = activated(2L);
        image.setFileMime("image/png");
        image.setFileSize(512L);
        Resource archive = activated(3L);
        archive.setFileMime("application/zip");
        archive.setFileSize(1024L * 1024);
        Resource other = activated(4L);
        other.setFileMime("application/octet-stream");
        other.setFileSize(1024L * 1024 * 1024);
        when(resourceMapper.selectList(any())).thenReturn(List.of(pdf, image, archive, other));

        Map<Long, TaskAttachmentVO> first = present(service, List.of(1L, 2L, 3L, 4L));
        TaskAttachmentVO attachment = first.get(1L);
        assertThat(attachment.getId()).isEqualTo("1");
        assertThat(attachment.getName()).isEqualTo("database-name.pdf");
        assertThat(attachment.getSizeText()).isEqualTo("2.0 KB");
        assertThat(attachment.getMediaType()).isEqualTo("document");
        assertThat(attachment.getAvailability()).isEqualTo("available");
        assertThat(attachment.getReadUrl()).matches("https://cdn.example/task_submit/1.pdf\\?token=[0-9a-f]{32}&t=960");
        assertThat(attachment.getExpiresAt()).isEqualTo(1260L);
        assertThat(first.get(2L).getMediaType()).isEqualTo("image");
        assertThat(first.get(2L).getSizeText()).isEqualTo("512 B");
        assertThat(first.get(3L).getMediaType()).isEqualTo("archive");
        assertThat(first.get(3L).getSizeText()).isEqualTo("1.0 MB");
        assertThat(first.get(4L).getMediaType()).isEqualTo("other");
        assertThat(first.get(4L).getSizeText()).isEqualTo("1.0 GB");
        verify(resourceMapper).selectList(any());
        verify(resourceMapper, never()).selectOne(any());
        verifyNoInteractions(storageFactory);

        TaskAttachmentVO reused = present(serviceAt(1_100L), List.of(1L)).get(1L);
        TaskAttachmentVO refreshed = present(serviceAt(1_200L), List.of(1L)).get(1L);
        assertThat(reused.getReadUrl()).isEqualTo(attachment.getReadUrl());
        assertThat(reused.getExpiresAt()).isEqualTo(1260L);
        assertThat(refreshed.getReadUrl()).isNotEqualTo(attachment.getReadUrl()).endsWith("&t=1200");
        assertThat(refreshed.getExpiresAt()).isEqualTo(1500L);
    }

    @Test
    void localTaskAttachmentRetainsOriginProofAndMatchingExpiry() {
        Resource local = activated(1L);
        local.setStorageType(StorageType.LOCAL);
        when(resourceMapper.selectList(any())).thenReturn(List.of(local));

        TaskAttachmentVO attachment = present(service, List.of(1L)).get(1L);

        assertThat(attachment.getReadUrl()).matches("https://cdn.example/common/file/local\\?key=task_submit/1.pdf"
                + "&expires=1260&signature=[0-9a-f]{64}&token=[0-9a-f]{32}&t=960");
        assertThat(attachment.getExpiresAt()).isEqualTo(1260L);
        verifyNoInteractions(storageFactory);
    }

    @Test
    void unavailableRowsAreNeverSignedAndDoNotLeakForeignNames() {
        Resource deprecated = activated(1L);
        deprecated.setProcessStatus(FileProcessStatus.Deprecated);
        Resource pending = activated(2L);
        pending.setProcessStatus(FileProcessStatus.Pending);
        Resource uploaded = activated(3L);
        uploaded.setProcessStatus(FileProcessStatus.Uploaded);
        Resource foreignOrg = activated(4L);
        foreignOrg.setOrgId(99L);
        Resource avatar = activated(5L);
        avatar.setBizType("avatar");
        Resource unbound = activated(6L);
        unbound.setBizId(0L);
        Resource crossTask = activated(7L);
        crossTask.setBizId(99L);
        when(resourceMapper.selectList(any())).thenReturn(List.of(
                deprecated, pending, uploaded, foreignOrg, avatar, unbound, crossTask));
        properties.setCdn(null);
        service = serviceAt(1_000L);

        Map<Long, TaskAttachmentVO> attachments = present(service,
                List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L));

        assertThat(attachments).hasSize(8);
        for (TaskAttachmentVO attachment : attachments.values()) {
            assertThat(attachment.getAvailability()).isEqualTo("unavailable");
            assertThat(attachment.getReadUrl()).isNull();
            assertThat(attachment.getExpiresAt()).isNull();
            assertThat(attachment.getSizeText()).isNull();
            assertThat(attachment.getMediaType()).isNull();
        }
        for (long id : List.of(1L, 4L, 5L, 6L, 7L, 8L)) {
            assertThat(attachments.get(id).getName()).isEqualTo("附件不可用");
        }
        verify(resourceMapper).selectList(any());
        verifyNoInteractions(storageFactory);
    }

    @Test
    void eligibleTaskAttachmentRequiresCdnWithoutNativeFallback() {
        when(resourceMapper.selectList(any())).thenReturn(List.of(activated(1L)));
        properties.getCdn().setEnabled(false);

        assertThatThrownBy(() -> present(service, List.of(1L)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("必须启用 CDN");
        properties.setCdn(null);
        service = serviceAt(1_000L);
        assertThatThrownBy(() -> present(service, List.of(1L)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("必须启用 CDN");
        verifyNoInteractions(storageFactory);
    }

    @Test
    void invalidCdnReuseConfigurationIsAnErrorNotUnavailable() {
        when(resourceMapper.selectList(any())).thenReturn(List.of(activated(1L)));
        properties.getCdn().setUrlReusePercent(100);

        assertThatThrownBy(() -> present(service, List.of(1L)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("配置无效");
        verifyNoInteractions(storageFactory);
    }

    @Test
    void signerConfigurationFailureIsNotMaskedAsUnavailable() {
        when(resourceMapper.selectList(any())).thenReturn(List.of(activated(1L)));
        properties.getCdn().setAuthKey("");
        service = serviceAt(1_000L);

        assertThatThrownBy(() -> present(service, List.of(1L)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("鉴权密钥必须配置");
        properties.getCdn().setAuthKey("test-secret");
        properties.getCdn().setUrlPrefix("https://cdn.example?query=invalid");
        service = serviceAt(1_000L);
        assertThatThrownBy(() -> present(service, List.of(1L)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("绝对 URL");
        verifyNoInteractions(storageFactory);
    }

    private void bind(List<Long> resourceIds) {
        service.bindBizResource(ResourceBindParamsBO.builder()
                .resourceIds(resourceIds)
                .bizType("task_submit")
                .bizId(TASK_ID)
                .orgId(ORG_ID)
                .creatorId(CREATOR_ID)
                .reuseBound(true)
                .build());
    }

    private Map<Long, TaskAttachmentVO> present(ResourceService resourceService, List<Long> resourceIds) {
        return new TaskAttachmentPresenter(resourceService).present(TASK_ID, ORG_ID, resourceIds);
    }

    private Resource uploaded(Long id) {
        return Resource.builder().id(id).orgId(ORG_ID).creatorId(CREATOR_ID)
                .bizType("task_submit").bizId(0L).processStatus(FileProcessStatus.Uploaded)
                .storageType(StorageType.OSS).storageKey("task_submit/" + id + ".pdf")
                .fileName(id + ".pdf").fileExt("pdf").fileSize(2048L).fileMime("application/pdf").build();
    }

    private Resource activated(Long id) {
        Resource resource = uploaded(id);
        resource.setBizId(TASK_ID);
        resource.setProcessStatus(FileProcessStatus.Activated);
        return resource;
    }

    private ResourceService serviceAt(long epochSeconds) {
        Clock clock = Clock.fixed(Instant.ofEpochSecond(epochSeconds), ZoneOffset.UTC);
        return new ResourceService(resourceMapper, storageFactory, new CdnUrlSigner(properties, clock),
                new LocalFileUrlSigner("test-local-signing-secret-32-bytes", properties, clock), properties, clock);
    }

}
