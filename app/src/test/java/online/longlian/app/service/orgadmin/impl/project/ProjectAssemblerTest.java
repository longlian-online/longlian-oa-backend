package online.longlian.app.service.orgadmin.impl.project;

import online.longlian.app.mapper.ProjectTypeMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.orgadmin.ProjectAdminListResultBO;
import online.longlian.app.pojo.entity.Project;
import online.longlian.app.pojo.entity.ProjectType;
import online.longlian.app.pojo.entity.User;
import online.longlian.common.enumeration.ProjectStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectAssemblerTest {

    @Mock
    private ProjectTypeMapper projectTypeMapper;
    @Mock
    private UserMapper userMapper;

    private ProjectAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new ProjectAssembler(projectTypeMapper, userMapper);
    }

    @Test
    void assembleAdminList_emptyList_returnsEmpty() {
        assertThat(assembler.assembleAdminList(Collections.emptyList())).isEmpty();
    }

    @Test
    void assembleAdminList_withProjects_returnsAssembled() {
        Project project = Project.builder()
                .id(1L).title("Test Project").typeId(10L)
                .status(ProjectStatus.IN_PROGRESS).creatorId(5L)
                .createdAt(LocalDateTime.now())
                .build();
        ProjectType type = ProjectType.builder().id(10L).name("Design").build();
        User creator = User.builder().id(5L).nickname("Alice").build();

        when(projectTypeMapper.selectBatchIds(anyList())).thenReturn(List.of(type));
        when(userMapper.selectBatchIds(anyList())).thenReturn(List.of(creator));

        List<ProjectAdminListResultBO> result = assembler.assembleAdminList(List.of(project));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Test Project");
        assertThat(result.get(0).getTypeName()).isEqualTo("Design");
        assertThat(result.get(0).getCreatorNickname()).isEqualTo("Alice");
    }

    @Test
    void assembleAdminList_missingTypeAndCreator_returnsNulls() {
        Project project = Project.builder()
                .id(1L).title("Orphan").typeId(99L)
                .status(ProjectStatus.IN_PROGRESS).creatorId(99L)
                .createdAt(LocalDateTime.now())
                .build();
        when(projectTypeMapper.selectBatchIds(anyList())).thenReturn(Collections.emptyList());
        when(userMapper.selectBatchIds(anyList())).thenReturn(Collections.emptyList());

        List<ProjectAdminListResultBO> result = assembler.assembleAdminList(List.of(project));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTypeName()).isNull();
        assertThat(result.get(0).getCreatorNickname()).isNull();
    }
}
