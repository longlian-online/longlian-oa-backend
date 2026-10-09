package online.longlian.app.service.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.enumeration.OrganizationRole;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.OrganizationMapper;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.entity.Organization;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.pojo.entity.User;
import online.longlian.common.enumeration.Status;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrganizationAuthorizationService {
    private final OrganizationMapper organizationMapper;
    private final OrganizationMemberMapper memberMapper;
    private final UserMapper userMapper;

    /** 所有组织写事务先锁组织行，避免审批和治理沿用锁前的快照。 */
    public Organization lockOrganization(Long orgId, boolean requireEnabled) {
        Organization organization = organizationMapper.selectOne(new LambdaQueryWrapper<Organization>()
                .eq(Organization::getId, orgId).last("FOR UPDATE"));
        if (organization == null) throw new AppException(ResultCode.DATA_NOT_EXIT, "组织不存在");
        if (requireEnabled && organization.getStatus() != Status.ENABLED)
            throw new AppException(ResultCode.OPERATION_FAIL, "组织已被禁用");
        return organization;
    }

    public OrganizationMember requireManager(Long orgId, Long operatorUserId) {
        OrganizationMember operator = memberMapper.selectOne(new LambdaQueryWrapper<OrganizationMember>()
                .eq(OrganizationMember::getOrgId, orgId).eq(OrganizationMember::getUserId, operatorUserId)
                .last("FOR UPDATE"));
        User user = userMapper.selectById(operatorUserId);
        if (user == null || user.getStatus() != Status.ENABLED || operator == null
                || operator.getStatus() != Status.ENABLED
                || OrganizationRole.fromValue(operator.getOrgRole()) != OrganizationRole.ORG_ADMIN)
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "当前用户无权管理组织");
        return operator;
    }
}
