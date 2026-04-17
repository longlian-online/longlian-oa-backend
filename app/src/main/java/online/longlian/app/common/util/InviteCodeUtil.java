package online.longlian.app.common.util;

import lombok.RequiredArgsConstructor;
import online.longlian.app.common.constants.InviteConstants;
import online.longlian.app.common.constants.RedisConstants;
import online.longlian.app.common.enumeration.InviteMode;
import online.longlian.app.pojo.bo.InviteCodeCacheBO;
import online.longlian.app.pojo.vo.orgadmin.InviteCodeVO;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class InviteCodeUtil {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern(InviteConstants.DEFAULT_DATE_TIME_PATTERN);

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 生成并缓存邀请码。
     */
    public InviteCodeVO generateInviteCode(
            InviteMode inviteMode,
            Long orgId,
            String orgName) {
        String inviteCode = RandomCodeUtil.generateCode(InviteConstants.INVITE_CODE_LENGTH);
        LocalDateTime expireAt = LocalDateTime.now().plusMinutes(InviteConstants.INVITE_EXPIRE_MINUTES);
        String expireAtText = expireAt.format(DATE_TIME_FORMATTER);

        InviteCodeCacheBO inviteData = InviteCodeCacheBO.builder()
                .inviteMode(inviteMode)
                .orgId(orgId)
                .orgName(orgName)
                .expireAt(expireAtText)
                .build();
        redisTemplate.opsForValue().set(
                RedisConstants.INVITE_CODE + inviteCode,
                inviteData,
                InviteConstants.INVITE_EXPIRE_MINUTES,
                TimeUnit.MINUTES
        );

        return InviteCodeVO.builder()
                .inviteCode(inviteCode)
                .expireAt(expireAtText)
                .build();
    }
}
