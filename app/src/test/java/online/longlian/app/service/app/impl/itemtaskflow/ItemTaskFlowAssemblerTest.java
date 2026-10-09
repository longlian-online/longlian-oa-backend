package online.longlian.app.service.app.impl.itemtaskflow;

import online.longlian.app.mapper.BaseTaskMapper;
import online.longlian.app.pojo.entity.BaseTask;
import online.longlian.app.pojo.entity.ItemTaskNode;
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
    void assembleNodes_withBaseTaskIcon_returnsUrlAndLucideName() {
        ItemTaskNode node = ItemTaskNode.builder()
                .id(1L)
                .baseTaskId(10L)
                .name("翻译")
                .metaSchema("[]")
                .sort(1)
                .parallelSort(0)
                .build();
        BaseTask baseTask = BaseTask.builder()
                .id(10L)
                .icon("Languages")
                .build();
        when(baseTaskMapper.selectBatchIds(List.of(10L))).thenReturn(List.of(baseTask));

        List<ItemTaskNodeVO> result = assembler.assembleNodes(List.of(node), Collections.emptyList());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBaseTaskIcon()).isEqualTo("Languages");
    }
}
