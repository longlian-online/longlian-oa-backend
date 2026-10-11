package online.longlian.app.service.orgadmin.impl.orgmember;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.enumeration.OrganizationRole;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.GroupApplicationMapper;
import online.longlian.app.mapper.OrganizationMapper;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberChangeRoleParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberChangeStatusParamsBO;
import online.longlian.app.pojo.entity.GroupApplication;
import online.longlian.app.pojo.entity.Organization;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.service.common.OrganizationAuthorizationService;
import online.longlian.app.service.otp.OTPServiceFactory;
import online.longlian.common.enumeration.Status;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationMemberServiceImplTest {
    @Mock private GroupApplicationMapper applications;
    @Mock private OrganizationMemberMapper members;
    @Mock private OrganizationMapper organizations;
    @Mock private UserMapper users;
    @Mock private OTPServiceFactory otpFactory;
    private OrganizationMemberServiceImpl service;

    @BeforeEach
    void setUp() {
        for (Class<?> type : List.of(OrganizationMember.class, Organization.class, User.class, GroupApplication.class))
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), type);
        Clock clock = Clock.systemUTC();
        OrganizationAuthorizationService authorization = new OrganizationAuthorizationService(organizations, members, users);
        service = new OrganizationMemberServiceImpl(clock, applications, members, otpFactory,
                mock(MemberQueryBuilder.class), mock(MemberAssembler.class), mock(ApplicationReviewHandler.class),
                new MemberStatusHandler(members, clock), mock(MemberSubmissionHandler.class), authorization,
                new OrganizationMemberPolicy());
        lenient().when(organizations.selectOne(any())).thenReturn(Organization.builder().status(Status.ENABLED).build());
        lenient().when(users.selectById(10L)).thenReturn(User.builder().status(Status.ENABLED).build());
    }

    /** 自己、所有者和同级管理员不受普通成员治理入口支配。 */
    @ParameterizedTest
    @CsvSource({"ORG_OWNER,ORG_USER,10", "ORG_OWNER,ORG_OWNER,20", "ORG_ADMIN,ORG_ADMIN,20",
            "ORG_ADMIN,ORG_OWNER,20", "ORG_USER,ORG_USER,20"})
    void shouldRejectForbiddenStatusRelationships(String actorRole, String targetRole, long targetUser) {
        when(members.selectOne(any())).thenReturn(member(1L, 10L, actorRole), member(2L, targetUser, targetRole));
        assertThatThrownBy(() -> service.changeMemberStatus(statusParams()))
                .isInstanceOf(AppException.class).extracting("code").isEqualTo(ResultCode.UNAUTHORIZED_OPERATION.getCode());
        verify(members, never()).update(isNull(), any());
    }

    /** 普通管理员不能调整普通成员角色，只有所有者可以治理管理员。 */
    @Test
    void shouldRejectRoleChangeByOrdinaryAdministrator() {
        when(members.selectOne(any())).thenReturn(member(1L, 10L, "ORG_ADMIN"), member(2L, 20L, "ORG_USER"));
        assertThatThrownBy(() -> service.changeMemberRole(roleParams(OrganizationRole.ORG_ADMIN)))
                .isInstanceOf(AppException.class).extracting("code").isEqualTo(ResultCode.UNAUTHORIZED_OPERATION.getCode());
        verify(members, never()).update(isNull(), any());
    }

    /** 已降级、禁用或不存在的操作者不能沿用入口鉴权结果。 */
    @Test
    void shouldRejectDisabledOrMissingOperatorBeforeMutation() {
        OrganizationMember disabled = member(1L, 10L, "ORG_ADMIN");
        disabled.setStatus(Status.DISABLED);
        when(members.selectOne(any())).thenReturn(disabled, null);
        assertThatThrownBy(() -> service.changeMemberStatus(statusParams()))
                .isInstanceOf(AppException.class).extracting("code").isEqualTo(ResultCode.UNAUTHORIZED_OPERATION.getCode());
        assertThatThrownBy(() -> service.changeMemberRole(roleParams(OrganizationRole.ORG_USER)))
                .isInstanceOf(AppException.class).extracting("code").isEqualTo(ResultCode.UNAUTHORIZED_OPERATION.getCode());
        verify(members, never()).update(isNull(), any());
    }

    /** 重复设置当前角色是幂等操作，不应更新时间戳或产生额外数据库写入。 */
    @Test
    void shouldNotRewriteUnchangedMemberRole() {
        when(members.selectOne(any())).thenReturn(member(1L, 10L, "ORG_OWNER"), member(2L, 20L, "ORG_ADMIN"));
        service.changeMemberRole(roleParams(OrganizationRole.ORG_ADMIN));
        verify(members, never()).update(isNull(), any());
    }

    /** 组织已经解散时，成员治理不能继续写入。 */
    @Test
    void shouldRejectMutationForDeletedOrganization() {
        when(organizations.selectOne(any())).thenReturn(null);
        assertThatThrownBy(() -> service.changeMemberStatus(statusParams()))
                .isInstanceOf(AppException.class).extracting("code").isEqualTo(ResultCode.DATA_NOT_EXIT.getCode());
        verifyNoInteractions(members);
    }

    private OrganizationMember member(Long id, Long userId, String role) {
        return OrganizationMember.builder().id(id).userId(userId).orgId(1L).orgRole(role).status(Status.ENABLED).build();
    }

    private OrgMemberChangeStatusParamsBO statusParams() {
        return OrgMemberChangeStatusParamsBO.builder().orgId(1L).operatorUserId(10L).memberId(2L).status(Status.DISABLED).build();
    }

    private OrgMemberChangeRoleParamsBO roleParams(OrganizationRole role) {
        return OrgMemberChangeRoleParamsBO.builder().orgId(1L).operatorUserId(10L).memberId(2L).orgRole(role).build();
    }
}
