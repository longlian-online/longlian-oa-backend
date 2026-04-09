package online.longlian.app.service.user.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.constants.InviteConstants;
import online.longlian.app.common.constants.RedisConstants;
import online.longlian.app.common.result.Result;
import online.longlian.app.mapper.OrganizationMapper;
import online.longlian.app.pojo.bo.InviteCacheBO;
import online.longlian.app.pojo.entity.Organization;
import online.longlian.app.pojo.vo.InviteLinkVO;
import online.longlian.app.service.user.OrganizationService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl extends ServiceImpl<OrganizationMapper, Organization> implements OrganizationService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern(InviteConstants.DEFAULT_DATE_TIME_PATTERN);

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Result<InviteLinkVO> generateInviteLink() {
        String inviteToken = RandomUtil.randomStringUpper(InviteConstants.INVITE_TOKEN_LENGTH);
        LocalDateTime expireAt = LocalDateTime.now().plusMinutes(InviteConstants.INVITE_EXPIRE_MINUTES);

        // 生成超管建组织场景的邀请链接，并写入 Redis
        InviteCacheBO inviteData = InviteCacheBO.builder()
                .inviteMode(InviteConstants.INVITE_MODE_SUPER_ADMIN_CREATE_ORG)
                .expireAt(expireAt.format(DATE_TIME_FORMATTER))
                .build();
        redisTemplate.opsForValue().set(
                RedisConstants.INVITE_LINK + inviteToken,
                inviteData,
                InviteConstants.INVITE_EXPIRE_MINUTES,
                TimeUnit.MINUTES
        );

        InviteLinkVO inviteLinkVO = InviteLinkVO.builder()
                .inviteToken(inviteToken)
                .inviteMode(InviteConstants.INVITE_MODE_SUPER_ADMIN_CREATE_ORG)
                .needFetchOrgInfo(false)
                .expireAt(expireAt.format(DATE_TIME_FORMATTER))
                .inviteUrl(InviteConstants.INVITE_URL_PATH + inviteToken)
                .build();
        return Result.success("生成成功", inviteLinkVO);
    }
}
