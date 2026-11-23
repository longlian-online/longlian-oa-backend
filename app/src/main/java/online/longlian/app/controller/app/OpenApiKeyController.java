package online.longlian.app.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.annotation.ResponseMessage;
import online.longlian.app.common.annotation.UserSession;
import online.longlian.app.common.enumeration.OrganizationDeclaration;
import online.longlian.app.common.properties.OpenApiProperties;
import online.longlian.app.common.resolver.SessionContext;
import online.longlian.app.pojo.bo.open.OpenApiKeyCreateParamsBO;
import online.longlian.app.pojo.bo.open.OpenApiKeyCreateResultBO;
import online.longlian.app.pojo.bo.open.OpenApiKeyGroupResultBO;
import online.longlian.app.pojo.bo.open.OpenApiKeyItemResultBO;
import online.longlian.app.pojo.bo.open.OpenApiKeyRevokeParamsBO;
import online.longlian.app.pojo.dto.app.OpenApiKeyCreateDTO;
import online.longlian.app.pojo.vo.app.OpenApiKeyCreatedVO;
import online.longlian.app.pojo.vo.app.OpenApiKeyGroupVO;
import online.longlian.app.pojo.vo.app.OpenApiKeyVO;
import online.longlian.app.service.open.OpenApiKeyService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@Tag(name = "OpenAPI密钥接口", description = "个人设置里的 OpenAPI 密钥管理，作用于当前组织")
@RequestMapping("/app/openapi-keys")
@RestController
public class OpenApiKeyController {

    private final OpenApiKeyService openApiKeyService;
    private final OpenApiProperties openApiProperties;

    @Operation(summary = "按组织分组的密钥列表")
    @GetMapping
    public List<OpenApiKeyGroupVO> listGroups(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext) {
        return openApiKeyService.listGroups(sessionContext.userId()).stream()
                .map(this::toGroupVO)
                .toList();
    }

    @Operation(summary = "在当前组织创建密钥，明文只返回一次")
    @PostMapping
    public OpenApiKeyCreatedVO create(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
            @Valid @RequestBody OpenApiKeyCreateDTO dto) {
        OpenApiKeyCreateResultBO result = openApiKeyService.create(new OpenApiKeyCreateParamsBO(
                sessionContext.userId(), sessionContext.orgId(), dto.getName()));
        OpenApiKeyCreatedVO vo = new OpenApiKeyCreatedVO();
        vo.setId(result.id());
        vo.setKey(result.key());
        return vo;
    }

    @Operation(summary = "吊销当前组织的密钥")
    @DeleteMapping("/{keyId}")
    @ResponseMessage("已吊销")
    public void revoke(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
            @PathVariable Long keyId) {
        openApiKeyService.revoke(new OpenApiKeyRevokeParamsBO(
                sessionContext.userId(), sessionContext.orgId(), keyId));
    }

    private OpenApiKeyGroupVO toGroupVO(OpenApiKeyGroupResultBO group) {
        OpenApiKeyGroupVO vo = new OpenApiKeyGroupVO();
        vo.setOrgId(group.orgId());
        vo.setOrgName(group.orgName());
        vo.setMaxKeys(group.maxKeys() != null ? group.maxKeys() : openApiProperties.getMaxKeysPerOrg());
        vo.setKeys(group.keys().stream().map(this::toKeyVO).toList());
        return vo;
    }

    private OpenApiKeyVO toKeyVO(OpenApiKeyItemResultBO item) {
        OpenApiKeyVO vo = new OpenApiKeyVO();
        vo.setId(item.id());
        vo.setName(item.name());
        vo.setCreatedAt(item.createdAt());
        vo.setLastUsedAt(item.lastUsedAt());
        return vo;
    }
}
