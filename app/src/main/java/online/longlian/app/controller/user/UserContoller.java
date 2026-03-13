package online.longlian.app.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.Result;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.pojo.dto.LoginByCodeDTO;
import online.longlian.app.pojo.dto.LoginByPwdDTO;
import online.longlian.app.pojo.vo.LoginVO;
import online.longlian.app.service.user.UserService;
import online.longlian.app.service.VerifyCodeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "用户管理接口", description = "用户相关的基础操作接口")
@RequestMapping("/app/user")
@RestController
@RequiredArgsConstructor
public class UserContoller  {

    private final UserService userService;
    private final VerifyCodeService verifyCodeService;

    @Operation(summary = "密码登录接口", description = "使用用户名+密码登录", security = {})
    @PostMapping("/login/pwd")
    public Result<LoginVO> loginByPwd(@RequestBody LoginByPwdDTO loginByPwdDTO) {
        return userService.loginByPwd(loginByPwdDTO);
    }

    @Operation(summary = "验证码登录接口", description = "使用邮箱+验证码登录", security = {})
    @PostMapping("/login/code")
    public Result<LoginVO> loginByCode(@RequestBody LoginByCodeDTO loginByCodeDTO) {
        return userService.loginByCode(loginByCodeDTO);
    }

    @Operation(summary = "发送邮箱验证码接口", security = {})
    @Parameter(name = "email", description = "接收验证码的邮箱", required = true, example = "test@longlian.com")
    @GetMapping("/send-code")
    public Result<Void> sendCode(@RequestParam String email) {
        if (!verifyCodeService.sendCode(email)){
            throw new AppException(ResultCode.OPERATION_FAIL,"验证码发送失败");
        }
        return Result.success("验证码发送请求已提交，请注意查收邮箱");
    }

    @Operation(summary = "退出登录接口")
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        return userService.logout(request);
    }

    @Operation(summary = "权限测试接口")
    @PreAuthorize("hasAuthority('test:hello')")
    @GetMapping("/hello")
    public Result<Void> hello() {
        return Result.success("hello");
    }
}