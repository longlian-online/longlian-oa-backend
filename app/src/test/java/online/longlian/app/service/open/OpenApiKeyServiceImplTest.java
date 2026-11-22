package online.longlian.app.service.open;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.properties.OpenApiProperties;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.util.Sha256Util;
import online.longlian.app.mapper.OpenApiKeyMapper;
import online.longlian.app.mapper.OrganizationMapper;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.pojo.bo.open.OpenApiKeyAuthResultBO;
import online.longlian.app.pojo.bo.open.OpenApiKeyCreateParamsBO;
import online.longlian.app.pojo.bo.open.OpenApiKeyCreateResultBO;
import online.longlian.app.pojo.bo.open.OpenApiKeyRevokeParamsBO;
import online.longlian.app.pojo.entity.OpenApiKey;
import online.longlian.app.pojo.entity.Organization;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.service.lock.LockService;
import online.longlian.common.enumeration.OpenApiKeyStatus;
import online.longlian.common.enumeration.Status;
import online.longlian.common.service.DistributedLockService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpenApiKeyServiceImplTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("Asia/Shanghai"));

    private OpenApiKeyMapper openApiKeyMapper;
    private OrganizationMapper organizationMapper;
    private OrganizationMemberMapper organizationMemberMapper;
    private LockService lockService;
    private OpenApiProperties properties;
    private OpenApiKeyServiceImpl service;

    @BeforeEach
    void setUp() {
        // LambdaUpdateWrapper.set 会即时解析列名，单测环境需先注册表信息
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), OpenApiKey.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), OrganizationMember.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Organization.class);
        openApiKeyMapper = mock(OpenApiKeyMapper.class);
        organizationMapper = mock(OrganizationMapper.class);
        organizationMemberMapper = mock(OrganizationMemberMapper.class);
        lockService = mock(LockService.class);
        properties = new OpenApiProperties();
        service = new OpenApiKeyServiceImpl(openApiKeyMapper, organizationMapper, organizationMemberMapper,
                lockService, properties, CLOCK);
    }

    private void lockAvailable() {
        when(lockService.tryAcquireOrThrow(anyString(), anyLong(), anyLong(), any()))
                .thenReturn(mock(DistributedLockService.Lock.class));
    }

    private OpenApiKey key(OpenApiKeyStatus status) {
        return OpenApiKey.builder()
                .id(1L).orgId(8L).userId(9L).name("脚本").keyHash("hash")
                .status(status).build();
    }

    private Organization organization(Status status) {
        Organization org = new Organization();
        org.setStatus(status);
        return org;
    }

    @Test
    void shouldCreateKeyAndStoreOnlyItsSha256() {
        lockAvailable();
        when(openApiKeyMapper.selectCount(any())).thenReturn(0L);
        when(openApiKeyMapper.insert(any(OpenApiKey.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, OpenApiKey.class).setId(55L);
            return 1;
        });

        OpenApiKeyCreateResultBO result = service.create(new OpenApiKeyCreateParamsBO(9L, 8L, "脚本"));

        assertThat(result.id()).isEqualTo(55L);
        assertThat(result.key()).startsWith("oa_").hasSize(46);
        ArgumentCaptor<OpenApiKey> captor = ArgumentCaptor.forClass(OpenApiKey.class);
        verify(openApiKeyMapper).insert(captor.capture());
        assertThat(captor.getValue().getKeyHash()).isEqualTo(Sha256Util.sha256Hex(result.key()));
        assertThat(captor.getValue().getUserId()).isEqualTo(9L);
        assertThat(captor.getValue().getOrgId()).isEqualTo(8L);
        assertThat(captor.getValue().getName()).isEqualTo("脚本");
        assertThat(captor.getValue().getStatus()).isEqualTo(OpenApiKeyStatus.ENABLED);
    }

    @Test
    void shouldFailCreateWhenOrgQuotaReached() {
        lockAvailable();
        when(openApiKeyMapper.selectCount(any())).thenReturn((long) properties.getMaxKeysPerOrg());

        assertThatThrownBy(() -> service.create(new OpenApiKeyCreateParamsBO(9L, 8L, "脚本")))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("已达上限");
        verify(openApiKeyMapper, never()).insert(any(OpenApiKey.class));
    }

    @Test
    void shouldFailCreateWhenQuotaLockUnavailable() {
        // LockService 在拿不到锁时抛 AppException；mock 直接模拟这一行为
        when(lockService.tryAcquireOrThrow(anyString(), anyLong(), anyLong(), any()))
                .thenThrow(new AppException(ResultCode.OPERATION_FAIL));

        assertThatThrownBy(() -> service.create(new OpenApiKeyCreateParamsBO(9L, 8L, "脚本")))
                .isInstanceOf(AppException.class);
        verify(openApiKeyMapper, never()).insert(any(OpenApiKey.class));
    }

    @Test
    void shouldResolveUserAndOrgWhenKeyAndMembershipActive() {
        when(openApiKeyMapper.selectOne(any())).thenReturn(key(OpenApiKeyStatus.ENABLED));
        when(organizationMemberMapper.selectOne(any())).thenReturn(new OrganizationMember());
        when(organizationMapper.selectById(8L)).thenReturn(organization(Status.ENABLED));

        OpenApiKeyAuthResultBO auth = service.authenticate("oa_valid");

        assertThat(auth.keyId()).isEqualTo(1L);
        assertThat(auth.userId()).isEqualTo(9L);
        assertThat(auth.orgId()).isEqualTo(8L);
    }

    @Test
    void shouldFailAuthenticateWithSameErrorForUnknownKey() {
        when(openApiKeyMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.authenticate("oa_unknown"))
                .isInstanceOf(AppException.class)
                .hasMessage("密钥无效或已吊销");
    }

    @Test
    void shouldFailAuthenticateForRevokedKey() {
        when(openApiKeyMapper.selectOne(any())).thenReturn(key(OpenApiKeyStatus.REVOKED));

        assertThatThrownBy(() -> service.authenticate("oa_revoked"))
                .isInstanceOf(AppException.class)
                .hasMessage("密钥无效或已吊销");
    }

    @Test
    void shouldFailAuthenticateWhenMembershipInactive() {
        when(openApiKeyMapper.selectOne(any())).thenReturn(key(OpenApiKeyStatus.ENABLED));
        when(organizationMemberMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.authenticate("oa_left"))
                .isInstanceOf(AppException.class)
                .hasMessage("密钥无效或已吊销");
    }

    @Test
    void shouldFailAuthenticateWhenOrganizationDisabled() {
        when(openApiKeyMapper.selectOne(any())).thenReturn(key(OpenApiKeyStatus.ENABLED));
        when(organizationMemberMapper.selectOne(any())).thenReturn(new OrganizationMember());
        when(organizationMapper.selectById(8L)).thenReturn(organization(Status.DISABLED));

        assertThatThrownBy(() -> service.authenticate("oa_disabled_org"))
                .isInstanceOf(AppException.class)
                .hasMessage("密钥无效或已吊销");
    }

    @Test
    void shouldFailRevokeKeyOfOtherUserOrOrg() {
        when(openApiKeyMapper.selectById(1L)).thenReturn(key(OpenApiKeyStatus.ENABLED));

        assertThatThrownBy(() -> service.revoke(new OpenApiKeyRevokeParamsBO(999L, 8L, 1L)))
                .isInstanceOf(AppException.class);
        verify(openApiKeyMapper, never()).update(any(), any());
    }

    @Test
    void shouldTreatRevokeOfRevokedKeyAsSuccess() {
        when(openApiKeyMapper.selectById(1L)).thenReturn(key(OpenApiKeyStatus.REVOKED));

        service.revoke(new OpenApiKeyRevokeParamsBO(9L, 8L, 1L));

        verify(openApiKeyMapper, never()).update(any(), any());
    }

    @Test
    void shouldRevokeEnabledKeyImmediately() {
        when(openApiKeyMapper.selectById(1L)).thenReturn(key(OpenApiKeyStatus.ENABLED));

        service.revoke(new OpenApiKeyRevokeParamsBO(9L, 8L, 1L));

        verify(openApiKeyMapper).update(any(), any());
    }

    @Test
    void shouldRevokeAllEnabledKeysOfUserOnPasswordChange() {
        service.revokeAllForUser(9L);

        verify(openApiKeyMapper).update(any(), any());
    }
}
