package online.longlian.app.service.app.impl;

import online.longlian.app.common.exception.AppException;
import online.longlian.app.pojo.bo.app.McpTaskQueryParamsBO;
import online.longlian.app.pojo.bo.app.ProjectAttachmentResultBO;
import online.longlian.app.pojo.bo.app.ProjectDetailResultBO;
import online.longlian.app.pojo.bo.common.TaskFormField;
import online.longlian.app.pojo.vo.app.ItemTaskFlowVO;
import online.longlian.app.pojo.vo.app.ItemTaskInstanceVO;
import online.longlian.app.pojo.vo.app.ItemTaskNodeVO;
import online.longlian.app.pojo.vo.app.TaskInstanceDetailVO;
import online.longlian.app.service.app.*;
import online.longlian.common.enumeration.TaskInstanceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class McpTaskQueryServiceImplTest {

    private final ProjectService projectService = mock(ProjectService.class);
    private final ItemService itemService = mock(ItemService.class);
    private final ItemTaskFlowService flowService = mock(ItemTaskFlowService.class);
    private final TaskInstanceService taskService = mock(TaskInstanceService.class);
    private final ProjectAttachmentService attachments = mock(ProjectAttachmentService.class);
    private final McpTaskQueryServiceImpl service = new McpTaskQueryServiceImpl(
            projectService, itemService, flowService, taskService, attachments);
    private final McpTaskQueryParamsBO params = new McpTaskQueryParamsBO(7L, 12L, 1L, 2L, 20L);
    private ItemTaskFlowVO flow;

    @BeforeEach
    void setUp() {
        when(projectService.getProjectDetail(1L, 7L, 12L))
                .thenReturn(ProjectDetailResultBO.builder().title("企划").description("翻译要求").metadata("{}").build());
        when(itemService.requireItemTitle(1L, 2L)).thenReturn("第一话");
        flow = ItemTaskFlowVO.builder().description("流程要求").nodes(List.of(
                node(10L, "翻译", 1, 1, TaskInstanceStatus.CLAIMED),
                node(20L, "校对", 2, 1, TaskInstanceStatus.PENDING),
                node(30L, "排版", 2, 2, TaskInstanceStatus.PENDING))).build();
        when(flowService.getItemTaskFlow(2L, 12L)).thenReturn(flow);
        when(taskService.listItemTaskInstances(any())).thenReturn(List.of(
                ItemTaskInstanceVO.builder().id(10L).assigneeId(7L).assigneeNickname("本人").build(),
                ItemTaskInstanceVO.builder().id(20L).build(), ItemTaskInstanceVO.builder().id(30L).build()));
        when(taskService.getTaskInstanceDetail(any())).thenAnswer(invocation -> {
            var detail = new TaskInstanceDetailVO();
            detail.setDraft(new TaskInstanceDetailVO.Draft(LocalDateTime.of(2026, 11, 8, 12, 0), List.of(), false));
            detail.setSubmission(new TaskInstanceDetailVO.Submission("SUBMITTED", null,
                    List.of(new TaskInstanceDetailVO.TextField("text", "译文", "text", "正式产出"))));
            return detail;
        });
        when(attachments.getProjectAttachments(any())).thenReturn(List.of(ProjectAttachmentResultBO.builder()
                .id(100L).fileId(101L).fileName("原文.txt").fileMime("text/plain").fileSize(8L).build()));
    }

    // 暂存不视为完成，所有前序阶段都完成后才提示可以处理下一阶段；同阶段不互相阻塞。
    @Test
    void shouldKeepNextStageBlockedByDraftAndAllowParallelTasksAfterSubmission() {
        var before = service.getProjectTasks(params);
        assertThat(before.tasks().get(0).hasDraft()).isTrue();
        assertThat(before.tasks().get(0).canWork()).isTrue();
        assertThat(before.tasks().get(1).canClaim()).isFalse();
        assertThat(before.tasks().get(1).blockedBy()).containsExactly("翻译（第 1 步）");

        flow.getNodes().get(0).setTaskStatus(TaskInstanceStatus.COMPLETED);
        var after = service.getProjectTasks(params);
        assertThat(after.tasks().get(1).canClaim()).isTrue();
        assertThat(after.tasks().get(2).canClaim()).isTrue();
        assertThat(after.tasks().get(2).blockedBy()).isEmpty();
    }

    // 执行视图聚合正式前序产出和企划材料，草稿只对接取人本人读取。
    @Test
    void shouldAssembleMaterialsAndPreviousSubmissionsWithoutReadingOthersDrafts() {
        flow.getNodes().get(0).setTaskStatus(TaskInstanceStatus.COMPLETED);
        var result = service.getTask(params);

        assertThat(result.projectDescription()).isEqualTo("翻译要求");
        assertThat(result.references().getFirst().attachmentId()).isEqualTo("100");
        assertThat(result.references().getFirst().fileId()).isEqualTo("101");
        assertThat(result.previousSubmissions().getFirst().instanceId()).isEqualTo("10");
        assertThat(result.draftValues()).isEmpty();
        verify(taskService, never()).getTaskDraft(any());
    }

    // 返回原始字段值，保留文件引用供 AI 修改时继续使用，不把展示文案当成提交值。
    @Test
    void shouldReturnOwnRawDraftValuesAndInputDefinitions() {
        when(taskService.listItemTaskInstances(any())).thenReturn(List.of(ItemTaskInstanceVO.builder()
                .id(20L).assigneeId(7L).build()));
        Map<String, Object> values = Map.of("file", Map.of("fileId", "9007199254740993"));
        when(taskService.getTaskDraft(any())).thenReturn(values);

        var result = service.getTask(params);

        assertThat(result.draftValues()).isEqualTo(values);
        assertThat(result.submitFields().getFirst().key()).isEqualTo("text");
    }

    // 任务必须来自选定的项目，不能把别的项目的字段与该任务拼接。
    @Test
    void shouldRejectTaskOutsideSelectedItem() {
        assertThatThrownBy(() -> service.getTask(new McpTaskQueryParamsBO(7L, 12L, 1L, 2L, 999L)))
                .isInstanceOf(AppException.class);
    }

    private ItemTaskNodeVO node(Long id, String name, int sort, int parallelSort, TaskInstanceStatus status) {
        return ItemTaskNodeVO.builder().taskInstanceId(id).name(name).sort(sort).parallelSort(parallelSort).taskStatus(status)
                .submitFields(List.of(new TaskFormField("text", "译文", "text", true, List.of()))).build();
    }
}
