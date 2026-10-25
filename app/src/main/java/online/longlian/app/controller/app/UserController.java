package online.longlian.app.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.annotation.ResponseMessage;
import online.longlian.app.common.annotation.UserSession;
import online.longlian.app.common.enumeration.OrganizationDeclaration;
import online.longlian.app.common.resolver.SessionContext;
import online.longlian.app.pojo.bo.app.OrgSimpleInfoBO;
import online.longlian.app.pojo.bo.app.UserGetJoinOrgInviteInfoParamsBO;
import online.longlian.app.pojo.bo.app.UserGetJoinOrgInviteInfoResultBO;
import online.longlian.app.pojo.bo.app.UserGetMyInfoResultBO;
import online.longlian.app.pojo.bo.app.UserConfigResultBO;
import online.longlian.app.pojo.bo.app.UserRegisterByInviteParamsBO;
import online.longlian.app.pojo.bo.app.UserResetPasswordParamsBO;
import online.longlian.app.pojo.bo.app.UserChangePasswordParamsBO;
import online.longlian.app.pojo.bo.app.UserBindEmailParamsBO;
import online.longlian.app.pojo.bo.app.UserSwitchOrgParamsBO;
import online.longlian.app.pojo.bo.app.UserSwitchOrgResultBO;
import online.longlian.app.pojo.bo.app.UserUpdateMyInfoParamsBO;
import online.longlian.app.pojo.bo.app.UserConfigParamsBO;
import online.longlian.app.pojo.dto.app.JoinByInviteCodeDTO;
import online.longlian.app.pojo.dto.app.RegisterByInviteDTO;
import online.longlian.app.pojo.dto.app.ResetPasswordDTO;
import online.longlian.app.pojo.dto.app.ChangePasswordDTO;
import online.longlian.app.pojo.dto.app.BindEmailDTO;
import online.longlian.app.pojo.dto.app.UpdateMyInfoDTO;
import online.longlian.app.pojo.dto.app.PatchUserConfigDTO;
import online.longlian.app.pojo.dto.app.UpdateUserConfigDTO;
import online.longlian.app.pojo.dto.common.OrgIdDTO;
import online.longlian.app.pojo.vo.admin.OrgSimpleInfoVO;
import online.longlian.app.pojo.vo.admin.UserOrgSwitchVO;
import online.longlian.app.pojo.vo.app.InviteInfoVO;
import online.longlian.app.pojo.vo.app.UserInfoVO;
import online.longlian.app.pojo.vo.app.UserConfigVO;
import online.longlian.app.service.app.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "用户接口", description = "用户相关的基础操作接口")
@RequestMapping("/app/user")
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "找回密码", description = "使用邮箱验证码重置密码", security = {})
    @PutMapping("/password")
    @UserSession(OrganizationDeclaration.NONE)
    @ResponseMessage("密码重置成功")
    public void resetPassword(@RequestBody @Valid ResetPasswordDTO resetPasswordDTO) {
        userService.resetPassword(
                UserResetPasswordParamsBO.builder()
                        .email(resetPasswordDTO.getEmail())
                        .code(resetPasswordDTO.getCode())
                        .password(resetPasswordDTO.getPassword())
                        .build()
        );
    }

    @Operation(summary = "修改密码", description = "使用原密码修改当前登录用户密码")
    @PatchMapping("/password")
    @ResponseMessage("密码已修改")
    public void changePassword(@UserSession(OrganizationDeclaration.NONE) SessionContext sessionContext,
                               @RequestBody @Valid ChangePasswordDTO changePasswordDTO) {
        userService.changePassword(
                UserChangePasswordParamsBO.builder()
                        .userId(sessionContext.userId())
                        .oldPassword(changePasswordDTO.getOldPassword())
                        .newPassword(changePasswordDTO.getNewPassword())
                        .build());
    }

    @Operation(summary = "绑定邮箱", description = "已登录且尚未绑定邮箱的用户，使用邮箱验证码绑定邮箱")
    @PutMapping("/email")
    @ResponseMessage("邮箱已绑定")
    public void bindEmail(@UserSession(OrganizationDeclaration.NONE) SessionContext sessionContext,
                          @RequestBody @Valid BindEmailDTO bindEmailDTO) {
        userService.bindEmail(
                UserBindEmailParamsBO.builder()
                        .userId(sessionContext.userId())
                        .email(bindEmailDTO.getEmail())
                        .code(bindEmailDTO.getCode())
                        .build());
    }

    @Operation(
        summary = "通过邀请码注册并创建组织",
        description = "使用超管生成的邀请码完成注册，并创建组织成为组织所有者",
        security = {}
    )
    @PostMapping("/register/create-organization")
    @UserSession(OrganizationDeclaration.NONE)
    @ResponseMessage("已提交申请")
    public void registerAndCreateOrganizationByInvite(@RequestBody @Valid RegisterByInviteDTO registerByInviteDTO) {
        UserRegisterByInviteParamsBO params = new UserRegisterByInviteParamsBO();
        BeanUtils.copyProperties(registerByInviteDTO, params);
        userService.registerAndCreateOrganizationByInvite(params);
    }

    @Operation(
        summary = "通过邀请码注册并加入组织",
        description = "使用邀请码提交注册入组申请，审批通过后创建账号并加入组织",
        security = {}
    )
    @PostMapping("/register/join-organization")
    @UserSession(OrganizationDeclaration.NONE)
    @ResponseMessage("已提交申请")
    public void registerAndJoinOrganizationByInvite(@RequestBody @Valid RegisterByInviteDTO registerByInviteDTO) {
        UserRegisterByInviteParamsBO params = new UserRegisterByInviteParamsBO();
        BeanUtils.copyProperties(registerByInviteDTO, params);
        userService.registerAndJoinOrganizationByInvite(params);
    }

    @Operation(
        summary = "获取加入组织邀请码对应的组织信息",
        description = "注册页使用。组织管理员生成的邀请码可通过该接口查询组织名称",
        security = {}
    )
    @GetMapping("/register/join-organization/invite-info")
    @UserSession(OrganizationDeclaration.NONE)
    @ResponseMessage("查询成功")
    public InviteInfoVO getJoinOrganizationInviteInfo(@RequestParam String inviteCode) {
        UserGetJoinOrgInviteInfoResultBO resultBO = userService.getJoinOrgInviteInfo(
                UserGetJoinOrgInviteInfoParamsBO.builder()
                        .inviteCode(inviteCode)
                        .build()
        );
        return InviteInfoVO.builder()
                .orgId(resultBO.getOrgId())
                .orgName(resultBO.getOrgName())
                .build();
    }

    @Operation(summary = "获取当前登录用户信息", description = "返回当前 Token 对应的用户信息")
    @GetMapping("/")
    @ResponseMessage("查询成功")
    public UserInfoVO getMyInfo(@UserSession(OrganizationDeclaration.NONE) SessionContext sessionContext) {
        UserGetMyInfoResultBO resultBO = userService.getMyInfo(sessionContext.userId());
        UserInfoVO userInfoVO = new UserInfoVO();
        BeanUtils.copyProperties(resultBO, userInfoVO);
        return userInfoVO;
    }

    @Operation(summary = "更新当前用户信息")
    @PutMapping("/")
    @ResponseMessage("更新成功")
    public void updateMyInfo(@UserSession(OrganizationDeclaration.NONE) SessionContext sessionContext,
                              @RequestBody @Valid UpdateMyInfoDTO updateMyInfoDTO) {
        userService.updateMyInfo(
                UserUpdateMyInfoParamsBO.builder()
                        .userId(sessionContext.userId())
                        .nickname(updateMyInfoDTO.getNickname())
                        .avatarFileId(updateMyInfoDTO.getAvatarFileId())
                        .build());
    }

    @Operation(summary = "获取当前用户配置", description = "返回完整用户配置，未配置时为空对象。无需选择组织")
    @GetMapping("/config")
    public UserConfigVO getConfig(@UserSession(OrganizationDeclaration.NONE) SessionContext sessionContext) {
        UserConfigResultBO result = userService.getConfig(sessionContext.userId());
        return new UserConfigVO(result.getConfig());
    }

    @Operation(summary = "整体替换当前用户配置", description = "用请求中的完整配置对象替换当前用户配置。无需选择组织")
    @PutMapping("/config")
    @ResponseMessage("用户配置已更新")
    public void replaceConfig(@UserSession(OrganizationDeclaration.NONE) SessionContext sessionContext,
                              @RequestBody @Valid UpdateUserConfigDTO dto) {
        userService.replaceConfig(UserConfigParamsBO.builder()
                .userId(sessionContext.userId())
                .config(dto.getConfig())
                .build());
    }

    @Operation(summary = "部分更新当前用户配置", description = "只更新请求中出现的配置字段；值为 null 时删除该字段，其他字段保持不变。无需选择组织")
    @PatchMapping("/config")
    @ResponseMessage("用户配置已更新")
    public void patchConfig(@UserSession(OrganizationDeclaration.NONE) SessionContext sessionContext,
                            @RequestBody @Valid PatchUserConfigDTO dto) {
        userService.patchConfig(UserConfigParamsBO.builder()
                .userId(sessionContext.userId())
                .config(dto.getConfig())
                .build());
    }

    @Operation(summary = "获取用户加入的组织列表", description = "查询用户加入的组织列表")
    @GetMapping("/organizations")
    @ResponseMessage("查询成功")
    public List<OrgSimpleInfoVO> getOrgSimpleInfo(@UserSession(OrganizationDeclaration.NONE) SessionContext sessionContext) {
        List<OrgSimpleInfoBO> orgList = userService.getMyOrganizations(sessionContext.userId());
        return orgList.stream()
                .map(orgSimpleInfoBO -> {
                    OrgSimpleInfoVO orgSimpleInfoVO = new OrgSimpleInfoVO();
                    BeanUtils.copyProperties(orgSimpleInfoBO, orgSimpleInfoVO);
                    return orgSimpleInfoVO;
                })
                .toList();
    }

    @Operation(
        summary = "已注册用户通过邀请码加入组织",
        description = "已登录用户使用组织管理员生成的邀请码，直接加入目标组织"
    )
    @PostMapping("/organizations/join-by-invite")
    @ResponseMessage("已提交申请")
    public void joinOrganizationByInvite(@UserSession(OrganizationDeclaration.NONE) SessionContext sessionContext,
                                          @RequestBody @Valid JoinByInviteCodeDTO joinByInviteCodeDTO) {
        userService.joinOrganizationByInvite(sessionContext.userId(), joinByInviteCodeDTO.getInviteCode());
    }

    @Operation(summary = "设置默认组织", description = "更新用户默认组织，不改变其他请求的组织作用域")
    @PostMapping("/switch")
    @ResponseMessage("切换成功")
    public UserOrgSwitchVO switchOrg(@UserSession(OrganizationDeclaration.NONE) SessionContext sessionContext,
                                      @RequestBody @Valid OrgIdDTO orgIdDTO) {
        UserSwitchOrgResultBO resultBO = userService.switchOrg(
                UserSwitchOrgParamsBO.builder()
                        .userId(sessionContext.userId())
                        .orgId(orgIdDTO.getOrgId())
                        .build()
        );
        UserOrgSwitchVO userOrgSwitchVO = new UserOrgSwitchVO();
        BeanUtils.copyProperties(resultBO, userOrgSwitchVO);
        return userOrgSwitchVO;
    }

}
