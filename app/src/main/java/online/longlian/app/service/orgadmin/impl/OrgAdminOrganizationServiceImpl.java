package online.longlian.app.service.orgadmin.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.mapper.OrganizationMapper;
import online.longlian.app.pojo.bo.OrgAdminGetOrganizationInfoResultBO;
import online.longlian.app.pojo.bo.OrgAdminUpdateOrganizationInfoParamsBO;
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
        return OrgAdminGetOrganizationInfoResultBO.builder()
                .id(organization.getId())
                .avatarFileId(organization.getAvatarFileId())
                .name(organization.getName())
                .description(organization.getDescription())
                .avatarUrl(organization.getAvatarFileId() != null
                        ? resourceService.getResourceReadUrl(organization.getAvatarFileId())
                        : null)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrganizationInfo(OrgAdminUpdateOrganizationInfoParamsBO params) {
        organizationMapper.update(null,
                new LambdaUpdateWrapper<Organization>()
                        .eq(Organization::getId, params.getOrgId())
                        .set(Organization::getName, params.getName())
                        .set(Organization::getAvatarFileId, params.getAvatarFileId())
                        .set(Organization::getDescription, params.getDescription())
        );
    }
}
