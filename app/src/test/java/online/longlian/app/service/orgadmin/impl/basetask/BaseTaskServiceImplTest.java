package online.longlian.app.service.orgadmin.impl.basetask;

import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.BaseTaskMapper;
import online.longlian.app.mapper.ItemTaskNodeMapper;
import online.longlian.app.mapper.TaskTemplateNodeMapper;
import online.longlian.app.pojo.bo.orgadmin.BaseTaskDeleteParamsBO;
import online.longlian.app.pojo.bo.orgadmin.BaseTaskUpdateParamsBO;
import online.longlian.app.pojo.entity.BaseTask;
import online.longlian.app.service.common.TaskFormService;
import online.longlian.common.enumeration.Status;
import online.longlian.app.service.orgadmin.BaseTaskService;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import online.longlian.app.pojo.bo.orgadmin.PlazaTaskTypeOptionResultBO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BaseTaskServiceImplTest {

    @Mock
    private BaseTaskMapper baseTaskMapper;
    @Mock
    private TaskTemplateNodeMapper taskTemplateNodeMapper;
    @Mock
    private ItemTaskNodeMapper itemTaskNodeMapper;
    @Mock
    private BaseTaskQueryBuilder baseTaskQueryBuilder;
    @Mock
    private BaseTaskAssembler baseTaskAssembler;
    private BaseTaskServiceImpl service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-31T00:00:00Z"), ZoneOffset.UTC);
        service = new BaseTaskServiceImpl(
                baseTaskMapper,
                taskTemplateNodeMapper,
                itemTaskNodeMapper,
                clock,
                baseTaskQueryBuilder,
                baseTaskAssembler,
                new TaskFormService()
        );
    }

    @Test
    void listPlazaTaskTypes_readsOnlyRequestedOrganization() {
        var options = List.of(new PlazaTaskTypeOptionResultBO(7L, "translate"));
        when(baseTaskMapper.selectPlazaTaskTypes(2L)).thenReturn(options);
        assertThat(service.listPlazaTaskTypes(2L)).containsExactlyElementsOf(options);
        verify(baseTaskMapper).selectPlazaTaskTypes(2L);
        verifyNoInteractions(itemTaskNodeMapper, taskTemplateNodeMapper);
    }

    @Test
    void updateBaseTask_missingTask_rejectsWithoutUpdating() {
        assertThatThrownBy(() -> service.updateBaseTask(updateParams()))
                .isInstanceOfSatisfying(AppException.class,
                        error -> assertThat(error.getCode()).isEqualTo(ResultCode.DATA_NOT_EXIT.getCode()));
        verify(baseTaskMapper, never()).update(any(), any());
    }

    @Test
    void updateBaseTask_otherOrganization_rejectsWithoutUpdating() {
        when(baseTaskMapper.selectOne(any())).thenReturn(BaseTask.builder().id(10L).orgId(2L).build());
        assertThatThrownBy(() -> service.updateBaseTask(updateParams()))
                .isInstanceOfSatisfying(AppException.class,
                        error -> assertThat(error.getCode()).isEqualTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
        verify(baseTaskMapper, never()).update(any(), any());
    }

    private BaseTaskUpdateParamsBO updateParams() {
        return BaseTaskUpdateParamsBO.builder().taskId(10L).orgId(1L)
                .name("翻译").icon("Languages").submitFields(List.of()).build();
    }


    @Test
    void deleteBaseTask_withoutReferences_softDeletesTask() {
        when(baseTaskMapper.selectOne(any())).thenReturn(BaseTask.builder().id(10L).orgId(1L).build());
        when(taskTemplateNodeMapper.selectCount(any())).thenReturn(0L);
        when(itemTaskNodeMapper.selectCount(any())).thenReturn(0L);

        service.deleteBaseTask(BaseTaskDeleteParamsBO.builder().taskId(10L).orgId(1L).build());

        verify(baseTaskMapper).deleteById(10L);
    }

    @Test
    void deleteBaseTask_referencedByTemplateOrItem_rejectsWithoutDelete() {
        when(baseTaskMapper.selectOne(any())).thenReturn(BaseTask.builder().id(10L).orgId(1L).status(Status.ENABLED).build());
        when(taskTemplateNodeMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.deleteBaseTask(BaseTaskDeleteParamsBO.builder().taskId(10L).orgId(1L).build()))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode()));
        verify(baseTaskMapper, never()).deleteById(10L);
    }

    @Test
    void deleteBaseTask_fromAnotherOrganization_rejectsWithoutDelete() {
        when(baseTaskMapper.selectOne(any())).thenReturn(BaseTask.builder().id(10L).orgId(2L).build());

        assertThatThrownBy(() -> service.deleteBaseTask(BaseTaskDeleteParamsBO.builder().taskId(10L).orgId(1L).build()))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
        verify(baseTaskMapper, never()).deleteById(10L);
    }
    // 空引用集合（如空模板）不加锁，也不应产生查询。
    @Test
    void shouldSkipLockingWhenTaskIdsEmpty() {
        service.lockBaseTasks(1L, List.of());

        verifyNoInteractions(baseTaskMapper);
    }

    // 部分任务存在也不能接受整个引用集合，重复 ID 不应掩盖缺失任务。
    @Test
    void shouldRejectPartiallyMissingTasks() {
        when(baseTaskMapper.selectList(any())).thenReturn(List.of(
                BaseTask.builder().id(10L).status(Status.ENABLED).build()));

        assertThatThrownBy(() -> service.lockBaseTasks(1L, List.of(20L, 10L, 20L)))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode()));
    }

    // 缺失任务不能被新增引用。
    @Test
    void shouldRejectMissingTask() {
        when(baseTaskMapper.selectList(any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.lockBaseTasks(1L, List.of(10L)))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode()));
    }

    // 禁用任务即使仍存在也不能用于新模板节点。
    @Test
    void shouldRejectDisabledTask() {
        when(baseTaskMapper.selectList(any())).thenReturn(List.of(
                BaseTask.builder().id(10L).status(Status.ENABLED).build(),
                BaseTask.builder().id(20L).status(Status.DISABLED).build()));

        assertThatThrownBy(() -> service.lockBaseTasks(1L, List.of(10L, 20L)))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode()));
    }

    // 脱离引用写入事务会提前释放锁，必须在执行查询前拒绝。
    @Test
    void shouldRejectLockingOutsideCallerTransaction() {
        ProxyFactory factory = new ProxyFactory(service);
        factory.addAdvice(new TransactionInterceptor(
                new DataSourceTransactionManager(new DriverManagerDataSource()),
                new AnnotationTransactionAttributeSource()));
        BaseTaskService proxy = (BaseTaskService) factory.getProxy();

        assertThatThrownBy(() -> proxy.lockBaseTasks(1L, List.of(10L)))
                .isInstanceOf(IllegalTransactionStateException.class);
        verifyNoInteractions(baseTaskMapper);
    }

    @Test
    void shouldRejectTaskFromAnotherOrganization() {
        when(baseTaskMapper.selectList(any())).thenReturn(List.of(
                BaseTask.builder().id(10L).orgId(1L).name("本组织任务").status(Status.ENABLED).build(),
                BaseTask.builder().id(20L).orgId(2L).name("组织B机密任务").status(Status.ENABLED).build()));

        assertThatThrownBy(() -> service.lockBaseTasks(1L, List.of(10L, 20L)))
                .isInstanceOfSatisfying(AppException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo(ResultCode.UNAUTHORIZED_OPERATION.getCode());
                    assertThat(ex.getMsg()).doesNotContain("组织B机密任务");
                });
    }

    @Test
    void shouldRejectLockWhenOrganizationMissing() {
        when(baseTaskMapper.selectList(any())).thenReturn(List.of(
                BaseTask.builder().id(10L).orgId(1L).status(Status.ENABLED).build()));

        assertThatThrownBy(() -> service.lockBaseTasks(null, List.of(10L)))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
    }
}
