package online.longlian.app.service.otp.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.constants.InviteConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.util.RandomCodeUtil;
import online.longlian.app.mapper.OrganizationJoinOtpMapper;
import online.longlian.app.mapper.OrganizationMapper;
import online.longlian.app.pojo.bo.OTPGenerateContextBO;
import online.longlian.app.pojo.bo.OTPUseContextBO;
import online.longlian.app.pojo.bo.OTPValidateContextBO;
import online.longlian.app.pojo.bo.OneTimePasswordCreateParamsBO;
import online.longlian.app.pojo.entity.OneTimePassword;
import online.longlian.app.pojo.entity.Organization;
import online.longlian.app.pojo.entity.OrganizationJoinOtp;
import online.longlian.app.service.otp.OneTimePasswordService;
import online.longlian.app.service.otp.OTPStrategyService;
import online.longlian.generator.enumeration.OTPType;
import online.longlian.generator.enumeration.Status;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrganizationJoinInviteService implements OTPStrategyService {

    private final OneTimePasswordService oneTimePasswordService;
    private final OrganizationJoinOtpMapper organizationJoinOtpMapper;
    private final OrganizationMapper organizationMapper;

    @Override
    public OTPType getOtpType() {
        return OTPType.OrganizationUserInvite;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OneTimePassword generate(OTPGenerateContextBO otpGenerateContextBO) {
        // 生成前校验目标组织存在且未禁用
        Organization organization = getEnabledOrganization(otpGenerateContextBO.getOrgId());
        LocalDateTime expiredAt = LocalDateTime.now().plusMinutes(InviteConstants.INVITE_EXPIRE_MINUTES);
        String inviteCode = RandomCodeUtil.generateCode(InviteConstants.INVITE_CODE_LENGTH);

        OneTimePassword otp = oneTimePasswordService.generateOTP(
                OneTimePasswordCreateParamsBO.builder()
                        .code(inviteCode)
                        .expiredAt(expiredAt)
                        .bizType(OTPType.OrganizationUserInvite)
                        .creatorId(otpGenerateContextBO.getCreatorId())
                        .build()
        );

        OrganizationJoinOtp organizationJoinOtp = OrganizationJoinOtp.builder()
                .otpId(otp.getId())
                .orgId(organization.getId())
                .build();
        organizationJoinOtpMapper.insert(organizationJoinOtp);

        return otp;
    }

    @Override
    public OneTimePassword getValid(OTPValidateContextBO otpValidateContextBO) {
        OneTimePassword otp = oneTimePasswordService.getValidOTP(otpValidateContextBO.getCode(), OTPType.OrganizationUserInvite);
        OrganizationJoinOtp organizationJoinOtp = organizationJoinOtpMapper.selectOne(
                new LambdaQueryWrapper<OrganizationJoinOtp>()
                        .eq(OrganizationJoinOtp::getOtpId, otp.getId())
                        .last("LIMIT 1")
        );
        if (organizationJoinOtp == null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "邀请码不存在");
        }
        return otp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void use(OTPUseContextBO otpUseContextBO) {
        oneTimePasswordService.useOTP(otpUseContextBO.getOtpId());
        if (otpUseContextBO.getUserId() != null) {
            int rows = organizationJoinOtpMapper.update(
                    null,
                    new LambdaUpdateWrapper<OrganizationJoinOtp>()
                            .eq(OrganizationJoinOtp::getOtpId, otpUseContextBO.getOtpId())
                            .isNull(OrganizationJoinOtp::getInvitedUserId)
                            .set(OrganizationJoinOtp::getInvitedUserId, otpUseContextBO.getUserId())
            );
            if (rows == 0) {
                throw new AppException(ResultCode.OPERATION_FAIL, "邀请码已被使用");
            }
        }
    }

    private Organization getEnabledOrganization(Long orgId) {
        Organization organization = organizationMapper.selectById(orgId);
        if (organization == null || organization.getStatus() == Status.DISABLED) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "组织不存在或已禁用");
        }
        return organization;
    }
}
