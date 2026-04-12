package online.longlian.app.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.dto.admin.OrgListDTO;
import online.longlian.app.pojo.dto.admin.OrgUpdateDTO;
import online.longlian.app.pojo.dto.common.ChangeStatusDTO;
import online.longlian.app.pojo.vo.admin.OrgDetailInfoVO;
import online.longlian.app.pojo.vo.common.PageResultVO;
import online.longlian.app.pojo.vo.orgadmin.InviteCodeVO;
import online.longlian.app.service.user.OrganizationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "组织接口", description = "组织相关接口")
@RequestMapping("/admin/organizations")
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ORG_ADMIN')")
public class OrganizationController {

    private final OrganizationService organizationService;

    @Operation(summary = "更新组织信息")
    @Parameter(name = "orgId", description = "组织ID")
    @PutMapping("/{orgId}")
    public Result<Void> updateOrgInfo(@PathVariable Long orgId, @RequestBody @Valid OrgUpdateDTO orgUpdateDTO) {
        // TODO
        // return organizationService.updateOrgInfo(orgId, orgUpdateDTO);
        return Result.success("更新成功");
    }

    @Operation(summary = "分页查询组织列表")
    @PostMapping("/")
    public Result<PageResultVO<OrgDetailInfoVO>> getOrgListInfo(@RequestBody @Valid OrgListDTO orgListDTO) {
        // TODO
        // return organizationService.getOrgListInfo(orgListDTO);
        return Result.success(null);
    }

    @Operation(
        summary = "生成邀请码（超管）",
        description = "生成一次性邀请码，有效期30分钟；供超管邀请新用户注册并创建组织使用"
    )
    @PostMapping("/invite-codes/create-org")
    public Result<InviteCodeVO> generateCreateOrgInviteCode() {
        return organizationService.generateCreateOrgInviteCode();
    }

    @Operation(summary = "操作组织状态", description = "启用或禁用指定组织。status: ENABLED-启用，DISABLED-禁用")
    @PatchMapping("/status")
    public Result<Void> changeOrgStatus(@RequestBody @Valid ChangeStatusDTO changeStatusDTO) {
        // TODO
        // organizationService.changeOrgStatus(changeStatusDTO.getStatus());
        return Result.success(null);
    }
}
