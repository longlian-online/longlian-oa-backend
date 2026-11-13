package online.longlian.app.mcp;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.resolver.SessionContext;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.security.UserDetailImpl;
import online.longlian.app.service.common.OrganizationMembershipService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class McpUserSession {

    public static final String AUTHENTICATION = "longlian.authentication";
    public static final String ORGANIZATION = "longlian.organization";

    private final OrganizationMembershipService organizationMembershipService;

    public SessionContext require(McpSyncServerExchange exchange) {
        Object authentication = exchange.transportContext().get(AUTHENTICATION);
        if (!(authentication instanceof Authentication authenticated)
                || !authenticated.isAuthenticated()
                || !(authenticated.getPrincipal() instanceof UserDetailImpl user)) {
            throw new AppException(ResultCode.UNAUTHORIZED);
        }
        Object session = exchange.transportContext().get(SessionContext.class.getName());
        if (session instanceof SessionContext bound) {
            return bound;
        }
        Object organization = exchange.transportContext().get(ORGANIZATION);
        if (!(organization instanceof String header) || header.isBlank()) {
            throw new AppException(ResultCode.PARAM_ERROR, "组织不能为空，请配置 X-Org-Id 请求头");
        }
        Long orgId = positiveId(header.trim());
        organizationMembershipService.requireEnabledMember(user.getId(), orgId);
        return new SessionContext(user.getId(), orgId);
    }

    public static Long positiveId(String value) {
        if (value == null || !value.matches("[1-9][0-9]*")) {
            throw new AppException(ResultCode.PARAM_ERROR, "ID 必须是正整数十进制字符串");
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new AppException(ResultCode.PARAM_ERROR, "ID 超出支持范围");
        }
    }
}
