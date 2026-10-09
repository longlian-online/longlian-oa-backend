package online.longlian.app.controller.orgadmin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.annotation.UserSession;
import online.longlian.app.common.enumeration.OrganizationDeclaration;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.resolver.SessionContext;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.pojo.bo.orgadmin.*;
import online.longlian.app.service.orgadmin.OrganizationLifecycleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name="组织成员与所有权生命周期")
public class OrganizationLifecycleController {
    private final OrganizationLifecycleService service;

    @Operation(summary="移除组织成员")
    @PreAuthorize("hasAnyRole('ORG_OWNER','ORG_ADMIN')")
    @DeleteMapping("/orgadmin/members/{memberId}")
    public void remove(@UserSession(OrganizationDeclaration.REQUIRED) SessionContext context,@PathVariable Long memberId) {
        service.removeMember(OrgMemberRemoveParamsBO.builder().orgId(context.orgId())
                .operatorUserId(context.userId()).memberId(memberId).build());
    }

    @Operation(summary="退出组织",description="组织所有者必须先转让所有权")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/app/organizations/{orgId}/membership")
    public void exit(@UserSession(OrganizationDeclaration.NONE) SessionContext context,@PathVariable Long orgId) {
        service.exitOrganization(OrgMemberExitParamsBO.builder().orgId(orgId).operatorUserId(context.userId()).build());
    }

    @Operation(summary="转让组织所有权")
    @PreAuthorize("hasRole('ORG_OWNER')")
    @PutMapping("/orgadmin/members/{memberId}/ownership")
    public void transfer(@UserSession(OrganizationDeclaration.REQUIRED) SessionContext context,@PathVariable Long memberId) {
        service.transferOwnership(OrgMemberTransferOwnershipParamsBO.builder().orgId(context.orgId())
                .operatorUserId(context.userId()).memberId(memberId).build());
    }

    @Operation(summary="解散组织",description="保留历史数据，不能通过启用接口恢复")
    @PreAuthorize("hasRole('ORG_OWNER')")
    @DeleteMapping("/orgadmin/organizations/{orgId}")
    public void dissolve(@UserSession(OrganizationDeclaration.REQUIRED) SessionContext context,@PathVariable Long orgId) {
        if (!orgId.equals(context.orgId())) throw new AppException(ResultCode.PARAM_ERROR,"组织参数不一致");
        service.dissolveOrganization(OrgDissolveParamsBO.builder().orgId(orgId).operatorUserId(context.userId()).build());
    }
}
