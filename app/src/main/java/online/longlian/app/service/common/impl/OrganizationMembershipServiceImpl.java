package online.longlian.app.service.common.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.constants.InviteConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.OrganizationMapper;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.entity.Organization;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.service.common.DefaultOrganization;
import online.longlian.app.service.common.OrganizationMemberProfile;
import online.longlian.app.service.common.OrganizationMembershipService;
import online.longlian.common.enumeration.Status;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;


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
    public OrganizationMember requireAssignableMember(Long userId, Long orgId) {
        if (userId == null || userId <= 0) {
            throw new AppException(ResultCode.PARAM_ERROR, "组员不能为空");
        }
        Organization organization = organizationMapper.selectById(orgId);
        if (organization == null || organization.getStatus() != Status.ENABLED) {
            throw new AppException(ResultCode.OPERATION_FAIL, "组织不可用");
        }
        OrganizationMember member = organizationMemberMapper.selectOne(
                new LambdaQueryWrapper<OrganizationMember>()
                        .eq(OrganizationMember::getUserId, userId)
                        .eq(OrganizationMember::getOrgId, orgId)
                        .last("LIMIT 1"));
        if (member == null) {
            throw new AppException(ResultCode.PARAM_ERROR, "所选组员不是当前组织成员");
        }
        if (member.getStatus() != Status.ENABLED) {
            throw new AppException(ResultCode.OPERATION_FAIL, "所选组员已被禁用");
        }
        return member;
    }

    @Override
    public List<OrganizationMemberProfile> listEnabledMembers(Long orgId) {
        List<OrganizationMember> members = organizationMemberMapper.selectList(
                new LambdaQueryWrapper<OrganizationMember>()
                        .eq(OrganizationMember::getOrgId, orgId)
                        .eq(OrganizationMember::getStatus, Status.ENABLED));
        if (members.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = members.stream().map(OrganizationMember::getUserId).distinct().toList();
        Map<Long, User> users = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return members.stream()
                .map(member -> users.get(member.getUserId()))
                .filter(Objects::nonNull)
                .map(user -> new OrganizationMemberProfile(user.getId(), displayName(user)))
                .sorted(Comparator.comparing(OrganizationMemberProfile::nickname, Comparator.nullsLast(String::compareTo))
                        .thenComparing(OrganizationMemberProfile::userId))
                .toList();
    }

    @Override
    public List<Long> listEnabledAdministratorIds(Long orgId) {
        return organizationMemberMapper.selectList(new LambdaQueryWrapper<OrganizationMember>()
                        .select(OrganizationMember::getUserId)
                        .eq(OrganizationMember::getOrgId, orgId)
                        .eq(OrganizationMember::getStatus, Status.ENABLED)
                        .in(OrganizationMember::getOrgRole, InviteConstants.ROLE_ORG_OWNER, InviteConstants.ROLE_ORG_ADMIN))
                .stream()
                .map(OrganizationMember::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
    }

    private String displayName(User user) {
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname().trim();
        }
        return user.getUsername();
    }

    @Override
    public DefaultOrganization findDefault(Long userId) {
        User user = userMapper.selectById(userId);
        Long defaultOrgId = user == null ? null : user.getDefaultOrgId();
        if (defaultOrgId == null || defaultOrgId <= 0) {
            return new DefaultOrganization(null, null);
        }
        OrganizationMember member = findEnabledMember(userId, defaultOrgId);
        String orgRole = member != null && StringUtils.hasText(member.getOrgRole()) ? member.getOrgRole() : null;
        return new DefaultOrganization(defaultOrgId, orgRole);
    }

    /**
     * 默认组织当前不可用时返回 null。调用方不得因此改选其他组织。
     */
    private OrganizationMember findEnabledMember(Long userId, Long orgId) {
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
