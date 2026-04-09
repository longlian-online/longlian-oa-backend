package online.longlian.app.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.dto.ChangeStatusDTO;
import online.longlian.app.pojo.dto.OrgIdDTO;
import online.longlian.app.pojo.dto.OrgListDTO;
import online.longlian.app.pojo.dto.OrgUpdateDTO;
import online.longlian.app.pojo.vo.InviteLinkVO;
import online.longlian.app.pojo.vo.OrgDetailInfoVO;
import online.longlian.app.pojo.vo.OrgSimpleInfoVO;
import online.longlian.app.pojo.vo.PageResultVO;
import online.longlian.app.pojo.vo.UserOrgSwitchVO;
import online.longlian.app.service.user.OrganizationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "组织接口", description = "组织相关接口")
@RequestMapping("/app/org")
@RestController
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @Operation(summary = "切换组织", description = "切换用户当前所在组织")
    @PostMapping("/switch")
    public Result<UserOrgSwitchVO> switchOrg(
            @RequestBody OrgIdDTO orgIdDTO) {
        // TODO
        // return organizationService.switchOrg(orgIdDTO.getOrgId);
        return Result.success(null);
    }

    @Operation(summary = "更新组织信息")
    @Parameter(name = "orgId", description = "组织ID")
    @PutMapping("/{orgId}")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public Result<Void> updateOrgInfo(@PathVariable Long orgId, @RequestBody @Valid OrgUpdateDTO orgUpdateDTO) {
        // TODO
        // return organizationService.updateOrgInfo(orgId, orgUpdateDTO);
        return Result.success("更新成功");
    }

    @Operation(summary = "分页查询组织列表")
    @PostMapping("/list")
    @PreAuthorize("hasRole('ORG_SUPER_ADMIN')")
    public Result<PageResultVO<OrgDetailInfoVO>> getOrgListInfo(@RequestBody @Valid OrgListDTO orgListDTO) {
        // TODO
        // return organizationService.getOrgListInfo(orgListDTO);
        return Result.success(null);
    }

    @Operation(
        summary = "生成组织邀请链接（超管）",
        description = "生成一次性邀请链接，有效期30分钟；新用户可通过链接注册并创建组织，注册后该用户成为该组织默认管理员"
    )
    @PostMapping("/invite/link")
    @PreAuthorize("hasRole('ORG_SUPER_ADMIN')")
    public Result<InviteLinkVO> generateInviteLink() {
        return organizationService.generateInviteLink();
    }

    @Operation(summary = "操作组织状态", description = "启用或禁用指定组织。status: ENABLED-启用，DISABLED-禁用")
    @PatchMapping("/status")
    @PreAuthorize("hasRole('ORG_SUPER_ADMIN')")
    public Result<Void> changeOrgStatus(@RequestBody @Valid ChangeStatusDTO changeStatusDTO) {
        // TODO
        // organizationService.changeOrgStatus(changeStatusDTO.getStatus());
        return Result.success(null);
    }
}
