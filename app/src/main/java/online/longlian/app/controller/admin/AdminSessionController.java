package online.longlian.app.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.constants.CommonConstants;
import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.bo.AdminLoginParamsBO;
import online.longlian.app.pojo.bo.AdminLoginResultBO;
import online.longlian.app.pojo.bo.AdminLogoutParamsBO;
import online.longlian.app.pojo.dto.admin.AdminLoginDTO;
import online.longlian.app.pojo.vo.admin.AdminLoginVO;
import online.longlian.app.service.admin.AdminSessionService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "管理员会话接口", description = "管理员会话相关接口")
@RequestMapping("/admin/session")
@RestController
@RequiredArgsConstructor
public class AdminSessionController {

    private final AdminSessionService adminSessionService;

    @Operation(summary = "管理员登录", description = "使用用户名+密码登录", security = {})
    @PostMapping("/login")
    public Result<AdminLoginVO> login(@RequestBody @Valid AdminLoginDTO dto) {
        AdminLoginResultBO resultBO = adminSessionService.login(
                AdminLoginParamsBO.builder()
                        .username(dto.getUsername())
                        .password(dto.getPassword())
                        .build()
        );
        AdminLoginVO loginVO = new AdminLoginVO();
        BeanUtils.copyProperties(resultBO, loginVO);
        return Result.success("登录成功", loginVO);
    }

    @Operation(summary = "管理员登出")
    @DeleteMapping("/")
    public Result<Void> logout(HttpServletRequest request) {
        adminSessionService.logout(
                AdminLogoutParamsBO.builder()
                        .adminId(adminSessionService.getCurrentAdminId())
                        .token((String) request.getAttribute(CommonConstants.CURRENT_TOKEN))
                        .build()
        );
        return Result.success("登出成功");
    }
}
