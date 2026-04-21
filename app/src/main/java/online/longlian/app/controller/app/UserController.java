package online.longlian.app.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.bo.UserSwitchOrgParamsBO;
import online.longlian.app.pojo.bo.UserSwitchOrgResultBO;
import online.longlian.app.pojo.dto.app.JoinByInviteCodeDTO;
import online.longlian.app.pojo.dto.app.RegisterByInviteDTO;
import online.longlian.app.pojo.dto.common.OrgIdDTO;
import online.longlian.app.pojo.vo.admin.OrgDetailInfoVO;
import online.longlian.app.pojo.vo.admin.OrgSimpleInfoVO;
import online.longlian.app.pojo.vo.admin.UserOrgSwitchVO;
import online.longlian.app.pojo.vo.app.InviteInfoVO;
import online.longlian.app.pojo.vo.app.UserInfoVO;
import online.longlian.app.service.user.OrganizationMemberService;
import online.longlian.app.service.user.SessionService;
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
    private final SessionService sessionService;

    @Operation(
        summary = "通过邀请码注册",
        description = "两种场景：1) 超管邀请码用于创建组织；2) 管理员邀请码用于加入组织，可用于新用户注册后直接入组，也可用于已登录用户申请入组",
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
    public Result<Void> joinByInviteCode(@RequestBody @Valid JoinByInviteCodeDTO joinByInviteCodeDTO) {
        return userService.joinByInviteCode(joinByInviteCodeDTO);
    }

    @Operation(summary = "切换组织", description = "切换用户当前所在组织")
    @PostMapping("/switch")
    public Result<UserOrgSwitchVO> switchOrg(@RequestBody @Valid OrgIdDTO orgIdDTO) {
        UserSwitchOrgResultBO resultBO = userService.switchOrg(
                UserSwitchOrgParamsBO.builder()
                        .userId(sessionService.getCurrentUserId())
                        .orgId(orgIdDTO.getOrgId())
                        .build()
        );
        UserOrgSwitchVO userOrgSwitchVO = UserOrgSwitchVO.builder()
                .id(resultBO.getId())
                .name(resultBO.getName())
                .avatarUrl(resultBO.getAvatarUrl())
                .role(resultBO.getRole())
                .build();
        return Result.success("切换成功", userOrgSwitchVO);
    }

    @Operation(
        summary = "根据邀请码获取组织信息",
        description = "注册页使用。仅组织管理员生成的邀请码会调用该接口返回组织信息"
    )
    @GetMapping("/invite-info")
    public Result<InviteInfoVO> getInviteOrgInfo(@RequestParam String inviteCode) {
        return organizationMemberService.getInviteOrgInfo(inviteCode);
    }
}
