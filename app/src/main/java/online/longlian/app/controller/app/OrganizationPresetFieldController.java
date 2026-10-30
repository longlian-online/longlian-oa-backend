package online.longlian.app.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.annotation.ResponseMessage;
import online.longlian.app.common.annotation.UserSession;
import online.longlian.app.common.enumeration.OrganizationDeclaration;
import online.longlian.app.common.resolver.SessionContext;
import online.longlian.app.pojo.vo.app.OrganizationPresetFieldVO;
import online.longlian.app.service.orgadmin.OrganizationPresetFieldService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "组织预置表单字段", description = "当前组织可供填写企划自定义字段时选择的预置字段")
@RequestMapping("/app/preset-fields")
@RestController("appOrganizationPresetFieldController")
@RequiredArgsConstructor
public class OrganizationPresetFieldController {

    private final OrganizationPresetFieldService organizationPresetFieldService;

    @Operation(summary = "查询当前组织预置表单字段", description = "按创建时间倒序、ID 升序，不含值")
    @GetMapping("")
    @ResponseMessage("查询成功")
    public List<OrganizationPresetFieldVO> listPresetFields(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext) {
        return organizationPresetFieldService.listPresetFieldOptions(sessionContext.orgId()).stream()
                .map(field -> OrganizationPresetFieldVO.builder()
                        .id(field.getId())
                        .name(field.getName())
                        .build())
                .toList();
    }
}
