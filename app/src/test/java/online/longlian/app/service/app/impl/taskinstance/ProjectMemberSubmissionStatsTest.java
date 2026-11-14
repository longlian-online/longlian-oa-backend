package online.longlian.app.service.app.impl.taskinstance;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.mapper.TaskSubmissionMapper;
import online.longlian.app.pojo.entity.TaskSubmission;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectMemberSubmissionStatsTest {

    @Mock
    private TaskSubmissionMapper taskSubmissionMapper;
    @InjectMocks
    private ProjectMemberSubmissionStats stats;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), TaskSubmission.class);
    }

    /** 当前页没有成员时不能发出空 IN 查询。 */
    @Test
    void shouldSkipQueryWhenPageHasNoMembers() {
        assertThat(stats.forMembers(1L, List.of())).isEmpty();
        verifyNoInteractions(taskSubmissionMapper);
    }

    /** 同一任务的多次有效提交只计一次，时间取最晚的一条。 */
    @Test
    void shouldCountDistinctTasksAndKeepLatestTime() {
        LocalDateTime earlier = LocalDateTime.of(2026, 3, 2, 9, 0);
        LocalDateTime middle = LocalDateTime.of(2026, 3, 3, 10, 0);
        LocalDateTime later = LocalDateTime.of(2026, 3, 4, 11, 0);
        when(taskSubmissionMapper.selectList(any())).thenReturn(List.of(
                row(1L, 10L, earlier),
                row(1L, 10L, later),
                row(1L, 11L, middle),
                row(2L, 12L, earlier)));

        var result = stats.forMembers(1L, List.of(1L, 2L, 3L));

        assertThat(result.get(1L).completedCount()).isEqualTo(2);
        assertThat(result.get(1L).lastSubmittedAt()).isEqualTo(later);
        assertThat(result.get(2L).completedCount()).isEqualTo(1);
        assertThat(result.get(2L).lastSubmittedAt()).isEqualTo(earlier);
        assertThat(result).doesNotContainKey(3L);
    }

    private static TaskSubmission row(long userId, long taskId, LocalDateTime createdAt) {
        return TaskSubmission.builder().submitterId(userId).taskInstanceId(taskId).createdAt(createdAt).build();
    }
}
