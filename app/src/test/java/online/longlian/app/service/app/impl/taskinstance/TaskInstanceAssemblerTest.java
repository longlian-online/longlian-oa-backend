package online.longlian.app.service.app.impl.taskinstance;

import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.common.ResourceReadUrlGetResultBO;
import online.longlian.app.pojo.entity.ItemTaskNode;
import online.longlian.app.pojo.entity.TaskInstance;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.pojo.vo.app.ItemTaskInstanceVO;
import online.longlian.app.service.resource.ResourceService;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    private TaskInstanceAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new TaskInstanceAssembler(userMapper, resourceService);
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
                .createdAt(LocalDateTime.now())
                .build(); // assigneeId is null

        List<ItemTaskInstanceVO> result = assembler.assembleInstances(List.of(instance), Collections.emptyMap());

        assertEquals(1, result.size());
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
                .build();

        List<ItemTaskInstanceVO> result = assembler.assembleInstances(List.of(instance), Map.of());

        assertEquals(1, result.size());
        assertNull(result.get(0).getName());
        assertNull(result.get(0).getSort());
    }
}
