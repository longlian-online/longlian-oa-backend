package online.longlian.app.service.orgadmin.impl.orgmember;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.common.enumeration.Status;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberStatusHandlerTest {

    @Mock
    private OrganizationMemberMapper organizationMemberMapper;

    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("UTC"));
    private MemberStatusHandler handler;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), OrganizationMember.class);
        handler = new MemberStatusHandler(organizationMemberMapper, clock);
    }

    @Test
    void getAndValidateMember_notFound_throws() {
        when(organizationMemberMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> handler.getAndValidateMember(99L, 1L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("成员不存在");
    }

    @Test
    void getAndValidateMember_wrongOrg_throws() {
        OrganizationMember member = OrganizationMember.builder().id(1L).orgId(2L).build();
        when(organizationMemberMapper.selectById(1L)).thenReturn(member);

        assertThatThrownBy(() -> handler.getAndValidateMember(1L, 99L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("成员不存在");
    }

    @Test
    void getAndValidateMember_valid_returnsMember() {
        OrganizationMember member = OrganizationMember.builder().id(1L).orgId(10L).build();
        when(organizationMemberMapper.selectById(1L)).thenReturn(member);

        OrganizationMember result = handler.getAndValidateMember(1L, 10L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void validateNotAdminDisable_adminDisable_throws() {
        OrganizationMember admin = OrganizationMember.builder().id(1L).orgRole("ORG_ADMIN").build();

        assertThatThrownBy(() -> handler.validateNotAdminDisable(admin, Status.DISABLED))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("管理员不可被禁用");
    }

    @Test
    void validateNotAdminDisable_adminEnable_noException() {
        OrganizationMember admin = OrganizationMember.builder().id(1L).orgRole("ORG_ADMIN").build();
        handler.validateNotAdminDisable(admin, Status.ENABLED);
    }

    @Test
    void validateNotAdminDisable_normalUserDisable_noException() {
        OrganizationMember user = OrganizationMember.builder().id(1L).orgRole("org_user").build();
        handler.validateNotAdminDisable(user, Status.DISABLED);
    }

    @Test
    void updateMemberStatus_callsMapper() {
        OrganizationMember member = OrganizationMember.builder().id(1L).build();
        when(organizationMemberMapper.update(isNull(), any())).thenReturn(1);

        handler.updateMemberStatus(member, Status.DISABLED);

        verify(organizationMemberMapper).update(isNull(), any());
    }
}
