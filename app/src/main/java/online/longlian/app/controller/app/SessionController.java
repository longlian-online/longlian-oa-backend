package online.longlian.app.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.annotation.ResponseMessage;
import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.bo.common.OTPGenerateContextBO;
import online.longlian.app.pojo.bo.app.SessionLoginByCodeParamsBO;
import online.longlian.app.pojo.bo.app.SessionLoginByPwdParamsBO;
import online.longlian.app.pojo.bo.app.SessionLoginResultBO;
import online.longlian.app.pojo.bo.app.SessionLogoutParamsBO;
import online.longlian.app.pojo.dto.app.EmailCodeDTO;
import online.longlian.app.pojo.dto.app.LoginByCodeDTO;
import online.longlian.app.pojo.dto.app.LoginByPwdDTO;
import online.longlian.app.pojo.vo.app.LoginVO;
import online.longlian.app.service.otp.OTPServiceFactory;
import online.longlian.app.service.app.SessionService;
import online.longlian.common.enumeration.OTPType;
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
    @ResponseMessage("登录成功")
    public LoginVO loginByPwd(@RequestBody @Valid LoginByPwdDTO loginByPwdDTO) {
        SessionLoginResultBO resultBO = sessionService.loginByPwd(
                SessionLoginByPwdParamsBO.builder()
                        .username(loginByPwdDTO.getUsername())
                        .password(loginByPwdDTO.getPassword())
                        .build()
        );
        LoginVO loginVO = new LoginVO();
        BeanUtils.copyProperties(resultBO, loginVO);
        return loginVO;
    }

    @Operation(summary = "验证码登录", description = "使用邮箱+验证码登录", security = {})
    @PostMapping("/email")
    @ResponseMessage("登录成功")
    public LoginVO loginByCode(@RequestBody @Valid LoginByCodeDTO loginByCodeDTO) {
        SessionLoginResultBO resultBO = sessionService.loginByCode(
                SessionLoginByCodeParamsBO.builder()
                        .email(loginByCodeDTO.getEmail())
                        .code(loginByCodeDTO.getCode())
                        .build()
        );
        LoginVO loginVO = new LoginVO();
        BeanUtils.copyProperties(resultBO, loginVO);
        return loginVO;
    }

    @Operation(summary = "发送邮箱验证码", security = {})
    @PostMapping("/email/code")
    @ResponseMessage("验证码发送请求已提交，请注意查收邮箱")
    public void sendCode(@RequestBody EmailCodeDTO emailCodeDTO) {
        otpServiceFactory.get(OTPType.EmailVerify).generate(
                OTPGenerateContextBO.builder()
                        .creatorId(0L)
                        .receiver(emailCodeDTO.getEmail())
                        .businessType(emailCodeDTO.getBusinessType())
                        .build()
        );
    }

    @Operation(summary = "退出登录")
    @DeleteMapping("/")
    @ResponseMessage("登出成功")
    public void logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }
        String token = authHeader.substring(7);
        sessionService.logout(
                SessionLogoutParamsBO.builder()
                        .userId(sessionService.getCurrentUserId())
                        .token(token)
                        .build()
        );
    }
}
