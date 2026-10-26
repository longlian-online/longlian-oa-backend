package online.longlian.app.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.annotation.ResponseMessage;
import online.longlian.app.common.annotation.UserSession;
import online.longlian.app.common.enumeration.OrganizationDeclaration;
import online.longlian.app.common.resolver.SessionContext;
import online.longlian.app.pojo.vo.app.OrganizationPresetTagVO;
import online.longlian.app.service.orgadmin.OrganizationPresetTagService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "组织预制标签", description = "当前组织可供填写企划标签时选择的预制标签")
@RequestMapping("/app/preset-tags")
@RestController("appOrganizationPresetTagController")
@RequiredArgsConstructor
public class OrganizationPresetTagController {
    private final OrganizationPresetTagService organizationPresetTagService;

    @Operation(summary = "查询当前组织预制标签", description = "按优先级降序、ID 升序，不含值")
    @GetMapping("")
    @ResponseMessage("查询成功")
    public List<OrganizationPresetTagVO> listPresetTags(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext) {
        return organizationPresetTagService.listPresetTagOptions(sessionContext.orgId()).stream()
                .map(tag -> OrganizationPresetTagVO.builder()
                        .id(tag.getId())
                        .name(tag.getName())
                        .color(tag.getColor())
                        .build())
                .toList();
    }
}
