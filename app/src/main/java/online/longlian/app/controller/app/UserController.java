package online.longlian.app.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.Result;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.pojo.dto.app.JoinByInviteCodeDTO;
import online.longlian.app.pojo.dto.app.RegisterByInviteDTO;
import online.longlian.app.pojo.dto.common.OrgIdDTO;
import online.longlian.app.pojo.vo.admin.OrgDetailInfoVO;
import online.longlian.app.pojo.vo.admin.OrgSimpleInfoVO;
import online.longlian.app.pojo.vo.admin.UserOrgSwitchVO;
import online.longlian.app.pojo.vo.app.InviteInfoVO;
import online.longlian.app.pojo.vo.app.UserInfoVO;
import online.longlian.app.service.VerifyCodeService;
import online.longlian.app.service.user.OrganizationMemberService;
import online.longlian.app.service.user.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "用户接口", description = "用户相关的基础操作接口")
@RequestMapping("/app/user")
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final OrganizationMemberService organizationMemberService;


    @Operation(
        summary = "通过邀请链接注册",
        description = "两种场景：1) 管理员邀请入组（组织由邀请链接绑定，注册后进入待审核）；2) 超管邀请创建组织（注册时填写组织名称，注册后创建组织并成为组织管理员）",
        security = {}
    )
    @PostMapping("/")
    public Result<Void> register(@RequestBody @Valid RegisterByInviteDTO registerByInviteDTO) {
        return userService.registerByInvite(registerByInviteDTO);
    }

    @Operation(summary = "获取当前登录用户信息", description = "返回当前 Token 对应的用户信息")
    @GetMapping("/")
    public Result<UserInfoVO> getMyInfo() {
        return userService.getMyInfo();
    }

    @Operation(summary = "获取用户加入的组织列表", description = "查询用户加入的组织列表")
    @GetMapping("/organizations")
    public Result<List<OrgSimpleInfoVO>> getOrgSimpleInfo() {
        // TODO
        // return organizationService.getOrgSimpleInfo(userId);
        return Result.success(null);
    }

    @Operation(summary = "获取指定组织详细详细，仅可查询用户已加入的组织，非用户已加入的组织则报错")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @GetMapping("/organizations/{orgId}")
    public Result<OrgDetailInfoVO> getOrgDetailInfo(@PathVariable("orgId") Long orgId) {
        // TODO
        // return organizationService.getOrgDetailInfo();
        return Result.success(null);
    }

    @Operation(
            summary = "通过邀请码加入组织（已登录用户）",
            description = "已登录用户输入管理员提供的邀请码，自动提交入组申请，等待管理员审核通过后正式加入"
    )
    @PostMapping("/organizations")
    public Result<Void> joinByInviteCode(
            @RequestBody @Valid JoinByInviteCodeDTO joinByInviteCodeDTO) {
        return userService.joinByInviteCode(joinByInviteCodeDTO);
    }


    @Operation(summary = "切换组织", description = "切换用户当前所在组织")
    @PostMapping("/switch")
    public Result<UserOrgSwitchVO> switchOrg(
            @RequestBody OrgIdDTO orgIdDTO) {
        // TODO
        // return organizationService.switchOrg(orgIdDTO.getOrgId);
        return Result.success(null);
    }

    @Operation(
            summary = "根据邀请码获取组织信息",
            description = "注册页使用。仅管理员邀请入组场景会调用该接口返回组织信息"
    )
    @GetMapping("/invite-info")
    public Result<InviteInfoVO> getInviteOrgInfo(@RequestParam String inviteToken) {
        return organizationMemberService.getInviteOrgInfo(inviteToken);
    }
}
