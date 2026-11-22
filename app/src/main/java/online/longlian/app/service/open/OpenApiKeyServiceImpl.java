package online.longlian.app.service.open;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.properties.OpenApiProperties;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.util.Sha256Util;
import online.longlian.app.mapper.OpenApiKeyMapper;
import online.longlian.app.mapper.OrganizationMapper;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.pojo.bo.open.OpenApiKeyAuthResultBO;
import online.longlian.app.pojo.bo.open.OpenApiKeyCreateParamsBO;
import online.longlian.app.pojo.bo.open.OpenApiKeyCreateResultBO;
import online.longlian.app.pojo.bo.open.OpenApiKeyGroupResultBO;
import online.longlian.app.pojo.bo.open.OpenApiKeyItemResultBO;
import online.longlian.app.pojo.bo.open.OpenApiKeyRevokeParamsBO;
import online.longlian.app.pojo.entity.OpenApiKey;
import online.longlian.app.pojo.entity.Organization;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.service.lock.LockService;
import online.longlian.common.enumeration.OpenApiKeyStatus;
import online.longlian.common.enumeration.Status;
import online.longlian.common.service.DistributedLockService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * OpenAPI 密钥服务。明文只在创建链路出现一次，服务端只存 SHA-256；
 * 吊销与成员资格失效都在每请求认证时生效，无需事件同步。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenApiKeyServiceImpl implements OpenApiKeyService {

    /** 密钥形态：oa_ 前缀 + 43 位随机串，约 256 位熵。 */
    private static final String KEY_PREFIX = "oa_";
    private static final int KEY_RANDOM_CHARS = 43;
    private static final String KEY_ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final SecureRandom RANDOM = new SecureRandom();

    /** 创建密钥的计数锁：短促 check-then-act 关键区，锁不可用时宁可创建失败也不无上限放行。 */
    private static final String CREATE_LOCK_PREFIX = "open:apikey:create:";

    private final OpenApiKeyMapper openApiKeyMapper;
    // 跨模块只读（standard #9）：密钥域的分组视图与成员资格校验需要组织名、组织状态与成员行，
    // 组织/成员域没有表达这些业务意图的方法；本域不写组织侧数据。
    private final OrganizationMapper organizationMapper;
    private final OrganizationMemberMapper organizationMemberMapper;
    private final LockService lockService;
    private final OpenApiProperties openApiProperties;
    private final Clock clock;

    @Override
    public OpenApiKeyAuthResultBO authenticate(String plaintextKey) {
        String hash = Sha256Util.sha256Hex(plaintextKey);
        OpenApiKey key = openApiKeyMapper.selectOne(new LambdaQueryWrapper<OpenApiKey>()
                .eq(OpenApiKey::getKeyHash, hash)
                .last("LIMIT 1"));
        // 密钥无效、吊销、成员资格失效、组织停用同语义返回，不区分原因，不泄露组织存在性
        if (key == null || key.getStatus() != OpenApiKeyStatus.ENABLED) {
            throw new AppException(ResultCode.UNAUTHORIZED, "密钥无效或已吊销");
        }
        OrganizationMember member = organizationMemberMapper.selectOne(
                new LambdaQueryWrapper<OrganizationMember>()
                        .eq(OrganizationMember::getOrgId, key.getOrgId())
                        .eq(OrganizationMember::getUserId, key.getUserId())
                        .eq(OrganizationMember::getStatus, Status.ENABLED)
                        .last("LIMIT 1"));
        if (member == null) {
            throw new AppException(ResultCode.UNAUTHORIZED, "密钥无效或已吊销");
        }
        Organization organization = organizationMapper.selectById(key.getOrgId());
        if (organization == null || organization.getStatus() != Status.ENABLED) {
            throw new AppException(ResultCode.UNAUTHORIZED, "密钥无效或已吊销");
        }
        return new OpenApiKeyAuthResultBO(key.getId(), key.getUserId(), key.getOrgId());
    }

    @Async
    @Override
    public void touchLastUsedAsync(Long keyId) {
        openApiKeyMapper.update(null, new LambdaUpdateWrapper<OpenApiKey>()
                .set(OpenApiKey::getLastUsedAt, LocalDateTime.now(clock))
                .eq(OpenApiKey::getId, keyId));
    }

    @Override
    public OpenApiKeyCreateResultBO create(OpenApiKeyCreateParamsBO params) {
        try (DistributedLockService.Lock lock = lockService.tryAcquireOrThrow(
                CREATE_LOCK_PREFIX + params.userId() + ":" + params.orgId(), 3, 10, TimeUnit.SECONDS)) {
            Long count = openApiKeyMapper.selectCount(new LambdaQueryWrapper<OpenApiKey>()
                    .eq(OpenApiKey::getUserId, params.userId())
                    .eq(OpenApiKey::getOrgId, params.orgId())
                    .eq(OpenApiKey::getStatus, OpenApiKeyStatus.ENABLED));
            if (count >= openApiProperties.getMaxKeysPerOrg()) {
                throw new AppException(ResultCode.OPERATION_FAIL, "该组织密钥数量已达上限");
            }
            String plaintext = KEY_PREFIX + randomKey();
            LocalDateTime now = LocalDateTime.now(clock);
            OpenApiKey key = OpenApiKey.builder()
                    .orgId(params.orgId())
                    .userId(params.userId())
                    .name(params.name())
                    .keyHash(Sha256Util.sha256Hex(plaintext))
                    .status(OpenApiKeyStatus.ENABLED)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            openApiKeyMapper.insert(key);
            return new OpenApiKeyCreateResultBO(key.getId(), plaintext);
        }
    }

    @Override
    public List<OpenApiKeyGroupResultBO> listGroups(Long userId) {
        List<OrganizationMember> memberships = organizationMemberMapper.selectList(
                new LambdaQueryWrapper<OrganizationMember>()
                        .eq(OrganizationMember::getUserId, userId)
                        .eq(OrganizationMember::getStatus, Status.ENABLED)
                        .orderByAsc(OrganizationMember::getJoinedAt)
                        .orderByAsc(OrganizationMember::getOrgId));
        if (memberships.isEmpty()) {
            return List.of();
        }
        List<Long> orgIds = memberships.stream().map(OrganizationMember::getOrgId).distinct().toList();
        Map<Long, String> orgNames = organizationMapper.selectList(new LambdaQueryWrapper<Organization>()
                        .in(Organization::getId, orgIds))
                .stream()
                .collect(Collectors.toMap(Organization::getId, Organization::getName, (a, b) -> a));
        Map<Long, List<OpenApiKeyItemResultBO>> keysByOrg = openApiKeyMapper.selectList(
                        new LambdaQueryWrapper<OpenApiKey>()
                                .eq(OpenApiKey::getUserId, userId)
                                .eq(OpenApiKey::getStatus, OpenApiKeyStatus.ENABLED)
                                .orderByAsc(OpenApiKey::getId))
                .stream()
                .collect(Collectors.groupingBy(OpenApiKey::getOrgId,
                        Collectors.mapping(this::toItemBO, Collectors.toList())));
        return memberships.stream()
                .map(member -> new OpenApiKeyGroupResultBO(
                        member.getOrgId(),
                        orgNames.getOrDefault(member.getOrgId(), ""),
                        openApiProperties.getMaxKeysPerOrg(),
                        keysByOrg.getOrDefault(member.getOrgId(), List.of())))
                .toList();
    }

    @Override
    public void revoke(OpenApiKeyRevokeParamsBO params) {
        OpenApiKey key = openApiKeyMapper.selectById(params.keyId());
        if (key == null || !key.getUserId().equals(params.userId()) || !key.getOrgId().equals(params.orgId())) {
            throw new AppException(ResultCode.DATA_NOT_EXIT);
        }
        if (key.getStatus() != OpenApiKeyStatus.ENABLED) {
            // 已吊销重复请求按成功处理
            return;
        }
        openApiKeyMapper.update(null, new LambdaUpdateWrapper<OpenApiKey>()
                .set(OpenApiKey::getStatus, OpenApiKeyStatus.REVOKED)
                .set(OpenApiKey::getRevokedAt, LocalDateTime.now(clock))
                .eq(OpenApiKey::getId, params.keyId())
                .eq(OpenApiKey::getStatus, OpenApiKeyStatus.ENABLED));
    }

    @Override
    public void revokeAllForUser(Long userId) {
        openApiKeyMapper.update(null, new LambdaUpdateWrapper<OpenApiKey>()
                .set(OpenApiKey::getStatus, OpenApiKeyStatus.REVOKED)
                .set(OpenApiKey::getRevokedAt, LocalDateTime.now(clock))
                .eq(OpenApiKey::getUserId, userId)
                .eq(OpenApiKey::getStatus, OpenApiKeyStatus.ENABLED));
    }

    private OpenApiKeyItemResultBO toItemBO(OpenApiKey key) {
        return new OpenApiKeyItemResultBO(key.getId(), key.getName(), key.getCreatedAt(), key.getLastUsedAt());
    }

    private String randomKey() {
        StringBuilder builder = new StringBuilder(KEY_RANDOM_CHARS);
        for (int i = 0; i < KEY_RANDOM_CHARS; i++) {
            builder.append(KEY_ALPHABET.charAt(RANDOM.nextInt(KEY_ALPHABET.length())));
        }
        return builder.toString();
    }
}
