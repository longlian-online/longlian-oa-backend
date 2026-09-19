package online.longlian.app.service.app.impl.itemtaskflow;

import online.longlian.app.common.exception.AppException;
import online.longlian.app.mapper.ItemMapper;
import online.longlian.app.mapper.ItemTaskFlowMapper;
import online.longlian.app.mapper.ItemTaskNodeMapper;
import online.longlian.app.mapper.ProjectMapper;
import online.longlian.app.mapper.TaskInstanceMapper;
import online.longlian.app.pojo.entity.Item;
import online.longlian.app.pojo.entity.Project;
import online.longlian.common.enumeration.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
}
