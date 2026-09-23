package online.longlian.app.service.common.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.OrganizationMapper;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.common.CurrentOrganizationContextBO;
import online.longlian.app.pojo.entity.Organization;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.service.common.CurrentOrganizationService;
import online.longlian.common.enumeration.Status;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CurrentOrganizationServiceImpl implements CurrentOrganizationService {

    private final UserMapper userMapper;
    private final OrganizationMapper organizationMapper;
    private final OrganizationMemberMapper organizationMemberMapper;

    @Override
    public Long resolveCurrentOrgId(Long userId) {
        return resolveCurrentOrgContext(userId, null).getOrgId();
    }

    @Override
    public CurrentOrganizationContextBO resolveCurrentOrgContext(Long userId, Long defaultOrgId) {
        OrganizationMember fallbackMember = resolveFallbackOrgMember(userId, defaultOrgId);
        if (fallbackMember == null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "当前无可用组织，请先加入组织");
        }
        return toCurrentOrganizationContext(fallbackMember);
    }

    @Override
    public CurrentOrganizationContextBO switchCurrentOrg(Long userId, Long targetOrgId) {
        return toCurrentOrganizationContext(requireAccessibleOrgMember(userId, targetOrgId));
    }

    private OrganizationMember resolveFallbackOrgMember(Long userId, Long defaultOrgId) {
        if (defaultOrgId == null || defaultOrgId <= 0) {
            User user = userMapper.selectById(userId);
            defaultOrgId = user == null ? null : user.getDefaultOrgId();
        }
        OrganizationMember defaultOrgMember = getAccessibleOrganizationMember(userId, defaultOrgId);
        if (defaultOrgMember != null) {
            return defaultOrgMember;
        }
        List<OrganizationMember> organizationMembers = organizationMemberMapper.selectList(
                new LambdaQueryWrapper<OrganizationMember>()
                        .eq(OrganizationMember::getUserId, userId)
                        .eq(OrganizationMember::getStatus, Status.ENABLED)
                        .orderByAsc(OrganizationMember::getJoinedAt));
        for (OrganizationMember organizationMember : organizationMembers) {
            OrganizationMember accessibleMember = getAccessibleOrganizationMember(userId, organizationMember.getOrgId());
            if (accessibleMember != null) {
                return accessibleMember;
            }
        }
        return null;
    }

    private OrganizationMember getAccessibleOrganizationMember(Long userId, Long orgId) {
        if (orgId == null || orgId <= 0) {
            return null;
        }
        Organization organization = organizationMapper.selectById(orgId);
        if (organization == null || organization.getStatus() != Status.ENABLED) {
            return null;
        }
        return organizationMemberMapper.selectOne(new LambdaQueryWrapper<OrganizationMember>()
                .eq(OrganizationMember::getUserId, userId)
                .eq(OrganizationMember::getOrgId, orgId)
                .eq(OrganizationMember::getStatus, Status.ENABLED)
                .last("LIMIT 1"));
    }

    private OrganizationMember requireAccessibleOrgMember(Long userId, Long orgId) {
        if (orgId == null || orgId <= 0) {
            throw new AppException(ResultCode.OPERATION_FAIL, "组织ID不合法");
        }
        Organization organization = organizationMapper.selectById(orgId);
        if (organization == null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "组织不存在");
        }
        if (organization.getStatus() != Status.ENABLED) {
            throw new AppException(ResultCode.OPERATION_FAIL, "组织已被禁用");
        }
        OrganizationMember organizationMember = organizationMemberMapper.selectOne(new LambdaQueryWrapper<OrganizationMember>()
                .eq(OrganizationMember::getUserId, userId)
                .eq(OrganizationMember::getOrgId, orgId)
                .eq(OrganizationMember::getStatus, Status.ENABLED)
                .last("LIMIT 1"));
        if (organizationMember == null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "您不是该组织成员或成员状态已被禁用");
        }
        return organizationMember;
    }

    private CurrentOrganizationContextBO toCurrentOrganizationContext(OrganizationMember organizationMember) {
        List<String> roles = StringUtils.hasText(organizationMember.getOrgRole())
                ? List.of(organizationMember.getOrgRole()) : List.of();
        return CurrentOrganizationContextBO.builder()
                .orgId(organizationMember.getOrgId())
                .memberId(organizationMember.getId())
                .roles(roles)
                .build();
    }
}
