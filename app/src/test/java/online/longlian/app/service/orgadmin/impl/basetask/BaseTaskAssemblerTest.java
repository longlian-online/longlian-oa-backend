package online.longlian.app.service.orgadmin.impl.basetask;

import online.longlian.app.mapper.TaskTemplateNodeMapper;
import online.longlian.app.pojo.bo.common.ResourceReadUrlGetResultBO;
import online.longlian.app.pojo.bo.orgadmin.BaseTaskListResultBO;
import online.longlian.app.pojo.entity.BaseTask;
import online.longlian.app.pojo.entity.TaskTemplateNode;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.common.enumeration.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BaseTaskAssemblerTest {

    @Mock
    private TaskTemplateNodeMapper taskTemplateNodeMapper;
    @Mock
    private ResourceService resourceService;

    private BaseTaskAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new BaseTaskAssembler(taskTemplateNodeMapper, resourceService);
    }

    @Test
    void assembleBaseTaskList_emptyList_returnsEmpty() {
        assertThat(assembler.assembleBaseTaskList(Collections.emptyList())).isEmpty();
    }

    @Test
    void assembleBaseTaskList_withTasks_returnsAssembled() {
        BaseTask task = BaseTask.builder()
                .id(1L).name("Draw").description("Draw something")
                .iconFileId(100L).metaSchema("{}").status(Status.ENABLED)
                .createdAt(LocalDateTime.now())
                .build();
        when(resourceService.getResourceReadUrls(anyList()))
                .thenReturn(Map.of(100L, new ResourceReadUrlGetResultBO("https://cdn/icon.png", 1L, "icon/100")));
        TaskTemplateNode node = TaskTemplateNode.builder().id(10L).baseTaskId(1L).build();
        when(taskTemplateNodeMapper.selectList(any())).thenReturn(List.of(node, node));

        List<BaseTaskListResultBO> result = assembler.assembleBaseTaskList(List.of(task));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Draw");
        assertThat(result.get(0).getIconUrl()).isEqualTo("https://cdn/icon.png");
        assertThat(result.get(0).getRefCount()).isEqualTo(2);
    }

    @Test
    void assembleBaseTaskList_noIcon_returnsNullUrl() {
        BaseTask task = BaseTask.builder()
                .id(1L).name("NoIcon").iconFileId(null)
                .status(Status.ENABLED).createdAt(LocalDateTime.now())
                .build();
        when(taskTemplateNodeMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<BaseTaskListResultBO> result = assembler.assembleBaseTaskList(List.of(task));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIconUrl()).isNull();
        assertThat(result.get(0).getRefCount()).isZero();
    }
}
