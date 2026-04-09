package online.longlian.app.service.user.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.constants.InviteConstants;
import online.longlian.app.common.constants.RedisConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.Result;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.enumeration.ApplicationStatus;
import online.longlian.app.common.enumeration.Status;
import online.longlian.app.common.util.ThreadLocalUtil;
import online.longlian.app.mapper.GroupApplicationMapper;
import online.longlian.app.mapper.OrganizationMapper;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.pojo.bo.InviteCacheBO;
import online.longlian.app.pojo.bo.InviteCodeCacheBO;
import online.longlian.app.pojo.dto.JoinByInviteCodeDTO;
import online.longlian.app.pojo.entity.GroupApplication;
import online.longlian.app.pojo.entity.Organization;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.pojo.vo.InviteCodeVO;
import online.longlian.app.pojo.vo.InviteInfoVO;
import online.longlian.app.pojo.vo.InviteLinkVO;
import online.longlian.app.service.user.OrganizationMemberService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OrganizationMemberServiceImpl implements OrganizationMemberService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern(InviteConstants.DEFAULT_DATE_TIME_PATTERN);

    private final RedisTemplate<String, Object> redisTemplate;
    private final OrganizationMapper organizationMapper;
    private final OrganizationMemberMapper organizationMemberMapper;
    private final GroupApplicationMapper groupApplicationMapper;

    @Override
    public Result<InviteLinkVO> generateInviteLink() {
        Long currentOrgId = getCurrentOrgId();
        Organization organization = getEnabledOrganization(currentOrgId);

        String inviteToken = RandomUtil.randomStringUpper(InviteConstants.INVITE_TOKEN_LENGTH);
        LocalDateTime expireAt = LocalDateTime.now().plusMinutes(InviteConstants.INVITE_EXPIRE_MINUTES);

        // 生成管理员邀请入组链接，并附带组织信息给注册页使用
        InviteCacheBO inviteData = InviteCacheBO.builder()
                .inviteMode(InviteConstants.INVITE_MODE_ORG_ADMIN_JOIN)
                .orgId(currentOrgId)
                .orgName(organization.getName())
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
                .inviteMode(InviteConstants.INVITE_MODE_ORG_ADMIN_JOIN)
                .needFetchOrgInfo(true)
                .expireAt(expireAt.format(DATE_TIME_FORMATTER))
                .inviteUrl(InviteConstants.INVITE_URL_PATH + inviteToken)
                .build();
        return Result.success("生成成功", inviteLinkVO);
    }

    @Override
    public Result<InviteInfoVO> getInviteOrgInfo(String inviteToken) {
        InviteCacheBO inviteData = getInviteData(inviteToken);
        if (!InviteConstants.INVITE_MODE_ORG_ADMIN_JOIN.equals(inviteData.getInviteMode())) {
            throw new AppException(ResultCode.OPERATION_FAIL, "当前邀请链接不支持查询组织信息");
        }
        Organization organization = getEnabledOrganization(inviteData.getOrgId());

        InviteInfoVO inviteInfoVO = InviteInfoVO.builder()
                .orgId(organization.getId())
                .orgName(organization.getName())
                .build();
        return Result.success("查询成功", inviteInfoVO);
    }


    private Long getCurrentOrgId() {
        Object currentOrgId = redisTemplate.opsForValue().get(RedisConstants.CURRENT_ORG + ThreadLocalUtil.getUserBO().getId());
        if (currentOrgId == null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "当前组织不存在，请先切换组织");
        }
        return Long.parseLong(String.valueOf(currentOrgId));
    }

    private Organization getEnabledOrganization(Long orgId) {
        Organization organization = organizationMapper.selectById(orgId);
        if (organization == null || organization.getStatus() == Status.DISABLED) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "组织不存在或已禁用");
        }
        return organization;
    }

    private InviteCacheBO getInviteData(String inviteToken) {
        InviteCacheBO inviteData = (InviteCacheBO) redisTemplate.opsForValue().get(RedisConstants.INVITE_LINK + inviteToken);
        if (inviteData == null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "邀请链接不存在或已过期");
        }
        return inviteData;
    }

}
