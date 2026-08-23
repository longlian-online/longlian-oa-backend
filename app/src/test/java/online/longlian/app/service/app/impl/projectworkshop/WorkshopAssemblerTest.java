package online.longlian.app.service.app.impl.projectworkshop;

import online.longlian.app.mapper.BaseTaskMapper;
import online.longlian.app.mapper.TaskTemplateNodeMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.common.ResourceReadUrlGetResultBO;
import online.longlian.app.pojo.entity.BaseTask;
import online.longlian.app.pojo.entity.Project;
import online.longlian.app.pojo.entity.TaskTemplate;
import online.longlian.app.pojo.entity.TaskTemplateNode;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.pojo.vo.app.WorkshopProjectInfoVO;
import online.longlian.app.pojo.vo.app.WorkshopTaskTemplateVO;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.common.enumeration.Status;
import online.longlian.common.enumeration.TaskTemplateScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkshopAssemblerTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private BaseTaskMapper baseTaskMapper;

    @Mock
    private TaskTemplateNodeMapper taskTemplateNodeMapper;

    @Mock
    private ResourceService resourceService;

    private WorkshopAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new WorkshopAssembler(userMapper, baseTaskMapper, taskTemplateNodeMapper, resourceService);
    }

    // ---- assembleProjectList ----

    @Test
    void assembleProjectList_emptyList_returnsEmptyList() {
        List<WorkshopProjectInfoVO> result = assembler.assembleProjectList(Collections.emptyList());

        assertTrue(result.isEmpty());
        verify(resourceService, never()).getResourceReadUrls(anyList());
        verify(userMapper, never()).selectBatchIds(anyList());
    }

    @Test
    void assembleProjectList_projectWithCoverAndCreatorAvatar_populatesUrls() {
        Project project = Project.builder()
                .id(1L)
                .title("Project A")
                .coverFileId(200L)
                .creatorId(10L)
                .build();
        User creator = User.builder().id(10L).avatarFileId(300L).build();

        when(userMapper.selectBatchIds(List.of(10L))).thenReturn(List.of(creator));
        when(resourceService.getResourceReadUrls(List.of(200L)))
                .thenReturn(Map.of(200L, new ResourceReadUrlGetResultBO("https://cdn/cover.png", 1L, "c")));
        when(resourceService.getResourceReadUrls(List.of(300L)))
                .thenReturn(Map.of(300L, new ResourceReadUrlGetResultBO("https://cdn/avatar.png", 1L, "a")));

        List<WorkshopProjectInfoVO> result = assembler.assembleProjectList(List.of(project));

        assertEquals(1, result.size());
        WorkshopProjectInfoVO vo = result.get(0);
        assertEquals(1L, vo.getId());
        assertEquals("Project A", vo.getTitle());
        assertEquals("https://cdn/cover.png", vo.getCoverUrl());
        assertEquals("https://cdn/avatar.png", vo.getCreatorAvatarUrl());
    }

    @Test
    void assembleProjectList_projectNoCoverNullCreatorAvatar_nullUrls() {
        Project project = Project.builder()
                .id(2L)
                .title("Project B")
                .coverFileId(null) // no cover
                .creatorId(10L)
                .build();
        User creator = User.builder().id(10L).avatarFileId(null).build(); // no avatar

        when(userMapper.selectBatchIds(List.of(10L))).thenReturn(List.of(creator));

        List<WorkshopProjectInfoVO> result = assembler.assembleProjectList(List.of(project));

        assertEquals(1, result.size());
        assertNull(result.get(0).getCoverUrl());
        assertNull(result.get(0).getCreatorAvatarUrl());
        // resourceService not called when there are no file IDs to resolve
        verify(resourceService, never()).getResourceReadUrls(anyList());
    }

    @Test
    void assembleProjectList_projectWithZeroCoverFileId_treatedAsNoCover() {
        Project project = Project.builder()
                .id(3L)
                .title("Project C")
                .coverFileId(0L) // 0 is treated as no cover
                .creatorId(10L)
                .build();
        User creator = User.builder().id(10L).avatarFileId(0L).build();

        when(userMapper.selectBatchIds(List.of(10L))).thenReturn(List.of(creator));

        List<WorkshopProjectInfoVO> result = assembler.assembleProjectList(List.of(project));

        assertEquals(1, result.size());
        assertNull(result.get(0).getCoverUrl());
        verify(resourceService, never()).getResourceReadUrls(anyList());
    }

    // ---- assembleTemplateList ----

    @Test
    void assembleTemplateList_emptyList_returnsEmptyList() {
        List<WorkshopTaskTemplateVO> result = assembler.assembleTemplateList(Collections.emptyList(), 1L);

        assertTrue(result.isEmpty());
        verify(taskTemplateNodeMapper, never()).selectList(any());
    }

    @Test
    void assembleTemplateList_templateWithNodes_populatesNodeFields() {
        TaskTemplate template = TaskTemplate.builder()
                .id(1L)
                .name("漫画模板")
                .description("描述")
                .scope(TaskTemplateScope.ORGANIZATION)
                .creatorId(50L)
                .build();
        TaskTemplateNode node = TaskTemplateNode.builder()
                .id(100L)
                .taskTemplateId(1L)
                .baseTaskId(200L)
                .sort(1)
                .parallelSort(0)
                .build();
        BaseTask baseTask = BaseTask.builder()
                .id(200L)
                .name("绘制")
                .iconFileId(300L)
                .build();

        when(taskTemplateNodeMapper.selectList(any())).thenReturn(List.of(node));
        when(baseTaskMapper.selectBatchIds(List.of(200L))).thenReturn(List.of(baseTask));
        when(resourceService.getResourceReadUrls(List.of(300L)))
                .thenReturn(Map.of(300L, new ResourceReadUrlGetResultBO("https://cdn/icon.png", 1L, "i")));

        List<WorkshopTaskTemplateVO> result = assembler.assembleTemplateList(List.of(template), 99L);

        assertEquals(1, result.size());
        WorkshopTaskTemplateVO vo = result.get(0);
        assertEquals(1L, vo.getId());
        assertEquals("漫画模板", vo.getName());
        assertEquals(1, vo.getTaskCount());
        assertEquals(1, vo.getNodes().size());
        assertEquals("绘制", vo.getNodes().get(0).getBaseTaskName());
        assertEquals("https://cdn/icon.png", vo.getNodes().get(0).getBaseTaskIconUrl());
        // creatorId(50L) != currentUserId(99L)
        assertEquals(false, vo.getIsMine());
    }

    @Test
    void assembleTemplateList_isMineWhenCreatorMatchesCurrentUser() {
        TaskTemplate template = TaskTemplate.builder()
                .id(2L)
                .name("个人模板")
                .scope(TaskTemplateScope.PERSONAL)
                .creatorId(77L)
                .build();

        when(taskTemplateNodeMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<WorkshopTaskTemplateVO> result = assembler.assembleTemplateList(List.of(template), 77L);

        assertEquals(true, result.get(0).getIsMine());
    }

    @Test
    void assembleTemplateList_nodeBaseTaskNotFound_nodeNameNull() {
        TaskTemplate template = TaskTemplate.builder()
                .id(3L)
                .name("模板")
                .scope(TaskTemplateScope.ORGANIZATION)
                .creatorId(1L)
                .build();
        TaskTemplateNode node = TaskTemplateNode.builder()
                .id(101L)
                .taskTemplateId(3L)
                .baseTaskId(999L) // unknown base task
                .sort(1)
                .parallelSort(0)
                .build();

        when(taskTemplateNodeMapper.selectList(any())).thenReturn(List.of(node));
        when(baseTaskMapper.selectBatchIds(List.of(999L))).thenReturn(Collections.emptyList());

        List<WorkshopTaskTemplateVO> result = assembler.assembleTemplateList(List.of(template), 1L);

        assertEquals(1, result.get(0).getTaskCount());
        assertNull(result.get(0).getNodes().get(0).getBaseTaskName());
    }
}
