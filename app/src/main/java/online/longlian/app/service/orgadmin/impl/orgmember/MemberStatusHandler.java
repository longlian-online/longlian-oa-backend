package online.longlian.app.service.orgadmin.impl.orgmember;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.common.enumeration.Status;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(propagation = Propagation.MANDATORY)
    public OrganizationMember getAndValidateMember(Long memberId, Long orgId) {
        OrganizationMember member = organizationMemberMapper.selectOne(new LambdaQueryWrapper<OrganizationMember>()
                .eq(OrganizationMember::getId, memberId).eq(OrganizationMember::getOrgId, orgId).last("FOR UPDATE"));
        if (member == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "成员不存在");
        }
        return member;
    }


    public void updateMemberStatus(OrganizationMember member, Status status) {
        if (member.getStatus() == status) return;
        organizationMemberMapper.update(null,
                new LambdaUpdateWrapper<OrganizationMember>()
                        .eq(OrganizationMember::getId, member.getId())
                        .set(OrganizationMember::getStatus, status)
                        .set(OrganizationMember::getUpdatedAt, LocalDateTime.now(clock)));
    }
}
