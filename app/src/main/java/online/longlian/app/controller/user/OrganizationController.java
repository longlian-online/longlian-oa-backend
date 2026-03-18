package online.longlian.app.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.vo.OrgSimpleInfoVO;
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
    @Operation(summary = "切换组织接口", description = "切换用户当前所在组织")
    @Parameter(name = "orgId", description = "组织ID")
    @GetMapping("/switch/{orgId}")
    public Result<UserOrgSwitchVO> switchOrg(@PathVariable Long orgId) {
        //TODO
        return Result.success(null);
        //return organizationService.switchOrg(userId);
    }

}
