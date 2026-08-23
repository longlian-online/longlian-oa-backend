package online.longlian.app.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.annotation.ResponseMessage;
import online.longlian.app.common.annotation.UserSession;
import online.longlian.app.common.resolver.SessionContext;
import online.longlian.app.pojo.bo.app.OrgSimpleInfoBO;
import online.longlian.app.pojo.bo.app.UserGetJoinOrgInviteInfoParamsBO;
import online.longlian.app.pojo.bo.app.UserGetJoinOrgInviteInfoResultBO;
import online.longlian.app.pojo.bo.app.UserGetMyInfoResultBO;
import online.longlian.app.pojo.bo.app.UserRegisterByInviteParamsBO;
import online.longlian.app.pojo.bo.app.UserResetPasswordParamsBO;
import online.longlian.app.pojo.bo.app.UserSwitchOrgParamsBO;
import online.longlian.app.pojo.bo.app.UserSwitchOrgResultBO;
import online.longlian.app.pojo.bo.app.UserUpdateMyInfoParamsBO;
import online.longlian.app.pojo.dto.app.JoinByInviteCodeDTO;
import online.longlian.app.pojo.dto.app.RegisterByInviteDTO;
import online.longlian.app.pojo.dto.app.ResetPasswordDTO;
import online.longlian.app.pojo.dto.app.UpdateMyInfoDTO;
import online.longlian.app.pojo.dto.common.OrgIdDTO;
import online.longlian.app.pojo.vo.admin.OrgSimpleInfoVO;
import online.longlian.app.pojo.vo.admin.UserOrgSwitchVO;
import online.longlian.app.pojo.vo.app.InviteInfoVO;
import online.longlian.app.pojo.vo.app.UserInfoVO;
import online.longlian.app.service.app.SessionService;
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
    private final SessionService sessionService;

    @Operation(summary = "找回密码", description = "使用邮箱验证码重置密码", security = {})
    @PutMapping("/password")
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

    @Operation(
        summary = "通过邀请码注册并创建组织",
        description = "使用超管生成的邀请码完成注册，并创建组织成为组织管理员",
        security = {}
    )
    @PostMapping("/register/create-organization")
    @ResponseMessage("已提交申请")
    public void registerAndCreateOrganizationByInvite(@RequestBody @Valid RegisterByInviteDTO registerByInviteDTO) {
        UserRegisterByInviteParamsBO params = new UserRegisterByInviteParamsBO();
        BeanUtils.copyProperties(registerByInviteDTO, params);
        userService.registerAndCreateOrganizationByInvite(params);
    }

    @Operation(
        summary = "通过邀请码注册并加入组织",
        description = "使用组织管理员生成的邀请码完成注册，并加入指定组织",
        security = {}
    )
    @PostMapping("/register/join-organization")
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
    public UserInfoVO getMyInfo(@UserSession SessionContext sessionContext) {
        UserGetMyInfoResultBO resultBO = userService.getMyInfo(sessionContext.userId());
        UserInfoVO userInfoVO = new UserInfoVO();
        BeanUtils.copyProperties(resultBO, userInfoVO);
        return userInfoVO;
    }

    @Operation(summary = "更新当前用户信息")
    @PutMapping("/")
    @ResponseMessage("更新成功")
    public void updateMyInfo(@UserSession SessionContext sessionContext,
                              @RequestBody @Valid UpdateMyInfoDTO updateMyInfoDTO) {
        userService.updateMyInfo(
                UserUpdateMyInfoParamsBO.builder()
                        .userId(sessionContext.userId())
                        .nickname(updateMyInfoDTO.getNickname())
                        .avatarFileId(updateMyInfoDTO.getAvatarFileId())
                        .build());
    }

    @Operation(summary = "获取用户加入的组织列表", description = "查询用户加入的组织列表")
    @GetMapping("/organizations")
    @ResponseMessage("查询成功")
    public List<OrgSimpleInfoVO> getOrgSimpleInfo(@UserSession SessionContext sessionContext) {
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
    public void joinOrganizationByInvite(@UserSession SessionContext sessionContext,
                                          @RequestBody @Valid JoinByInviteCodeDTO joinByInviteCodeDTO) {
        userService.joinOrganizationByInvite(sessionContext.userId(), joinByInviteCodeDTO.getInviteCode());
    }

    @Operation(summary = "切换组织", description = "切换用户当前所在组织")
    @PostMapping("/switch")
    @ResponseMessage("切换成功")
    public UserOrgSwitchVO switchOrg(@UserSession SessionContext sessionContext,
                                      @RequestBody @Valid OrgIdDTO orgIdDTO) {
        UserSwitchOrgResultBO resultBO = userService.switchOrg(
                UserSwitchOrgParamsBO.builder()
                        .userId(sessionContext.userId())
                        .orgId(orgIdDTO.getOrgId())
                        .build()
        );
        sessionService.refreshCurrentUserOrg(resultBO.getId(), resultBO.getRoles());
        UserOrgSwitchVO userOrgSwitchVO = new UserOrgSwitchVO();
        BeanUtils.copyProperties(resultBO, userOrgSwitchVO);
        return userOrgSwitchVO;
    }

}
