package online.longlian.app.service.invite.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.constants.InviteConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.util.RandomCodeUtil;
import online.longlian.app.mapper.OrganizationInviteCodeMapper;
import online.longlian.app.mapper.OrganizationMapper;
import online.longlian.app.pojo.bo.invite.OrganizationInviteCodeAcceptParamsBO;
import online.longlian.app.pojo.bo.invite.OrganizationInviteCodeGenerateParamsBO;
import online.longlian.app.pojo.bo.invite.OrganizationInviteCodeGenerateResultBO;
import online.longlian.app.pojo.entity.Organization;
import online.longlian.app.pojo.entity.OrganizationInviteCode;
import online.longlian.app.service.invite.OrganizationInviteCodeService;
import online.longlian.common.enumeration.OTPType;
import online.longlian.common.enumeration.Status;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrganizationInviteCodeServiceImpl implements OrganizationInviteCodeService {

    private static final int GENERATE_ATTEMPTS = 5;
    private static final int NOT_ACCEPTED = 0;
    private static final int ACCEPTED = 1;

    private final OrganizationInviteCodeMapper organizationInviteCodeMapper;
    private final OrganizationMapper organizationMapper;
    private final Clock clock;

    @Override
    public OrganizationInviteCodeGenerateResultBO generate(OrganizationInviteCodeGenerateParamsBO params) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (params.getExpiredAt() == null || !params.getExpiredAt().isAfter(now)) {
            throw new AppException(ResultCode.PARAM_ERROR, "到期时间必须晚于当前时间");
        }
        if (params.getBizType() != OTPType.OrganizationInvite && params.getBizType() != OTPType.OrganizationUserInvite) {
            throw new AppException(ResultCode.PARAM_ERROR, "邀请码类型无效");
        }

        Long orgId = null;
        if (params.getBizType() == OTPType.OrganizationUserInvite) {
            orgId = requireEnabledOrganization(params.getOrgId()).getId();
        }

        OrganizationInviteCode record = OrganizationInviteCode.builder()
                .code(nextCode(now))
                .bizType(params.getBizType())
                .orgId(orgId)
                .creatorId(params.getCreatorId())
                .expiredAt(params.getExpiredAt())
                .accepted(NOT_ACCEPTED)
                .build();
        organizationInviteCodeMapper.insert(record);
        return OrganizationInviteCodeGenerateResultBO.builder()
                .id(record.getId())
                .inviteCode(record.getCode())
                .expiredAt(record.getExpiredAt())
                .build();
    }

    @Override
    public OrganizationInviteCode getValid(String code, OTPType bizType) {
        LocalDateTime now = LocalDateTime.now(clock);
        OrganizationInviteCode active = organizationInviteCodeMapper.selectOne(new LambdaQueryWrapper<OrganizationInviteCode>()
                .eq(OrganizationInviteCode::getCode, code)
                .eq(OrganizationInviteCode::getBizType, bizType)
                .eq(OrganizationInviteCode::getAccepted, NOT_ACCEPTED)
                .gt(OrganizationInviteCode::getExpiredAt, now)
                .orderByDesc(OrganizationInviteCode::getId)
                .last("LIMIT 1"));
        if (active != null) {
            return active;
        }
        throw unavailable(latest(code, bizType), now);
    }

    @Override
    public void accept(OrganizationInviteCodeAcceptParamsBO params) {
        LocalDateTime now = LocalDateTime.now(clock);
        LambdaUpdateWrapper<OrganizationInviteCode> update = new LambdaUpdateWrapper<OrganizationInviteCode>()
                .eq(OrganizationInviteCode::getId, params.getId())
                .eq(OrganizationInviteCode::getAccepted, NOT_ACCEPTED)
                .gt(OrganizationInviteCode::getExpiredAt, now)
                .set(OrganizationInviteCode::getAccepted, ACCEPTED)
                .set(OrganizationInviteCode::getAcceptedAt, now);
        if (params.getInviteeUserId() != null) {
            update.set(OrganizationInviteCode::getInviteeUserId, params.getInviteeUserId());
        }
        if (params.getOrgId() != null) {
            update.set(OrganizationInviteCode::getOrgId, params.getOrgId());
        }
        if (params.getOrgMemberId() != null) {
            update.set(OrganizationInviteCode::getOrgMemberId, params.getOrgMemberId());
        }
        if (organizationInviteCodeMapper.update(null, update) == 0) {
            throw unavailable(organizationInviteCodeMapper.selectById(params.getId()), now);
        }
    }

    @Override
    public void bindInvitee(Long inviteCodeId, Long inviteeUserId, Long orgMemberId) {
        if (inviteCodeId == null) {
            return;
        }
        LambdaUpdateWrapper<OrganizationInviteCode> update = new LambdaUpdateWrapper<OrganizationInviteCode>()
                .eq(OrganizationInviteCode::getId, inviteCodeId)
                .set(OrganizationInviteCode::getOrgMemberId, orgMemberId);
        if (inviteeUserId != null) {
            update.set(OrganizationInviteCode::getInviteeUserId, inviteeUserId);
        }
        organizationInviteCodeMapper.update(null, update);
    }

    private String nextCode(LocalDateTime now) {
        for (int attempt = 0; attempt < GENERATE_ATTEMPTS; attempt++) {
            String code = RandomCodeUtil.generateCode(InviteConstants.INVITE_CODE_LENGTH);
            Long activeCount = organizationInviteCodeMapper.selectCount(new LambdaQueryWrapper<OrganizationInviteCode>()
                    .eq(OrganizationInviteCode::getCode, code)
                    .eq(OrganizationInviteCode::getAccepted, NOT_ACCEPTED)
                    .gt(OrganizationInviteCode::getExpiredAt, now));
            if (activeCount == null || activeCount == 0) {
                return code;
            }
        }
        throw new AppException(ResultCode.OPERATION_FAIL, "邀请码生成失败");
    }

    private OrganizationInviteCode latest(String code, OTPType bizType) {
        return organizationInviteCodeMapper.selectOne(new LambdaQueryWrapper<OrganizationInviteCode>()
                .eq(OrganizationInviteCode::getCode, code)
                .eq(OrganizationInviteCode::getBizType, bizType)
                .orderByDesc(OrganizationInviteCode::getId)
                .last("LIMIT 1"));
    }

    private AppException unavailable(OrganizationInviteCode inviteCode, LocalDateTime now) {
        if (inviteCode == null) {
            return new AppException(ResultCode.OPERATION_FAIL, "邀请码无效");
        }
        if (Integer.valueOf(ACCEPTED).equals(inviteCode.getAccepted()) || inviteCode.getAcceptedAt() != null) {
            return new AppException(ResultCode.OPERATION_FAIL, "邀请码已接受");
        }
        if (inviteCode.getExpiredAt() == null || !inviteCode.getExpiredAt().isAfter(now)) {
            return new AppException(ResultCode.OPERATION_FAIL, "邀请码已过期");
        }
        return new AppException(ResultCode.OPERATION_FAIL, "邀请码无效");
    }

    // 发加入组织邀请前必须确认目标组织仍启用。组织模块没有不带管理员角色校验的查询方法，这里直接读组织表。
    private Organization requireEnabledOrganization(Long orgId) {
        if (orgId == null) {
            throw new AppException(ResultCode.PARAM_ERROR, "目标组织不能为空");
        }
        Organization organization = organizationMapper.selectById(orgId);
        if (organization == null || organization.getStatus() == Status.DISABLED) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "组织不存在或已禁用");
        }
        return organization;
    }
}
