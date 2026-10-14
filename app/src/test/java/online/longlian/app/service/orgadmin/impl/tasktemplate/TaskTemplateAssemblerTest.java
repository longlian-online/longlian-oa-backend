package online.longlian.app.service.orgadmin.impl.tasktemplate;

import online.longlian.app.mapper.BaseTaskMapper;
import online.longlian.app.mapper.TaskTemplateNodeMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateDetailResultBO;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateListResultBO;
import online.longlian.app.pojo.entity.BaseTask;
import online.longlian.app.pojo.entity.TaskTemplate;
import online.longlian.app.pojo.entity.TaskTemplateNode;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.service.common.TaskFormService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskTemplateAssemblerTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private BaseTaskMapper baseTaskMapper;
    @Mock
    private TaskTemplateNodeMapper taskTemplateNodeMapper;

    private TaskTemplateAssembler assembler;
    private TaskFormService taskFormService;

    @BeforeEach
    void setUp() {
        taskFormService = spy(new TaskFormService());
        assembler = new TaskTemplateAssembler(userMapper, baseTaskMapper, taskTemplateNodeMapper, taskFormService);
    }

    @Test
    void assembleList_emptyList_returnsEmpty() {
        assertThat(assembler.assembleList(Collections.emptyList())).isEmpty();
        verify(taskTemplateNodeMapper, never()).selectList(any());
        verify(taskFormService, never()).parseFields(any());
    }

    @Test
    void assembleList_withTemplates_returnsAssembled() {
        TaskTemplate template = TaskTemplate.builder()
                .id(1L).name("Template A").description("Desc")
                .status(Status.ENABLED).refCount(3).creatorId(5L)
                .createdAt(LocalDateTime.now())
                .build();
        User creator = User.builder().id(5L).nickname("Alice").build();
        when(userMapper.selectBatchIds(anyList())).thenReturn(List.of(creator));
        when(taskTemplateNodeMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<TaskTemplateListResultBO> result = assembler.assembleList(List.of(template));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Template A");
        assertThat(result.get(0).getCreatorNickname()).isEqualTo("Alice");
        assertThat(result.get(0).getRefCount()).isEqualTo(3);
        assertThat(result.get(0).getTaskCount()).isZero();
        assertThat(result.get(0).getNodes()).isEmpty();
        verify(baseTaskMapper, never()).selectBatchIds(anyList());
        verify(taskFormService, never()).parseFields(any());
    }

    @Test
    void assembleList_withNodes_returnsStagePreviewWithoutSubmitFields() {
        TaskTemplate template = TaskTemplate.builder()
                .id(1L).name("Template").status(Status.ENABLED).refCount(0).creatorId(5L)
                .createdAt(LocalDateTime.now())
                .build();
        when(userMapper.selectBatchIds(anyList())).thenReturn(List.of(
                User.builder().id(5L).nickname("Alice").build()));
        when(taskTemplateNodeMapper.selectList(any())).thenReturn(List.of(
                TaskTemplateNode.builder().id(11L).taskTemplateId(1L).baseTaskId(100L).sort(1).parallelSort(1).build(),
                TaskTemplateNode.builder().id(12L).taskTemplateId(1L).baseTaskId(200L).sort(1).parallelSort(2).build(),
                TaskTemplateNode.builder().id(13L).taskTemplateId(1L).baseTaskId(100L).sort(2).parallelSort(1).build()));
        when(baseTaskMapper.selectBatchIds(List.of(100L, 200L))).thenReturn(List.of(
                BaseTask.builder().id(100L).name("翻译").icon("Languages").metaSchema("[{\"key\":\"link\"}]").build(),
                BaseTask.builder().id(200L).name("校对").icon("BookOpen").metaSchema("[]").build()));

        List<TaskTemplateListResultBO> result = assembler.assembleList(List.of(template));

        assertThat(result).singleElement().satisfies(row -> {
            assertThat(row.getTaskCount()).isEqualTo(3);
            assertThat(row.getNodes()).extracting(node -> node.getBaseTaskName())
                    .containsExactly("翻译", "校对", "翻译");
            assertThat(row.getNodes()).extracting(node -> node.getBaseTaskIcon())
                    .containsExactly("Languages", "BookOpen", "Languages");
            assertThat(row.getNodes()).extracting(node -> node.getSort()).containsExactly(1, 1, 2);
            assertThat(row.getNodes()).extracting(node -> node.getParallelSort()).containsExactly(1, 2, 1);
        });
        verify(taskTemplateNodeMapper, times(1)).selectList(any());
        verify(baseTaskMapper, times(1)).selectBatchIds(List.of(100L, 200L));
        verify(taskFormService, never()).parseFields(any());
    }

    @Test
    void assembleList_missingBaseTask_leavesStageNameBlank() {
        TaskTemplate withNode = TaskTemplate.builder()
                .id(1L).name("残缺").status(Status.ENABLED).refCount(0).creatorId(5L)
                .createdAt(LocalDateTime.now())
                .build();
        TaskTemplate empty = TaskTemplate.builder()
                .id(2L).name("空").status(Status.ENABLED).refCount(0).creatorId(5L)
                .createdAt(LocalDateTime.now())
                .build();
        when(userMapper.selectBatchIds(anyList())).thenReturn(List.of(
                User.builder().id(5L).nickname("Alice").build()));
        when(taskTemplateNodeMapper.selectList(any())).thenReturn(List.of(
                TaskTemplateNode.builder().id(11L).taskTemplateId(1L).baseTaskId(999L).sort(1).parallelSort(1).build()));
        when(baseTaskMapper.selectBatchIds(List.of(999L))).thenReturn(Collections.emptyList());

        List<TaskTemplateListResultBO> result = assembler.assembleList(List.of(withNode, empty));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTaskCount()).isEqualTo(1);
        assertThat(result.get(0).getNodes()).singleElement().satisfies(node -> {
            assertThat(node.getBaseTaskId()).isEqualTo(999L);
            assertThat(node.getBaseTaskName()).isNull();
            assertThat(node.getBaseTaskIcon()).isNull();
        });
        assertThat(result.get(1).getTaskCount()).isZero();
        assertThat(result.get(1).getNodes()).isEmpty();
        verify(taskTemplateNodeMapper, times(1)).selectList(any());
        verify(taskFormService, never()).parseFields(any());
    }

    @Test
    void assembleDetail_withNodes_returnsFullDetail() {
        TaskTemplate template = TaskTemplate.builder()
                .id(1L).name("Template").description("Desc")
                .status(Status.ENABLED).refCount(1).creatorId(5L)
                .createdAt(LocalDateTime.now())
                .build();
        User creator = User.builder().id(5L).nickname("Bob").build();
        when(userMapper.selectBatchIds(anyList())).thenReturn(List.of(creator));

        TaskTemplateNode node = TaskTemplateNode.builder()
                .id(10L).taskTemplateId(1L).baseTaskId(100L).sort(1).parallelSort(0)
                .build();
        when(taskTemplateNodeMapper.selectList(any())).thenReturn(List.of(node));

        BaseTask baseTask = BaseTask.builder().id(100L).name("Draw")
                .icon("SquarePen").metaSchema("[]").build();
        when(baseTaskMapper.selectBatchIds(anyList())).thenReturn(List.of(baseTask));

        TaskTemplateDetailResultBO result = assembler.assembleDetail(template);

        assertThat(result.getName()).isEqualTo("Template");
        assertThat(result.getCreatorNickname()).isEqualTo("Bob");
        assertThat(result.getNodes()).hasSize(1);
        assertThat(result.getNodes().get(0).getBaseTaskName()).isEqualTo("Draw");
        assertThat(result.getNodes().get(0).getBaseTaskIcon()).isEqualTo("SquarePen");
    }

    @Test
    void assembleDetail_noNodes_returnsEmptyNodes() {
        TaskTemplate template = TaskTemplate.builder()
                .id(1L).name("Empty").creatorId(5L)
                .status(Status.ENABLED).refCount(0)
                .createdAt(LocalDateTime.now())
                .build();
        User creator = User.builder().id(5L).nickname("Eve").build();
        when(userMapper.selectBatchIds(anyList())).thenReturn(List.of(creator));
        when(taskTemplateNodeMapper.selectList(any())).thenReturn(Collections.emptyList());

        TaskTemplateDetailResultBO result = assembler.assembleDetail(template);

        assertThat(result.getNodes()).isEmpty();
    }
}
