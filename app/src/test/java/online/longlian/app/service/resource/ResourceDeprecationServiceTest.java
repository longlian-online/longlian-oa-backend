package online.longlian.app.service.resource;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.mapper.ResourceMapper;
import online.longlian.app.pojo.entity.Resource;
import online.longlian.common.enumeration.FileProcessStatus;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class ResourceDeprecationServiceTest {

    @Mock
    private ResourceMapper resourceMapper;

    private ResourceDeprecationService service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Resource.class);
        service = new ResourceDeprecationService(resourceMapper);
    }

    @Test
    void deprecateUnreferenced_marksEachActivatedResourceWithExecuteTime() {
        LocalDateTime executeTime = LocalDateTime.of(2026, 11, 7, 3, 4, 5);
        when(resourceMapper.selectList(any())).thenReturn(List.of(resource(4L), resource(9L)), List.of());

        service.deprecateUnreferenced(executeTime);

        verify(resourceMapper).deprecateIfUnreferenced(4L, executeTime,
                FileProcessStatus.Activated.getCode(), FileProcessStatus.Deprecated.getCode());
        verify(resourceMapper).deprecateIfUnreferenced(9L, executeTime,
                FileProcessStatus.Activated.getCode(), FileProcessStatus.Deprecated.getCode());
        ArgumentCaptor<Wrapper<Resource>> query = ArgumentCaptor.forClass(Wrapper.class);
        verify(resourceMapper).selectList(query.capture());
        assertThat(query.getAllValues().get(0).getSqlSegment()).contains("process_status", "LIMIT");
    }

    @Test
    void deprecateUnreferenced_scansPastAFullBatch() {
        List<Resource> firstPage = new ArrayList<>();
        for (long id = 1; id <= 100; id++) {
            firstPage.add(resource(id));
        }
        when(resourceMapper.selectList(any())).thenReturn(firstPage, List.of(resource(101L)));

        service.deprecateUnreferenced(LocalDateTime.of(2026, 11, 7, 0, 0));

        verify(resourceMapper, times(101)).deprecateIfUnreferenced(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());
        verify(resourceMapper, times(2)).selectList(any());
    }

    @Test
    void deprecateUnreferenced_doesNothingWhenNoActivatedResource() {
        when(resourceMapper.selectList(any())).thenReturn(List.of());

        service.deprecateUnreferenced(LocalDateTime.of(2026, 11, 7, 0, 0));

        verify(resourceMapper, never()).deprecateIfUnreferenced(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void deprecateUnreferenced_logsOnlyResourcesThatWereDeprecated(CapturedOutput output) {
        LocalDateTime executeTime = LocalDateTime.of(2026, 11, 7, 3, 4, 5);
        when(resourceMapper.selectList(any())).thenReturn(List.of(
                resource(4L, "cover", 20L, 1L), resource(9L, "avatar", 3L, 1L)));
        when(resourceMapper.deprecateIfUnreferenced(org.mockito.ArgumentMatchers.eq(4L), org.mockito.ArgumentMatchers.eq(executeTime),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(1);
        when(resourceMapper.deprecateIfUnreferenced(org.mockito.ArgumentMatchers.eq(9L), org.mockito.ArgumentMatchers.eq(executeTime),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(0);

        service.deprecateUnreferenced(executeTime);

        assertThat(output).contains("资源弃置: id=4, bizType=cover, bizId=20, orgId=1, executeTime=2026-11-07T03:04:05")
                .contains("资源弃置本轮完成: count=1, executeTime=2026-11-07T03:04:05")
                .doesNotContain("id=9");
    }

    private Resource resource(long id) {
        return resource(id, "cover", id, 1L);
    }

    private Resource resource(long id, String bizType, long bizId, long orgId) {
        return Resource.builder().id(id).bizType(bizType).bizId(bizId).orgId(orgId)
                .processStatus(FileProcessStatus.Activated).build();
    }
}
