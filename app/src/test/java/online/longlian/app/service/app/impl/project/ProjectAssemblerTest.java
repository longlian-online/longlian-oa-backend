package online.longlian.app.service.app.impl.project;

import online.longlian.app.mapper.ProjectTypeMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.app.ProjectListResultBO;
import online.longlian.app.pojo.bo.common.ResourceReadUrlGetResultBO;
import online.longlian.app.pojo.entity.Project;
import online.longlian.app.pojo.entity.ProjectType;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.common.enumeration.ProjectStatus;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectAssemblerTest {

    @Mock
    private ProjectTypeMapper projectTypeMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ResourceService resourceService;

    private ProjectAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new ProjectAssembler(projectTypeMapper, userMapper, resourceService);
    }

    @Test
    void assembleList_emptyList_returnsEmptyList() {
        List<ProjectListResultBO> result = assembler.assembleList(Collections.emptyList());

        assertTrue(result.isEmpty());
        verify(projectTypeMapper, never()).selectBatchIds(anyList());
        verify(userMapper, never()).selectBatchIds(anyList());
    }

    @Test
    void assembleList_projectWithAllFields_populatesBO() {
        Project project = Project.builder()
                .id(1L)
                .title("龙连漫画")
                .description("OA协作企划")
                .typeId(5L)
                .creatorId(10L)
                .coverFileId(200L)
                .status(ProjectStatus.IN_PROGRESS)
                .metadata("{}")
                .build();
        ProjectType type = ProjectType.builder().id(5L).name("漫画").build();
        User creator = User.builder().id(10L).avatarFileId(300L).build();

        when(projectTypeMapper.selectBatchIds(List.of(5L))).thenReturn(List.of(type));
        when(userMapper.selectBatchIds(List.of(10L))).thenReturn(List.of(creator));
        when(resourceService.getResourceReadUrls(List.of(200L)))
                .thenReturn(Map.of(200L, new ResourceReadUrlGetResultBO("https://cdn/cover.png", 1L, "c")));
        when(resourceService.getResourceReadUrls(List.of(300L)))
                .thenReturn(Map.of(300L, new ResourceReadUrlGetResultBO("https://cdn/avatar.png", 1L, "a")));

        List<ProjectListResultBO> result = assembler.assembleList(List.of(project));

        assertEquals(1, result.size());
        ProjectListResultBO bo = result.get(0);
        assertEquals(1L, bo.getId());
        assertEquals("龙连漫画", bo.getTitle());
        assertEquals("OA协作企划", bo.getDescription());
        assertEquals("漫画", bo.getProjectType());
        assertEquals(ProjectStatus.IN_PROGRESS, bo.getProjectStatus());
        assertEquals("https://cdn/cover.png", bo.getCoverUrl());
        assertEquals("https://cdn/avatar.png", bo.getCreatorAvatarUrl());
        assertEquals("{}", bo.getMetadata());
    }

    @Test
    void assembleList_projectNoCoverNullCreatorAvatar_nullUrls() {
        Project project = Project.builder()
                .id(2L)
                .title("Project B")
                .typeId(5L)
                .creatorId(10L)
                .coverFileId(null) // no cover
                .build();
        ProjectType type = ProjectType.builder().id(5L).name("小说").build();
        User creator = User.builder().id(10L).avatarFileId(null).build();

        when(projectTypeMapper.selectBatchIds(List.of(5L))).thenReturn(List.of(type));
        when(userMapper.selectBatchIds(List.of(10L))).thenReturn(List.of(creator));

        List<ProjectListResultBO> result = assembler.assembleList(List.of(project));

        assertEquals(1, result.size());
        assertNull(result.get(0).getCoverUrl());
        assertNull(result.get(0).getCreatorAvatarUrl());
        verify(resourceService, never()).getResourceReadUrls(anyList());
    }

    @Test
    void assembleList_projectWithZeroCoverFileId_treatedAsNoCover() {
        Project project = Project.builder()
                .id(3L)
                .title("Project C")
                .typeId(5L)
                .creatorId(10L)
                .coverFileId(0L) // zero treated as absent
                .build();
        User creator = User.builder().id(10L).avatarFileId(0L).build();

        when(projectTypeMapper.selectBatchIds(List.of(5L))).thenReturn(List.of(
                ProjectType.builder().id(5L).name("视频").build()));
        when(userMapper.selectBatchIds(List.of(10L))).thenReturn(List.of(creator));

        List<ProjectListResultBO> result = assembler.assembleList(List.of(project));

        assertNull(result.get(0).getCoverUrl());
        verify(resourceService, never()).getResourceReadUrls(anyList());
    }

    @Test
    void assembleList_creatorNotFoundInUserMap_nullCreatorAvatarUrl() {
        // creator not returned from DB (e.g., deleted user)
        Project project = Project.builder()
                .id(4L)
                .title("Project D")
                .typeId(5L)
                .creatorId(99L)
                .coverFileId(null)
                .build();

        when(projectTypeMapper.selectBatchIds(List.of(5L))).thenReturn(List.of(
                ProjectType.builder().id(5L).name("美术").build()));
        when(userMapper.selectBatchIds(List.of(99L))).thenReturn(Collections.emptyList());

        List<ProjectListResultBO> result = assembler.assembleList(List.of(project));

        assertEquals(1, result.size());
        assertNull(result.get(0).getCreatorAvatarUrl());
    }
}
