package online.longlian.app.service.orgadmin.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.constants.InviteConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.generator.enumeration.Status;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 处理成员启用/禁用操作。
 */
@Component
@RequiredArgsConstructor
public class MemberStatusHandler {

    private final OrganizationMemberMapper organizationMemberMapper;
    private final Clock clock;

    public OrganizationMember getAndValidateMember(Long memberId, Long orgId) {
        OrganizationMember member = organizationMemberMapper.selectById(memberId);
        if (member == null || !orgId.equals(member.getOrgId())) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "成员不存在");
        }
        return member;
    }

    public void validateNotAdminDisable(OrganizationMember member, Status targetStatus) {
        if (targetStatus == Status.DISABLED && InviteConstants.ROLE_ORG_ADMIN.equals(member.getOrgRole())) {
            throw new AppException(ResultCode.OPERATION_FAIL, "管理员不可被禁用");
        }
    }

    public void updateMemberStatus(OrganizationMember member, Status status) {
        organizationMemberMapper.update(null,
                new LambdaUpdateWrapper<OrganizationMember>()
                        .eq(OrganizationMember::getId, member.getId())
                        .set(OrganizationMember::getStatus, status)
                        .set(OrganizationMember::getUpdatedAt, LocalDateTime.now(clock)));
    }
}
