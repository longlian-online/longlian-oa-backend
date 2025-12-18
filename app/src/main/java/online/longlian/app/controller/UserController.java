package online.longlian.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author longlian
 * @since 2025-12-12
 */
@RestController
@RequestMapping("/app/user")
@Tag(name = "用户相关接口", description = "用户登录接口")
@Slf4j
public class UserController {
    @Operation(summary = "用户登录接口")
    @GetMapping("/login")
    public void login(){
        log.debug("login");
    }
}