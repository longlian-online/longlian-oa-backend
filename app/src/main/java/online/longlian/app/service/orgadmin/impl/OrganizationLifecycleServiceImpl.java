package online.longlian.app.service.orgadmin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.enumeration.OrganizationRole;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.GroupApplicationMapper;
import online.longlian.app.mapper.OrganizationMapper;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.orgadmin.OrgDissolveParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberExitParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberRemoveParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberTransferOwnershipParamsBO;
import online.longlian.app.pojo.entity.GroupApplication;
import online.longlian.app.pojo.entity.Organization;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.service.common.OrganizationAuthorizationService;
import online.longlian.app.service.orgadmin.OrganizationLifecycleService;
import online.longlian.app.service.orgadmin.impl.orgmember.OrganizationMemberPolicy;
import online.longlian.common.enumeration.ApplicationStatus;
import online.longlian.common.enumeration.Status;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
public class OrganizationLifecycleServiceImpl implements OrganizationLifecycleService {
    private final OrganizationMapper organizations;
    private final OrganizationMemberMapper members;
    private final UserMapper users;
    private final GroupApplicationMapper applications;
    private final OrganizationAuthorizationService authorization;
    private final OrganizationMemberPolicy policy;
    private final Clock clock;

    @Override
    public void removeMember(OrgMemberRemoveParamsBO params) {
        authorization.lockOrganization(params.getOrgId(), true);
        OrganizationMember operator = authorization.requireManager(params.getOrgId(), params.getOperatorUserId());
        OrganizationMember target = target(params.getOrgId(), params.getMemberId());
        policy.requireManageableTarget(operator, target);
        deleteMembership(target);
    }

    @Override
    public void exitOrganization(OrgMemberExitParamsBO params) {
        authorization.lockOrganization(params.getOrgId(), false);
        OrganizationMember member = members.selectOne(new LambdaQueryWrapper<OrganizationMember>()
                .eq(OrganizationMember::getOrgId, params.getOrgId())
                .eq(OrganizationMember::getUserId, params.getOperatorUserId()).last("FOR UPDATE"));
        if (member == null) throw new AppException(ResultCode.DATA_NOT_EXIT, "成员不存在");
        if (OrganizationRole.fromValue(member.getOrgRole()) == OrganizationRole.ORG_OWNER)
            throw new AppException(ResultCode.OPERATION_FAIL, "所有者转让所有权后才能退出");
        deleteMembership(member);
    }

    @Override
    public void transferOwnership(OrgMemberTransferOwnershipParamsBO params) {
        authorization.lockOrganization(params.getOrgId(), true);
        OrganizationMember owner = authorization.requireOwner(params.getOrgId(), params.getOperatorUserId());
        OrganizationMember target = target(params.getOrgId(), params.getMemberId());
        if (owner.getUserId().equals(target.getUserId()) || target.getStatus() != Status.ENABLED
                || OrganizationRole.fromValue(target.getOrgRole()) == OrganizationRole.ORG_OWNER)
            throw new AppException(ResultCode.OPERATION_FAIL, "转让目标必须是其他启用成员");
        User targetUser = users.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getId, target.getUserId()).last("FOR UPDATE"));
        if (targetUser == null || targetUser.getStatus() != Status.ENABLED)
            throw new AppException(ResultCode.OPERATION_FAIL, "转让目标账号不可用");
        LocalDateTime now = LocalDateTime.now(clock);
        // 所有者角色的两个变更同事务提交；组织锁使其他治理请求只能看见转让前或转让后。
        changeRole(owner.getId(), OrganizationRole.ORG_ADMIN, now);
        changeRole(target.getId(), OrganizationRole.ORG_OWNER, now);
    }

    @Override
    public void dissolveOrganization(OrgDissolveParamsBO params) {
        authorization.lockOrganization(params.getOrgId(), true);
        authorization.requireOwner(params.getOrgId(), params.getOperatorUserId());
        LocalDateTime now = LocalDateTime.now(clock);
        organizations.update(null, new LambdaUpdateWrapper<Organization>()
                .eq(Organization::getId, params.getOrgId()).set(Organization::getDeletedAt, now));
        users.update(null, new LambdaUpdateWrapper<User>().eq(User::getDefaultOrgId, params.getOrgId())
                .set(User::getDefaultOrgId, 0L));
        applications.update(null, new LambdaUpdateWrapper<GroupApplication>()
                .eq(GroupApplication::getOrgId, params.getOrgId()).eq(GroupApplication::getStatus, ApplicationStatus.PENDING)
                .set(GroupApplication::getStatus, ApplicationStatus.REJECTED)
                .set(GroupApplication::getReviewerId, params.getOperatorUserId()).set(GroupApplication::getReviewedAt, now)
                .set(GroupApplication::getReviewRemark, "组织已解散").set(GroupApplication::getUpdatedAt, now));
    }

    private OrganizationMember target(Long orgId, Long memberId) {
        OrganizationMember member = members.selectOne(new LambdaQueryWrapper<OrganizationMember>()
                .eq(OrganizationMember::getId, memberId).eq(OrganizationMember::getOrgId, orgId).last("FOR UPDATE"));
        if (member == null) throw new AppException(ResultCode.DATA_NOT_EXIT, "成员不存在");
        return member;
    }

    private void deleteMembership(OrganizationMember member) {
        members.deleteById(member.getId());
        users.update(null, new LambdaUpdateWrapper<User>().eq(User::getId, member.getUserId())
                .eq(User::getDefaultOrgId, member.getOrgId()).set(User::getDefaultOrgId, 0L));
    }

    private void changeRole(Long memberId, OrganizationRole role, LocalDateTime now) {
        members.update(null, new LambdaUpdateWrapper<OrganizationMember>().eq(OrganizationMember::getId, memberId)
                .set(OrganizationMember::getOrgRole, role.name()).set(OrganizationMember::getUpdatedAt, now));
    }
}
