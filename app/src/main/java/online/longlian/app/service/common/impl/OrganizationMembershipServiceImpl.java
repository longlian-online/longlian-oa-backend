package online.longlian.app.service.common.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.OrganizationMapper;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.entity.Organization;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.service.common.OrganizationMembershipService;
import online.longlian.common.enumeration.Status;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrganizationMembershipServiceImpl implements OrganizationMembershipService {

    private final OrganizationMapper organizationMapper;
    private final OrganizationMemberMapper organizationMemberMapper;
    private final UserMapper userMapper;

    @Override
    public OrganizationMember requireEnabledMember(Long userId, Long orgId) {
        if (orgId == null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "组织不能为空");
        }
        if (orgId <= 0) {
            throw new AppException(ResultCode.OPERATION_FAIL, "组织ID不合法");
        }

        Organization organization = organizationMapper.selectById(orgId);
        if (organization == null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "组织不存在");
        }
        if (organization.getStatus() != Status.ENABLED) {
            throw new AppException(ResultCode.OPERATION_FAIL, "组织已被禁用");
        }

        OrganizationMember organizationMember = organizationMemberMapper.selectOne(
                new LambdaQueryWrapper<OrganizationMember>()
                        .eq(OrganizationMember::getUserId, userId)
                        .eq(OrganizationMember::getOrgId, orgId)
                        .last("LIMIT 1")
        );
        if (organizationMember == null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "您不是该组织成员");
        }
        if (organizationMember.getStatus() != Status.ENABLED) {
            throw new AppException(ResultCode.OPERATION_FAIL, "您在该组织中的成员状态已被禁用");
        }
        return organizationMember;
    }

    @Override
    public OrganizationMember suggestForLogin(Long userId) {
        User user = userMapper.selectById(userId);
        Long defaultOrgId = user == null ? null : user.getDefaultOrgId();
        if (defaultOrgId != null && defaultOrgId > 0) {
            OrganizationMember defaultMember = findEnabledMember(userId, defaultOrgId);
            if (defaultMember != null) {
                return defaultMember;
            }
        }

        List<OrganizationMember> organizationMembers = organizationMemberMapper.selectList(
                new LambdaQueryWrapper<OrganizationMember>()
                        .eq(OrganizationMember::getUserId, userId)
                        .eq(OrganizationMember::getStatus, Status.ENABLED)
                        .orderByAsc(OrganizationMember::getJoinedAt)
        );
        for (OrganizationMember organizationMember : organizationMembers) {
            OrganizationMember enabledMember = findEnabledMember(userId, organizationMember.getOrgId());
            if (enabledMember != null) {
                return enabledMember;
            }
        }
        throw new AppException(ResultCode.OPERATION_FAIL, "当前无可用组织，请先加入组织");
    }

    /**
     * 登录回退用：失败返回 null，避免和 {@link #requireEnabledMember} 的失败即抛混在一起。
     */
    private OrganizationMember findEnabledMember(Long userId, Long orgId) {
        if (orgId == null || orgId <= 0) {
            return null;
        }
        Organization organization = organizationMapper.selectById(orgId);
        if (organization == null || organization.getStatus() != Status.ENABLED) {
            return null;
        }
        return organizationMemberMapper.selectOne(
                new LambdaQueryWrapper<OrganizationMember>()
                        .eq(OrganizationMember::getUserId, userId)
                        .eq(OrganizationMember::getOrgId, orgId)
                        .eq(OrganizationMember::getStatus, Status.ENABLED)
                        .last("LIMIT 1")
        );
    }
}
