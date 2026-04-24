package online.longlian.app.service.user.impl;

import lombok.RequiredArgsConstructor;
import lombok.NonNull;
import online.longlian.app.common.constants.InviteConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.util.RandomCodeUtil;
import online.longlian.app.mapper.OrganizationJoinOptMapper;
import online.longlian.app.mapper.OrganizationMapper;
import online.longlian.app.pojo.bo.OneTimePasswordCreateParamsBO;
import online.longlian.app.pojo.bo.OrgAdminGenerateJoinOrgInviteCodeParamsBO;
import online.longlian.app.pojo.bo.OrgAdminGenerateJoinOrgInviteCodeResultBO;
import online.longlian.app.pojo.entity.OneTimePassword;
import online.longlian.app.pojo.entity.Organization;
import online.longlian.app.pojo.entity.OrganizationJoinOpt;
import online.longlian.app.service.common.OneTimePasswordService;
import online.longlian.app.service.user.OrganizationMemberService;
import online.longlian.generator.enumeration.OTPType;
import online.longlian.generator.enumeration.Status;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class OrganizationMemberServiceImpl implements OrganizationMemberService {

    private static final DateTimeFormatter DEFAULT_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(InviteConstants.DEFAULT_DATE_TIME_PATTERN);

    private final OrganizationMapper organizationMapper;
    private final OneTimePasswordService oneTimePasswordService;
    private final OrganizationJoinOptMapper organizationJoinOptMapper;

    @Override
    public OrgAdminGenerateJoinOrgInviteCodeResultBO generateJoinOrgInviteCode(@NonNull OrgAdminGenerateJoinOrgInviteCodeParamsBO params) {
        Organization organization = getEnabledOrganization(params.getOrgId());
        LocalDateTime expiredAt = LocalDateTime.now().plusMinutes(InviteConstants.INVITE_EXPIRE_MINUTES);
        String inviteCode = RandomCodeUtil.generateCode(InviteConstants.INVITE_CODE_LENGTH);

        OneTimePassword oneTimePassword = oneTimePasswordService.generateOTP(
                OneTimePasswordCreateParamsBO.builder()
                        .code(inviteCode)
                        .expiredAt(expiredAt)
                        .bizType(OTPType.OrganizationUserInvite)
                        .creatorId(params.getCreatorId())
                        .build()
        );

        OrganizationJoinOpt organizationJoinOpt = OrganizationJoinOpt.builder()
                .otpId(oneTimePassword.getId())
                .orgId(organization.getId())
                .build();
        organizationJoinOptMapper.insert(organizationJoinOpt);

        return OrgAdminGenerateJoinOrgInviteCodeResultBO.builder()
                .inviteCode(inviteCode)
                .expireAt(expiredAt.format(DEFAULT_DATE_TIME_FORMATTER))
                .build();
    }

    private Organization getEnabledOrganization(Long orgId) {
        Organization organization = organizationMapper.selectById(orgId);
        if (organization == null || organization.getStatus() == Status.DISABLED) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "组织不存在或已禁用");
        }
        return organization;
    }
}
