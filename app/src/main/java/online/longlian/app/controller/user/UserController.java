package online.longlian.app.controller.user;

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
import online.longlian.app.pojo.dto.RegisterCreateOrgDTO;
import online.longlian.app.pojo.vo.InviteInfoVO;
import online.longlian.app.pojo.vo.LoginVO;
import online.longlian.app.pojo.vo.UserInfoVO;
import online.longlian.app.service.user.UserService;
import online.longlian.app.service.VerifyCodeService;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "用户管理接口", description = "用户相关的基础操作接口")
@RequestMapping("/app/user")
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final VerifyCodeService verifyCodeService;

    // -------------------------
    // 登录
    // -------------------------

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

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        return userService.logout(request);
    }

    // -------------------------
    // 验证码
    // -------------------------

    @Operation(summary = "发送邮箱验证码", security = {})
    @Parameter(name = "email", description = "接收验证码的邮箱", required = true, example = "test@longlian.com")
    @GetMapping("/send-code")
    public Result<Void> sendCode(@RequestParam String email) {
        if (!verifyCodeService.sendCode(email)) {
            throw new AppException(ResultCode.OPERATION_FAIL, "验证码发送失败");
        }
        return Result.success("验证码发送请求已提交，请注意查收邮箱");
    }

    // -------------------------
    // 注册
    // -------------------------

    @Operation(
        summary = "查询邀请链接信息",
        description = "用户点击邀请链接后调用，返回邀请类型及组织信息（如组织名称）供注册页面展示；token 无效或已过期返回 4004",
        security = {}
    )
    @Parameter(name = "token", description = "邀请链接中的 token")
    @GetMapping("/invite/{token}")
    public Result<InviteInfoVO> getInviteInfo(@PathVariable String token) {
        // TODO
        // return userService.getInviteInfo(token);
        return Result.success("查询成功", null);
    }

    @Operation(
        summary = "通过邀请链接注册（加入已有组织）",
        description = "用户通过组织管理员生成的邀请链接注册；注册成功后自动提交入组申请，等待管理员审核通过后正式加入组织",
        security = {}
    )
    @PostMapping("/register/by-invite")
    public Result<Void> registerByInvite(@RequestBody @Valid RegisterByInviteDTO registerByInviteDTO) {
        // TODO
        // userService.registerByInvite(registerByInviteDTO);
        return Result.success("注册成功，请等待管理员审核");
    }

    @Operation(
        summary = "通过邀请链接注册并创建组织",
        description = "用户通过创建组织邀请链接注册，同时指定新组织名称；提交后进入审核流程，审核通过后自动成为该组织管理员（ORG_ADMIN）",
        security = {}
    )
    @PostMapping("/register/create-org")
    public Result<Void> registerCreateOrg(@RequestBody @Valid RegisterCreateOrgDTO registerCreateOrgDTO) {
        // TODO
        // userService.registerCreateOrg(registerCreateOrgDTO);
        return Result.success("申请已提交，审核通过后将自动成为组织管理员");
    }

    // -------------------------
    // 用户信息
    // -------------------------

    @Operation(summary = "获取当前登录用户信息", description = "返回当前 Token 对应的用户信息")
    @GetMapping("/info/me")
    public Result<UserInfoVO> getMyInfo() {
        // TODO
        // return userService.getMyInfo();
        return Result.success("查询成功", null);
    }

    @Operation(summary = "获取指定用户信息", description = "根据用户ID查询用户基本信息（昵称、头像、邮箱、用户名等）")
    @Parameter(name = "userId", description = "用户ID")
    @GetMapping("/info/{userId}")
    public Result<UserInfoVO> getUserInfo(@PathVariable Long userId) {
        // TODO
        // return userService.getUserInfo(userId);
        return Result.success("查询成功", null);
    }
}
