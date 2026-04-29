package online.longlian.app.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.bo.UserGetJoinOrgInviteInfoParamsBO;
import online.longlian.app.pojo.bo.UserGetJoinOrgInviteInfoResultBO;
import online.longlian.app.pojo.bo.UserGetMyInfoResultBO;
import online.longlian.app.pojo.bo.UserRegisterByInviteParamsBO;
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
import online.longlian.app.service.user.SessionService;
import online.longlian.app.service.user.UserService;
import org.springframework.beans.BeanUtils;
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
    private final SessionService sessionService;

    @Operation(
        summary = "通过邀请码注册并创建组织",
        description = "使用超管生成的邀请码完成注册，并创建组织成为组织管理员",
        security = {}
    )
    @PostMapping("/register/create-organization")
    public Result<Void> registerAndCreateOrganizationByInvite(@RequestBody @Valid RegisterByInviteDTO registerByInviteDTO) {
        UserRegisterByInviteParamsBO params = new UserRegisterByInviteParamsBO();
        BeanUtils.copyProperties(registerByInviteDTO, params);
        userService.registerAndCreateOrganizationByInvite(params);
        return Result.success("已提交申请");
    }

    @Operation(
        summary = "通过邀请码注册并加入组织",
        description = "使用组织管理员生成的邀请码完成注册，并加入指定组织",
        security = {}
    )
    @PostMapping("/register/join-organization")
    public Result<Void> registerAndJoinOrganizationByInvite(@RequestBody @Valid RegisterByInviteDTO registerByInviteDTO) {
        UserRegisterByInviteParamsBO params = new UserRegisterByInviteParamsBO();
        BeanUtils.copyProperties(registerByInviteDTO, params);
        userService.registerAndJoinOrganizationByInvite(params);
        return Result.success("已提交申请");
    }

    @Operation(
        summary = "获取加入组织邀请码对应的组织信息",
        description = "注册页使用。组织管理员生成的邀请码可通过该接口查询组织名称",
        security = {}
    )
    @GetMapping("/register/join-organization/invite-info")
    public Result<InviteInfoVO> getJoinOrganizationInviteInfo(@RequestParam String inviteCode) {
        UserGetJoinOrgInviteInfoResultBO resultBO = userService.getJoinOrgInviteInfo(
                UserGetJoinOrgInviteInfoParamsBO.builder()
                        .inviteCode(inviteCode)
                        .build()
        );
        InviteInfoVO inviteInfoVO = InviteInfoVO.builder()
                .orgId(resultBO.getOrgId())
                .orgName(resultBO.getOrgName())
                .build();
        return Result.success("查询成功", inviteInfoVO);
    }

    @Operation(summary = "获取当前登录用户信息", description = "返回当前 Token 对应的用户信息")
    @GetMapping("/")
    public Result<UserInfoVO> getMyInfo() {
        UserGetMyInfoResultBO resultBO = userService.getMyInfo(
                sessionService.getCurrentUserId()
        );
        UserInfoVO userInfoVO = new UserInfoVO();
        BeanUtils.copyProperties(resultBO, userInfoVO);
        return Result.success("查询成功", userInfoVO);
    }

    @Operation(summary = "获取用户加入的组织列表", description = "查询用户加入的组织列表")
    @GetMapping("/organizations")
    public Result<List<OrgSimpleInfoVO>> getOrgSimpleInfo() {
        // TODO
        // return organizationService.getOrgSimpleInfo(userId);
        return Result.success(null);
    }

    @Operation(
        summary = "已注册用户通过邀请码加入组织",
        description = "已登录用户使用组织管理员生成的邀请码，直接加入目标组织"
    )
    @PostMapping("/organizations/join-by-invite")
    public Result<Void> joinOrganizationByInvite(@RequestBody @Valid JoinByInviteCodeDTO joinByInviteCodeDTO) {
        userService.joinOrganizationByInvite(sessionService.getCurrentUserId(), joinByInviteCodeDTO.getInviteCode());
        return Result.success("已提交申请");
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
        sessionService.refreshCurrentUserOrg(resultBO.getId(), resultBO.getRoles());
        UserOrgSwitchVO userOrgSwitchVO = new UserOrgSwitchVO();
        BeanUtils.copyProperties(resultBO, userOrgSwitchVO);
        return Result.success("切换成功", userOrgSwitchVO);
    }

}
