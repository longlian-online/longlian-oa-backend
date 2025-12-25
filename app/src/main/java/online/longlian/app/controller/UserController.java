package online.longlian.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 用户信息表 前端控制器
 * </p>
 *
 * @author longlian
 * @since 2025-12-19
 */
@Slf4j
@RestController
@RequestMapping("/app/user")
@Tag(name = "用户管理接口", description = "用户相关的基础操作接口")
public class UserController {

    @GetMapping("/test")
    @Operation(summary = "日志测试接口")
    public String testLog() {
        log.debug("这是DEBUG日志（仅dev环境显示）");
        log.info("这是INFO日志（dev/sit环境显示）");
        log.warn("这是WARN日志（所有环境显示）");
        log.error("这是ERROR日志（所有环境显示）");
        return "日志测试成功";
    }
}
