package online.longlian.app.service.app.impl.taskinstance;

import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.common.ResourceReadUrlGetResultBO;
import online.longlian.app.pojo.entity.ItemTaskNode;
import online.longlian.app.pojo.entity.TaskInstance;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.pojo.vo.app.ItemTaskInstanceVO;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.app.service.common.TaskFormService;
import online.longlian.app.pojo.bo.common.TaskFormField;
import online.longlian.app.pojo.entity.TaskSubmission;
import online.longlian.app.pojo.vo.app.TaskInstanceDetailVO;
import online.longlian.app.pojo.vo.app.TaskAttachmentVO;
import online.longlian.common.enumeration.TaskInstanceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskInstanceAssemblerTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private ResourceService resourceService;
    @Mock
    private TaskAttachmentPresenter taskAttachmentPresenter;

    private TaskInstanceAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new TaskInstanceAssembler(userMapper, resourceService, new TaskFormService(), taskAttachmentPresenter);
    }

    @Test
    void assembleInstances_emptyList_returnsEmptyList() {
        List<ItemTaskInstanceVO> result = assembler.assembleInstances(Collections.emptyList(), Map.of());

        assertTrue(result.isEmpty());
        verify(userMapper, never()).selectBatchIds(anyList());
        verify(resourceService, never()).getResourceReadUrls(anyList());
    }

    @Test
    void assembleInstances_instancesWithNoAssignee_skipsUserLookup() {
        TaskInstance instance = TaskInstance.builder()
                .id(1L)
                .status(TaskInstanceStatus.PENDING)
                .revision(0L)
                .createdAt(LocalDateTime.now())
                .build(); // assigneeId is null

        List<ItemTaskInstanceVO> result = assembler.assembleInstances(List.of(instance), Collections.emptyMap());

        assertEquals(1, result.size());
        assertEquals("0", result.get(0).getRevision());
        assertNull(result.get(0).getAssigneeId());
        verify(userMapper, never()).selectBatchIds(anyList());
    }

    @Test
    void assembleInstances_instanceWithAssigneeAndNode_populatesAllFields() {
        User assignee = User.builder()
                .id(10L)
                .nickname("alice")
                .avatarFileId(100L)
                .build();
        TaskInstance instance = TaskInstance.builder()
                .id(1L)
                .itemTaskNodeId(50L)
                .assigneeId(10L)
                .status(TaskInstanceStatus.CLAIMED)
                .revision(Long.MAX_VALUE)
                .createdAt(LocalDateTime.now())
                .completedAt(null)
                .build();
        ItemTaskNode node = ItemTaskNode.builder()
                .id(50L)
                .name("翻译")
                .sort(2)
                .parallelSort(1)
                .build();

        when(userMapper.selectBatchIds(List.of(10L))).thenReturn(List.of(assignee));
        when(resourceService.getResourceReadUrls(List.of(100L)))
                .thenReturn(Map.of(100L, new ResourceReadUrlGetResultBO("https://cdn/avatar.png", 1L, "key")));

        List<ItemTaskInstanceVO> result = assembler.assembleInstances(
                List.of(instance), Map.of(50L, node));

        assertEquals(1, result.size());
        ItemTaskInstanceVO vo = result.get(0);
        assertEquals(1L, vo.getId());
        assertEquals("9223372036854775807", vo.getRevision());
        assertEquals(10L, vo.getAssigneeId());
        assertEquals("alice", vo.getAssigneeNickname());
        assertEquals("https://cdn/avatar.png", vo.getAssigneeAvatarUrl());
        assertEquals("翻译", vo.getName());
        assertEquals(2, vo.getSort());
        assertEquals(1, vo.getParallelSort());
        assertEquals(TaskInstanceStatus.CLAIMED, vo.getStatus());
    }

    @Test
    void assembleInstances_instanceWithAssigneeButNoAvatar_nullAvatarUrl() {
        User assignee = User.builder()
                .id(10L)
                .nickname("bob")
                .avatarFileId(null)  // no avatar
                .build();
        TaskInstance instance = TaskInstance.builder()
                .id(2L)
                .assigneeId(10L)
                .status(TaskInstanceStatus.PENDING)
                .revision(0L)
                .build();

        when(userMapper.selectBatchIds(List.of(10L))).thenReturn(List.of(assignee));

        List<ItemTaskInstanceVO> result = assembler.assembleInstances(List.of(instance), Collections.emptyMap());

        assertEquals(1, result.size());
        assertEquals("bob", result.get(0).getAssigneeNickname());
        assertNull(result.get(0).getAssigneeAvatarUrl());
        // resourceService not called when no avatarFileIds
        verify(resourceService, never()).getResourceReadUrls(anyList());
    }

    @Test
    void assembleInstances_instanceWithNodeNotInMap_nodeFieldsAreNull() {
        TaskInstance instance = TaskInstance.builder()
                .id(3L)
                .itemTaskNodeId(999L) // not in map
                .status(TaskInstanceStatus.PENDING)
                .revision(0L)
                .build();

        List<ItemTaskInstanceVO> result = assembler.assembleInstances(List.of(instance), Map.of());

        assertEquals(1, result.size());
        assertNull(result.get(0).getName());
        assertNull(result.get(0).getSort());
    }

    @Test
    void shouldAssembleTaskWithoutSubmissionAndWithoutAttachmentLookup() {
        TaskInstance instance = TaskInstance.builder().id(1L).status(TaskInstanceStatus.PENDING).build();
        ItemTaskNode node = ItemTaskNode.builder().name("翻译").sort(2).metaSchema("[]").build();
        TaskInstanceDetailVO detail = assembler.assembleDetail(instance, node, null, 10L);
        assertEquals("1", detail.getTask().id());
        assertEquals("翻译", detail.getTask().name());
        assertEquals(2, detail.getTask().stage());
        assertEquals("PENDING", detail.getTask().status());
        assertEquals("not_submitted", detail.getSubmission().state());
        assertTrue(detail.getSubmission().fields().isEmpty());
        org.mockito.Mockito.verifyNoInteractions(resourceService, taskAttachmentPresenter);
    }

    @Test
    void shouldRenderSchemaOrderedScalarsZeroEmptyAndMultiline() {
        TaskFormService forms = new TaskFormService();
        List<TaskFormField> fields = List.of(new TaskFormField("count", "数量", "number", true, List.of()),
                new TaskFormField("notes", "备注", "textarea", false, List.of()),
                new TaskFormField("pick", "选择", "select", false, List.of("A")),
                new TaskFormField("empty", "空值", "text", false, List.of()),
                new TaskFormField("file", "附件", "file", false, List.of()));
        ItemTaskNode node = ItemTaskNode.builder().name("翻译").sort(2).metaSchema(forms.serializeFields(fields)).build();
        TaskInstance instance = TaskInstance.builder().id(1L).status(TaskInstanceStatus.COMPLETED).build();
        LocalDateTime submitted = LocalDateTime.of(2026, 10, 8, 12, 30);
        TaskSubmission submission = TaskSubmission.builder().createdAt(submitted)
                .metadata(forms.validateValues(fields, Map.of("notes", "first\n  second \n", "pick", "A", "count", "0.00")).serialize()).build();
        when(taskAttachmentPresenter.present(1L, 10L, List.of())).thenReturn(Map.of());

        TaskInstanceDetailVO detail = assembler.assembleDetail(instance, node, submission, 10L);

        assertEquals("submitted", detail.getSubmission().state());
        assertEquals(submitted, detail.getSubmission().submittedAt());
        assertEquals(List.of(new TaskInstanceDetailVO.TextField("count", "数量", "text", "0"),
                new TaskInstanceDetailVO.TextField("notes", "备注", "multiline", "first\n  second \n"),
                new TaskInstanceDetailVO.TextField("pick", "选择", "text", "A"),
                new TaskInstanceDetailVO.TextField("empty", "空值", "text", "未填写"),
                new TaskInstanceDetailVO.FileField("file", "附件", "file", null)), detail.getSubmission().fields());
    }

    @Test
    void shouldUseScopedBatchAttachmentsAndDatabaseNames() {
        TaskFormService forms = new TaskFormService();
        List<TaskFormField> fields = List.of(new TaskFormField("file", "附件", "file", true, List.of()));
        ItemTaskNode node = ItemTaskNode.builder().name("翻译").sort(1).metaSchema(forms.serializeFields(fields)).build();
        TaskInstance instance = TaskInstance.builder().id(1L).status(TaskInstanceStatus.COMPLETED).build();
        TaskSubmission submission = TaskSubmission.builder().metadata("{\"file\":{\"fileId\":\"9\"}}").build();
        TaskAttachmentVO attachment = new TaskAttachmentVO();
        attachment.setId("9");
        attachment.setName("database.pdf");
        attachment.setAvailability("unavailable");
        when(taskAttachmentPresenter.present(1L, 10L, List.of(9L))).thenReturn(Map.of(9L, attachment));

        TaskInstanceDetailVO detail = assembler.assembleDetail(instance, node, submission, 10L);

        assertEquals(List.of(new TaskInstanceDetailVO.FileField("file", "附件", "file", attachment)),
                detail.getSubmission().fields());
        verify(taskAttachmentPresenter).present(1L, 10L, List.of(9L));
        verify(resourceService, never()).getResourceReadUrls(anyList());
    }

    @Test
    void shouldAssembleVersionsWithStatusParticipantsAndFallbackOwnerlessNickname() {
        TaskFormService forms = new TaskFormService();
        List<TaskFormField> fields = List.of(new TaskFormField("notes", "说明", "text", false, List.of()),
                new TaskFormField("file", "附件", "file", false, List.of()));
        ItemTaskNode node = ItemTaskNode.builder().name("翻译").sort(1).metaSchema(forms.serializeFields(fields)).build();
        TaskInstance instance = TaskInstance.builder().id(1L).status(TaskInstanceStatus.COMPLETED).build();
        LocalDateTime rejectedAt = LocalDateTime.of(2026, 10, 8, 9, 0);
        TaskSubmission rejected = TaskSubmission.builder()
                .id(11L).versionNo(2).metadata("{\"notes\":\"v2\"}")
                .status(online.longlian.common.enumeration.TaskSubmissionStatus.REJECTED)
                .submitterId(5L).reviewerId(9L).reviewedAt(rejectedAt).reviewComment("再改改")
                .createdAt(LocalDateTime.of(2026, 10, 8, 8, 0))
                .build();
        TaskSubmission reset = TaskSubmission.builder()
                .id(10L).versionNo(1).metadata("{\"notes\":\"v1\"}")
                .status(online.longlian.common.enumeration.TaskSubmissionStatus.RESET)
                .submitterId(5L)
                .createdAt(LocalDateTime.of(2026, 10, 7, 8, 0))
                .build();
        User submitter = User.builder().id(5L).nickname("阿苏").build();
        User reviewer = User.builder().id(9L).nickname("小林").build();
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(submitter, reviewer));
        when(taskAttachmentPresenter.present(1L, 10L, List.of())).thenReturn(Map.of());

        List<online.longlian.app.pojo.vo.app.TaskSubmissionVersionVO> versions =
                assembler.assembleSubmissionVersions(instance, node, List.of(rejected, reset), 10L);

        assertEquals(2, versions.size());
        online.longlian.app.pojo.vo.app.TaskSubmissionVersionVO second = versions.get(0);
        assertEquals(2, second.getVersionNo());
        assertEquals("REJECTED", second.getStatus());
        assertEquals("阿苏", second.getSubmitter().nickname());
        assertEquals("小林", second.getReviewer().nickname());
        assertEquals(rejectedAt, second.getReviewedAt());
        assertEquals("再改改", second.getReviewComment());
        assertEquals(List.of(
                new TaskInstanceDetailVO.TextField("notes", "说明", "text", "v2"),
                new TaskInstanceDetailVO.FileField("file", "附件", "file", null)), second.getFields());
        online.longlian.app.pojo.vo.app.TaskSubmissionVersionVO first = versions.get(1);
        assertEquals(1, first.getVersionNo());
        assertEquals("RESET", first.getStatus());
        assertNull(first.getReviewer());
        assertFalse(first.isUnreadable());
        verify(userMapper).selectBatchIds(java.util.Set.of(5L, 9L));
        verify(resourceService, never()).getResourceReadUrls(anyList());
    }

    @Test
    void shouldIsolateCorruptedLegacyVersionWithoutBlockingOthers() {
        TaskFormService forms = new TaskFormService();
        List<TaskFormField> fields = List.of(new TaskFormField("notes", "说明", "text", false, List.of()));
        ItemTaskNode node = ItemTaskNode.builder().name("翻译").sort(1).metaSchema(forms.serializeFields(fields)).build();
        TaskInstance instance = TaskInstance.builder().id(1L).status(TaskInstanceStatus.COMPLETED).build();
        TaskSubmission broken = TaskSubmission.builder()
                .id(10L).versionNo(1).metadata("{")
                .status(online.longlian.common.enumeration.TaskSubmissionStatus.RESET)
                .submitterId(5L)
                .createdAt(LocalDateTime.of(2026, 10, 7, 8, 0))
                .build();
        TaskSubmission good = TaskSubmission.builder()
                .id(11L).versionNo(2).metadata("{\"notes\":\"v2\"}")
                .status(online.longlian.common.enumeration.TaskSubmissionStatus.SUBMITTED)
                .submitterId(5L)
                .createdAt(LocalDateTime.of(2026, 10, 8, 8, 0))
                .build();
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(User.builder().id(5L).nickname("阿苏").build()));
        when(taskAttachmentPresenter.present(1L, 10L, List.of())).thenReturn(Map.of());

        List<online.longlian.app.pojo.vo.app.TaskSubmissionVersionVO> versions =
                assembler.assembleSubmissionVersions(instance, node, List.of(good, broken), 10L);

        assertFalse(versions.get(0).isUnreadable());
        assertEquals("v2", ((TaskInstanceDetailVO.TextField) versions.get(0).getFields().get(0)).text());
        assertTrue(versions.get(1).isUnreadable());
        assertTrue(versions.get(1).getFields().isEmpty());
    }


    @Test
    void shouldResolveAvatarUrlsAndFallBackForDeletedParticipants() {
        TaskFormService forms = new TaskFormService();
        List<TaskFormField> fields = List.of(new TaskFormField("notes", "说明", "text", false, List.of()));
        ItemTaskNode node = ItemTaskNode.builder().name("翻译").sort(1).metaSchema(forms.serializeFields(fields)).build();
        TaskInstance instance = TaskInstance.builder().id(1L).status(TaskInstanceStatus.COMPLETED).build();
        TaskSubmission withAvatar = TaskSubmission.builder()
                .id(11L).versionNo(3).metadata("{\"notes\":\"v3\"}")
                .status(online.longlian.common.enumeration.TaskSubmissionStatus.SUBMITTED)
                .submitterId(5L)
                .createdAt(LocalDateTime.of(2026, 10, 9, 8, 0))
                .build();
        TaskSubmission ownerlessReviewer = TaskSubmission.builder()
                .id(10L).versionNo(2).metadata("{\"notes\":\"v2\"}")
                .status(online.longlian.common.enumeration.TaskSubmissionStatus.REJECTED)
                .submitterId(5L).reviewerId(999L)
                .reviewedAt(LocalDateTime.of(2026, 10, 8, 18, 0)).reviewComment(" ")
                .createdAt(LocalDateTime.of(2026, 10, 8, 8, 0))
                .build();
        TaskSubmission reviewerAbsent = TaskSubmission.builder()
                .id(12L).versionNo(1).metadata("{\"notes\":\"v1\"}")
                .status(online.longlian.common.enumeration.TaskSubmissionStatus.REJECTED)
                .submitterId(5L)
                .createdAt(LocalDateTime.of(2026, 10, 7, 8, 0))
                .build();
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(
                List.of(User.builder().id(5L).nickname("阿苏").avatarFileId(77L).build()));
        ResourceReadUrlGetResultBO signed = new ResourceReadUrlGetResultBO("https://cdn.test/77", 1L, "a/77.png");
        when(resourceService.getResourceReadUrls(List.of(77L))).thenReturn(Map.of(77L, signed));
        when(taskAttachmentPresenter.present(1L, 10L, List.of())).thenReturn(Map.of());

        List<online.longlian.app.pojo.vo.app.TaskSubmissionVersionVO> versions =
                assembler.assembleSubmissionVersions(
                        instance, node, List.of(withAvatar, ownerlessReviewer, reviewerAbsent), 10L);

        assertEquals("https://cdn.test/77", versions.get(0).getSubmitter().avatarUrl());
        assertEquals("阿苏", versions.get(0).getSubmitter().nickname());
        online.longlian.app.pojo.vo.app.TaskSubmissionVersionVO rejected = versions.get(1);
        assertEquals("999", rejected.getReviewer().id());
        assertNull(rejected.getReviewer().nickname());
        assertNull(rejected.getReviewer().avatarUrl());
        assertEquals(" ", rejected.getReviewComment());
        assertNull(versions.get(2).getReviewer());
        verify(resourceService).getResourceReadUrls(List.of(77L));
    }
}
