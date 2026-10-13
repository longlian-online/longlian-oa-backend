package online.longlian.app.service.app.impl.itemtaskflow;

import online.longlian.app.common.exception.AppException;
import online.longlian.app.mapper.ItemMapper;
import online.longlian.app.mapper.ItemTaskFlowMapper;
import online.longlian.app.mapper.ItemTaskNodeMapper;
import online.longlian.app.mapper.ProjectMapper;
import online.longlian.app.mapper.TaskInstanceMapper;
import online.longlian.app.pojo.entity.Item;
import online.longlian.app.pojo.entity.ItemTaskFlow;
import online.longlian.app.pojo.entity.ItemTaskNode;
import online.longlian.app.pojo.entity.Project;
import online.longlian.app.pojo.vo.app.ItemTaskFlowVO;
import online.longlian.app.pojo.vo.app.ItemTaskNodeVO;
import online.longlian.common.enumeration.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemTaskFlowServiceImplTest {

    @Mock private ItemMapper itemMapper;
    @Mock private ItemTaskFlowMapper itemTaskFlowMapper;
    @Mock private ItemTaskNodeMapper itemTaskNodeMapper;
    @Mock private TaskInstanceMapper taskInstanceMapper;
    @Mock private ItemTaskFlowAssembler itemTaskFlowAssembler;
    @Mock private ProjectMapper projectMapper;

    private ItemTaskFlowServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ItemTaskFlowServiceImpl(itemMapper, itemTaskFlowMapper, itemTaskNodeMapper,
                taskInstanceMapper, itemTaskFlowAssembler, projectMapper);
    }

    @Test
    void getItemTaskFlow_disabledProject_throwsNotFound() {
        when(itemMapper.selectById(1L)).thenReturn(Item.builder().id(1L).projectId(2L).build());
        when(projectMapper.selectById(2L)).thenReturn(Project.builder()
                .id(2L).orgId(1L).resourceStatus(Status.DISABLED).build());

        assertThatThrownBy(() -> service.getItemTaskFlow(1L, 1L)).isInstanceOf(AppException.class);
        verify(itemTaskFlowMapper, never()).selectOne(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getItemTaskFlow_missingItem_throwsNotFound() {
        when(itemMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> service.getItemTaskFlow(1L, 1L)).isInstanceOf(AppException.class);
        verify(projectMapper, never()).selectById(any());
    }

    @Test
    void getItemTaskFlow_missingFlow_throwsNotFound() {
        when(itemMapper.selectById(1L)).thenReturn(Item.builder().id(1L).projectId(2L).build());
        when(projectMapper.selectById(2L)).thenReturn(Project.builder()
                .id(2L).orgId(1L).resourceStatus(Status.ENABLED).build());
        when(itemTaskFlowMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.getItemTaskFlow(1L, 1L)).isInstanceOf(AppException.class);
    }

    @Test
    void getItemTaskFlow_itemWithoutNodes_returnsEmptyNodes() {
        when(itemMapper.selectById(1L)).thenReturn(Item.builder()
                .id(1L).projectId(2L).title("第3话").build());
        when(projectMapper.selectById(2L)).thenReturn(Project.builder()
                .id(2L).orgId(1L).resourceStatus(Status.ENABLED).build());
        when(itemTaskFlowMapper.selectOne(any())).thenReturn(ItemTaskFlow.builder()
                .id(3L).itemId(1L).projectId(2L).name("漫画汉化流程").build());
        when(itemTaskNodeMapper.selectList(any())).thenReturn(List.of());
        when(itemTaskFlowAssembler.assembleNodes(anyList(), anyList())).thenReturn(List.of());

        ItemTaskFlowVO flowVO = service.getItemTaskFlow(1L, 1L);

        assertThat(flowVO.getItemTitle()).isEqualTo("第3话");
        assertThat(flowVO.getNodes()).isEmpty();
        verify(taskInstanceMapper, never()).selectList(any());
    }

    @Test
    void getItemTaskFlow_enabledProject_returnsItemTitleAndFlowInfo() {
        when(itemMapper.selectById(1L)).thenReturn(Item.builder()
                .id(1L).projectId(2L).title("第3话").build());
        when(projectMapper.selectById(2L)).thenReturn(Project.builder()
                .id(2L).orgId(1L).resourceStatus(Status.ENABLED).build());
        when(itemTaskFlowMapper.selectOne(any())).thenReturn(ItemTaskFlow.builder()
                .id(3L).itemId(1L).projectId(2L).name("漫画汉化流程").description("流程说明").build());
        when(itemTaskNodeMapper.selectList(any())).thenReturn(List.of(ItemTaskNode.builder()
                .id(4L).itemTaskFlowId(3L).name("翻译").sort(1).parallelSort(1).build()));
        when(taskInstanceMapper.selectList(any())).thenReturn(List.of());
        when(itemTaskFlowAssembler.assembleNodes(anyList(), anyList())).thenReturn(List.of(
                ItemTaskNodeVO.builder().id(5L).name("翻译").sort(1).parallelSort(1).build()));

        ItemTaskFlowVO flowVO = service.getItemTaskFlow(1L, 1L);

        assertThat(flowVO.getItemTitle()).isEqualTo("第3话");
        assertThat(flowVO.getName()).isEqualTo("漫画汉化流程");
        assertThat(flowVO.getDescription()).isEqualTo("流程说明");
        assertThat(flowVO.getNodes()).hasSize(1);
        assertThat(flowVO.getNodes().getFirst().getName()).isEqualTo("翻译");
    }
}
