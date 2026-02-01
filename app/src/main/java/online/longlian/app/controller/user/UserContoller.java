package online.longlian.app.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.service.user.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "用户管理接口", description = "用户相关的基础操作接口")
@RequestMapping("/app/admin")
@RestController
public class UserContoller  {

    @Autowired
    private IUserService userService;

    @GetMapping("/user/{id}")
    @Operation(summary = "日志测试接口")
    public User getUser(@PathVariable Long id) {
        return userService.getByIdCached(id);
    }
}