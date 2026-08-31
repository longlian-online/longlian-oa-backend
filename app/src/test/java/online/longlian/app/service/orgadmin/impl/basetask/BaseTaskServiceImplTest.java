package online.longlian.app.service.orgadmin.impl.basetask;

import online.longlian.app.mapper.BaseTaskMapper;
import online.longlian.app.pojo.bo.orgadmin.BaseTaskCreateParamsBO;
import online.longlian.app.pojo.entity.BaseTask;
import online.longlian.app.service.resource.ResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BaseTaskServiceImplTest {

    @Mock
    private BaseTaskMapper baseTaskMapper;

    @Mock
    private ResourceService resourceService;

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
                resourceService,
                clock,
                baseTaskQueryBuilder,
                baseTaskAssembler
        );
    }

    @Test
    void createBaseTask_withImageAndLucideIcon_persistsBoth() {
        doAnswer(invocation -> {
            BaseTask task = invocation.getArgument(0);
            task.setId(100L);
            return 1;
        }).when(baseTaskMapper).insert(any(BaseTask.class));

        service.createBaseTask(BaseTaskCreateParamsBO.builder()
                .orgId(1L)
                .creatorId(2L)
                .name("翻译")
                .description("翻译内容")
                .iconFileId(20L)
                .iconName("Languages")
                .metaSchema("[]")
                .build());

        ArgumentCaptor<BaseTask> taskCaptor = ArgumentCaptor.forClass(BaseTask.class);
        verify(baseTaskMapper).insert(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getIconFileId()).isEqualTo(20L);
        assertThat(taskCaptor.getValue().getIconName()).isEqualTo("Languages");
        verify(resourceService).bindBizId(20L, 100L, 2L, 1L);
    }
}
