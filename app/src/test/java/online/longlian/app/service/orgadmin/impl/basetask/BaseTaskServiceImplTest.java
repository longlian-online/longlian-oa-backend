package online.longlian.app.service.orgadmin.impl.basetask;

import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.BaseTaskMapper;
import online.longlian.app.mapper.ItemTaskNodeMapper;
import online.longlian.app.mapper.TaskTemplateNodeMapper;
import online.longlian.app.pojo.bo.orgadmin.BaseTaskDeleteParamsBO;
import online.longlian.app.pojo.entity.BaseTask;
import online.longlian.app.service.common.TaskFormService;
import online.longlian.common.enumeration.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
}
