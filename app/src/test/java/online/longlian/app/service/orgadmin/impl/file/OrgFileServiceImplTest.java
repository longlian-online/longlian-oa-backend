package online.longlian.app.service.orgadmin.impl.file;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.ItemMapper;
import online.longlian.app.mapper.ItemTaskNodeMapper;
import online.longlian.app.mapper.ProjectMapper;
import online.longlian.app.mapper.ProjectTypeMapper;
import online.longlian.app.mapper.TaskInstanceMapper;
import online.longlian.app.mapper.TaskSubmissionMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.common.ActivatedResource;
import online.longlian.app.pojo.bo.common.ActivatedResourceRead;
import online.longlian.app.pojo.bo.orgadmin.OrgFileDiscardParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgFileBatchDownloadParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgFileBatchDownloadResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgFileBatchTargetParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgFileDownloadParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgFileEntriesResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgFileListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgFileTypeItemBO;
import online.longlian.app.pojo.entity.Item;
import online.longlian.app.pojo.entity.ItemTaskNode;
import online.longlian.app.pojo.entity.Project;
import online.longlian.app.pojo.entity.ProjectType;
import online.longlian.app.pojo.entity.TaskInstance;
import online.longlian.app.pojo.entity.TaskSubmission;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.service.orgadmin.OrgFileService;
import online.longlian.app.service.common.TaskFormService;
import online.longlian.app.service.resource.ResourceDeprecationService;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.common.enumeration.OrgFileEntryKind;
import online.longlian.common.enumeration.OrgFileView;
import online.longlian.common.enumeration.OrgFileBatchTargetKind;
import online.longlian.common.enumeration.StorageType;
import online.longlian.common.enumeration.TaskSubmissionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrgFileServiceImplTest {

    private static final String SCHEMA = """
            [{"key":"script","label":"译稿","type":"file","required":true,"options":[]},\
            {"key":"extra","label":"对照","type":"file","required":false,"options":[]},\
            {"key":"note","label":"备注","type":"text","required":false,"options":[]}]""";

    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private ProjectTypeMapper projectTypeMapper;
    @Mock
    private ItemMapper itemMapper;
    @Mock
    private ItemTaskNodeMapper itemTaskNodeMapper;
    @Mock
    private TaskInstanceMapper taskInstanceMapper;
    @Mock
    private TaskSubmissionMapper taskSubmissionMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private ResourceService resourceService;
    @Mock
    private ResourceDeprecationService resourceDeprecationService;
    @Mock
    private Clock clock;
    @Spy
    private TaskFormService taskFormService = new TaskFormService();
    @InjectMocks
    private OrgFileServiceImpl service;

    @BeforeEach
    void noDeprecatedFiles() {
        lenient().when(resourceDeprecationService.deprecatedIds(any())).thenReturn(java.util.Set.of());
    }

    @Test
    void shouldListProjectsFromTheDatabasePage() {
        Page<Project> page = new Page<>(1, OrgFileService.PAGE_SIZE);
        page.setTotal(2);
        page.setRecords(List.of(project(2L, "白企划"), project(1L, "阿企划")));
        when(projectMapper.selectPage(any(), any())).thenReturn(page);

        OrgFileEntriesResultBO result = service.list(OrgFileListParamsBO.builder()
                .orgId(1L).pageNum(1).pageSize(OrgFileService.PAGE_SIZE).build());

        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getEntries()).extracting("name").containsExactly("白企划", "阿企划");
        assertThat(result.getEntries()).allMatch(entry -> entry.getKind() == OrgFileEntryKind.FOLDER);
        assertThat(result.getProjectId()).isNull();
    }

    @Test
    void shouldFailWhenProjectIsOutsideOrganization() {
        when(projectMapper.selectById(9L)).thenReturn(Project.builder().id(9L).orgId(2L).title("别人的企划").build());

        assertThatThrownBy(() -> service.list(OrgFileListParamsBO.builder().orgId(1L).projectId(9L).build()))
                .isInstanceOf(AppException.class)
                .extracting("code")
                .isEqualTo(ResultCode.DATA_NOT_EXIT.getCode());
    }

    @Test
    void shouldListOnlyLatestSubmittedFilesAndMarkMissingResource() {
        when(itemMapper.selectById(3L)).thenReturn(Item.builder().id(3L).projectId(1L).title("第一话").build());
        when(projectMapper.selectById(1L)).thenReturn(project(1L, "阿企划"));
        when(itemTaskNodeMapper.selectList(any())).thenReturn(List.of(
                ItemTaskNode.builder().id(7L).itemId(3L).name("翻译").metaSchema(SCHEMA).sort(1).build()));
        when(taskInstanceMapper.selectList(any())).thenReturn(List.of(
                TaskInstance.builder().id(8L).itemId(3L).projectId(1L).itemTaskNodeId(7L).build()));
        when(taskSubmissionMapper.selectList(any())).thenReturn(List.of(
                submission(1L, 1, "{\"script\":{\"fileId\":\"100\"},\"note\":\"旧\"}"),
                submission(2L, 2, "{\"script\":{\"fileId\":\"101\"},\"extra\":{\"fileId\":\"202\"},\"note\":\"新\"}")));
        when(userMapper.selectList(any())).thenReturn(List.of(User.builder().id(4L).nickname("小林").build()));
        when(resourceService.findActivated(eq("task_submit"), eq(8L), eq(1L), eq(List.of(101L, 202L))))
                .thenReturn(Map.of(101L, resource(101L, "第一话译稿.pdf")));

        OrgFileEntriesResultBO result = service.list(OrgFileListParamsBO.builder()
                .orgId(1L).projectId(1L).itemId(3L).pageNum(1).pageSize(OrgFileService.PAGE_SIZE).build());

        assertThat(result.getProjectTitle()).isEqualTo("阿企划");
        assertThat(result.getItemTitle()).isEqualTo("第一话");
        assertThat(result.getEntries()).hasSize(2);
        assertThat(result.getEntries().get(0).getName()).isEqualTo("第一话译稿.pdf");
        assertThat(result.getEntries().get(0).getTaskName()).isEqualTo("翻译");
        assertThat(result.getEntries().get(0).getAvailability()).isEqualTo("available");
        assertThat(result.getEntries().get(0).getSubmitterName()).isEqualTo("小林");
        assertThat(result.getEntries().get(0).getFileSize()).isEqualTo(2048L);
        assertThat(result.getEntries().get(1).getName()).isEqualTo("对照");
        assertThat(result.getEntries().get(1).getAvailability()).isEqualTo("unavailable");
        assertThat(result.getEntries().get(1).getFileId()).isEqualTo(202L);
    }

    @Test
    void shouldSkipTaskWhenStoredSubmissionIsCorrupt() {
        when(itemMapper.selectById(3L)).thenReturn(Item.builder().id(3L).projectId(1L).title("第一话").build());
        when(projectMapper.selectById(1L)).thenReturn(project(1L, "阿企划"));
        when(itemTaskNodeMapper.selectList(any())).thenReturn(List.of(
                ItemTaskNode.builder().id(7L).name("翻译").metaSchema("not-json").build()));
        when(taskInstanceMapper.selectList(any())).thenReturn(List.of(
                TaskInstance.builder().id(8L).itemTaskNodeId(7L).build()));
        when(taskSubmissionMapper.selectList(any())).thenReturn(List.of(
                submission(2L, 2, "{\"script\":{\"fileId\":\"101\"}}")));

        OrgFileEntriesResultBO result = service.list(OrgFileListParamsBO.builder()
                .orgId(1L).itemId(3L).pageNum(1).pageSize(OrgFileService.PAGE_SIZE).build());

        assertThat(result.getEntries()).isEmpty();
    }

    @Test
    void shouldSignCurrentSubmissionFile() {
        TaskInstance instance = TaskInstance.builder().id(8L).itemId(3L).projectId(1L).itemTaskNodeId(7L).build();
        when(taskInstanceMapper.selectById(8L)).thenReturn(instance);
        when(itemMapper.selectById(3L)).thenReturn(Item.builder().id(3L).projectId(1L).title("第一话").build());
        when(projectMapper.selectById(1L)).thenReturn(project(1L, "阿企划"));
        when(itemTaskNodeMapper.selectById(7L)).thenReturn(
                ItemTaskNode.builder().id(7L).metaSchema(SCHEMA).build());
        when(taskSubmissionMapper.selectList(any())).thenReturn(List.of(
                submission(2L, 2, "{\"script\":{\"fileId\":\"101\"},\"note\":\"新\"}")));
        when(resourceService.findActivated("task_submit", 8L, 1L, List.of(101L)))
                .thenReturn(Map.of(101L, resource(101L, "第一话译稿.pdf")));
        when(resourceService.signDownload(any())).thenReturn(
                new ActivatedResourceRead(101L, "第一话译稿.pdf", 2048L, "application/pdf", "https://files/101", 50L));

        var result = service.download(OrgFileDownloadParamsBO.builder()
                .orgId(1L).taskInstanceId(8L).fileId(101L).build());

        assertThat(result.getReadUrl()).isEqualTo("https://files/101");
        assertThat(result.getExpireAt()).isEqualTo(50L);
    }

    @Test
    void shouldRemoveCurrentFileAndAskDeprecationToRespectOtherReferences() {
        when(taskInstanceMapper.selectById(8L)).thenReturn(
                TaskInstance.builder().id(8L).itemId(3L).projectId(1L).itemTaskNodeId(7L).build());
        when(itemMapper.selectById(3L)).thenReturn(Item.builder().id(3L).projectId(1L).build());
        when(projectMapper.selectById(1L)).thenReturn(project(1L, "阿企划"));
        when(itemTaskNodeMapper.selectById(7L)).thenReturn(
                ItemTaskNode.builder().id(7L).metaSchema(SCHEMA).build());
        when(taskSubmissionMapper.selectList(any())).thenReturn(List.of(
                submission(2L, 2, "{\"script\":{\"fileId\":\"101\"},\"note\":\"新\"}")));
        when(clock.instant()).thenReturn(Instant.parse("2026-11-07T02:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("Asia/Shanghai"));
        when(resourceDeprecationService.deprecateTaskSubmitFile(
                eq(101L), eq(8L), eq(1L), eq(LocalDateTime.of(2026, 11, 7, 10, 0)))).thenReturn(true);

        service.discard(OrgFileDiscardParamsBO.builder()
                .orgId(1L).taskInstanceId(8L).fileId(101L).fieldKey("script").build());

        verify(taskSubmissionMapper, never()).update(any(), any());
        verify(resourceDeprecationService).deprecateTaskSubmitFile(
                eq(101L), eq(8L), eq(1L), eq(LocalDateTime.of(2026, 11, 7, 10, 0)));
    }

    @Test
    void shouldHideDeprecatedFilesFromTheFolder() {
        when(itemMapper.selectById(3L)).thenReturn(Item.builder().id(3L).projectId(1L).title("第一话").build());
        when(projectMapper.selectById(1L)).thenReturn(project(1L, "阿企划"));
        when(itemTaskNodeMapper.selectList(any())).thenReturn(List.of(
                ItemTaskNode.builder().id(7L).itemId(3L).name("翻译").metaSchema(SCHEMA).sort(1).build()));
        when(taskInstanceMapper.selectList(any())).thenReturn(List.of(
                TaskInstance.builder().id(8L).itemId(3L).projectId(1L).itemTaskNodeId(7L).build()));
        when(taskSubmissionMapper.selectList(any())).thenReturn(List.of(
                submission(2L, 2, "{\"script\":{\"fileId\":\"101\"},\"note\":\"新\"}")));
        when(resourceDeprecationService.deprecatedIds(List.of(101L))).thenReturn(java.util.Set.of(101L));

        OrgFileEntriesResultBO result = service.list(OrgFileListParamsBO.builder()
                .orgId(1L).projectId(1L).itemId(3L).pageNum(1).pageSize(OrgFileService.PAGE_SIZE).build());

        assertThat(result.getEntries()).isEmpty();
    }

    @Test
    void shouldFailDiscardWhenFieldDoesNotHoldThatFile() {
        when(taskInstanceMapper.selectById(8L)).thenReturn(
                TaskInstance.builder().id(8L).itemId(3L).projectId(1L).itemTaskNodeId(7L).build());
        when(itemMapper.selectById(3L)).thenReturn(Item.builder().id(3L).projectId(1L).build());
        when(projectMapper.selectById(1L)).thenReturn(project(1L, "阿企划"));
        when(itemTaskNodeMapper.selectById(7L)).thenReturn(
                ItemTaskNode.builder().id(7L).metaSchema(SCHEMA).build());
        when(taskSubmissionMapper.selectList(any())).thenReturn(List.of(
                submission(2L, 2, "{\"script\":{\"fileId\":\"101\"}}")));

        assertThatThrownBy(() -> service.discard(OrgFileDiscardParamsBO.builder()
                .orgId(1L).taskInstanceId(8L).fileId(101L).fieldKey("extra").build()))
                .isInstanceOf(AppException.class)
                .extracting("code")
                .isEqualTo(ResultCode.DATA_NOT_EXIT.getCode());
    }

    @Test
    void shouldFailDownloadWhenFileIsNotInCurrentSubmission() {
        when(taskInstanceMapper.selectById(8L)).thenReturn(
                TaskInstance.builder().id(8L).itemId(3L).projectId(1L).itemTaskNodeId(7L).build());
        when(itemMapper.selectById(3L)).thenReturn(Item.builder().id(3L).projectId(1L).build());
        when(projectMapper.selectById(1L)).thenReturn(project(1L, "阿企划"));
        when(itemTaskNodeMapper.selectById(7L)).thenReturn(
                ItemTaskNode.builder().id(7L).metaSchema(SCHEMA).build());
        when(taskSubmissionMapper.selectList(any())).thenReturn(List.of(
                submission(2L, 2, "{\"script\":{\"fileId\":\"101\"}}")));

        assertThatThrownBy(() -> service.download(OrgFileDownloadParamsBO.builder()
                .orgId(1L).taskInstanceId(8L).fileId(100L).build()))
                .isInstanceOf(AppException.class)
                .extracting("code")
                .isEqualTo(ResultCode.DATA_NOT_EXIT.getCode());
    }

    @Test
    void shouldReturnTheSecondPageOfFolders() {
        Page<Project> page = new Page<>(2, OrgFileService.PAGE_SIZE);
        page.setTotal(51);
        page.setRecords(List.of(project(51L, "051")));
        when(projectMapper.selectPage(any(), any())).thenReturn(page);

        OrgFileEntriesResultBO result = service.list(OrgFileListParamsBO.builder()
                .orgId(1L).pageNum(2).pageSize(OrgFileService.PAGE_SIZE).build());

        assertThat(result.getTotal()).isEqualTo(51);
        assertThat(result.getEntries()).extracting("name").containsExactly("051");
    }

    @Test
    void shouldListTypesFromTheDatabasePage() {
        Page<ProjectType> page = new Page<>(1, OrgFileService.PAGE_SIZE);
        page.setTotal(1);
        page.setRecords(List.of(ProjectType.builder().id(2L).orgId(1L).name("漫画").priority(5).build()));
        when(projectTypeMapper.selectPage(any(), any())).thenReturn(page);

        OrgFileEntriesResultBO result = service.list(OrgFileListParamsBO.builder()
                .orgId(1L).view(OrgFileView.type).pageNum(1).pageSize(OrgFileService.PAGE_SIZE).build());

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getTypeId()).isNull();
        assertThat(result.getEntries()).extracting("name").containsExactly("漫画");
        assertThat(result.getEntries()).allMatch(entry -> entry.getKind() == OrgFileEntryKind.FOLDER);
    }

    @Test
    void shouldRejectTypeOutsideOrganization() {
        when(projectTypeMapper.selectById(4L))
                .thenReturn(ProjectType.builder().id(4L).orgId(2L).name("外组织").build());

        assertThatThrownBy(() -> service.list(OrgFileListParamsBO.builder()
                .orgId(1L).view(OrgFileView.type).typeId(4L).pageNum(1).pageSize(OrgFileService.PAGE_SIZE).build()))
                .isInstanceOf(AppException.class)
                .extracting("code")
                .isEqualTo(ResultCode.DATA_NOT_EXIT.getCode());
    }

    @Test
    void shouldListTypeFilesWithoutNestedFolders() {
        when(projectTypeMapper.selectById(2L)).thenReturn(ProjectType.builder().id(2L).orgId(1L).name("漫画").build());
        when(itemMapper.selectByProjectType(eq(1L), eq(2L), anyLong(), anyInt()))
                .thenReturn(List.of(new OrgFileTypeItemBO(3L, "第一话", 1L, "阿企划")))
                .thenReturn(List.of());
        when(itemTaskNodeMapper.selectList(any())).thenReturn(List.of(
                ItemTaskNode.builder().id(7L).itemId(3L).name("翻译").metaSchema(SCHEMA).sort(1).build()));
        when(taskInstanceMapper.selectList(any())).thenReturn(List.of(
                TaskInstance.builder().id(8L).itemId(3L).projectId(1L).itemTaskNodeId(7L).build()));
        when(taskSubmissionMapper.selectList(any())).thenReturn(List.of(
                submission(2L, 2, "{\"script\":{\"fileId\":\"101\"}}")));
        when(userMapper.selectList(any())).thenReturn(List.of(User.builder().id(4L).nickname("小林").build()));
        when(resourceService.findActivated(eq("task_submit"), eq(8L), eq(1L), eq(List.of(101L))))
                .thenReturn(Map.of(101L, resource(101L, "第一话译稿.pdf")));

        OrgFileEntriesResultBO result = service.list(OrgFileListParamsBO.builder()
                .orgId(1L).view(OrgFileView.type).typeId(2L).projectId(9L)
                .pageNum(1).pageSize(OrgFileService.PAGE_SIZE).build());

        assertThat(result.getTypeName()).isEqualTo("漫画");
        assertThat(result.getProjectId()).isNull();
        assertThat(result.getEntries()).allMatch(entry -> entry.getKind() == OrgFileEntryKind.FILE);
        assertThat(result.getEntries()).extracting("name").containsExactly("第一话译稿.pdf");
        assertThat(result.getEntries()).extracting("subtitle").containsExactly("阿企划 · 第一话");
    }

    @Test
    void shouldLoadOnlyTheRequestedPageOfTypeFiles() {
        when(projectTypeMapper.selectById(2L)).thenReturn(ProjectType.builder().id(2L).orgId(1L).name("漫画").build());
        when(itemMapper.selectByProjectType(eq(1L), eq(2L), anyLong(), anyInt()))
                .thenReturn(List.of(
                        new OrgFileTypeItemBO(3L, "第一话", 1L, "阿企划"),
                        new OrgFileTypeItemBO(4L, "第二话", 1L, "阿企划")))
                .thenReturn(List.of());
        when(itemTaskNodeMapper.selectList(any())).thenReturn(List.of(
                ItemTaskNode.builder().id(7L).itemId(3L).name("翻译").metaSchema(SCHEMA).sort(1).build(),
                ItemTaskNode.builder().id(8L).itemId(4L).name("校对").metaSchema(SCHEMA).sort(1).build()));
        when(taskInstanceMapper.selectList(any())).thenReturn(List.of(
                TaskInstance.builder().id(8L).itemId(3L).projectId(1L).itemTaskNodeId(7L).build(),
                TaskInstance.builder().id(9L).itemId(4L).projectId(1L).itemTaskNodeId(8L).build()));
        when(taskSubmissionMapper.selectList(any())).thenReturn(List.of(
                submission(2L, 2, "{\"script\":{\"fileId\":\"101\"}}"),
                TaskSubmission.builder().id(3L).taskInstanceId(9L).submitterId(4L).versionNo(1)
                        .metadata("{\"script\":{\"fileId\":\"202\"}}").status(TaskSubmissionStatus.SUBMITTED)
                        .createdAt(LocalDateTime.of(2026, 11, 7, 11, 0)).build()));
        when(userMapper.selectList(any())).thenReturn(List.of(User.builder().id(4L).nickname("小林").build()));
        when(resourceService.findActivated(eq("task_submit"), eq(9L), eq(1L), eq(List.of(202L))))
                .thenReturn(Map.of(202L, resource(202L, "第二话译稿.pdf")));

        OrgFileEntriesResultBO result = service.list(OrgFileListParamsBO.builder()
                .orgId(1L).view(OrgFileView.type).typeId(2L)
                .pageNum(2).pageSize(1).build());

        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getEntries()).extracting("name").containsExactly("第二话译稿.pdf");
        assertThat(result.getEntries()).extracting("subtitle").containsExactly("阿企划 · 第二话");
        verify(resourceService, never()).findActivated(eq("task_submit"), eq(8L), eq(1L), eq(List.of(101L)));
    }

    @Test
    void shouldListItemsAcrossPlanningsWithPlanningName() {
        Page<Item> page = new Page<>(1, OrgFileService.PAGE_SIZE);
        page.setTotal(1);
        page.setRecords(List.of(Item.builder().id(10L).projectId(2L).title("第一话").build()));
        when(itemMapper.selectPage(any(), any())).thenReturn(page);
        when(projectMapper.selectList(any())).thenReturn(List.of(project(2L, "阿企划")));

        OrgFileEntriesResultBO result = service.list(OrgFileListParamsBO.builder()
                .orgId(1L).view(OrgFileView.item).pageNum(1).pageSize(OrgFileService.PAGE_SIZE).build());

        assertThat(result.getProjectId()).isNull();
        assertThat(result.getEntries()).extracting("name").containsExactly("第一话");
        assertThat(result.getEntries()).extracting("subtitle").containsExactly("阿企划");
    }

    @Test
    void shouldSignCurrentFileOnceWhenSelectedWithItsProject() {
        TaskInstance instance = TaskInstance.builder().id(8L).itemId(3L).projectId(1L).itemTaskNodeId(7L).build();
        when(projectMapper.selectById(1L)).thenReturn(project(1L, "阿企划"));
        when(itemMapper.selectList(any())).thenReturn(List.of(
                Item.builder().id(3L).projectId(1L).title("第一话").sort(1L).build(),
                Item.builder().id(4L).projectId(1L).title("第二话").sort(2L).build()));
        when(itemMapper.selectById(3L)).thenReturn(Item.builder().id(3L).projectId(1L).title("第一话").build());
        when(itemTaskNodeMapper.selectList(any())).thenReturn(List.of(
                ItemTaskNode.builder().id(7L).itemId(3L).name("翻译").metaSchema(SCHEMA).sort(1).build(),
                ItemTaskNode.builder().id(8L).itemId(4L).name("校对").metaSchema(SCHEMA).sort(1).build()));
        when(itemTaskNodeMapper.selectById(7L)).thenReturn(
                ItemTaskNode.builder().id(7L).itemId(3L).metaSchema(SCHEMA).build());
        when(taskInstanceMapper.selectList(any())).thenReturn(List.of(
                instance,
                TaskInstance.builder().id(9L).itemId(4L).projectId(1L).itemTaskNodeId(8L).build()));
        when(taskInstanceMapper.selectById(8L)).thenReturn(instance);
        when(taskSubmissionMapper.selectList(any())).thenReturn(List.of(
                submission(2L, 2, "{\"script\":{\"fileId\":\"101\"}}"),
                TaskSubmission.builder().id(3L).taskInstanceId(9L).submitterId(4L).versionNo(1)
                        .metadata("{\"script\":{\"fileId\":\"202\"}}").status(TaskSubmissionStatus.SUBMITTED)
                        .createdAt(LocalDateTime.of(2026, 11, 7, 11, 0)).build()));
        when(userMapper.selectList(any())).thenReturn(List.of());
        when(resourceService.findActivated(eq("task_submit"), eq(8L), eq(1L), eq(List.of(101L))))
                .thenReturn(Map.of(101L, resource(101L, "第一话译稿.pdf")));
        when(resourceService.findActivated(eq("task_submit"), eq(9L), eq(1L), eq(List.of(202L))))
                .thenReturn(Map.of());
        when(resourceService.signDownload(any())).thenReturn(
                new ActivatedResourceRead(101L, "第一话译稿.pdf", 2048L, "application/pdf", "https://files/101", 50L));

        OrgFileBatchDownloadResultBO result = service.downloadBatch(OrgFileBatchDownloadParamsBO.builder()
                .orgId(1L)
                .targets(List.of(
                        OrgFileBatchTargetParamsBO.builder().kind(OrgFileBatchTargetKind.PROJECT).projectId(1L).build(),
                        OrgFileBatchTargetParamsBO.builder()
                                .kind(OrgFileBatchTargetKind.FILE)
                                .taskInstanceId(8L)
                                .fileId(101L)
                                .fieldKey("script")
                                .build()))
                .build());

        assertThat(result.getFiles()).hasSize(1);
        assertThat(result.getFiles().get(0).getFileName()).isEqualTo("第一话译稿.pdf");
        assertThat(result.getFiles().get(0).getFieldKey()).isEqualTo("script");
        assertThat(result.getFiles().get(0).getReadUrl()).isEqualTo("https://files/101");
        assertThat(result.getFiles().get(0).getExpireAt()).isEqualTo(50L);
        verify(resourceService, times(1)).signDownload(any());
    }

    @Test
    void shouldSkipUnavailableFileAndRejectOneOutsideTheCurrentSubmission() {
        TaskInstance instance = TaskInstance.builder().id(8L).itemId(3L).projectId(1L).itemTaskNodeId(7L).build();
        when(taskInstanceMapper.selectById(8L)).thenReturn(instance);
        when(itemMapper.selectById(3L)).thenReturn(Item.builder().id(3L).projectId(1L).build());
        when(projectMapper.selectById(1L)).thenReturn(project(1L, "阿企划"));
        when(itemTaskNodeMapper.selectById(7L)).thenReturn(
                ItemTaskNode.builder().id(7L).metaSchema(SCHEMA).build());
        when(taskSubmissionMapper.selectList(any())).thenReturn(List.of(
                submission(2L, 2, "{\"script\":{\"fileId\":\"101\"}}")));
        when(resourceService.findActivated(eq("task_submit"), eq(8L), eq(1L), eq(List.of(101L))))
                .thenReturn(Map.of());

        OrgFileBatchDownloadResultBO skipped = service.downloadBatch(OrgFileBatchDownloadParamsBO.builder()
                .orgId(1L)
                .targets(List.of(OrgFileBatchTargetParamsBO.builder()
                        .kind(OrgFileBatchTargetKind.FILE)
                        .taskInstanceId(8L)
                        .fileId(101L)
                        .fieldKey("script")
                        .build()))
                .build());

        assertThat(skipped.getFiles()).isEmpty();
        assertThatThrownBy(() -> service.downloadBatch(OrgFileBatchDownloadParamsBO.builder()
                .orgId(1L)
                .targets(List.of(OrgFileBatchTargetParamsBO.builder()
                        .kind(OrgFileBatchTargetKind.FILE)
                        .taskInstanceId(8L)
                        .fileId(100L)
                        .fieldKey("script")
                        .build()))
                .build()))
                .isInstanceOf(AppException.class)
                .extracting("code")
                .isEqualTo(ResultCode.DATA_NOT_EXIT.getCode());
        verify(resourceService, never()).signDownload(any());
    }

    @Test
    void shouldRejectAnEmptyOrOversizedBatch() {
        assertThatThrownBy(() -> service.downloadBatch(OrgFileBatchDownloadParamsBO.builder()
                .orgId(1L).targets(List.of()).build()))
                .isInstanceOf(AppException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());

        List<OrgFileBatchTargetParamsBO> targets = new java.util.ArrayList<>();
        for (int index = 0; index < 101; index++) {
            targets.add(OrgFileBatchTargetParamsBO.builder().kind(OrgFileBatchTargetKind.ITEM).itemId(1L).build());
        }
        assertThatThrownBy(() -> service.downloadBatch(OrgFileBatchDownloadParamsBO.builder()
                .orgId(1L).targets(targets).build()))
                .isInstanceOf(AppException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    private Project project(Long id, String title) {
        return Project.builder().id(id).orgId(1L).title(title).build();
    }

    private TaskSubmission submission(Long id, int version, String metadata) {
        return TaskSubmission.builder()
                .id(id)
                .taskInstanceId(8L)
                .submitterId(4L)
                .versionNo(version)
                .metadata(metadata)
                .status(TaskSubmissionStatus.SUBMITTED)
                .createdAt(LocalDateTime.of(2026, 11, 7, 10, version, 0))
                .build();
    }

    private ActivatedResource resource(Long id, String name) {
        return new ActivatedResource(id, name, 2048L, "application/pdf", StorageType.COS, "task_submit/" + id + ".pdf", 1L);
    }
}
