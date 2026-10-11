package online.longlian.app.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.annotation.UserSession;
import online.longlian.app.common.enumeration.OrganizationDeclaration;
import online.longlian.app.common.resolver.SessionContext;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberExitParamsBO;
import online.longlian.app.service.orgadmin.OrganizationLifecycleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/app/organizations")
@Tag(name = "用户组织成员关系", description = "当前用户退出组织")
public class OrganizationMembershipController {
    private final OrganizationLifecycleService organizationLifecycleService;

    @Operation(summary = "退出组织", description = "组织所有者必须先转让所有权")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{orgId}/membership")
    public void exit(@UserSession(OrganizationDeclaration.NONE) SessionContext context,
                     @PathVariable Long orgId) {
        organizationLifecycleService.exitOrganization(OrgMemberExitParamsBO.builder()
                .orgId(orgId)
                .operatorUserId(context.userId())
                .build());
    }
}
