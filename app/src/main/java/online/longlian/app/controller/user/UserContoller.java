package online.longlian.app.controller.user;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.result.Result;

import online.longlian.app.pojo.dto.LoginReqDTO;

import online.longlian.app.service.UserService;

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

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginReqDTO loginReqDTO) {
        return userService.login(loginReqDTO);
    }
    @PreAuthorize("hasAuthority('test:hello')")
    @GetMapping("/hello")
    public Result<String> hello() {
        return Result.success("hello");
    }
}