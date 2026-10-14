package online.longlian.app.service.orgadmin.impl.projecttype;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import online.longlian.app.mapper.ProjectMapper;
import online.longlian.app.pojo.entity.Project;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeListResultBO;
import online.longlian.app.pojo.entity.ProjectType;
import online.longlian.app.pojo.entity.User;
import online.longlian.common.enumeration.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectTypeAssemblerTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private ProjectMapper projectMapper;

    private ProjectTypeAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new ProjectTypeAssembler(userMapper, projectMapper);
    }

    @Test
    void assembleList_emptyList_returnsEmpty() {
        assertThat(assembler.assembleList(Collections.emptyList(), 1L)).isEmpty();
    }

    @Test
    void assembleList_withTypes_returnsAssembledWithRefCount() {
        ProjectType type = ProjectType.builder()
                .id(1L).name("Design").status(Status.ENABLED)
                .creatorId(5L).createdAt(LocalDateTime.now())
                .build();
        User creator = User.builder().id(5L).nickname("Alice").build();
        when(userMapper.selectBatchIds(anyList())).thenReturn(List.of(creator));
        when(projectMapper.selectMaps(any())).thenReturn(List.of(Map.<String, Object>of("type_id", 1L, "ref_count", 2L)));

        List<ProjectTypeListResultBO> result = assembler.assembleList(List.of(type), 1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Design");
        assertThat(result.get(0).getCreatorNickname()).isEqualTo("Alice");
        assertThat(result.get(0).getRefCount()).isEqualTo(2);
        verify(projectMapper, never()).selectList(any());
    }

    @Test
    void assembleList_refCountQuery_groupsByTypeAndFiltersByOrg() {
        ProjectType type = ProjectType.builder()
                .id(1L).name("Design").status(Status.ENABLED)
                .creatorId(5L).createdAt(LocalDateTime.now())
                .build();
        when(userMapper.selectBatchIds(anyList())).thenReturn(Collections.emptyList());
        when(projectMapper.selectMaps(any())).thenReturn(Collections.emptyList());

        assembler.assembleList(List.of(type), 7L);

        ArgumentCaptor<QueryWrapper<Project>> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(projectMapper).selectMaps(captor.capture());
        QueryWrapper<Project> wrapper = captor.getValue();
        assertThat(wrapper.getSqlSelect()).contains("type_id").contains("COUNT(*)");
        String segment = wrapper.getCustomSqlSegment();
        assertThat(segment).contains("org_id").contains("type_id").contains("GROUP BY");
        assertThat(wrapper.getParamNameValuePairs().values()).contains(7L, 1L);
    }

    @Test
    void assembleList_creatorNotFound_returnsNullNicknameAndZeroRefCount() {
        ProjectType type = ProjectType.builder()
                .id(1L).name("Dev").status(Status.ENABLED)
                .creatorId(99L).createdAt(LocalDateTime.now())
                .build();
        when(userMapper.selectBatchIds(anyList())).thenReturn(Collections.emptyList());
        when(projectMapper.selectMaps(any())).thenReturn(Collections.emptyList());

        List<ProjectTypeListResultBO> result = assembler.assembleList(List.of(type), 1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCreatorNickname()).isNull();
        assertThat(result.get(0).getRefCount()).isZero();
    }
}
