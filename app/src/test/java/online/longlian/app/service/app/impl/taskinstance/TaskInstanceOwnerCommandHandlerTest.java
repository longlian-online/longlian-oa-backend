package online.longlian.app.service.app.impl.taskinstance;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.ItemTaskNodeMapper;
import online.longlian.app.mapper.TaskInstanceMapper;
import online.longlian.app.mapper.TaskSubmissionMapper;
import online.longlian.app.pojo.entity.ItemTaskNode;
import online.longlian.app.pojo.entity.TaskInstance;
import online.longlian.app.pojo.entity.TaskSubmission;
import online.longlian.app.service.common.TaskFormService;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.common.enumeration.TaskInstanceStatus;
import online.longlian.common.enumeration.TaskSubmissionStatus;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskInstanceOwnerCommandHandlerTest {

    private static final LocalDateTime NOW = LocalDateTime.parse("2026-01-01T00:00:00");
    private static final LocalDateTime CLAIMED_AT = NOW.minusDays(2);
    private static final String REASON = "请重新核对术语和附件";

    @Mock
    private TaskInstanceMapper taskInstanceMapper;
    @Mock
    private TaskSubmissionMapper taskSubmissionMapper;
    @Mock
    private ItemTaskNodeMapper itemTaskNodeMapper;
    @Mock
    private MemberSubmitCountHandler memberSubmitCountHandler;
    @Mock
    private ResourceService resourceService;

    private TaskInstanceCommandHandler handler;

    @BeforeEach
    void setUp() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), TaskInstance.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), TaskSubmission.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), ItemTaskNode.class);
        handler = new TaskInstanceCommandHandler(taskInstanceMapper, taskSubmissionMapper, itemTaskNodeMapper,
                memberSubmitCountHandler, Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC),
                new TaskFormService(), resourceService);
    }

    @Test
    void shouldOwnerCancelClaimedInstanceAndClearOnlyCurrentAssignment() {
        TaskInstance instance = instance(false);
        givenStages(List.of(), List.of());
        when(taskInstanceMapper.update(isNull(), any())).thenReturn(1);

        handler.ownerCancel(instance, "7");

        LambdaUpdateWrapper<TaskInstance> update = capturedInstanceUpdate();
        assertTransition(update, TaskInstanceStatus.CLAIMED, TaskInstanceStatus.PENDING, 7L);
        assertThat(parameter(update, update.getSqlSet(), "assignee_id")).isNull();
        assertThat(parameter(update, update.getSqlSet(), "claimed_at")).isNull();
        assertThat(parameter(update, update.getSqlSet(), "updated_at")).isEqualTo(NOW);
        verifyNoInteractions(taskSubmissionMapper, memberSubmitCountHandler, resourceService);
        verifyNoMoreInteractions(taskInstanceMapper);
    }

    @Test
    void shouldOwnerRejectOnlyCurrentSubmissionWithReviewMetadataAndOneCountReversal() {
        TaskInstance instance = instance(true);
        givenStages(List.of(), List.of());
        TaskSubmission previous = submission(40L, TaskSubmissionStatus.REJECTED, 3);
        TaskSubmission current = submission(41L, TaskSubmissionStatus.SUBMITTED, 4);
        TaskSubmission reset = submission(42L, TaskSubmissionStatus.RESET, 5);
        givenSubmissionHistory(List.of(previous, current, reset));
        when(taskInstanceMapper.update(isNull(), any())).thenReturn(1);
        when(taskSubmissionMapper.update(isNull(), any())).thenReturn(1);

        handler.ownerReject(instance, 20L, "7", REASON);

        LambdaUpdateWrapper<TaskInstance> update = capturedInstanceUpdate();
        assertTransition(update, TaskInstanceStatus.COMPLETED, TaskInstanceStatus.CLAIMED, 7L);
        assertThat(update.getSqlSet()).doesNotContain("assignee_id", "claimed_at");
        assertThat(parameter(update, update.getSqlSet(), "submitted_at")).isNull();
        assertThat(parameter(update, update.getSqlSet(), "completed_at")).isNull();
        LambdaUpdateWrapper<TaskSubmission> submissionUpdate = capturedSubmissionUpdate();
        assertThat(parameter(submissionUpdate, submissionUpdate.getSqlSegment(), "id")).isEqualTo(41L);
        assertThat(parameter(submissionUpdate, submissionUpdate.getSqlSegment(), "task_instance_id")).isEqualTo(1L);
        assertThat(parameter(submissionUpdate, submissionUpdate.getSqlSegment(), "status"))
                .isEqualTo(TaskSubmissionStatus.SUBMITTED);
        assertThat(parameter(submissionUpdate, submissionUpdate.getSqlSet(), "status"))
                .isEqualTo(TaskSubmissionStatus.REJECTED);
        assertThat(parameter(submissionUpdate, submissionUpdate.getSqlSet(), "reviewer_id")).isEqualTo(20L);
        assertThat(parameter(submissionUpdate, submissionUpdate.getSqlSet(), "review_comment")).isEqualTo(REASON);
        assertThat(parameter(submissionUpdate, submissionUpdate.getSqlSet(), "reviewed_at")).isEqualTo(NOW);
        assertThat(parameter(submissionUpdate, submissionUpdate.getSqlSet(), "updated_at")).isEqualTo(NOW);
        // The submission's author is deliberately different from the preserved assignee.
        verify(memberSubmitCountHandler).revertSubmitCount(11L, 2L);
        verifyNoMoreInteractions(memberSubmitCountHandler);
        verify(taskSubmissionMapper, never()).insert(any(TaskSubmission.class));
        assertThat(previous.getStatus()).isEqualTo(TaskSubmissionStatus.REJECTED);
        assertThat(reset.getStatus()).isEqualTo(TaskSubmissionStatus.RESET);
        verifyNoInteractions(resourceService);
    }

    @ParameterizedTest
    @CsvSource({"false,PENDING", "false,COMPLETED", "true,PENDING", "true,CLAIMED"})
    void shouldRejectOwnerOperationFromIllegalStateWithoutWrites(boolean reject, TaskInstanceStatus status) {
        TaskInstance instance = instance(reject);
        instance.setStatus(status);

        assertOperationFails(() -> operate(reject, instance, "7"));

        verifyNoInteractions(taskInstanceMapper, itemTaskNodeMapper, taskSubmissionMapper,
                memberSubmitCountHandler, resourceService);
        assertThat(instance.getStatus()).isEqualTo(status);
        assertThat(instance.getRevision()).isEqualTo(7L);
    }

    @ParameterizedTest
    @CsvSource({"false,6", "false,8", "true,6", "true,8"})
    void shouldRejectStaleOrFutureRevisionBeforeAnySideEffect(boolean reject, String expectedRevision) {
        TaskInstance instance = instance(reject);

        assertOperationFails(() -> operate(reject, instance, expectedRevision));

        verifyNoInteractions(taskInstanceMapper, itemTaskNodeMapper, taskSubmissionMapper,
                memberSubmitCountHandler, resourceService);
        assertThat(instance.getRevision()).isEqualTo(7L);
        assertThat(instance.getStatus()).isEqualTo(reject ? TaskInstanceStatus.COMPLETED : TaskInstanceStatus.CLAIMED);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldCompareLargeRevisionExactlyWithoutFloatingPointLoss(boolean reject) {
        TaskInstance instance = instance(reject);
        instance.setRevision(9_007_199_254_740_993L);

        assertOperationFails(() -> operate(reject, instance, "9007199254740992"));

        verifyNoInteractions(taskInstanceMapper, itemTaskNodeMapper, taskSubmissionMapper,
                memberSubmitCountHandler, resourceService);
        assertThat(instance.getRevision()).isEqualTo(9_007_199_254_740_993L);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldRejectWhenNodeDisappearsBeforeDownstreamCheck(boolean reject) {
        TaskInstance instance = instance(reject);
        when(itemTaskNodeMapper.selectById(4L)).thenReturn(null);

        assertThatThrownBy(() -> operate(reject, instance, "7"))
                .isInstanceOfSatisfying(AppException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ResultCode.DATA_NOT_EXIT.getCode()));

        verifyNoInteractions(taskInstanceMapper, taskSubmissionMapper, memberSubmitCountHandler, resourceService);
        assertThat(instance.getRevision()).isEqualTo(7L);
        assertThat(instance.getStatus()).isEqualTo(reject ? TaskInstanceStatus.COMPLETED : TaskInstanceStatus.CLAIMED);
    }

    @ParameterizedTest
    @CsvSource({"false,3", "false,5", "true,3", "true,5"})
    void shouldRejectAnyLaterCompletedStageIncludingAnotherFlow(boolean reject, int completedSort) {
        TaskInstance instance = instance(reject);
        givenStages(List.of(node(6L, 3, 5L), node(8L, 5, 99L)), List.of(
                laterInstance(completedSort == 3 ? 6L : 8L, TaskInstanceStatus.COMPLETED)));

        assertOperationFails(() -> operate(reject, instance, "7"));

        verify(taskInstanceMapper, never()).update(isNull(), any());
        verifyNoInteractions(taskSubmissionMapper, memberSubmitCountHandler, resourceService);
        assertThat(instance.getRevision()).isEqualTo(7L);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldAllowOwnerOperationAndPreserveLaterClaimedStage(boolean reject) {
        TaskInstance instance = instance(reject);
        TaskInstance later = laterInstance(8L, TaskInstanceStatus.CLAIMED);
        givenStages(List.of(node(8L, 5, 99L)), List.of(later));
        givenSuccessfulWrite(reject);

        operate(reject, instance, "7");

        assertTransition(capturedInstanceUpdate(), instance(reject).getStatus(),
                reject ? TaskInstanceStatus.CLAIMED : TaskInstanceStatus.PENDING, 7L);
        assertThat(later.getStatus()).isEqualTo(TaskInstanceStatus.CLAIMED);
        assertThat(later.getAssigneeId()).isEqualTo(30L);
        assertThat(later.getClaimedAt()).isEqualTo(CLAIMED_AT);
        assertThat(later.getRevision()).isEqualTo(3L);
        if (!reject) {
            verifyNoInteractions(taskSubmissionMapper, memberSubmitCountHandler);
        }
        verifyNoInteractions(resourceService);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldNotBlockOwnerOperationForSameSortCompletedParallelSlot(boolean reject) {
        TaskInstance instance = instance(reject);
        TaskInstance parallel = laterInstance(6L, TaskInstanceStatus.COMPLETED);
        givenStages(List.of(node(6L, 2, 5L)), List.of(parallel));
        givenSuccessfulWrite(reject);

        operate(reject, instance, "7");

        assertTransition(capturedInstanceUpdate(), instance(reject).getStatus(),
                reject ? TaskInstanceStatus.CLAIMED : TaskInstanceStatus.PENDING, 7L);
        verify(taskInstanceMapper, never()).selectCount(any());
        assertThat(parallel.getStatus()).isEqualTo(TaskInstanceStatus.COMPLETED);
        assertThat(parallel.getRevision()).isEqualTo(3L);
        if (!reject) {
            verifyNoInteractions(taskSubmissionMapper, memberSubmitCountHandler);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldFailInstanceCasWithoutChangingSubmissionOrCounts(boolean reject) {
        TaskInstance instance = instance(reject);
        givenStages(List.of(), List.of());
        if (reject) {
            givenSubmissionHistory(List.of(submission(41L, TaskSubmissionStatus.SUBMITTED, 4)));
        }
        when(taskInstanceMapper.update(isNull(), any())).thenReturn(0);

        assertOperationFails(() -> operate(reject, instance, "7"));

        assertTransition(capturedInstanceUpdate(), instance(reject).getStatus(),
                reject ? TaskInstanceStatus.CLAIMED : TaskInstanceStatus.PENDING, 7L);
        verify(taskSubmissionMapper, never()).update(isNull(), any());
        verify(taskSubmissionMapper, never()).insert(any(TaskSubmission.class));
        verifyNoInteractions(memberSubmitCountHandler, resourceService);
        assertThat(instance.getRevision()).isEqualTo(7L);
        assertThat(instance.getStatus()).isEqualTo(instance(reject).getStatus());
    }

    @Test
    void shouldRejectWhenHistoryHasNoCurrentSubmittedVersion() {
        TaskInstance instance = instance(true);
        givenStages(List.of(), List.of());
        givenSubmissionHistory(List.of(submission(40L, TaskSubmissionStatus.REJECTED, 3),
                submission(42L, TaskSubmissionStatus.RESET, 5)));

        assertOperationFails(() -> handler.ownerReject(instance, 20L, "7", REASON));

        assertNoStateWrites();
    }

    @Test
    void shouldRejectWhenNoSubmissionHistoryExists() {
        TaskInstance instance = instance(true);
        givenStages(List.of(), List.of());
        givenSubmissionHistory(List.of());

        assertOperationFails(() -> handler.ownerReject(instance, 20L, "7", REASON));

        assertNoStateWrites();
    }

    @Test
    void shouldRejectMultipleCurrentSubmittedVersionsWithoutChoosingLatest() {
        TaskInstance instance = instance(true);
        givenStages(List.of(), List.of());
        givenSubmissionHistory(List.of(submission(41L, TaskSubmissionStatus.SUBMITTED, 4),
                submission(42L, TaskSubmissionStatus.SUBMITTED, 5)));

        assertOperationFails(() -> handler.ownerReject(instance, 20L, "7", REASON));

        assertNoStateWrites();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 2})
    void shouldFailSubmissionCasWithoutReversingCount(int updatedRows) {
        TaskInstance instance = instance(true);
        givenStages(List.of(), List.of());
        givenSubmissionHistory(List.of(submission(41L, TaskSubmissionStatus.SUBMITTED, 4)));
        when(taskInstanceMapper.update(isNull(), any())).thenReturn(1);
        when(taskSubmissionMapper.update(isNull(), any())).thenReturn(updatedRows);

        assertOperationFails(() -> handler.ownerReject(instance, 20L, "7", REASON));

        assertTransition(capturedInstanceUpdate(), TaskInstanceStatus.COMPLETED, TaskInstanceStatus.CLAIMED, 7L);
        LambdaUpdateWrapper<TaskSubmission> update = capturedSubmissionUpdate();
        assertThat(parameter(update, update.getSqlSegment(), "id")).isEqualTo(41L);
        assertThat(parameter(update, update.getSqlSegment(), "status")).isEqualTo(TaskSubmissionStatus.SUBMITTED);
        verifyNoInteractions(memberSubmitCountHandler, resourceService);
    }

    @ParameterizedTest
    @CsvSource({"false,0", "true,0", "false,9007199254740993", "true,9007199254740993"})
    void shouldAcceptExactZeroOrLargeRevisionAndIncrementOnce(boolean reject, long revision) {
        TaskInstance instance = instance(reject);
        instance.setRevision(revision);
        givenStages(List.of(), List.of());
        givenSuccessfulWrite(reject);

        operate(reject, instance, Long.toString(revision));

        assertTransition(capturedInstanceUpdate(), instance(reject).getStatus(),
                reject ? TaskInstanceStatus.CLAIMED : TaskInstanceStatus.PENDING, revision);
        if (reject) {
            verify(memberSubmitCountHandler).revertSubmitCount(11L, 2L);
            verifyNoMoreInteractions(memberSubmitCountHandler);
        } else {
            verifyNoInteractions(taskSubmissionMapper, memberSubmitCountHandler);
        }
    }

    private TaskInstance instance(boolean reject) {
        return TaskInstance.builder().id(1L).projectId(2L).itemId(3L).itemTaskNodeId(4L).taskFlowId(5L)
                .status(reject ? TaskInstanceStatus.COMPLETED : TaskInstanceStatus.CLAIMED)
                .assigneeId(10L).claimedAt(CLAIMED_AT).revision(7L)
                .submittedAt(reject ? NOW.minusDays(1) : null)
                .completedAt(reject ? NOW.minusDays(1) : null).build();
    }

    private ItemTaskNode node(long id, int sort, long flowId) {
        return ItemTaskNode.builder().id(id).itemId(3L).itemTaskFlowId(flowId).sort(sort).build();
    }

    private TaskInstance laterInstance(long nodeId, TaskInstanceStatus status) {
        return TaskInstance.builder().id(90L).projectId(2L).itemId(3L).itemTaskNodeId(nodeId)
                .status(status).assigneeId(30L).claimedAt(CLAIMED_AT).revision(3L).build();
    }

    private TaskSubmission submission(long id, TaskSubmissionStatus status, int version) {
        return TaskSubmission.builder().id(id).projectId(2L).itemId(3L).taskInstanceId(1L)
                .itemTaskNodeId(4L).submitterId(11L).status(status).versionNo(version).build();
    }

    private void givenStages(List<ItemTaskNode> nodes, List<TaskInstance> instances) {
        when(itemTaskNodeMapper.selectById(4L)).thenReturn(node(4L, 2, 5L));
        when(itemTaskNodeMapper.selectList(any())).thenAnswer(invocation -> {
            AbstractWrapper<?, ?, ?> query = invocation.getArgument(0);
            String sql = query.getSqlSegment();
            assertThat(parameter(query, sql, "item_id")).isEqualTo(3L);
            assertThat(sql).contains("sort >").doesNotContain("item_task_flow_id");
            Matcher sort = Pattern.compile("sort\\s*>\\s*#\\{ew\\.paramNameValuePairs\\.(MPGENVAL\\d+)\\}").matcher(sql);
            assertThat(sort.find()).isTrue();
            assertThat(query.getParamNameValuePairs().get(sort.group(1))).isEqualTo(2);
            List<ItemTaskNode> later = nodes.stream().filter(node -> node.getSort() > 2).toList();
            if (!later.isEmpty()) {
                when(taskInstanceMapper.selectCount(any())).thenAnswer(countInvocation -> {
                    AbstractWrapper<?, ?, ?> countQuery = countInvocation.getArgument(0);
                    String countSql = countQuery.getSqlSegment();
                    assertThat(parameter(countQuery, countSql, "item_id")).isEqualTo(3L);
                    assertThat(parameter(countQuery, countSql, "status")).isEqualTo(TaskInstanceStatus.COMPLETED);
                    assertThat(countSql).contains("item_task_node_id IN");
                    assertThat(countQuery.getParamNameValuePairs().values())
                            .containsAll(later.stream().map(ItemTaskNode::getId).toList());
                    return instances.stream().filter(value -> value.getStatus() == TaskInstanceStatus.COMPLETED)
                            .filter(value -> later.stream().anyMatch(node -> node.getId().equals(value.getItemTaskNodeId())))
                            .count();
                });
            }
            return later;
        });
    }

    private void givenSubmissionHistory(List<TaskSubmission> history) {
        when(taskSubmissionMapper.selectList(any())).thenAnswer(invocation -> {
            AbstractWrapper<?, ?, ?> query = invocation.getArgument(0);
            String sql = query.getSqlSegment();
            assertThat(parameter(query, sql, "task_instance_id")).isEqualTo(1L);
            assertThat(parameter(query, sql, "status")).isEqualTo(TaskSubmissionStatus.SUBMITTED);
            assertThat(sql).doesNotContain("LIMIT");
            return history.stream().filter(value -> value.getStatus() == TaskSubmissionStatus.SUBMITTED).toList();
        });
    }

    private void givenSuccessfulWrite(boolean reject) {
        when(taskInstanceMapper.update(isNull(), any())).thenReturn(1);
        if (reject) {
            givenSubmissionHistory(List.of(submission(41L, TaskSubmissionStatus.SUBMITTED, 4)));
            when(taskSubmissionMapper.update(isNull(), any())).thenReturn(1);
        }
    }

    private void operate(boolean reject, TaskInstance instance, String expectedRevision) {
        if (reject) {
            handler.ownerReject(instance, 20L, expectedRevision, REASON);
        } else {
            handler.ownerCancel(instance, expectedRevision);
        }
    }

    private void assertOperationFails(Runnable operation) {
        assertThatThrownBy(operation::run).isInstanceOfSatisfying(AppException.class,
                exception -> assertThat(exception.getCode()).isEqualTo(ResultCode.OPERATION_FAIL.getCode()));
    }

    private void assertNoStateWrites() {
        verify(taskInstanceMapper, never()).update(isNull(), any());
        verify(taskSubmissionMapper, never()).update(isNull(), any());
        verify(taskSubmissionMapper, never()).insert(any(TaskSubmission.class));
        verifyNoInteractions(memberSubmitCountHandler, resourceService);
    }

    private void assertTransition(LambdaUpdateWrapper<TaskInstance> update, TaskInstanceStatus from,
                                  TaskInstanceStatus to, long revision) {
        String condition = update.getSqlSegment();
        assertThat(parameter(update, condition, "id")).isEqualTo(1L);
        assertThat(parameter(update, condition, "status")).isEqualTo(from);
        assertThat(parameter(update, condition, "revision")).isEqualTo(revision);
        assertThat(parameter(update, update.getSqlSet(), "status")).isEqualTo(to);
        assertThat(update.getSqlSet().replaceAll("\\s+", "")).contains("revision=revision+1");
    }

    private Object parameter(AbstractWrapper<?, ?, ?> wrapper, String sql, String column) {
        Matcher matcher = Pattern.compile("(?<![a-z_])" + Pattern.quote(column)
                + "\\s*=\\s*#\\{ew\\.paramNameValuePairs\\.(MPGENVAL\\d+)\\}").matcher(sql);
        assertThat(matcher.find()).as("bound SQL parameter for %s in %s", column, sql).isTrue();
        return wrapper.getParamNameValuePairs().get(matcher.group(1));
    }

    @SuppressWarnings("unchecked")
    private LambdaUpdateWrapper<TaskInstance> capturedInstanceUpdate() {
        ArgumentCaptor<Wrapper<TaskInstance>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(taskInstanceMapper).update(isNull(), captor.capture());
        return (LambdaUpdateWrapper<TaskInstance>) captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private LambdaUpdateWrapper<TaskSubmission> capturedSubmissionUpdate() {
        ArgumentCaptor<Wrapper<TaskSubmission>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(taskSubmissionMapper).update(isNull(), captor.capture());
        return (LambdaUpdateWrapper<TaskSubmission>) captor.getValue();
    }
}
