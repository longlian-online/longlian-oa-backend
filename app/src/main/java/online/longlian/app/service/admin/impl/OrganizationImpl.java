package online.longlian.app.service.admin.impl;


import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import online.longlian.app.mapper.OrganizationMapper;
import online.longlian.app.pojo.bo.AdminOrganizationListBO;
import online.longlian.app.pojo.bo.AdminOrganizationUpdateStatusParamsBO;
import online.longlian.app.pojo.entity.Organization;
import online.longlian.app.pojo.vo.admin.OrgDetailInfoVO;
import online.longlian.app.pojo.vo.common.PageResultVO;
import online.longlian.app.pojo.vo.orgadmin.InviteCodeVO;
import online.longlian.app.service.admin.OrganizationService;
import online.longlian.app.service.resource.StorageService;

import java.util.List;
import java.util.Map;

import static com.baomidou.mybatisplus.core.toolkit.Wrappers.lambdaUpdate;

@AllArgsConstructor
public class OrganizationImpl implements OrganizationService {
    private final OrganizationMapper organizationMapper;
    private final StorageService storageService;

    public PageResultVO<OrgDetailInfoVO> getOrgListInfo(@NonNull AdminOrganizationListBO params) {
        Page<Organization> page = new Page<>(params.getPageNum(), params.getPageSize());
        Page<Organization> organizationPage = new LambdaQueryChainWrapper<>(organizationMapper)
                .select(
                        Organization::getId, Organization::getName,
                        Organization::getName,
                        Organization::getAvatarFileId,
                        Organization::getStatus,
                        Organization::getCreatedAt
                ).page(page);

        List<Organization> organizations = organizationPage.getRecords();
        long total = organizationPage.getTotal();

        List<Long> avatarIds = organizations.stream().map(Organization::getAvatarFileId).toList();
        Map<Long, String> fileUrlMap = storageService.getResourceReadUrls(avatarIds);

        List<OrgDetailInfoVO> list = organizations.stream().map(organization -> new OrgDetailInfoVO(organization.getId(),
                organization.getName(),
                fileUrlMap.get(organization.getAvatarFileId()),
                organization.getStatus(),
                organization.getCreatedAt()
        )).toList();


        PageResultVO<OrgDetailInfoVO> result = new PageResultVO<>();
        result.setList(list);
        result.setTotal(total);

        return result;
    }

    public InviteCodeVO generateCreateOrgInviteCode() {
        return null;
    }

    public void updateOrgStatus(@NonNull AdminOrganizationUpdateStatusParamsBO params) {
        LambdaUpdateWrapper<Organization> wrapper = lambdaUpdate(Organization.class).eq(Organization::getId, params.getOrganizationId()).set(Organization::getId, params.getOrganizationId());
        organizationMapper.update(null, wrapper);
    }
}
