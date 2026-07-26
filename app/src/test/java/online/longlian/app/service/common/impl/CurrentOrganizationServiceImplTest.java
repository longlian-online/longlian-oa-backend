package online.longlian.app.service.common.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.constants.RedisConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.mapper.OrganizationMapper;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.common.CurrentOrganizationContextBO;
import online.longlian.app.pojo.entity.Organization;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.pojo.entity.User;
import online.longlian.common.enumeration.Status;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurrentOrganizationServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private UserMapper userMapper;
    @Mock
    private OrganizationMapper organizationMapper;
    @Mock
    private OrganizationMemberMapper organizationMemberMapper;

    private CurrentOrganizationServiceImpl service;

    @BeforeEach
    void setUp() {
        MybatisConfiguration config = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(config, ""), OrganizationMember.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(config, ""), Organization.class);
        service = new CurrentOrganizationServiceImpl(redisTemplate, userMapper, organizationMapper, organizationMemberMapper);
    }

    private void mockRedisGet(Object value) {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.get(anyString())).thenReturn(value);
    }

    private void mockRedisTtl(long ttl) {
        lenient().when(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(ttl);
    }

    private Organization enabledOrg(Long id) {
        return Organization.builder().id(id).status(Status.ENABLED).build();
    }

    private OrganizationMember enabledMember(Long userId, Long orgId) {
        return OrganizationMember.builder()
                .id(100L).userId(userId).orgId(orgId).orgRole("org_user")
                .status(Status.ENABLED).build();
    }

    @Test
    void resolveCurrentOrgContext_cachedOrgValid_returnsCached() {
        mockRedisGet("10");
        when(organizationMapper.selectById(10L)).thenReturn(enabledOrg(10L));
        when(organizationMemberMapper.selectOne(any())).thenReturn(enabledMember(1L, 10L));

        CurrentOrganizationContextBO ctx = service.resolveCurrentOrgContext(1L, null);

        assertThat(ctx.getOrgId()).isEqualTo(10L);
        assertThat(ctx.getMemberId()).isEqualTo(100L);
        assertThat(ctx.getRoles()).containsExactly("org_user");
    }

    @Test
    void resolveCurrentOrgContext_noCache_fallbackToDefaultOrg() {
        mockRedisGet(null);
        mockRedisTtl(3600L);
        User user = User.builder().id(1L).defaultOrgId(20L).build();
        when(userMapper.selectById(1L)).thenReturn(user);
        when(organizationMapper.selectById(20L)).thenReturn(enabledOrg(20L));
        when(organizationMemberMapper.selectOne(any())).thenReturn(enabledMember(1L, 20L));

        CurrentOrganizationContextBO ctx = service.resolveCurrentOrgContext(1L, null);

        assertThat(ctx.getOrgId()).isEqualTo(20L);
        verify(valueOperations).set(eq(RedisConstants.CURRENT_ORG + 1L), eq(20L), eq(3600L), eq(TimeUnit.SECONDS));
    }

    @Test
    void resolveCurrentOrgContext_noCache_fallbackToFirstJoinedOrg() {
        mockRedisGet(null);
        mockRedisTtl(-1L);
        User user = User.builder().id(1L).defaultOrgId(null).build();
        when(userMapper.selectById(1L)).thenReturn(user);

        OrganizationMember member1 = enabledMember(1L, 30L);
        when(organizationMemberMapper.selectList(any())).thenReturn(List.of(member1));
        when(organizationMapper.selectById(30L)).thenReturn(enabledOrg(30L));
        when(organizationMemberMapper.selectOne(any())).thenReturn(member1);

        CurrentOrganizationContextBO ctx = service.resolveCurrentOrgContext(1L, null);

        assertThat(ctx.getOrgId()).isEqualTo(30L);
    }

    @Test
    void resolveCurrentOrgContext_noAccessibleOrg_throwsException() {
        mockRedisGet(null);
        User user = User.builder().id(1L).defaultOrgId(null).build();
        when(userMapper.selectById(1L)).thenReturn(user);
        when(organizationMemberMapper.selectList(any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.resolveCurrentOrgContext(1L, null))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("当前无可用组织");
        verify(redisTemplate).delete(RedisConstants.CURRENT_ORG + 1L);
    }

    @Test
    void requireCurrentOrgContext_noCachedOrg_throwsException() {
        mockRedisGet(null);

        assertThatThrownBy(() -> service.requireCurrentOrgContext(1L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("当前组织不存在");
    }

    @Test
    void requireCurrentOrgContext_orgDisabled_clearsCacheAndThrows() {
        mockRedisGet("10");
        Organization disabledOrg = Organization.builder().id(10L).status(Status.DISABLED).build();
        when(organizationMapper.selectById(10L)).thenReturn(disabledOrg);

        assertThatThrownBy(() -> service.requireCurrentOrgContext(1L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("组织已被禁用");
        verify(redisTemplate).delete(RedisConstants.CURRENT_ORG + 1L);
    }

    @Test
    void requireCurrentOrgContext_notMember_throwsException() {
        mockRedisGet("10");
        when(organizationMapper.selectById(10L)).thenReturn(enabledOrg(10L));
        when(organizationMemberMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.requireCurrentOrgContext(1L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("您不是该组织成员");
    }

    @Test
    void requireCurrentOrgContext_memberDisabled_throwsException() {
        mockRedisGet("10");
        when(organizationMapper.selectById(10L)).thenReturn(enabledOrg(10L));
        OrganizationMember disabledMember = OrganizationMember.builder()
                .id(100L).userId(1L).orgId(10L).status(Status.DISABLED).build();
        when(organizationMemberMapper.selectOne(any())).thenReturn(disabledMember);

        assertThatThrownBy(() -> service.requireCurrentOrgContext(1L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("成员状态已被禁用");
    }

    @Test
    void switchCurrentOrg_validTarget_cachesNewOrg() {
        mockRedisTtl(7200L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(organizationMapper.selectById(50L)).thenReturn(enabledOrg(50L));
        when(organizationMemberMapper.selectOne(any())).thenReturn(enabledMember(1L, 50L));

        service.switchCurrentOrg(1L, 50L);

        verify(valueOperations).set(eq(RedisConstants.CURRENT_ORG + 1L), eq(50L), eq(7200L), eq(TimeUnit.SECONDS));
    }

    @Test
    void switchCurrentOrg_orgNotFound_throwsException() {
        when(organizationMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.switchCurrentOrg(1L, 99L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("组织不存在");
    }

    @Test
    void clearCurrentOrg_deletesRedisKey() {
        service.clearCurrentOrg(1L);
        verify(redisTemplate).delete(RedisConstants.CURRENT_ORG + 1L);
    }

    @Test
    void getCachedCurrentOrgId_invalidNumber_returnsNull() {
        mockRedisGet("not_a_number");

        // resolveCurrentOrgContext will fallback since cached is null
        User user = User.builder().id(1L).defaultOrgId(null).build();
        when(userMapper.selectById(1L)).thenReturn(user);
        when(organizationMemberMapper.selectList(any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.resolveCurrentOrgContext(1L, null))
                .isInstanceOf(AppException.class);
    }

    @Test
    void resolveCurrentOrgContext_memberWithNoRole_returnsEmptyRoles() {
        mockRedisGet("10");
        when(organizationMapper.selectById(10L)).thenReturn(enabledOrg(10L));
        OrganizationMember memberNoRole = OrganizationMember.builder()
                .id(100L).userId(1L).orgId(10L).orgRole(null)
                .status(Status.ENABLED).build();
        when(organizationMemberMapper.selectOne(any())).thenReturn(memberNoRole);

        CurrentOrganizationContextBO ctx = service.resolveCurrentOrgContext(1L, null);

        assertThat(ctx.getRoles()).isEmpty();
    }

    @Test
    void requireCurrentOrgContext_orgIdNull_throwsException() {
        mockRedisGet("0");

        assertThatThrownBy(() -> service.requireCurrentOrgContext(1L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("当前组织不存在");
    }

    @Test
    void requireAccessibleOrgMember_orgIdNegative_throwsException() {
        mockRedisGet("-1");

        assertThatThrownBy(() -> service.requireCurrentOrgContext(1L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("当前组织不存在");
    }
}
