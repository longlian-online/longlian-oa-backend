package online.longlian.app.service.app.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.UserOrgApiKeyMapper;
import online.longlian.app.pojo.bo.app.UserOrgApiKeyParamsBO;
import online.longlian.app.pojo.bo.app.UserOrgApiKeyResultBO;
import online.longlian.app.pojo.bo.app.UserOrgApiKeyGenerateResultBO;
import online.longlian.app.pojo.bo.common.McpApiKeyIdentityResultBO;
import online.longlian.app.pojo.bo.common.OrganizationMembershipOrganizationResultBO;
import online.longlian.app.pojo.bo.common.OrganizationMembershipParamsBO;
import online.longlian.app.pojo.bo.common.OrganizationMembershipStatusResultBO;
import online.longlian.app.pojo.entity.UserOrgApiKey;
import online.longlian.app.service.app.UserOrgApiKeyService;
import online.longlian.app.service.common.OrganizationMembershipService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserOrgApiKeyServiceImpl extends ServiceImpl<UserOrgApiKeyMapper, UserOrgApiKey> implements UserOrgApiKeyService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final OrganizationMembershipService membershipService;
    private final Clock clock;

    @Override
    public List<OrganizationMembershipOrganizationResultBO> listOwnOrganizations(Long userId) {
        return membershipService.listOwnOrganizations(userId);
    }

    @Override
    public UserOrgApiKeyResultBO getCurrent(UserOrgApiKeyParamsBO params) {
        var membership = requireOwnMembership(params);
        UserOrgApiKey key = current(params);
        String state = key == null ? "NOT_CREATED" : key.getKeyHash() == null ? "REVOKED"
                : membership.enabled() ? "ACTIVE" : "SUSPENDED";
        return new UserOrgApiKeyResultBO(key == null ? null : key.getId(), params.orgId(), state,
                key == null ? null : key.getKeyPrefix(), key == null ? null : key.getGeneratedAt(),
                key == null ? null : key.getRevokedAt(), membership.enabled() ? null : suspendedReason(membership),
                membership.enabled(), key != null && key.getKeyHash() != null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserOrgApiKeyGenerateResultBO generate(UserOrgApiKeyParamsBO params) {
        var membership = requireOwnMembership(params);
        if (!membership.enabled()) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, suspendedReason(membership));
        }
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = "ll_mcp_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String digest = hash(raw);
        String prefix = raw.substring(0, 15);
        LocalDateTime now = LocalDateTime.now(clock);
        UserOrgApiKey key = current(params);
        if (key == null) {
            key = UserOrgApiKey.builder().userId(params.userId()).orgId(params.orgId()).keyHash(digest)
                    .keyPrefix(prefix).generatedAt(now).createdAt(now).updatedAt(now).build();
            try {
                baseMapper.insert(key);
            } catch (DuplicateKeyException exception) {
                throw new AppException(ResultCode.OPERATION_FAIL, "Key 状态已更新，请重新操作");
            }
        } else {
            baseMapper.update(null, new LambdaUpdateWrapper<UserOrgApiKey>().eq(UserOrgApiKey::getId, key.getId())
                    .set(UserOrgApiKey::getKeyHash, digest).set(UserOrgApiKey::getKeyPrefix, prefix)
                    .set(UserOrgApiKey::getGeneratedAt, now).set(UserOrgApiKey::getRevokedAt, null)
                    .set(UserOrgApiKey::getUpdatedAt, now));
        }
        return new UserOrgApiKeyGenerateResultBO(key.getId(), params.orgId(), raw, prefix, now);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revoke(UserOrgApiKeyParamsBO params) {
        requireOwnMembership(params);
        revokeMatching(new LambdaUpdateWrapper<UserOrgApiKey>().eq(UserOrgApiKey::getUserId, params.userId())
                .eq(UserOrgApiKey::getOrgId, params.orgId()));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void revokeAllForUser(Long userId) {
        revokeMatching(new LambdaUpdateWrapper<UserOrgApiKey>().eq(UserOrgApiKey::getUserId, userId));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void revokeForMembership(UserOrgApiKeyParamsBO params) {
        revokeMatching(new LambdaUpdateWrapper<UserOrgApiKey>().eq(UserOrgApiKey::getUserId, params.userId())
                .eq(UserOrgApiKey::getOrgId, params.orgId()));
    }

    @Override
    public McpApiKeyIdentityResultBO authenticate(String apiKey) {
        if (!apiKey.matches("ll_mcp_[A-Za-z0-9_-]{43}")) {
            throw new AppException(ResultCode.UNAUTHORIZED, "API Key 无效或已作废");
        }
        UserOrgApiKey key = baseMapper.selectOne(new LambdaQueryWrapper<UserOrgApiKey>()
                .eq(UserOrgApiKey::getKeyHash, hash(apiKey)));
        if (key == null) {
            throw new AppException(ResultCode.UNAUTHORIZED, "API Key 无效或已作废");
        }
        OrganizationMembershipStatusResultBO membership;
        try {
            membership = membershipService.getOwnMembershipStatus(new OrganizationMembershipParamsBO(key.getUserId(), key.getOrgId()));
        } catch (AppException exception) {
            throw new AppException(ResultCode.UNAUTHORIZED, "API Key 对应的成员关系不可用");
        }
        if (!membership.userEnabled()) {
            throw new AppException(ResultCode.UNAUTHORIZED, "账号已被禁用");
        }
        if (!membership.enabled()) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, suspendedReason(membership));
        }
        return new McpApiKeyIdentityResultBO(key.getUserId(), key.getOrgId(), key.getId());
    }

    private OrganizationMembershipStatusResultBO requireOwnMembership(UserOrgApiKeyParamsBO params) {
        var membership = membershipService.getOwnMembershipStatus(new OrganizationMembershipParamsBO(params.userId(), params.orgId()));
        if (!membership.userEnabled()) {
            throw new AppException(ResultCode.UNAUTHORIZED);
        }
        return membership;
    }

    private UserOrgApiKey current(UserOrgApiKeyParamsBO params) {
        return baseMapper.selectOne(new LambdaQueryWrapper<UserOrgApiKey>()
                .eq(UserOrgApiKey::getUserId, params.userId()).eq(UserOrgApiKey::getOrgId, params.orgId()));
    }

    private void revokeMatching(LambdaUpdateWrapper<UserOrgApiKey> update) {
        LocalDateTime now = LocalDateTime.now(clock);
        baseMapper.update(null, update.isNotNull(UserOrgApiKey::getKeyHash).set(UserOrgApiKey::getKeyHash, null)
                .set(UserOrgApiKey::getRevokedAt, now).set(UserOrgApiKey::getUpdatedAt, now));
    }

    private String suspendedReason(OrganizationMembershipStatusResultBO membership) {
        return !membership.organizationEnabled() ? "组织已被禁用" : "您在该组织中的成员状态已被禁用";
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("运行环境缺少 SHA-256", exception);
        }
    }
}
