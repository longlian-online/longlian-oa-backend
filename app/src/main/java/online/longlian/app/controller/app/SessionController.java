package online.longlian.app.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.bo.OTPGenerateContextBO;
import online.longlian.app.pojo.bo.SessionLoginByCodeParamsBO;
import online.longlian.app.pojo.bo.SessionLoginByPwdParamsBO;
import online.longlian.app.pojo.bo.SessionLoginResultBO;
import online.longlian.app.pojo.bo.SessionLogoutParamsBO;
import online.longlian.app.pojo.dto.app.EmailLoginCodeDTO;
import online.longlian.app.pojo.dto.app.LoginByCodeDTO;
import online.longlian.app.pojo.dto.app.LoginByPwdDTO;
import online.longlian.app.pojo.vo.app.LoginVO;
import online.longlian.app.service.otp.OTPServiceFactory;
import online.longlian.app.service.user.SessionService;
import online.longlian.generator.enumeration.OTPType;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "用户会话接口", description = "用户会话相关接口")
@RequestMapping("/app/session")
@RestController
@RequiredArgsConstructor
public class SessionController {
    private final SessionService sessionService;
    private final OTPServiceFactory otpServiceFactory;

    @Operation(summary = "密码登录", description = "使用用户名+密码登录", security = {})
    @PostMapping("/pwd")
    public Result<LoginVO> loginByPwd(@RequestBody @Valid LoginByPwdDTO loginByPwdDTO) {
        SessionLoginResultBO resultBO = sessionService.loginByPwd(
                SessionLoginByPwdParamsBO.builder()
                        .username(loginByPwdDTO.getUsername())
                        .password(loginByPwdDTO.getPassword())
                        .build()
        );
        LoginVO loginVO = new LoginVO();
        BeanUtils.copyProperties(resultBO, loginVO);
        return Result.success("登录成功", loginVO);
    }

    @Operation(summary = "验证码登录", description = "使用邮箱+验证码登录", security = {})
    @PostMapping("/email")
    public Result<LoginVO> loginByCode(@RequestBody @Valid LoginByCodeDTO loginByCodeDTO) {
        SessionLoginResultBO resultBO = sessionService.loginByCode(
                SessionLoginByCodeParamsBO.builder()
                        .email(loginByCodeDTO.getEmail())
                        .code(loginByCodeDTO.getCode())
                        .build()
        );
        LoginVO loginVO = new LoginVO();
        BeanUtils.copyProperties(resultBO, loginVO);
        return Result.success("登录成功", loginVO);
    }

    @Operation(summary = "发送邮箱验证码", security = {})
    @PostMapping("/email/code")
    public Result<Void> sendCode(@RequestBody EmailLoginCodeDTO emailLoginCodeDTO) {
        otpServiceFactory.get(OTPType.EmailVerify).generate(
                OTPGenerateContextBO.builder()
                        .target(emailLoginCodeDTO.getEmail())
                        .creatorId(0L)
                        .build()
        );
        return Result.success("验证码发送请求已提交，请注意查收邮箱");
    }

    @Operation(summary = "退出登录")
    @DeleteMapping("/")
    public Result<Void> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Result.success("登出成功");
        }
        String token = authHeader.substring(7);
        sessionService.logout(
                SessionLogoutParamsBO.builder()
                        .userId(sessionService.getCurrentUserId())
                        .token(token)
                        .build()
        );
        return Result.success("登出成功");
    }
}
