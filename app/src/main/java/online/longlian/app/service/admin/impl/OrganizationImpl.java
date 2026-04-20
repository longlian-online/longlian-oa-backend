package online.longlian.app.service.admin.impl;


import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import online.longlian.app.common.constants.InviteConstants;
import online.longlian.app.common.util.RandomCodeUtil;
import online.longlian.app.mapper.OrganizationCreateOptMapper;
import online.longlian.app.mapper.OrganizationMapper;
import online.longlian.app.pojo.bo.AdminGenerateCreateOrgInviteCodeParamsBO;
import online.longlian.app.pojo.bo.AdminGenerateInviteCodeResultBO;
import online.longlian.app.pojo.bo.AdminOrganizationListParamsBO;
import online.longlian.app.pojo.bo.AdminOrganizationListResultBO;
import online.longlian.app.pojo.bo.AdminOrganizationUpdateStatusParamsBO;
import online.longlian.app.pojo.bo.OneTimePasswordCreateParamsBO;
import online.longlian.app.pojo.bo.PageResultBO;
import online.longlian.app.pojo.entity.OneTimePassword;
import online.longlian.app.pojo.entity.Organization;
import online.longlian.app.pojo.entity.OrganizationCreateOpt;
import online.longlian.app.service.admin.OrganizationService;
import online.longlian.app.service.common.OneTimePasswordService;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.generator.enumeration.OPTStatus;
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
    private final OrganizationCreateOptMapper organizationCreateOptMapper;

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
        Map<Long, String> fileUrlMap = resourceService.getResourceReadUrls(avatarIds);

        List<AdminOrganizationListResultBO> list = organizations.stream().map(organization -> new AdminOrganizationListResultBO(organization.getId(),
                organization.getName(),
                fileUrlMap.get(organization.getAvatarFileId()),
                organization.getStatus(),
                organization.getCreatedAt()
        )).toList();


        return new PageResultBO<>(list, total);
    }

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

        OrganizationCreateOpt organizationCreateOpt = OrganizationCreateOpt.builder()
                .otpId(oneTimePassword.getId())
                .status(OPTStatus.PENDING)
                .build();
        organizationCreateOptMapper.insert(organizationCreateOpt);

        return AdminGenerateInviteCodeResultBO.builder()
                .inviteCode(inviteCode)
                .expireAt(expiredAt.format(DEFAULT_DATE_TIME_FORMATTER))
                .build();
    }

    public void updateOrgStatus(@NonNull AdminOrganizationUpdateStatusParamsBO params) {
        LambdaUpdateWrapper<Organization> wrapper = lambdaUpdate(Organization.class).eq(Organization::getId, params.getOrganizationId()).set(Organization::getId, params.getOrganizationId());
        organizationMapper.update(null, wrapper);
    }
}
