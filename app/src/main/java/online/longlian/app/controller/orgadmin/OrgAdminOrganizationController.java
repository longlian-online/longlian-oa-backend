package online.longlian.app.controller.orgadmin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.annotation.ResponseMessage;
import online.longlian.app.common.annotation.UserSession;
import online.longlian.app.common.resolver.SessionContext;
import online.longlian.app.pojo.bo.orgadmin.OrgAdminGetOrganizationInfoResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgAdminUpdateOrganizationInfoParamsBO;
import online.longlian.app.pojo.dto.orgadmin.OrgAdminUpdateOrganizationInfoDTO;
import online.longlian.app.pojo.vo.orgadmin.OrgAdmintOrganizationInfoVO;
import online.longlian.app.service.orgadmin.OrgAdminOrganizationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "组织信息管理接口", description = "组织相关接口")
@RequestMapping("/orgadmin/organizations")
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ORG_ADMIN')")
public class OrgAdminOrganizationController {

    private final OrgAdminOrganizationService orgAdminOrganizationService;

    @Operation(summary = "获取组织信息", description = "获取组织管理员所在组织的头像、组名和简介")
    @GetMapping("")
    @ResponseMessage("查询成功")
    public OrgAdmintOrganizationInfoVO getOrganizationInfo(@UserSession SessionContext sessionContext) {
        OrgAdminGetOrganizationInfoResultBO resultBO = orgAdminOrganizationService.getOrganizationInfo(
                sessionContext.orgId());

        return OrgAdmintOrganizationInfoVO.builder()
                .id(resultBO.getId())
                .name(resultBO.getName())
                .avatarUrl(resultBO.getAvatarUrl())
                .description(resultBO.getDescription())
                .build();
    }

    @Operation(summary = "更新组织信息", description = "更新组织的头像、组名和简介")
    @PutMapping("")
    @ResponseMessage("更新成功")
    public void updateOrganizationInfo(@UserSession SessionContext sessionContext,
                                        @RequestBody @Valid OrgAdminUpdateOrganizationInfoDTO updateOrganizationInfoDTO) {
        orgAdminOrganizationService.updateOrganizationInfo(
                OrgAdminUpdateOrganizationInfoParamsBO.builder()
                        .orgId(sessionContext.orgId())
                        .userId(sessionContext.userId())
                        .name(updateOrganizationInfoDTO.getName())
                        .avatarFileId(updateOrganizationInfoDTO.getAvatarFileId())
                        .description(updateOrganizationInfoDTO.getDescription())
                        .build()
        );
    }
}
