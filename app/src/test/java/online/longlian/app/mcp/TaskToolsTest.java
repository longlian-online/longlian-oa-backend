package online.longlian.app.mcp;

import com.alibaba.fastjson2.JSON;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import online.longlian.app.common.resolver.SessionContext;
import online.longlian.app.pojo.bo.app.TaskInstanceSubmitParamsBO;
import online.longlian.app.pojo.bo.common.ResourceCreateParamsBO;
import online.longlian.app.pojo.vo.common.ResourceCreateVO;
import online.longlian.app.service.app.*;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.common.enumeration.StorageType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TaskToolsTest {

    // 上传固定任务业务类型，并只暴露直传需要的数据；长整型 ID 不变成 JSON 数字。
    @Test
    void shouldFixUploadBusinessAndPreserveSignedHeadersWithoutStorageKey() {
        McpUserSession session = mock(McpUserSession.class);
        ResourceService resources = mock(ResourceService.class);
        when(session.require(any())).thenReturn(new SessionContext(7L, 12L));
        when(resources.create(any())).thenReturn(new ResourceCreateVO(9007199254740993L, "https://upload.example/file",
                "private-storage-key", StorageType.COS, "attachment; filename=exact.txt"));
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            TaskTools tools = new TaskTools(session, null, null, null, null, null, resources, factory.getValidator());
            var result = JSON.parseObject(tools.createTaskUpload(null, "成果.txt", "txt", 3L, "text/plain"));

            assertThat(result.getString("fileId")).isEqualTo("9007199254740993");
            assertThat(result.getJSONObject("headers").getString("Content-Disposition")).isEqualTo("attachment; filename=exact.txt");
            assertThat(result).doesNotContainKeys("key", "storageType");
            ArgumentCaptor<ResourceCreateParamsBO> params = ArgumentCaptor.forClass(ResourceCreateParamsBO.class);
            verify(resources).create(params.capture());
            assertThat(params.getValue().getBizType()).isEqualTo("task_submit");
            assertThat(params.getValue().getCreatorId()).isEqualTo(7L);
            assertThat(params.getValue().getOrgId()).isEqualTo(12L);
        }
    }

    // 暂存只调用现有暂存服务，完整保留字段；不会额外发起正式提交。
    @Test
    void shouldSaveFullDraftAsCurrentUserWithoutSubmitting() {
        McpUserSession session = mock(McpUserSession.class);
        TaskInstanceService tasks = mock(TaskInstanceService.class);
        when(session.require(any())).thenReturn(new SessionContext(7L, 12L));
        when(tasks.saveTaskDraft(any())).thenReturn(9L);
        Map<String, Object> values = Map.of("text", "译文", "file", Map.of("fileId", "100"));
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            TaskTools tools = new TaskTools(session, null, null, null, tasks, null, null, factory.getValidator());
            assertThat(JSON.parseObject(tools.saveTaskDraft(null, "9", values)).getBoolean("saved")).isTrue();
            ArgumentCaptor<TaskInstanceSubmitParamsBO> params = ArgumentCaptor.forClass(TaskInstanceSubmitParamsBO.class);
            verify(tasks).saveTaskDraft(params.capture());
            assertThat(params.getValue().getUserId()).isEqualTo(7L);
            assertThat(params.getValue().getOrgId()).isEqualTo(12L);
            assertThat(params.getValue().getValues()).isEqualTo(values);
            verifyNoMoreInteractions(tasks);
        }
    }
}
