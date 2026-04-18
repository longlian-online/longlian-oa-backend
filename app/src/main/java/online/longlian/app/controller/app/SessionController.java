package online.longlian.app.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.Result;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.pojo.dto.app.EmailLoginCodeDTO;
import online.longlian.app.pojo.dto.app.LoginByCodeDTO;
import online.longlian.app.pojo.dto.app.LoginByPwdDTO;
import online.longlian.app.pojo.vo.app.LoginVO;
import online.longlian.app.service.VerifyCodeService;
import online.longlian.app.service.user.SessionService;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "用户会话接口", description = "用户会话相关接口")
@RequestMapping("/app/session")
@RestController
@RequiredArgsConstructor
public class SessionController {
    private final SessionService sessionService;
    private final VerifyCodeService verifyCodeService;

    @Operation(summary = "密码登录", description = "使用用户名+密码登录", security = {})
    @PostMapping("/pwd")
    public Result<LoginVO> loginByPwd(@RequestBody @Valid LoginByPwdDTO loginByPwdDTO) {
        return sessionService.loginByPwd(loginByPwdDTO);
    }

    @Operation(summary = "验证码登录", description = "使用邮箱+验证码登录", security = {})
    @PostMapping("/email")
    public Result<LoginVO> loginByCode(@RequestBody @Valid LoginByCodeDTO loginByCodeDTO) {
        return sessionService.loginByCode(loginByCodeDTO);
    }

    @Operation(summary = "发送邮箱验证码", security = {})
    @PostMapping("/email/code")
    public Result<Void> sendCode(@RequestBody EmailLoginCodeDTO emailLoginCodeDTO) {
        if (!verifyCodeService.sendCode(emailLoginCodeDTO.getEmail())) {
            throw new AppException(ResultCode.OPERATION_FAIL, "验证码发送失败");
        }
        return Result.success("验证码发送请求已提交，请注意查收邮箱");
    }

    @Operation(summary = "退出登录")
    @DeleteMapping("/")
    public Result<Void> logout(HttpServletRequest request) {
        return sessionService.logout(request);
    }
}
