package online.longlian.app.service.admin.impl;


import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import online.longlian.app.common.constants.InviteConstants;
import online.longlian.app.common.util.RandomCodeUtil;
import online.longlian.app.mapper.OrganizationCreateOtpMapper;
import online.longlian.app.mapper.OrganizationMapper;
import online.longlian.app.pojo.bo.*;
import online.longlian.app.pojo.entity.OneTimePassword;
import online.longlian.app.pojo.entity.Organization;
import online.longlian.app.pojo.entity.OrganizationCreateOtp;
import online.longlian.app.service.admin.OrganizationService;
import online.longlian.app.service.common.OneTimePasswordService;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.generator.enumeration.OTPType;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static com.baomidou.mybatisplus.core.toolkit.Wrappers.lambdaUpdate;

@Service
@AllArgsConstructor
public class OrganizationImpl implements OrganizationService {
    private static final DateTimeFormatter DEFAULT_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(InviteConstants.DEFAULT_DATE_TIME_PATTERN);

    private final OrganizationMapper organizationMapper;
    private final ResourceService resourceService;
    private final OneTimePasswordService oneTimePasswordService;
    private final OrganizationCreateOtpMapper organizationCreateOtpMapper;

    public PageResultBO<AdminOrganizationListResultBO> getOrgListInfo(@NonNull AdminOrganizationListParamsBO params) {
        Page<Organization> page = new Page<>(params.getPage().getPageNum(), params.getPage().getPageSize());
        Page<Organization> organizationPage = new LambdaQueryChainWrapper<>(organizationMapper)
                .select(
                        Organization::getId,
                        Organization::getName,
                        Organization::getAvatarFileId,
                        Organization::getStatus,
                        Organization::getCreatedAt
                ).page(page);

        List<Organization> organizations = organizationPage.getRecords();
        long total = organizationPage.getTotal();

        List<Long> avatarIds = organizations.stream().map(Organization::getAvatarFileId).toList();
        Map<Long, ResourceReadUrlGetResultBO> resourceMap = resourceService.getResourceReadUrls(avatarIds);

        ResourceReadUrlGetResultBO defaultResource = new ResourceReadUrlGetResultBO("", 0L, "");
        List<AdminOrganizationListResultBO> list = organizations.stream().map(organization -> new AdminOrganizationListResultBO(organization.getId(),
                organization.getName(),
                resourceMap.getOrDefault(organization.getAvatarFileId(), defaultResource).getUrl(),
                organization.getStatus(),
                organization.getCreatedAt()
        )).toList();


        return new PageResultBO<>(list, total);
    }

    @Override
    public AdminGenerateInviteCodeResultBO generateCreateOrgInviteCode(@NonNull AdminGenerateCreateOrgInviteCodeParamsBO params) {
        LocalDateTime expiredAt = LocalDateTime.now().plusMinutes(InviteConstants.INVITE_EXPIRE_MINUTES);
        String inviteCode = RandomCodeUtil.generateCode(InviteConstants.INVITE_CODE_LENGTH);

        OneTimePassword oneTimePassword = oneTimePasswordService.generateOTP(
                OneTimePasswordCreateParamsBO.builder()
                        .code(inviteCode)
                        .expiredAt(expiredAt)
                        .bizType(OTPType.OrganizationInvite)
                        .creatorId(params.getCreatorId())
                        .build()
        );

        OrganizationCreateOtp organizationCreateOtp = OrganizationCreateOtp.builder()
                .otpId(oneTimePassword.getId())
                .build();
        organizationCreateOtpMapper.insert(organizationCreateOtp);

        return AdminGenerateInviteCodeResultBO.builder()
                .inviteCode(inviteCode)
                .expireAt(expiredAt.format(DEFAULT_DATE_TIME_FORMATTER))
                .build();
    }

    public void updateOrgStatus(@NonNull AdminOrganizationUpdateStatusParamsBO params) {
        LambdaUpdateWrapper<Organization> wrapper = lambdaUpdate(Organization.class).eq(Organization::getId, params.getOrganizationId()).set(Organization::getStatus, params.getStatus());
        organizationMapper.update(null, wrapper);
    }
}
