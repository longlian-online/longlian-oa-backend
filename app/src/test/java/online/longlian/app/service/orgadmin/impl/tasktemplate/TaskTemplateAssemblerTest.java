package online.longlian.app.service.orgadmin.impl.tasktemplate;

import online.longlian.app.mapper.BaseTaskMapper;
import online.longlian.app.mapper.TaskTemplateNodeMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.common.ResourceReadUrlGetResultBO;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateDetailResultBO;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateListResultBO;
import online.longlian.app.pojo.entity.BaseTask;
import online.longlian.app.pojo.entity.TaskTemplate;
import online.longlian.app.pojo.entity.TaskTemplateNode;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.common.enumeration.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskTemplateAssemblerTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private BaseTaskMapper baseTaskMapper;
    @Mock
    private TaskTemplateNodeMapper taskTemplateNodeMapper;
    @Mock
    private ResourceService resourceService;

    private TaskTemplateAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new TaskTemplateAssembler(userMapper, baseTaskMapper, taskTemplateNodeMapper, resourceService);
    }

    @Test
    void assembleList_emptyList_returnsEmpty() {
        assertThat(assembler.assembleList(Collections.emptyList())).isEmpty();
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

        List<TaskTemplateListResultBO> result = assembler.assembleList(List.of(template));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Template A");
        assertThat(result.get(0).getCreatorNickname()).isEqualTo("Alice");
        assertThat(result.get(0).getRefCount()).isEqualTo(3);
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

        BaseTask baseTask = BaseTask.builder().id(100L).name("Draw").iconFileId(200L)
                .iconName("SquarePen").metaSchema("{}").build();
        when(baseTaskMapper.selectBatchIds(anyList())).thenReturn(List.of(baseTask));
        when(resourceService.getResourceReadUrls(anyList()))
                .thenReturn(Map.of(200L, new ResourceReadUrlGetResultBO("https://cdn/icon.png", 1L, "icon/200")));

        TaskTemplateDetailResultBO result = assembler.assembleDetail(template);

        assertThat(result.getName()).isEqualTo("Template");
        assertThat(result.getCreatorNickname()).isEqualTo("Bob");
        assertThat(result.getNodes()).hasSize(1);
        assertThat(result.getNodes().get(0).getBaseTaskName()).isEqualTo("Draw");
        assertThat(result.getNodes().get(0).getBaseTaskIconUrl()).isEqualTo("https://cdn/icon.png");
        assertThat(result.getNodes().get(0).getBaseTaskIconName()).isEqualTo("SquarePen");
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
