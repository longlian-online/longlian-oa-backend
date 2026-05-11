package online.longlian.app.service.common.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.constants.RedisConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.OrganizationMapper;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.CurrentOrganizationContextBO;
import online.longlian.app.pojo.entity.Organization;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.service.common.CurrentOrganizationService;
import online.longlian.common.enumeration.Status;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CurrentOrganizationServiceImpl implements CurrentOrganizationService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserMapper userMapper;
    private final OrganizationMapper organizationMapper;
    private final OrganizationMemberMapper organizationMemberMapper;

    @Override
    public Long resolveCurrentOrgId(Long userId) {
        return resolveCurrentOrgContext(userId, null).getOrgId();
    }

    @Override
    public CurrentOrganizationContextBO resolveCurrentOrgContext(Long userId, Long defaultOrgId) {
        Long cachedOrgId = getCachedCurrentOrgId(userId);
        OrganizationMember cachedMember = getAccessibleOrganizationMember(userId, cachedOrgId);
        if (cachedMember != null) {
            return toCurrentOrganizationContext(cachedMember);
        }

        OrganizationMember fallbackMember = resolveFallbackOrgMember(userId, defaultOrgId);
        if (fallbackMember == null) {
            clearCurrentOrg(userId);
            throw new AppException(ResultCode.OPERATION_FAIL, "当前无可用组织，请先加入组织");
        }

        cacheCurrentOrgId(userId, fallbackMember.getOrgId(), getCurrentOrgTtlSeconds(userId));
        return toCurrentOrganizationContext(fallbackMember);
    }

    @Override
    public Long requireCurrentOrgId(Long userId) {
        return requireCurrentOrgContext(userId).getOrgId();
    }

    @Override
    public CurrentOrganizationContextBO requireCurrentOrgContext(Long userId) {
        Long cachedOrgId = getCachedCurrentOrgId(userId);
        if (cachedOrgId == null || cachedOrgId <= 0) {
            throw new AppException(ResultCode.OPERATION_FAIL, "当前组织不存在，请重新选择组织");
        }

        OrganizationMember organizationMember;
        try {
            organizationMember = requireAccessibleOrgMember(userId, cachedOrgId);
        } catch (AppException e) {
            clearCurrentOrg(userId);
            throw e;
        }
        return toCurrentOrganizationContext(organizationMember);
    }

    /**
     * 初始化用户当前组织
     */
    @Override
    public void refreshCurrentOrgTtl(Long userId, Long currentOrgId, long ttlSeconds) {
        cacheCurrentOrgId(userId, currentOrgId, ttlSeconds);
    }

    /**
     * 切换用户当前组织
     */
    @Override
    public void switchCurrentOrg(Long userId, Long targetOrgId) {
        requireAccessibleOrgMember(userId, targetOrgId);
        cacheCurrentOrgId(userId, targetOrgId, getCurrentOrgTtlSeconds(userId));
    }

    /**
     * 清除用户当前组织缓存
     */
    @Override
    public void clearCurrentOrg(Long userId) {
        redisTemplate.delete(RedisConstants.CURRENT_ORG + userId);
    }

    /**
     * 获取用户的备用组织ID（默认组织 -> 第一个加入的有效组织）
     */
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
                        .orderByAsc(OrganizationMember::getJoinedAt)
        );

        // 遍历获取第一个可用组织
        for (OrganizationMember organizationMember : organizationMembers) {
            OrganizationMember accessibleMember = getAccessibleOrganizationMember(userId, organizationMember.getOrgId());
            if (accessibleMember != null) {
                return accessibleMember;
            }
        }

        return null;
    }

    /**
     * 判断组织是否可访问（组织有效 + 用户是该组织启用状态成员）
     */
    private OrganizationMember getAccessibleOrganizationMember(Long userId, Long orgId) {
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

    private CurrentOrganizationContextBO toCurrentOrganizationContext(OrganizationMember organizationMember) {
        List<String> roles = StringUtils.hasText(organizationMember.getOrgRole())
                ? List.of(organizationMember.getOrgRole())
                : List.of();
        return CurrentOrganizationContextBO.builder()
                .orgId(organizationMember.getOrgId())
                .memberId(organizationMember.getId())
                .roles(roles)
                .build();
    }

    private OrganizationMember requireAccessibleOrgMember(Long userId, Long orgId) {
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

    /**
     * 从Redis获取用户当前组织缓存
     */
    private Long getCachedCurrentOrgId(Long userId) {
        Object cachedValue = redisTemplate.opsForValue().get(RedisConstants.CURRENT_ORG + userId);
        if (cachedValue == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(cachedValue));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 缓存用户当前组织ID到Redis
     */
    private void cacheCurrentOrgId(Long userId, Long orgId, Long ttlSeconds) {
        redisTemplate.opsForValue().set(
                RedisConstants.CURRENT_ORG + userId,
                orgId,
                ttlSeconds,
                TimeUnit.SECONDS
        );
    }

    private long getCurrentOrgTtlSeconds(Long userId) {
        long expireSeconds = redisTemplate.getExpire(RedisConstants.LOGIN_USER + userId, TimeUnit.SECONDS);
        if (expireSeconds <= 0) {
            return RedisConstants.EXPIRE_TIME.longValue();
        }
        return expireSeconds;
    }
}
