package online.longlian.app.service.otp.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.constants.InviteConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.util.RandomCodeUtil;
import online.longlian.app.mapper.OrganizationCreateOtpMapper;
import online.longlian.app.pojo.bo.OTPGenerateContextBO;
import online.longlian.app.pojo.bo.OTPUseContextBO;
import online.longlian.app.pojo.bo.OTPValidateContextBO;
import online.longlian.app.pojo.bo.OneTimePasswordCreateParamsBO;
import online.longlian.app.pojo.entity.OneTimePassword;
import online.longlian.app.pojo.entity.OrganizationCreateOtp;
import online.longlian.app.service.otp.OneTimePasswordService;
import online.longlian.app.service.otp.OTPStrategyService;
import online.longlian.generator.enumeration.OTPType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrganizationCreateInviteService implements OTPStrategyService {

    private final OneTimePasswordService oneTimePasswordService;
    private final OrganizationCreateOtpMapper organizationCreateOtpMapper;

    @Override
    public OTPType getOtpType() {
        return OTPType.OrganizationInvite;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OneTimePassword generate(OTPGenerateContextBO otpGenerateContextBO) {
        LocalDateTime expiredAt = LocalDateTime.now().plusMinutes(InviteConstants.INVITE_EXPIRE_MINUTES);
        String inviteCode = RandomCodeUtil.generateCode(InviteConstants.INVITE_CODE_LENGTH);

        OneTimePassword oneTimePassword = oneTimePasswordService.generateOTP(
                OneTimePasswordCreateParamsBO.builder()
                        .code(inviteCode)
                        .expiredAt(expiredAt)
                        .bizType(OTPType.OrganizationInvite)
                        .creatorId(otpGenerateContextBO.getCreatorId())
                        .build()
        );

        // 关联 OrganizationCreateOtp 记录：注册流程中需要通过 otpId 校验邀请码未被占用
        OrganizationCreateOtp organizationCreateOtp = OrganizationCreateOtp.builder()
                .otpId(oneTimePassword.getId())
                .build();
        organizationCreateOtpMapper.insert(organizationCreateOtp);

        return oneTimePassword;
    }

    @Override
    public OneTimePassword getValid(OTPValidateContextBO otpValidateContextBO) {
        OneTimePassword oneTimePassword = oneTimePasswordService.getValidOTP(otpValidateContextBO.getCode(), OTPType.OrganizationInvite);
        OrganizationCreateOtp organizationCreateOtp = organizationCreateOtpMapper.selectOne(
                new LambdaQueryWrapper<OrganizationCreateOtp>()
                        .eq(OrganizationCreateOtp::getOtpId, oneTimePassword.getId())
                        .last("LIMIT 1")
        );
        if (organizationCreateOtp == null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "邀请码不存在");
        }
        return oneTimePassword;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void use(OTPUseContextBO otpUseContextBO) {
        oneTimePasswordService.useOTP(otpUseContextBO.getOtpId());
        int rows = organizationCreateOtpMapper.update(
                null,
                new LambdaUpdateWrapper<OrganizationCreateOtp>()
                        .eq(OrganizationCreateOtp::getOtpId, otpUseContextBO.getOtpId())
                        .isNull(OrganizationCreateOtp::getInvitedUserId)
                        .set(OrganizationCreateOtp::getInvitedUserId, otpUseContextBO.getUserId())
                        .set(otpUseContextBO.getOrgId() != null, OrganizationCreateOtp::getOrgId, otpUseContextBO.getOrgId())
        );
        if (rows == 0) {
            throw new AppException(ResultCode.OPERATION_FAIL, "邀请码已被使用");
        }
    }
}
