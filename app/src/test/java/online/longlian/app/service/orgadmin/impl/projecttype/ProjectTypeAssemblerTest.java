package online.longlian.app.service.orgadmin.impl.projecttype;

import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeListResultBO;
import online.longlian.app.pojo.entity.ProjectType;
import online.longlian.app.pojo.entity.User;
import online.longlian.common.enumeration.Status;
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
class ProjectTypeAssemblerTest {

    @Mock
    private UserMapper userMapper;

    private ProjectTypeAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new ProjectTypeAssembler(userMapper);
    }

    @Test
    void assembleList_emptyList_returnsEmpty() {
        assertThat(assembler.assembleList(Collections.emptyList())).isEmpty();
    }

    @Test
    void assembleList_withTypes_returnsAssembled() {
        ProjectType type = ProjectType.builder()
                .id(1L).name("Design").status(Status.ENABLED)
                .creatorId(5L).createdAt(LocalDateTime.now())
                .build();
        User creator = User.builder().id(5L).nickname("Alice").build();
        when(userMapper.selectBatchIds(anyList())).thenReturn(List.of(creator));

        List<ProjectTypeListResultBO> result = assembler.assembleList(List.of(type));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Design");
        assertThat(result.get(0).getCreatorNickname()).isEqualTo("Alice");
    }

    @Test
    void assembleList_creatorNotFound_returnsNullNickname() {
        ProjectType type = ProjectType.builder()
                .id(1L).name("Dev").status(Status.ENABLED)
                .creatorId(99L).createdAt(LocalDateTime.now())
                .build();
        when(userMapper.selectBatchIds(anyList())).thenReturn(Collections.emptyList());

        List<ProjectTypeListResultBO> result = assembler.assembleList(List.of(type));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCreatorNickname()).isNull();
    }
}
