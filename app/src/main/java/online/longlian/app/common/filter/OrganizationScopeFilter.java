package online.longlian.app.common.filter;

import com.alibaba.fastjson2.JSON;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.constants.CommonConstants;
import online.longlian.app.common.constants.InviteConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.Result;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.security.OrganizationScope;
import online.longlian.app.common.security.UserDetailImpl;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.service.common.OrganizationMembershipService;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrganizationScopeFilter extends OncePerRequestFilter {

    public static final String ORG_ID_HEADER = "X-Org-Id";
    public static final String SCOPE_ATTRIBUTE = OrganizationScopeFilter.class.getName() + ".SCOPE";

    private final OrganizationMembershipService organizationMembershipService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication != null && authentication.getPrincipal() instanceof UserDetailImpl user)) {
            filterChain.doFilter(request, response);
            return;
        }
        String header = request.getHeader(ORG_ID_HEADER);
        if (header == null) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            long orgId = parseOrgId(header);
            OrganizationMember member = organizationMembershipService.requireEnabledMember(user.getId(), orgId);
            if (!InviteConstants.ROLE_ORG_ADMIN.equals(member.getOrgRole())
                    && !InviteConstants.ROLE_ORG_USER.equals(member.getOrgRole())) {
                throw new AppException(ResultCode.OPERATION_FAIL, "组织角色无效");
            }
            // 替换而不是追加：登录缓存里的旧角色不能跟到这次声明的组织。
            UsernamePasswordAuthenticationToken replaced = new UsernamePasswordAuthenticationToken(
                    user,
                    authentication.getCredentials(),
                    List.of(new SimpleGrantedAuthority("ROLE_" + member.getOrgRole())));
            SecurityContextHolder.getContext().setAuthentication(replaced);
            request.setAttribute(SCOPE_ATTRIBUTE,
                    new OrganizationScope(member.getOrgId(), member.getId(), member.getOrgRole()));
            filterChain.doFilter(request, response);
        } catch (AppException e) {
            SecurityContextHolder.clearContext();
            writeFailure(response, Result.fail(e.getCode(), e.getMsg()));
        } catch (DataAccessException e) {
            SecurityContextHolder.clearContext();
            log.error("组织作用域查询失败 | type={}", e.getClass().getName());
            writeFailure(response, Result.fail(ResultCode.FAIL));
        }
    }

    private long parseOrgId(String header) {
        String trimmed = header.trim();
        if (trimmed.isEmpty()) {
            throw new AppException(ResultCode.OPERATION_FAIL, "组织不能为空");
        }
        if (!trimmed.matches("[0-9]+")) {
            throw new AppException(ResultCode.OPERATION_FAIL, "组织ID不合法");
        }
        try {
            long orgId = Long.parseLong(trimmed);
            if (orgId <= 0) {
                throw new AppException(ResultCode.OPERATION_FAIL, "组织ID不合法");
            }
            return orgId;
        } catch (NumberFormatException e) {
            throw new AppException(ResultCode.OPERATION_FAIL, "组织ID不合法");
        }
    }

    private void writeFailure(HttpServletResponse response, Result<?> result) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(HttpStatus.OK.value());
        response.setContentType(CommonConstants.CONTENT_TYPE);
        response.getWriter().write(JSON.toJSONString(result));
    }
}
