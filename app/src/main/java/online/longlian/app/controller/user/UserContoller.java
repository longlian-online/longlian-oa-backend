package online.longlian.app.controller.user;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.Result;

import online.longlian.app.common.result.ResultCode;
import online.longlian.app.pojo.dto.LoginByCodeDTO;
import online.longlian.app.pojo.dto.LoginByPwdDTO;

import online.longlian.app.service.user.UserService;

import online.longlian.app.service.VerifyCodeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@Tag(name = "用户管理接口", description = "用户相关的基础操作接口")
@RequestMapping("/app/user")
@RestController
@RequiredArgsConstructor
public class UserContoller  {

    private final UserService userService;
    private final VerifyCodeService verifyCodeService;
    @PostMapping("/login/pwd")
    public Result<Map<String, Object>> loginByPwd(@RequestBody LoginByPwdDTO loginByPwdDTO) {
        return userService.loginByPwd(loginByPwdDTO);
    }
    @PostMapping("/login/code")
    public Result<Map<String, Object>> loginByCode(@RequestBody LoginByCodeDTO loginByCodeDTO) {
        return userService.loginByCode(loginByCodeDTO);
    }
    @GetMapping("/send-code")
    public Result<Void> sendCode(@RequestParam String email) {
        if (!verifyCodeService.sendCode(email)){
            throw new AppException(ResultCode.OPERATION_FAIL);
        }
        return Result.success("验证码发送成功");
    }
    @PreAuthorize("hasAuthority('test:hello')")
    @GetMapping("/hello")
    public Result<Void> hello() {
        return Result.success("hello");
    }
}