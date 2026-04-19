package online.longlian.app.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.bo.*;
import online.longlian.app.pojo.dto.admin.OrgListDTO;
import online.longlian.app.pojo.dto.common.ChangeStatusDTO;
import online.longlian.app.pojo.vo.admin.OrgDetailInfoVO;
import online.longlian.app.pojo.vo.common.PageResultVO;
import online.longlian.app.pojo.vo.orgadmin.InviteCodeVO;
import online.longlian.app.service.admin.OrganizationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "组织接口", description = "组织相关接口")
@RequestMapping("/admin/organizations")
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ORG_ADMIN')")
public class OrganizationController {

    private final OrganizationService organizationService;

    @Operation(summary = "分页查询组织列表")
    @GetMapping("/")
    public Result<PageResultVO<OrgDetailInfoVO>> getOrgListInfo(@RequestBody @Valid OrgListDTO dto) {
        AdminOrganizationListParamsBO bo = new AdminOrganizationListParamsBO(
                dto.getOrgName(),
                dto.getStartCreateTime(),
                dto.getEndCreateTime(),
                new PageParamsBO(dto.getPageNum(), dto.getPageSize())
        );

        PageResultBO<AdminOrganizationListResultBO> organizationPage = organizationService.getOrgListInfo(bo);

        List<OrgDetailInfoVO> list = organizationPage
                .getList()
                .stream()
                .map(org ->
                        new OrgDetailInfoVO(org.getId(), org.getName(), org.getAvatarUrl(), org.getStatus(), org.getCreatedAt())
                )
                .toList();

        PageResultVO<OrgDetailInfoVO> pageResultVO = new PageResultVO<>(list, organizationPage.getTotal());

        return Result.success("ok", pageResultVO);
    }

    @Operation(
        summary = "生成邀请码（超管）",
        description = "生成一次性邀请码，有效期30分钟；供超管邀请新用户注册并创建组织使用"
    )
    @PostMapping("/invite-codes/create-org")
    public Result<InviteCodeVO> generateCreateOrgInviteCode() {
        return null;
    }

    @Operation(summary = "操作组织状态", description = "启用或禁用指定组织。status: ENABLED-启用，DISABLED-禁用")
    @PatchMapping("/status")
    public Result<Void> changeOrgStatus(@RequestBody @Valid ChangeStatusDTO dto) {
        AdminOrganizationUpdateStatusParamsBO bo = new AdminOrganizationUpdateStatusParamsBO(dto.getOrgId(), dto.getStatus());
        organizationService.updateOrgStatus(bo);
        return Result.success("ok");
    }
}
