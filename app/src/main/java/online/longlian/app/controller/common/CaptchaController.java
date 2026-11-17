package online.longlian.app.controller.common;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.annotation.UserSession;
import online.longlian.app.common.enumeration.OrganizationDeclaration;
import online.longlian.app.pojo.bo.common.CaptchaConfigResultBO;
import online.longlian.app.pojo.vo.common.CaptchaConfigVO;
import online.longlian.app.service.captcha.CaptchaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "人机验证配置接口")
@RestController
@RequestMapping("/common/captcha")
@RequiredArgsConstructor
public class CaptchaController {
    private final CaptchaService captchaService;

    @Operation(summary = "获取人机验证公开配置", description = "无需登录，不返回密钥或内网验票地址")
    @SecurityRequirements
    @UserSession(OrganizationDeclaration.NONE)
    @GetMapping("/config")
    public CaptchaConfigVO getConfig() {
        CaptchaConfigResultBO config = captchaService.getConfig();
        return new CaptchaConfigVO(config.isEnabled(), config.getServerUrl(), config.getSiteKey());
    }
}
