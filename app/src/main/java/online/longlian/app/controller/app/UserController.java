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
import online.longlian.app.pojo.dto.LoginByCodeDTO;
import online.longlian.app.pojo.dto.LoginByPwdDTO;
import online.longlian.app.pojo.dto.RegisterByInviteDTO;
import online.longlian.app.pojo.vo.LoginVO;
import online.longlian.app.pojo.vo.OrgDetailInfoVO;
import online.longlian.app.pojo.vo.OrgSimpleInfoVO;
import online.longlian.app.pojo.vo.UserInfoVO;
import online.longlian.app.service.VerifyCodeService;
import online.longlian.app.service.user.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "用户管理接口", description = "用户相关的基础操作接口")
@RequestMapping("/app/user")
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final VerifyCodeService verifyCodeService;

    @Operation(summary = "密码登录", description = "使用用户名+密码登录", security = {})
    @PostMapping("/login/pwd")
    public Result<LoginVO> loginByPwd(@RequestBody @Valid LoginByPwdDTO loginByPwdDTO) {
        return userService.loginByPwd(loginByPwdDTO);
    }

    @Operation(summary = "验证码登录", description = "使用邮箱+验证码登录", security = {})
    @PostMapping("/login/code")
    public Result<LoginVO> loginByCode(@RequestBody @Valid LoginByCodeDTO loginByCodeDTO) {
        return userService.loginByCode(loginByCodeDTO);
    }

    @Operation(summary = "发送邮箱验证码", security = {})
    @Parameter(name = "email", description = "接收验证码的邮箱", required = true, example = "test@longlian.com")
    @GetMapping("/send-code")
    public Result<Void> sendCode(@RequestParam String email) {
        if (!verifyCodeService.sendCode(email)) {
            throw new AppException(ResultCode.OPERATION_FAIL, "验证码发送失败");
        }
        return Result.success("验证码发送请求已提交，请注意查收邮箱");
    }

    @Operation(
        summary = "通过邀请链接注册",
        description = "两种场景：1) 管理员邀请入组（组织由邀请链接绑定，注册后进入待审核）；2) 超管邀请创建组织（注册时填写组织名称，注册后创建组织并成为组织管理员）",
        security = {}
    )
    @PostMapping("/register/invite")
    public Result<Void> registerByInvite(@RequestBody @Valid RegisterByInviteDTO registerByInviteDTO) {
        return userService.registerByInvite(registerByInviteDTO);
    }

    @Operation(summary = "获取当前登录用户信息", description = "返回当前 Token 对应的用户信息")
    @GetMapping("/info/me")
    public Result<UserInfoVO> getMyInfo() {
        return userService.getMyInfo();
    }

    @Operation(summary = "获取指定用户信息", description = "根据用户ID查询用户基本信息（昵称、头像、邮箱、用户名等）")
    @Parameter(name = "userId", description = "用户ID")
    @GetMapping("/info/{userId}")
    public Result<UserInfoVO> getUserInfo(@PathVariable Long userId) {
        // TODO
        // return userService.getUserInfo(userId);
        return Result.success("查询成功", null);
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        return userService.logout(request);
    }

    @Operation(summary = "获取用户加入的组织列表", description = "根据用户ID查询用户加入的组织列表")
    @Parameter(name = "userId", description = "用户ID")
    @GetMapping("/user-join/{userId}")
    public Result<List<OrgSimpleInfoVO>> getOrgSimpleInfo(@PathVariable Long userId) {
        // TODO
        // return organizationService.getOrgSimpleInfo(userId);
        return Result.success(null);
    }

    @Operation(summary = "获取当前组织详细信息")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @GetMapping("/detail")
    public Result<OrgDetailInfoVO> getOrgDetailInfo() {
        // TODO
        // return organizationService.getOrgDetailInfo();
        return Result.success(null);
    }
}
