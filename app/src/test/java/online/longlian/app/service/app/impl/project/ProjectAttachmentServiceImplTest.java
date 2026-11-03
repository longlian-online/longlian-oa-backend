package online.longlian.app.service.app.impl.project;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.ProjectAttachmentMapper;
import online.longlian.app.pojo.bo.app.ProjectAccessResultBO;
import online.longlian.app.pojo.bo.app.ProjectAttachmentCreateParamsBO;
import online.longlian.app.pojo.bo.app.ProjectAttachmentDeleteParamsBO;
import online.longlian.app.pojo.bo.app.ProjectAttachmentDownloadParamsBO;
import online.longlian.app.pojo.bo.app.ProjectAttachmentDownloadResultBO;
import online.longlian.app.pojo.bo.app.ProjectAttachmentListParamsBO;
import online.longlian.app.pojo.bo.app.ProjectAttachmentResultBO;
import online.longlian.app.pojo.bo.common.ActivatedResource;
import online.longlian.app.pojo.bo.common.ActivatedResourceRead;
import online.longlian.app.pojo.bo.common.ResourceBindParamsBO;
import online.longlian.app.pojo.entity.ProjectAttachment;
import online.longlian.app.service.app.ProjectService;
import online.longlian.app.service.app.UserService;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.common.enumeration.StorageType;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectAttachmentServiceImplTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-01T02:00:00Z"), ZoneOffset.UTC);

    @Mock
    private ProjectAttachmentMapper projectAttachmentMapper;
    @Mock
    private ProjectService projectService;
    @Mock
    private ResourceService resourceService;
    @Mock
    private UserService userService;
    @Mock
    private ProjectAttachmentHandler projectAttachmentHandler;

    private ProjectAttachmentServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), ProjectAttachment.class);
        service = new ProjectAttachmentServiceImpl(
                projectAttachmentMapper, projectService, resourceService, userService,
                projectAttachmentHandler, CLOCK);
    }

    @Test
    void getProjectAttachments_returnsAuthorizedMetadataWithoutSigning() {
        when(projectService.requireVisibleProject(any())).thenReturn(new ProjectAccessResultBO(2L));
        when(projectAttachmentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                attachment(200L, 20L, 2L),
                attachment(100L, 10L, 3L)));
        when(resourceService.findActivated("project_attachment", 100L, 1L, List.of(20L, 10L)))
                .thenReturn(Map.of(
                        20L, activated(20L, "企划设定.pdf"),
                        10L, activated(10L, "参考图.png")));
        when(userService.displayNames(List.of(2L, 3L))).thenReturn(Map.of(2L, "张三", 3L, "李四"));

        List<ProjectAttachmentResultBO> result = service.getProjectAttachments(listParams());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(200L);
        assertThat(result.get(0).getFileId()).isEqualTo(20L);
        assertThat(result.get(0).getFileName()).isEqualTo("企划设定.pdf");
        assertThat(result.get(0).getFileMime()).isEqualTo("application/pdf");
        assertThat(result.get(0).getUploaderId()).isEqualTo(2L);
        assertThat(result.get(0).getUploaderName()).isEqualTo("张三");
        assertThat(result.get(0).getCreatedAt()).isEqualTo(LocalDateTime.parse("2026-05-01T10:00:00"));
        verify(resourceService, never()).signDownload(any());
        verify(resourceService, never()).signActivated(any());
    }

    /** 已废弃文件不再出现在列表里，也不会补签任何链接。 */
    @Test
    void getProjectAttachments_filtersOutUnavailableResource() {
        when(projectService.requireVisibleProject(any())).thenReturn(new ProjectAccessResultBO(2L));
        when(projectAttachmentMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(attachment(200L, 20L, 2L), attachment(100L, 10L, 2L)));
        when(resourceService.findActivated("project_attachment", 100L, 1L, List.of(20L, 10L)))
                .thenReturn(Map.of(20L, activated(20L, "企划设定.pdf")));
        when(userService.displayNames(List.of(2L))).thenReturn(Map.of(2L, "张三"));

        List<ProjectAttachmentResultBO> result = service.getProjectAttachments(listParams());

        assertThat(result).extracting(ProjectAttachmentResultBO::getFileId).containsExactly(20L);
    }

    @Test
    void getProjectAttachments_empty_returnsEmptyWithoutResourceLookup() {
        when(projectService.requireVisibleProject(any())).thenReturn(new ProjectAccessResultBO(2L));
        when(projectAttachmentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        assertThat(service.getProjectAttachments(listParams())).isEmpty();
        verifyNoInteractions(resourceService, userService);
    }

    @Test
    void createProjectAttachment_bindsUploadedFileThenInsertsAssociation() {
        when(projectService.lockEnabledProject(any())).thenReturn(new ProjectAccessResultBO(2L));

        service.createProjectAttachment(ProjectAttachmentCreateParamsBO.builder()
                .projectId(100L).fileId(20L).userId(2L).orgId(1L).build());

        ArgumentCaptor<ResourceBindParamsBO> bind = ArgumentCaptor.forClass(ResourceBindParamsBO.class);
        verify(resourceService).bindBizResource(bind.capture());
        assertThat(bind.getValue().getResourceId()).isEqualTo(20L);
        assertThat(bind.getValue().getBizType()).isEqualTo("project_attachment");
        assertThat(bind.getValue().getBizId()).isEqualTo(100L);
        assertThat(bind.getValue().getCreatorId()).isEqualTo(2L);
        assertThat(bind.getValue().getOrgId()).isEqualTo(1L);

        ArgumentCaptor<ProjectAttachment> inserted = ArgumentCaptor.forClass(ProjectAttachment.class);
        verify(projectAttachmentMapper).insert(inserted.capture());
        assertThat(inserted.getValue().getProjectId()).isEqualTo(100L);
        assertThat(inserted.getValue().getFileId()).isEqualTo(20L);
        assertThat(inserted.getValue().getUploaderId()).isEqualTo(2L);
        assertThat(inserted.getValue().getCreatedAt()).isEqualTo(LocalDateTime.parse("2026-05-01T02:00:00"));
    }

    /** 只有企划负责人能编辑附件，非负责人在绑定前就被拒绝。 */
    @Test
    void createProjectAttachment_nonCreatorRejectedBeforeBinding() {
        when(projectService.lockEnabledProject(any())).thenReturn(new ProjectAccessResultBO(9L));

        assertThatThrownBy(() -> service.createProjectAttachment(ProjectAttachmentCreateParamsBO.builder()
                .projectId(100L).fileId(20L).userId(2L).orgId(1L).build()))
                .isInstanceOfSatisfying(AppException.class,
                        error -> assertThat(error.getCode()).isEqualTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));

        verify(resourceService, never()).bindBizResource(any());
        verify(projectAttachmentMapper, never()).insert(any(ProjectAttachment.class));
    }

    @Test
    void deleteProjectAttachment_nonCreatorRejected() {
        when(projectService.lockEnabledProject(any())).thenReturn(new ProjectAccessResultBO(9L));

        assertThatThrownBy(() -> service.deleteProjectAttachment(ProjectAttachmentDeleteParamsBO.builder()
                .projectId(100L).attachmentId(200L).userId(2L).orgId(1L).build()))
                .isInstanceOfSatisfying(AppException.class,
                        error -> assertThat(error.getCode()).isEqualTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));

        verify(projectAttachmentHandler, never()).abandonAttachment(any(), any());
    }

    @Test
    void deleteProjectAttachment_notFoundThrows() {
        when(projectService.lockEnabledProject(any())).thenReturn(new ProjectAccessResultBO(2L));
        when(projectAttachmentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> service.deleteProjectAttachment(ProjectAttachmentDeleteParamsBO.builder()
                .projectId(100L).attachmentId(200L).userId(2L).orgId(1L).build()))
                .isInstanceOfSatisfying(AppException.class,
                        error -> assertThat(error.getCode()).isEqualTo(ResultCode.DATA_NOT_EXIT.getCode()));

        verify(projectAttachmentHandler, never()).abandonAttachment(any(), any());
    }

    @Test
    void deleteProjectAttachment_creatorAbandonsAssociation() {
        ProjectAttachment attachment = attachment(200L, 20L, 2L);
        when(projectService.lockEnabledProject(any())).thenReturn(new ProjectAccessResultBO(2L));
        when(projectAttachmentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(attachment);

        service.deleteProjectAttachment(ProjectAttachmentDeleteParamsBO.builder()
                .projectId(100L).attachmentId(200L).userId(2L).orgId(1L).build());

        verify(projectAttachmentHandler).abandonAttachment(attachment, 1L);
    }

    @Test
    void getProjectAttachmentDownload_returnsFreshSignedUrl() {
        when(projectService.requireVisibleProject(any())).thenReturn(new ProjectAccessResultBO(2L));
        when(projectAttachmentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(attachment(200L, 20L, 2L));
        ActivatedResource resource = activated(20L, "企划设定.pdf");
        when(resourceService.findActivated("project_attachment", 100L, 1L, List.of(20L)))
                .thenReturn(Map.of(20L, resource));
        when(resourceService.signDownload(resource)).thenReturn(new ActivatedResourceRead(
                20L, "企划设定.pdf", 3L, "application/pdf",
                "/common/file/local?key=project_attachment/20.pdf&expires=1", 1_800L));

        ProjectAttachmentDownloadResultBO result = service.getProjectAttachmentDownload(
                ProjectAttachmentDownloadParamsBO.builder()
                        .projectId(100L).attachmentId(200L).orgId(1L).build());

        assertThat(result.getReadUrl()).isEqualTo("/common/file/local?key=project_attachment/20.pdf&expires=1");
        assertThat(result.getExpireAt()).isEqualTo(1_800L);
    }

    @Test
    void getProjectAttachmentDownload_missingAssociationThrows() {
        when(projectService.requireVisibleProject(any())).thenReturn(new ProjectAccessResultBO(2L));
        when(projectAttachmentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> service.getProjectAttachmentDownload(ProjectAttachmentDownloadParamsBO.builder()
                .projectId(100L).attachmentId(200L).orgId(1L).build()))
                .isInstanceOfSatisfying(AppException.class,
                        error -> assertThat(error.getCode()).isEqualTo(ResultCode.DATA_NOT_EXIT.getCode()));

        verify(resourceService, never()).signDownload(any());
    }

    @Test
    void getProjectAttachmentDownload_unavailableResourceThrows() {
        when(projectService.requireVisibleProject(any())).thenReturn(new ProjectAccessResultBO(2L));
        when(projectAttachmentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(attachment(200L, 20L, 2L));
        when(resourceService.findActivated("project_attachment", 100L, 1L, List.of(20L))).thenReturn(Map.of());

        assertThatThrownBy(() -> service.getProjectAttachmentDownload(ProjectAttachmentDownloadParamsBO.builder()
                .projectId(100L).attachmentId(200L).orgId(1L).build()))
                .isInstanceOfSatisfying(AppException.class,
                        error -> assertThat(error.getCode()).isEqualTo(ResultCode.DATA_NOT_EXIT.getCode()));

        verify(resourceService, never()).signDownload(any());
    }

    private ProjectAttachmentListParamsBO listParams() {
        return ProjectAttachmentListParamsBO.builder().projectId(100L).orgId(1L).build();
    }

    private ProjectAttachment attachment(Long id, Long fileId, Long uploaderId) {
        return ProjectAttachment.builder()
                .id(id)
                .projectId(100L)
                .fileId(fileId)
                .uploaderId(uploaderId)
                .createdAt(LocalDateTime.parse("2026-05-01T10:00:00"))
                .build();
    }

    private ActivatedResource activated(Long id, String fileName) {
        return new ActivatedResource(id, fileName, 3L, "application/pdf",
                StorageType.LOCAL, "project_attachment/" + id + ".pdf", 1L);
    }
}
