package online.longlian.app.service.app.impl.itemtaskflow;

import online.longlian.app.mapper.BaseTaskMapper;
import online.longlian.app.pojo.entity.BaseTask;
import online.longlian.app.pojo.entity.ItemTaskNode;
import online.longlian.app.pojo.entity.TaskInstance;
import online.longlian.common.enumeration.TaskInstanceStatus;
import online.longlian.app.pojo.vo.app.ItemTaskNodeVO;
import online.longlian.app.service.common.TaskFormService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemTaskFlowAssemblerTest {

    @Mock
    private BaseTaskMapper baseTaskMapper;

    private ItemTaskFlowAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new ItemTaskFlowAssembler(baseTaskMapper, new TaskFormService());
    }

    @Test
    void assembleNodes_preservesSnapshotsAndJoinsInstancesByNodeId() {
        ItemTaskNode first = ItemTaskNode.builder()
                .id(1L).baseTaskId(10L).name("自定义翻译")
                .metaSchema("[{\"key\":\"summary\",\"label\":\"历史摘要\",\"type\":\"text\",\"required\":true,\"options\":[]}]")
                .sort(1).parallelSort(0).build();
        ItemTaskNode second = ItemTaskNode.builder()
                .id(2L).baseTaskId(20L).name("历史审核")
                .metaSchema("[]").sort(2).parallelSort(0).build();
        when(baseTaskMapper.selectBatchIds(List.of(10L, 20L)))
                .thenReturn(List.of(BaseTask.builder().id(10L).name("已改名")
                        .icon("Camera").metaSchema("[]").build()));
        TaskInstance instance = TaskInstance.builder().id(100L).itemTaskNodeId(2L)
                .status(TaskInstanceStatus.CLAIMED).build();

        List<ItemTaskNodeVO> result = assembler.assembleNodes(List.of(first, second), List.of(instance));

        assertThat(result).extracting(ItemTaskNodeVO::getName)
                .containsExactly("自定义翻译", "历史审核");
        assertThat(result.get(0).getBaseTaskIcon()).isEqualTo("Camera");
        assertThat(result.get(0).getSubmitFields()).extracting(field -> field.label())
                .containsExactly("历史摘要");
        assertThat(result.get(0).getTaskInstanceId()).isNull();
        assertThat(result.get(1).getBaseTaskIcon()).isNull();
        assertThat(result.get(1).getTaskInstanceId()).isEqualTo(100L);
        assertThat(result.get(1).getTaskStatus()).isEqualTo(TaskInstanceStatus.CLAIMED);
    }

    @Test
    void assembleNodes_withoutNodes_doesNotExposeOrphanInstances() {
        TaskInstance orphan = TaskInstance.builder().id(100L).itemTaskNodeId(2L)
                .status(TaskInstanceStatus.CLAIMED).build();

        assertThat(assembler.assembleNodes(Collections.emptyList(), List.of(orphan))).isEmpty();
    }
}
