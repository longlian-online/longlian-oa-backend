package online.longlian.app.controller.common;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.annotation.UserSession;
import online.longlian.app.common.enumeration.OrganizationDeclaration;
import online.longlian.app.pojo.vo.common.SystemInfoVO;
import online.longlian.app.service.common.SystemInfoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "系统信息接口")
@RestController
@RequestMapping("/common/system")
@RequiredArgsConstructor
public class SystemController {

    private final SystemInfoService systemInfoService;

    @Operation(summary = "获取系统信息", description = "无需登录，返回当前系统信息")
    @SecurityRequirements
    @UserSession(OrganizationDeclaration.NONE)
    @GetMapping("/info")
    public SystemInfoVO getSystemInfo() {
        return systemInfoService.getSystemInfo();
    }
}
