package online.longlian.app.service.common.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.constants.RedisConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.OrganizationMapper;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.entity.Organization;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.service.common.CurrentOrganizationService;
import online.longlian.generator.enumeration.Status;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

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
        Long cachedOrgId = getCachedCurrentOrgId(userId);
        if (isAccessibleOrg(userId, cachedOrgId)) {
            return cachedOrgId;
        }

        Long fallbackOrgId = resolveFallbackOrgId(userId);
        if (fallbackOrgId == null) {
            clearCurrentOrg(userId);
            throw new AppException(ResultCode.OPERATION_FAIL, "当前无可用组织，请先加入组织或切换组织");
        }

        cacheCurrentOrgId(userId, fallbackOrgId, getCurrentOrgTtlSeconds(userId));
        return fallbackOrgId;
    }

    /**
     * 初始化用户当前组织
     */
    @Override
    public void initializeCurrentOrg(Long userId, long ttlSeconds) {
        Long fallbackOrgId = resolveFallbackOrgId(userId);
        if (fallbackOrgId == null) {
            clearCurrentOrg(userId);
            return;
        }
        cacheCurrentOrgId(userId, fallbackOrgId, ttlSeconds);
    }

    /**
     * 切换用户当前组织
     */
    @Override
    public void switchCurrentOrg(Long userId, Long targetOrgId) {
        assertAccessibleOrg(userId, targetOrgId);
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
    private Long resolveFallbackOrgId(Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null
                && user.getDefaultOrgId() != null
                && user.getDefaultOrgId() > 0
                && isAccessibleOrg(userId, user.getDefaultOrgId())) {
            return user.getDefaultOrgId();
        }
        // 查询用户启用状态的组织成员信息，按加入时间升序
        List<OrganizationMember> organizationMembers = organizationMemberMapper.selectList(
                new LambdaQueryWrapper<OrganizationMember>()
                        .eq(OrganizationMember::getUserId, userId)
                        .eq(OrganizationMember::getStatus, Status.ENABLED)
                        .orderByAsc(OrganizationMember::getJoinedAt)
        );

        // 遍历获取第一个可用组织
        for (OrganizationMember organizationMember : organizationMembers) {
            if (isAccessibleOrg(userId, organizationMember.getOrgId())) {
                return organizationMember.getOrgId();
            }
        }

        return null;
    }

    /**
     * 判断组织是否可访问（组织有效 + 用户是该组织启用状态成员）
     */
    private boolean isAccessibleOrg(Long userId, Long orgId) {
        if (orgId == null || orgId <= 0) {
            return false;
        }

        Organization organization = organizationMapper.selectById(orgId);
        if (organization == null || organization.getStatus() != Status.ENABLED) {
            return false;
        }

        OrganizationMember organizationMember = organizationMemberMapper.selectOne(
                new LambdaQueryWrapper<OrganizationMember>()
                        .eq(OrganizationMember::getUserId, userId)
                        .eq(OrganizationMember::getOrgId, orgId)
                        .eq(OrganizationMember::getStatus, Status.ENABLED)
                        .last("LIMIT 1")
        );

        return organizationMember != null;
    }

    /**
     * 断言组织可访问，抛出异常原因。
     */
    private void assertAccessibleOrg(Long userId, Long orgId) {
        if (orgId == null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "目标组织不能为空");
        }
        if (orgId <= 0) {
            throw new AppException(ResultCode.OPERATION_FAIL, "目标组织ID不合法");
        }

        Organization organization = organizationMapper.selectById(orgId);
        if (organization == null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "目标组织不存在");
        }
        if (organization.getStatus() != Status.ENABLED) {
            throw new AppException(ResultCode.OPERATION_FAIL, "目标组织已被禁用");
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
    }

    /**
     * 从Redis获取用户当前组织缓存
     */
    private Long getCachedCurrentOrgId(Long userId) {
        Object cachedValue = redisTemplate.opsForValue().get(RedisConstants.CURRENT_ORG + userId);
        if (cachedValue == null) {
            return null;
        }
        return Long.parseLong(String.valueOf(cachedValue));
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
        Long expireSeconds = redisTemplate.getExpire(RedisConstants.LOGIN_USER + userId, TimeUnit.SECONDS);
        if (expireSeconds == null || expireSeconds <= 0) {
            return RedisConstants.EXPIRE_TIME.longValue();
        }
        return expireSeconds;
    }
}
