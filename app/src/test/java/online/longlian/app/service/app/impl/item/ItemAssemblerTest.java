package online.longlian.app.service.app.impl.item;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.mapper.ItemTaskNodeMapper;
import online.longlian.app.mapper.TaskInstanceMapper;
import online.longlian.app.pojo.entity.Item;
import online.longlian.app.pojo.entity.TaskInstance;
import online.longlian.app.pojo.entity.ItemTaskNode;
import online.longlian.app.pojo.vo.app.ProjectItemListVO;
import online.longlian.common.enumeration.ItemNodeState;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemAssemblerTest {

    @Mock
    private ItemTaskNodeMapper itemTaskNodeMapper;
    @Mock
    private TaskInstanceMapper taskInstanceMapper;

    private ItemAssembler assembler;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ItemTaskNode.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), TaskInstance.class);
        assembler = new ItemAssembler(itemTaskNodeMapper, taskInstanceMapper);
    }

    @Test
    void assembleList_emptyList_returnsEmpty() {
        assertThat(assembler.assembleList(Collections.emptyList())).isEmpty();
        verify(itemTaskNodeMapper, never()).selectList(any());
        verify(taskInstanceMapper, never()).selectList(any());
    }

    @Test
    void assembleList_nodeWithoutInstances_isLocked() {
        when(itemTaskNodeMapper.selectList(any())).thenReturn(List.of(
                ItemTaskNode.builder().id(10L).itemId(1L).name("锁定").sort(1).parallelSort(1).build()));
        when(taskInstanceMapper.selectList(any())).thenReturn(List.of());

        List<ProjectItemListVO> result = assembler.assembleList(List.of(
                Item.builder().id(1L).title("项目").build()));

        assertThat(result).singleElement().satisfies(item ->
                assertThat(item.getNodes()).singleElement().satisfies(node ->
                        assertThat(node.getState()).isEqualTo(ItemNodeState.LOCKED)));
    }
}
