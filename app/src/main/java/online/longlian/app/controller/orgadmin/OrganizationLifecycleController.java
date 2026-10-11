package online.longlian.app.controller.orgadmin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.annotation.UserSession;
import online.longlian.app.common.enumeration.OrganizationDeclaration;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.resolver.SessionContext;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.pojo.bo.orgadmin.OrgDissolveParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberRemoveParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberTransferOwnershipParamsBO;
import online.longlian.app.service.orgadmin.OrganizationLifecycleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/orgadmin")
@Tag(name="组织治理", description="成员移除、所有权转让和组织解散")
public class OrganizationLifecycleController {
    private final OrganizationLifecycleService service;

    @Operation(summary="移除组织成员")
    @PreAuthorize("hasAnyRole('ORG_OWNER','ORG_ADMIN')")
    @DeleteMapping("/members/{memberId}")
    public void remove(@UserSession(OrganizationDeclaration.REQUIRED) SessionContext context,@PathVariable Long memberId) {
        service.removeMember(OrgMemberRemoveParamsBO.builder().orgId(context.orgId())
                .operatorUserId(context.userId()).memberId(memberId).build());
    }

    @Operation(summary="转让组织所有权")
    @PreAuthorize("hasRole('ORG_OWNER')")
    @PutMapping("/members/{memberId}/ownership")
    public void transfer(@UserSession(OrganizationDeclaration.REQUIRED) SessionContext context,@PathVariable Long memberId) {
        service.transferOwnership(OrgMemberTransferOwnershipParamsBO.builder().orgId(context.orgId())
                .operatorUserId(context.userId()).memberId(memberId).build());
    }

    @Operation(summary="解散组织",description="保留历史数据，不能通过启用接口恢复")
    @PreAuthorize("hasRole('ORG_OWNER')")
    @DeleteMapping("/organizations/{orgId}")
    public void dissolve(@UserSession(OrganizationDeclaration.REQUIRED) SessionContext context,@PathVariable Long orgId) {
        if (!orgId.equals(context.orgId())) throw new AppException(ResultCode.PARAM_ERROR,"组织参数不一致");
        service.dissolveOrganization(OrgDissolveParamsBO.builder().orgId(orgId).operatorUserId(context.userId()).build());
    }
}
