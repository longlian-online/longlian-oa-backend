package online.longlian.app.service.orgadmin.impl.orgmember;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.mapper.BaseTaskMapper;
import online.longlian.app.mapper.ItemTaskNodeMapper;
import online.longlian.app.mapper.TaskSubmissionMapper;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberBaseTaskSubmitCountResultBO;
import online.longlian.app.pojo.entity.BaseTask;
import online.longlian.app.pojo.entity.ItemTaskNode;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.pojo.entity.TaskSubmission;
import online.longlian.common.enumeration.Status;
import online.longlian.common.enumeration.TaskSubmissionStatus;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberSubmissionHandlerTest {

    @Mock
    private BaseTaskMapper baseTaskMapper;
    @Mock
    private ItemTaskNodeMapper itemTaskNodeMapper;
    @Mock
    private TaskSubmissionMapper taskSubmissionMapper;

    private MemberSubmissionHandler handler;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), BaseTask.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ItemTaskNode.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), TaskSubmission.class);
        handler = new MemberSubmissionHandler(
                baseTaskMapper, itemTaskNodeMapper, taskSubmissionMapper, new SubmissionCountAssembler());
    }

    @Test
    void getSubmitCounts_countsOnlySubmittedRows() {
        when(baseTaskMapper.selectList(any())).thenReturn(List.of(
                BaseTask.builder().id(100L).name("翻译").status(Status.ENABLED).build()));
        when(itemTaskNodeMapper.selectList(any())).thenReturn(List.of(
                ItemTaskNode.builder().id(10L).baseTaskId(100L).build()));
        when(taskSubmissionMapper.selectList(any())).thenReturn(List.of());

        handler.getSubmitCounts(OrganizationMember.builder().id(1L).userId(7L).orgId(1L).submitCount(1).build());

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<LambdaQueryWrapper> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(taskSubmissionMapper).selectList(captor.capture());
        LambdaQueryWrapper<?> wrapper = captor.getValue();
        assertThat(wrapper.getSqlSegment()).contains("submitter_id", "item_task_node_id", "status");
        assertThat(wrapper.getParamNameValuePairs().values())
                .contains(7L, TaskSubmissionStatus.SUBMITTED);
    }

    @Test
    void getSubmitCounts_returnsZeroWhenBaseTasksHaveNoNodes() {
        when(baseTaskMapper.selectList(any())).thenReturn(List.of(
                BaseTask.builder().id(100L).name("翻译").status(Status.ENABLED).build()));
        when(itemTaskNodeMapper.selectList(any())).thenReturn(List.of());

        OrgMemberBaseTaskSubmitCountResultBO result = handler.getSubmitCounts(
                OrganizationMember.builder().id(1L).userId(7L).orgId(1L).submitCount(1).build());

        assertThat(result.getTotalSubmitCount()).isEqualTo(1);
        assertThat(result.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getBaseTaskName()).isEqualTo("翻译");
            assertThat(item.getSubmitCount()).isZero();
        });
        verify(taskSubmissionMapper, never()).selectList(any());
    }

}
