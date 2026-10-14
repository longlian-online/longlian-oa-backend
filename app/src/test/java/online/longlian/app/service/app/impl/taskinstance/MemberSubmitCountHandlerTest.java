package online.longlian.app.service.app.impl.taskinstance;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.mapper.ProjectMapper;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.pojo.entity.Project;
import online.longlian.common.enumeration.Status;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberSubmitCountHandlerTest {

    @Mock
    private OrganizationMemberMapper organizationMemberMapper;
    @Mock
    private ProjectMapper projectMapper;

    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("UTC"));
    private MemberSubmitCountHandler handler;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), OrganizationMember.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Project.class);
        handler = new MemberSubmitCountHandler(organizationMemberMapper, projectMapper, clock);
    }

    @Test
    void incrementSubmitCount_updatesLastSubmittedAt() {
        when(projectMapper.selectById(2L)).thenReturn(Project.builder().id(2L).orgId(1L).build());
        when(organizationMemberMapper.selectOne(any())).thenReturn(
                OrganizationMember.builder().id(10L).userId(100L).orgId(1L).status(Status.ENABLED).build());
        when(organizationMemberMapper.update(isNull(), any())).thenReturn(1);

        handler.incrementSubmitCount(100L, 2L);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<LambdaUpdateWrapper> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(organizationMemberMapper).update(isNull(), captor.capture());
        assertThat(captor.getValue().getSqlSet())
                .contains("submit_count = submit_count + 1", "last_submitted_at", "updated_at");
    }

    @Test
    void revertSubmitCount_doesNotRefreshLastSubmittedAt() {
        when(projectMapper.selectById(2L)).thenReturn(Project.builder().id(2L).orgId(1L).build());
        when(organizationMemberMapper.selectOne(any())).thenReturn(
                OrganizationMember.builder().id(10L).userId(100L).orgId(1L).status(Status.ENABLED).build());
        when(organizationMemberMapper.update(isNull(), any())).thenReturn(1);

        handler.revertSubmitCount(100L, 2L);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<LambdaUpdateWrapper> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(organizationMemberMapper).update(isNull(), captor.capture());
        String sqlSet = captor.getValue().getSqlSet();
        assertThat(sqlSet)
                .contains("submit_count = submit_count - 1", "updated_at")
                .doesNotContain("last_submitted_at");
    }
}
