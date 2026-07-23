package online.longlian.app.service.orgadmin.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.OrganizationMapper;
import online.longlian.app.pojo.bo.orgadmin.OrgAdminGetOrganizationInfoResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgAdminUpdateOrganizationInfoParamsBO;
import online.longlian.app.pojo.entity.Organization;
import online.longlian.app.service.orgadmin.OrgAdminOrganizationService;
import online.longlian.app.service.resource.ResourceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrgAdminOrganizationServiceImpl implements OrgAdminOrganizationService {

    private final OrganizationMapper organizationMapper;
    private final ResourceService resourceService;

    @Override
    public OrgAdminGetOrganizationInfoResultBO getOrganizationInfo(Long orgId) {
        Organization organization = organizationMapper.selectById(orgId);
        if (organization == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "组织不存在");
        }
        return OrgAdminGetOrganizationInfoResultBO.builder()
                .id(organization.getId())
                .avatarFileId(organization.getAvatarFileId())
                .name(organization.getName())
                .description(organization.getDescription())
                .avatarUrl(organization.getAvatarFileId() != null && organization.getAvatarFileId() > 0
                        ? resourceService.getResourceReadUrl(organization.getAvatarFileId())
                        : null)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrganizationInfo(OrgAdminUpdateOrganizationInfoParamsBO params) {
        resourceService.bindBizId(params.getAvatarFileId(), params.getOrgId(), params.getUserId(), params.getOrgId());
        organizationMapper.update(null,
                new LambdaUpdateWrapper<Organization>()
                        .eq(Organization::getId, params.getOrgId())
                        .set(Organization::getName, params.getName())
                        .set(Organization::getAvatarFileId, params.getAvatarFileId())
                        .set(Organization::getDescription, params.getDescription())
        );
    }
}
