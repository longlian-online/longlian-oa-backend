package online.longlian.app.service.common;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.BaseTaskMapper;
import online.longlian.app.pojo.entity.BaseTask;
import online.longlian.common.enumeration.Status;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BaseTaskReferenceServiceTest {

    @Mock
    private BaseTaskMapper baseTaskMapper;

    private BaseTaskReferenceService service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), BaseTask.class);
        service = new BaseTaskReferenceService(baseTaskMapper);
    }

    // 不同节点顺序必须收敛为同一锁顺序，重复引用不能重复加锁。
    @Test
    void shouldLockDistinctEnabledTasksInAscendingOrder() {
        List<Long> lockedIds = new ArrayList<>();
        when(baseTaskMapper.selectOne(any())).thenAnswer(invocation -> {
            LambdaQueryWrapper<BaseTask> query = invocation.getArgument(0);
            query.getSqlSegment();
            Long id = (Long) query.getParamNameValuePairs().values().iterator().next();
            lockedIds.add(id);
            return BaseTask.builder().id(id).status(Status.ENABLED).build();
        });

        service.lockBaseTasks(Arrays.asList(20L, null, 10L, 20L));

        assertThat(lockedIds).containsExactly(10L, 20L);
    }

    // 缺失任务不能被新增引用。
    @Test
    void shouldRejectMissingTask() {
        when(baseTaskMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.lockBaseTasks(List.of(10L)))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode()));
    }

    // 禁用任务即使仍存在也不能用于新模板节点。
    @Test
    void shouldRejectDisabledTask() {
        when(baseTaskMapper.selectOne(any())).thenReturn(BaseTask.builder().id(10L).status(Status.DISABLED).build());

        assertThatThrownBy(() -> service.lockBaseTasks(List.of(10L)))
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
        BaseTaskReferenceService proxy = (BaseTaskReferenceService) factory.getProxy();

        assertThatThrownBy(() -> proxy.lockBaseTasks(List.of(10L)))
                .isInstanceOf(IllegalTransactionStateException.class);
        verifyNoInteractions(baseTaskMapper);
    }
}
