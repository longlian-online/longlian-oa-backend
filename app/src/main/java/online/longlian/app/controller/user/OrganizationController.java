package online.longlian.app.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.dto.OrgListDTO;
import online.longlian.app.pojo.dto.OrgUpdateDTO;
import online.longlian.app.pojo.vo.OrgDetailInfoVO;
import online.longlian.app.pojo.vo.OrgSimpleInfoVO;
import online.longlian.app.pojo.vo.PageResultVO;
import online.longlian.app.pojo.vo.UserOrgSwitchVO;
import online.longlian.app.service.user.OrganizationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "组织接口", description = "组织相关接口")
@RequestMapping("/app/org")
@RestController
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @Operation(summary = "获取用户加入的组织列表接口", description = "根据用户ID查询用户加入的组织列表")
    @Parameter(name = "userId", description = "用户ID")
    @GetMapping("/user-join/{userId}")
    public Result<List<OrgSimpleInfoVO>> getOrgSimpleInfo(@PathVariable Long userId) {
        //TODO
        return Result.success(null);
        //return organizationService.getOrgSimpleInfo(userId);
    }

    @Operation(summary = "获取当前组织详细信息接口")
    @GetMapping("/detail")
    public Result<OrgDetailInfoVO> getOrgDetailInfo() {
        //TODO
        return Result.success(null);
        //return organizationService.getOrgDetailInfo();
    }

    @Operation(summary = "分页查询组织列表接口")
    @PostMapping("/list")
    public Result<PageResultVO<OrgDetailInfoVO>> getOrgListInfo(@RequestBody @Valid OrgListDTO orgListDTO) {
        //TODO
        return Result.success(null);
        //return organizationService.getOrgListInfo(orgListDTO);
    }

    @Operation(summary = "更新组织信息接口")
    @PutMapping("/{orgId}")
    public Result<Void> updateOrgInfo(@PathVariable Long orgId, @RequestBody @Valid OrgUpdateDTO orgUpdateDTO) {
        //TODO
        return Result.success("更新成功");
        //return organizationService.updateOrgInfo(orgId, orgUpdateDTO);
    }

    @Operation(summary = "切换组织接口", description = "切换用户当前所在组织")
    @Parameter(name = "orgId", description = "目标组织ID")
    @PostMapping("/switch/{orgId}")
    public Result<UserOrgSwitchVO> switchOrg(@PathVariable Long orgId) {
        //TODO
        return Result.success(null);
        //return organizationService.switchOrg(orgId);
    }
    @Operation(summary = "操作组织状态接口", description = "启用或禁用指定组织，status: 1-启用，0-禁用")
    @Parameter(name = "orgId", description = "组织ID")
    @Parameter(name = "status", description = "目标状态：1-启用，0-禁用")
    @PatchMapping("/{orgId}/status")
    public Result<Void> changeOrgStatus(@PathVariable Long orgId, @RequestParam Integer status) {
        //TODO
        return Result.success(null);
        //return organizationService.changeOrgStatus(orgId, status);
    }

}
