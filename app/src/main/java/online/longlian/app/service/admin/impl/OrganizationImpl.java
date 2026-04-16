package online.longlian.app.service.admin.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import online.longlian.app.common.result.Result;
import online.longlian.app.mapper.OrganizationMapper;
import online.longlian.app.pojo.bo.AdminOrganizationListBO;
import online.longlian.app.pojo.dto.common.ChangeStatusDTO;
import online.longlian.app.pojo.entity.Organization;
import online.longlian.app.pojo.vo.admin.OrgDetailInfoVO;
import online.longlian.app.pojo.vo.common.PageResultVO;
import online.longlian.app.pojo.vo.orgadmin.InviteCodeVO;
import online.longlian.app.service.admin.OrganizationService;
import online.longlian.app.service.resource.StorageService;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class OrganizationImpl implements OrganizationService {
    private final OrganizationMapper organizationMapper;
    private final StorageService storageService;

    public PageResultVO<OrgDetailInfoVO> getOrgListInfo(AdminOrganizationListBO params) {
        var page = new Page<Organization>(params.getPageNum(), params.getPageSize());
        var organizationPage = new LambdaQueryChainWrapper<>(organizationMapper)
                .select(
                    Organization::getId,Organization::getName,
                        Organization::getName,
                        Organization::getAvatarFileId,
                        Organization::getStatus,
                        Organization::getCreatedAt
                ).page(page);

        var organizations = organizationPage.getRecords();
        var total = organizationPage.getTotal();

        var avatarIds = organizations.stream().map(Organization::getAvatarFileId).toList();
        var fileUrlMap = storageService.getFileUrls(avatarIds);

        var list = organizations.stream().map(organization -> new OrgDetailInfoVO(organization.getId(),
                organization.getName(),
                fileUrlMap.get(organization.getAvatarFileId()),
                organization.getStatus(),
                organization.getCreatedAt()
        )).toList();


        var result = new PageResultVO<OrgDetailInfoVO>();
        result.setList(list);
        result.setTotal(total);

        return result;
    }

    public InviteCodeVO generateCreateOrgInviteCode() {
        return null;
    }

    public Result<Void> changeOrgStatus(@RequestBody @Valid ChangeStatusDTO changeStatusDTO) {
        return null;
    }
}
