package online.longlian.app.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.annotation.ResponseMessage;
import online.longlian.app.common.annotation.UserSession;
import online.longlian.app.common.enumeration.OrganizationDeclaration;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.resolver.SessionContext;
import online.longlian.app.pojo.bo.app.UserOrgApiKeyParamsBO;
import online.longlian.app.pojo.vo.app.UserOrgApiKeyGenerateVO;
import online.longlian.app.pojo.vo.app.UserOrgApiKeyVO;
import online.longlian.app.pojo.vo.app.UserOrgApiKeyOrganizationVO;
import online.longlian.app.service.app.UserOrgApiKeyService;
import online.longlian.app.common.properties.LonglianProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "用户组织 API Key")
@RestController
@RequiredArgsConstructor
@RequestMapping("/app/user/organizations")
public class UserOrgApiKeyController {

    private final UserOrgApiKeyService apiKeyService;
    private final LonglianProperties longlianProperties;

    @Value("${spring.ai.mcp.server.streamable-http.mcp-endpoint:/mcp}")
    private String mcpEndpoint;

    @Operation(summary = "获取本人可管理 API Key 的组织")
    @GetMapping("/api-key-settings")
    public List<UserOrgApiKeyOrganizationVO> listOwnOrganizations(@UserSession(OrganizationDeclaration.NONE) SessionContext session) {
        return apiKeyService.listOwnOrganizations(session.userId()).stream()
                .map(org -> new UserOrgApiKeyOrganizationVO(org.id().toString(), org.name())).toList();
    }

    @Operation(summary = "获取本人组织 API Key 状态")
    @GetMapping("/{orgId}/api-keys/current")
    public UserOrgApiKeyVO getCurrent(@UserSession(OrganizationDeclaration.NONE) SessionContext session,
                                      @PathVariable Long orgId, HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        var key = apiKeyService.getCurrent(toParams(session, orgId));
        return new UserOrgApiKeyVO(key.id() == null ? null : key.id().toString(), orgId.toString(), key.state(),
                key.prefix(), key.generatedAt(), key.revokedAt(), key.suspendedReason(), key.canGenerate(), key.canRevoke());
    }

    @Operation(summary = "生成或重新生成本人组织 API Key")
    @PostMapping("/{orgId}/api-keys")
    @ResponseMessage("生成成功")
    public UserOrgApiKeyGenerateVO generate(@UserSession(OrganizationDeclaration.NONE) SessionContext session,
                                            @PathVariable Long orgId, HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        var key = apiKeyService.generate(toParams(session, orgId));
        return new UserOrgApiKeyGenerateVO(key.id().toString(), orgId.toString(), key.apiKey(), key.prefix(), key.generatedAt(),
                longlianProperties.getServerUrl().replaceAll("/+$", "") + mcpEndpoint);
    }

    @Operation(summary = "撤销本人组织 API Key")
    @DeleteMapping("/{orgId}/api-keys/current")
    @ResponseMessage("已撤销")
    public void revoke(@UserSession(OrganizationDeclaration.NONE) SessionContext session,
                       @PathVariable Long orgId) {
        apiKeyService.revoke(toParams(session, orgId));
    }

    private UserOrgApiKeyParamsBO toParams(SessionContext session, Long orgId) {
        if (orgId <= 0) {
            throw new AppException(ResultCode.PARAM_ERROR, "组织 ID 必须为正数");
        }
        return new UserOrgApiKeyParamsBO(session.userId(), orgId);
    }
}
