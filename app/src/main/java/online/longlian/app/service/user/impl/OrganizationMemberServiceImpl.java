package online.longlian.app.service.user.impl;

import lombok.RequiredArgsConstructor;
import online.longlian.app.common.constants.RedisConstants;
import online.longlian.app.common.enumeration.InviteMode;
import online.longlian.app.common.enumeration.Status;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.Result;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.util.InviteCodeUtil;
import online.longlian.app.mapper.OrganizationMapper;
import online.longlian.app.pojo.bo.InviteCodeCacheBO;
import online.longlian.app.pojo.entity.Organization;
import online.longlian.app.pojo.vo.app.InviteInfoVO;
import online.longlian.app.pojo.vo.orgadmin.InviteCodeVO;
import online.longlian.app.service.user.OrganizationMemberService;
import online.longlian.app.service.user.SessionService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrganizationMemberServiceImpl implements OrganizationMemberService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final OrganizationMapper organizationMapper;
    private final InviteCodeUtil inviteCodeUtil;
    private final SessionService sessionService;

    @Override
    public Result<InviteCodeVO> generateInviteCode() {
        Long currentOrgId = getCurrentOrgId();
        Organization organization = getEnabledOrganization(currentOrgId);

        InviteCodeVO inviteCodeVO = inviteCodeUtil.generateInviteCode(
                InviteMode.ORG_ADMIN_INVITE,
                currentOrgId,
                organization.getName()
        );
        return Result.success("生成成功", inviteCodeVO);
    }

    @Override
    public Result<InviteInfoVO> getInviteOrgInfo(String inviteCode) {
        InviteCodeCacheBO inviteData = getInviteCodeData(inviteCode);
        if (inviteData.getInviteMode() != InviteMode.ORG_ADMIN_INVITE) {
            throw new AppException(ResultCode.OPERATION_FAIL, "当前邀请码不支持查询组织信息");
        }

        Organization organization = getEnabledOrganization(inviteData.getOrgId());
        InviteInfoVO inviteInfoVO = InviteInfoVO.builder()
                .orgId(organization.getId())
                .orgName(organization.getName())
                .build();
        return Result.success("查询成功", inviteInfoVO);
    }

    private Long getCurrentOrgId() {
        Object currentOrgId = redisTemplate.opsForValue().get(RedisConstants.CURRENT_ORG + sessionService.getCurrentUserId());
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

    private InviteCodeCacheBO getInviteCodeData(String inviteCode) {
        InviteCodeCacheBO inviteData = (InviteCodeCacheBO) redisTemplate.opsForValue().get(RedisConstants.INVITE_CODE + inviteCode);
        if (inviteData == null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "邀请码不存在或已过期");
        }
        return inviteData;
    }
}
