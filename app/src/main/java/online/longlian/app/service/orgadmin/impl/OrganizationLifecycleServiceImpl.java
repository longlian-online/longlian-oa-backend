package online.longlian.app.service.orgadmin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.enumeration.OrganizationRole;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.*;
import online.longlian.app.pojo.bo.orgadmin.*;
import online.longlian.app.pojo.entity.*;
import online.longlian.app.service.common.LockService;
import online.longlian.app.service.common.OrganizationAuthorizationService;
import online.longlian.app.service.orgadmin.OrganizationLifecycleService;
import online.longlian.app.service.orgadmin.impl.orgmember.OrganizationMemberPolicy;
import online.longlian.common.enumeration.ApplicationStatus;
import online.longlian.common.enumeration.Status;
import online.longlian.common.service.DistributedLockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OrganizationLifecycleServiceImpl extends ServiceImpl<OrganizationMapper, Organization>
        implements OrganizationLifecycleService {
    private final OrganizationMapper organizations;
    private final OrganizationMemberMapper members;
    private final UserMapper users;
    private final GroupApplicationMapper applications;
    private final OrganizationAuthorizationService authorization;
    private final OrganizationMemberPolicy policy;
    private final LockService locks;
    private final PlatformTransactionManager transactions;
    private final Clock clock;

    @Override
    public void removeMember(OrgMemberRemoveParamsBO params) {
        mutate(params.getOrgId(), true, () -> {
            OrganizationMember operator = authorization.requireManager(params.getOrgId(),params.getOperatorUserId());
            OrganizationMember target = target(params.getOrgId(),params.getMemberId());
            policy.requireManageableTarget(operator,target);
            deleteMembership(target);
        });
    }

    @Override
    public void exitOrganization(OrgMemberExitParamsBO params) {
        mutate(params.getOrgId(), false, () -> {
            OrganizationMember member = members.selectOne(new LambdaQueryWrapper<OrganizationMember>()
                    .eq(OrganizationMember::getOrgId,params.getOrgId())
                    .eq(OrganizationMember::getUserId,params.getOperatorUserId()).last("FOR UPDATE"));
            if (member == null) throw new AppException(ResultCode.DATA_NOT_EXIT,"成员不存在");
            if (OrganizationRole.fromValue(member.getOrgRole()) == OrganizationRole.ORG_OWNER)
                throw new AppException(ResultCode.OPERATION_FAIL,"所有者转让所有权后才能退出");
            deleteMembership(member);
        });
    }

    @Override
    public void transferOwnership(OrgMemberTransferOwnershipParamsBO params) {
        mutate(params.getOrgId(), true, () -> {
            OrganizationMember owner = authorization.requireOwner(params.getOrgId(),params.getOperatorUserId());
            OrganizationMember target = target(params.getOrgId(),params.getMemberId());
            if (owner.getUserId().equals(target.getUserId()) || target.getStatus() != Status.ENABLED
                    || OrganizationRole.fromValue(target.getOrgRole()) == OrganizationRole.ORG_OWNER)
                throw new AppException(ResultCode.OPERATION_FAIL,"转让目标必须是其他启用成员");
            User targetUser = users.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getId,target.getUserId()).last("FOR UPDATE"));
            if (targetUser == null || targetUser.getStatus() != Status.ENABLED)
                throw new AppException(ResultCode.OPERATION_FAIL,"转让目标账号不可用");
            // 先释放唯一所有者索引，再升级目标；第二步失败会回滚恢复原所有者。
            changeRole(owner,"ORG_OWNER","ORG_ADMIN");
            changeRole(target,target.getOrgRole(),"ORG_OWNER");
        });
    }

    @Override
    public void dissolveOrganization(OrgDissolveParamsBO params) {
        mutate(params.getOrgId(), true, () -> {
            authorization.requireOwner(params.getOrgId(),params.getOperatorUserId());
            LocalDateTime now = LocalDateTime.now(clock);
            if (organizations.update(null,new LambdaUpdateWrapper<Organization>()
                    .eq(Organization::getId,params.getOrgId()).set(Organization::getDeletedAt,now)) != 1)
                throw new AppException(ResultCode.OPERATION_FAIL,"组织状态已变更");
            users.update(null,new LambdaUpdateWrapper<User>().eq(User::getDefaultOrgId,params.getOrgId())
                    .set(User::getDefaultOrgId,0L));
            applications.update(null,new LambdaUpdateWrapper<GroupApplication>()
                    .eq(GroupApplication::getOrgId,params.getOrgId()).eq(GroupApplication::getStatus,ApplicationStatus.PENDING)
                    .set(GroupApplication::getStatus,ApplicationStatus.REJECTED).set(GroupApplication::getPasswordHash,null)
                    .set(GroupApplication::getReviewerId,params.getOperatorUserId()).set(GroupApplication::getReviewedAt,now)
                    .set(GroupApplication::getReviewRemark,"组织已解散").set(GroupApplication::getUpdatedAt,now));
        });
    }

    private void mutate(Long orgId, boolean requireEnabled, Runnable action) {
        try (DistributedLockService.Lock lock = locks.tryAcquireOrThrow("org:member:role:"+orgId,0,TimeUnit.SECONDS)) {
            TransactionTemplate transaction = new TransactionTemplate(transactions);
            transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            transaction.executeWithoutResult(status -> {
                authorization.lockOrganization(orgId,requireEnabled);
                action.run();
            });
        }
    }

    private OrganizationMember target(Long orgId, Long memberId) {
        OrganizationMember member = members.selectOne(new LambdaQueryWrapper<OrganizationMember>()
                .eq(OrganizationMember::getId,memberId).eq(OrganizationMember::getOrgId,orgId).last("FOR UPDATE"));
        if (member == null) throw new AppException(ResultCode.DATA_NOT_EXIT,"成员不存在");
        return member;
    }

    private void deleteMembership(OrganizationMember member) {
        if (members.deleteById(member.getId()) != 1) throw new AppException(ResultCode.OPERATION_FAIL,"成员关系已变更");
        users.update(null,new LambdaUpdateWrapper<User>().eq(User::getId,member.getUserId())
                .eq(User::getDefaultOrgId,member.getOrgId()).set(User::getDefaultOrgId,0L));
    }

    private void changeRole(OrganizationMember member, String previous, String role) {
        if (members.update(null,new LambdaUpdateWrapper<OrganizationMember>().eq(OrganizationMember::getId,member.getId())
                .eq(OrganizationMember::getOrgRole,previous).set(OrganizationMember::getOrgRole,role)
                .set(OrganizationMember::getUpdatedAt,LocalDateTime.now(clock))) != 1)
            throw new AppException(ResultCode.OPERATION_FAIL,"成员角色已变更");
    }
}
