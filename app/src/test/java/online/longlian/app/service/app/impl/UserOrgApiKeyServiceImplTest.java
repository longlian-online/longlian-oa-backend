package online.longlian.app.service.app.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.mapper.UserOrgApiKeyMapper;
import online.longlian.app.pojo.bo.app.UserOrgApiKeyParamsBO;
import online.longlian.app.pojo.bo.common.OrganizationMembershipStatusResultBO;
import online.longlian.app.pojo.bo.common.OrganizationMembershipParamsBO;
import online.longlian.app.pojo.entity.UserOrgApiKey;
import online.longlian.app.service.common.OrganizationMembershipService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserOrgApiKeyServiceImplTest {

    private final UserOrgApiKeyMapper mapper = mock(UserOrgApiKeyMapper.class);
    private final OrganizationMembershipService membership = mock(OrganizationMembershipService.class);
    private final UserOrgApiKeyParamsBO params = new UserOrgApiKeyParamsBO(7L, 12L);
    private final UserOrgApiKeyServiceImpl service = new UserOrgApiKeyServiceImpl(membership,
            Clock.fixed(Instant.parse("2026-11-10T00:00:00Z"), ZoneOffset.UTC));

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), UserOrgApiKey.class);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        when(membership.getOwnMembershipStatus(new OrganizationMembershipParamsBO(7L, 12L))).thenReturn(new OrganizationMembershipStatusResultBO(true, true, true));
    }

    // 原文只作为生成结果返回，持久化对象中只有摘要和展示前缀。
    @Test
    void shouldGenerateWithoutStoringPlaintext() {
        doAnswer(invocation -> {
            ((UserOrgApiKey) invocation.getArgument(0)).setId(99L);
            return 1;
        }).when(mapper).insert(any(UserOrgApiKey.class));
        var result = service.generate(params);
        var captor = org.mockito.ArgumentCaptor.forClass(UserOrgApiKey.class);
        verify(mapper).insert(captor.capture());
        assertThat(result.apiKey()).matches("ll_mcp_[A-Za-z0-9_-]{43}");
        assertThat(captor.getValue().getKeyHash()).hasSize(64).isNotEqualTo(result.apiKey());
        assertThat(result.toString()).doesNotContain(result.apiKey());
        assertThat(result.id()).isEqualTo(99L);
    }

    // 禁用时允许查看状态和撤销能力，但不允许重新生成。
    @Test
    void shouldExposeSuspensionWithoutGeneratingAnotherKey() {
        when(membership.getOwnMembershipStatus(new OrganizationMembershipParamsBO(7L, 12L))).thenReturn(new OrganizationMembershipStatusResultBO(true, true, false));
        when(mapper.selectOne(any())).thenReturn(UserOrgApiKey.builder().id(99L).keyHash("hash").keyPrefix("prefix").build());
        var result = service.getCurrent(params);
        assertThat(result.state()).isEqualTo("SUSPENDED");
        assertThat(result.canRevoke()).isTrue();
        assertThat(result.canGenerate()).isFalse();
        assertThatThrownBy(() -> service.generate(params)).isInstanceOf(AppException.class);
        verify(mapper, never()).insert(any(UserOrgApiKey.class));
    }

    // 已撤销凭据不会因为组织或成员重新启用而恢复。
    @Test
    void shouldKeepRevokedStateWhenMembershipIsRestored() {
        when(mapper.selectOne(any())).thenReturn(UserOrgApiKey.builder().id(99L).keyHash(null).keyPrefix("prefix").build());
        assertThat(service.getCurrent(params).state()).isEqualTo("REVOKED");
        assertThat(service.getCurrent(params).canRevoke()).isFalse();
    }

    // 非法格式不查库，未知摘要也不产生认证身份。
    @Test
    void shouldRejectInvalidOrUnknownKeys() {
        assertThatThrownBy(() -> service.authenticate("invalid")).isInstanceOf(AppException.class);
        verifyNoInteractions(mapper);
        assertThatThrownBy(() -> service.authenticate("ll_mcp_" + "x".repeat(43))).isInstanceOf(AppException.class);
    }

    // 认证身份由数据库记录确定，不能由调用方指定用户或组织。
    @Test
    void shouldAuthenticateStoredIdentityWithCurrentMembership() {
        when(mapper.selectOne(any())).thenReturn(UserOrgApiKey.builder().id(99L).userId(7L).orgId(12L).build());
        var result = service.authenticate("ll_mcp_" + "x".repeat(43));
        assertThat(result.userId()).isEqualTo(7L);
        assertThat(result.orgId()).isEqualTo(12L);
        verify(membership).getOwnMembershipStatus(new OrganizationMembershipParamsBO(7L, 12L));
    }
}
