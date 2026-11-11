package online.longlian.app.controller.app;

import online.longlian.app.common.resolver.SessionContext;
import online.longlian.app.pojo.bo.app.taskinstance.TaskAttachmentResultBO;
import online.longlian.app.pojo.bo.app.taskinstance.TaskInstanceDetailResultBO;
import online.longlian.app.pojo.bo.app.taskinstance.TaskSubmissionVersionResultBO;
import online.longlian.app.pojo.vo.app.TaskInstanceDetailVO;
import online.longlian.app.pojo.vo.app.TaskSubmissionVersionVO;
import online.longlian.app.service.app.MyTaskService;
import online.longlian.app.service.app.TaskInstanceService;
import online.longlian.app.service.app.TaskPlazaService;
import online.longlian.app.service.orgadmin.BaseTaskService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaskInstanceControllerMappingTest {

    private final TaskInstanceService taskInstanceService = mock(TaskInstanceService.class);
    private final TaskInstanceController controller = new TaskInstanceController(
            taskInstanceService, mock(TaskPlazaService.class), mock(BaseTaskService.class), mock(MyTaskService.class));

    @Test
    void shouldMapTaskInstanceDetailResultToVO() {
        LocalDateTime submittedAt = LocalDateTime.of(2026, 11, 11, 10, 0);
        LocalDateTime draftUpdatedAt = LocalDateTime.of(2026, 11, 11, 11, 0);
        TaskInstanceDetailResultBO resultBO = new TaskInstanceDetailResultBO();
        resultBO.setTask(new TaskInstanceDetailResultBO.TaskSummaryResultBO("10", "校对", 2, "COMPLETED",
                new TaskInstanceDetailResultBO.TaskAssigneeResultBO("7", "张三", "avatar-url")));
        resultBO.setSubmission(new TaskInstanceDetailResultBO.TaskSubmissionResultBO("submitted", submittedAt,
                List.of(
                        new TaskInstanceDetailResultBO.TaskTextFieldResultBO("summary", "摘要", "text", "内容"),
                        new TaskInstanceDetailResultBO.TaskFileFieldResultBO("file", "附件", "file",
                                new TaskAttachmentResultBO("5", "a.pdf", "1 KB", "document", "available")))));
        resultBO.setDraft(new TaskInstanceDetailResultBO.TaskDraftResultBO(draftUpdatedAt,
                List.of(new TaskInstanceDetailResultBO.TaskTextFieldResultBO("summary", "摘要", "multiline", "草稿")),
                false));
        when(taskInstanceService.getTaskInstanceDetail(any())).thenReturn(resultBO);

        TaskInstanceDetailVO detailVO = controller.getTaskInstanceDetail(new SessionContext(7L, 1L), 10L);

        assertThat(detailVO.getTask().id()).isEqualTo("10");
        assertThat(detailVO.getTask().assignee().nickname()).isEqualTo("张三");
        assertThat(detailVO.getSubmission().state()).isEqualTo("submitted");
        assertThat(detailVO.getSubmission().submittedAt()).isEqualTo(submittedAt);
        assertThat(detailVO.getSubmission().fields()).hasSize(2);
        assertThat(detailVO.getSubmission().fields().get(0))
                .isInstanceOfSatisfying(TaskInstanceDetailVO.TaskTextFieldVO.class,
                        field -> assertThat(field.text()).isEqualTo("内容"));
        assertThat(detailVO.getSubmission().fields().get(1))
                .isInstanceOfSatisfying(TaskInstanceDetailVO.TaskFileFieldVO.class,
                        field -> assertThat(field.file().getName()).isEqualTo("a.pdf"));
        assertThat(detailVO.getDraft().updatedAt()).isEqualTo(draftUpdatedAt);
        assertThat(detailVO.getDraft().fields()).singleElement()
                .isInstanceOfSatisfying(TaskInstanceDetailVO.TaskTextFieldVO.class,
                        field -> assertThat(field.text()).isEqualTo("草稿"));
    }

    @Test
    void shouldMapTaskSubmissionVersionsToVO() {
        LocalDateTime submittedAt = LocalDateTime.of(2026, 11, 11, 10, 0);
        TaskSubmissionVersionResultBO resultBO = TaskSubmissionVersionResultBO.builder()
                .versionNo(2)
                .status("REJECTED")
                .submittedAt(submittedAt)
                .submitter(new TaskSubmissionVersionResultBO.TaskParticipantResultBO("7", "张三", null))
                .reviewer(new TaskSubmissionVersionResultBO.TaskParticipantResultBO("8", "李四", "avatar-url"))
                .reviewComment("请补充")
                .fields(List.of(new TaskInstanceDetailResultBO.TaskTextFieldResultBO("summary", "摘要", "text", "内容")))
                .build();
        when(taskInstanceService.listSubmissionVersions(any())).thenReturn(List.of(resultBO));

        List<TaskSubmissionVersionVO> versionVOs = controller.listSubmissionVersions(new SessionContext(7L, 1L), 10L);

        assertThat(versionVOs).singleElement().satisfies(versionVO -> {
            assertThat(versionVO.getVersionNo()).isEqualTo(2);
            assertThat(versionVO.getSubmitter().nickname()).isEqualTo("张三");
            assertThat(versionVO.getReviewer().nickname()).isEqualTo("李四");
            assertThat(versionVO.getReviewComment()).isEqualTo("请补充");
            assertThat(versionVO.getFields()).singleElement()
                    .isInstanceOfSatisfying(TaskInstanceDetailVO.TaskTextFieldVO.class,
                            field -> assertThat(field.text()).isEqualTo("内容"));
        });
    }
}
