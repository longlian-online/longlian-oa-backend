package online.longlian.app.controller.orgadmin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.annotation.ResponseMessage;
import online.longlian.app.common.annotation.UserSession;
import online.longlian.app.common.enumeration.OrganizationDeclaration;
import online.longlian.app.common.resolver.SessionContext;
import online.longlian.app.pojo.bo.common.PageParamsBO;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetTagCreateParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetTagDeleteParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetTagListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetTagListResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetTagUpdateParamsBO;
import online.longlian.app.pojo.dto.orgadmin.OrganizationPresetTagCreateDTO;
import online.longlian.app.pojo.dto.orgadmin.OrganizationPresetTagListDTO;
import online.longlian.app.pojo.dto.orgadmin.OrganizationPresetTagUpdateDTO;
import online.longlian.app.pojo.vo.common.PageResultVO;
import online.longlian.app.pojo.vo.orgadmin.OrganizationPresetTagAdminVO;
import online.longlian.app.service.orgadmin.OrganizationPresetTagService;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
@Tag(name = "组织预制标签", description = "组织预制标签的增删改查，仅管理员可操作")
@RequestMapping("/orgadmin/preset-tags")
@RestController("orgAdminOrganizationPresetTagController")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ORG_ADMIN')")
public class OrganizationPresetTagController {

    private final OrganizationPresetTagService organizationPresetTagService;

    @Operation(summary = "分页查询组织预制标签", description = "支持标签名模糊搜索，默认按创建时间倒序")
    @GetMapping("")
    @ResponseMessage("查询成功")
    public PageResultVO<OrganizationPresetTagAdminVO> listPresetTags(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
            @ModelAttribute @Valid OrganizationPresetTagListDTO listDTO) {
        PageResultBO<OrganizationPresetTagListResultBO> resultBO = organizationPresetTagService.listPresetTags(
                OrganizationPresetTagListParamsBO.builder()
                        .orgId(sessionContext.orgId())
                        .keyword(listDTO.getKeyword())
                        .orderDir(listDTO.getOrderDir())
                        .page(new PageParamsBO(listDTO.getPageNum(), listDTO.getPageSize()))
                        .build()
        );
        List<OrganizationPresetTagAdminVO> list = resultBO.getList().stream().map(bo -> {
            OrganizationPresetTagAdminVO vo = new OrganizationPresetTagAdminVO();
            BeanUtils.copyProperties(bo, vo);
            return vo;
        }).toList();
        return new PageResultVO<>(list, resultBO.getTotal());
    }

    @Operation(summary = "创建组织预制标签")
    @PostMapping("")
    @ResponseMessage("创建成功")
    public void createPresetTag(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
            @RequestBody @Valid OrganizationPresetTagCreateDTO createDTO) {
        organizationPresetTagService.createPresetTag(OrganizationPresetTagCreateParamsBO.builder()
                .orgId(sessionContext.orgId())
                .creatorId(sessionContext.userId())
                .name(createDTO.getName().trim())
                .color(createDTO.getColor())
                .priority(createDTO.getPriority() == null ? 0 : createDTO.getPriority())
                .build());
    }

    @Operation(summary = "修改组织预制标签", description = "颜色不传或为空时改为默认；优先级省略时保持原值")
    @PutMapping("/{tagId}")
    @ResponseMessage("修改成功")
    public void updatePresetTag(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
            @PathVariable Long tagId,
            @RequestBody @Valid OrganizationPresetTagUpdateDTO updateDTO) {
        organizationPresetTagService.updatePresetTag(OrganizationPresetTagUpdateParamsBO.builder()
                .tagId(tagId)
                .orgId(sessionContext.orgId())
                .name(updateDTO.getName().trim())
                .color(updateDTO.getColor())
                .priority(updateDTO.getPriority())
                .build());
    }

    @Operation(summary = "删除组织预制标签", description = "删除后不能再选择。已经写入企划的标签不受影响")
    @DeleteMapping("/{tagId}")
    @ResponseMessage("删除成功")
    public void deletePresetTag(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
            @PathVariable Long tagId) {
        organizationPresetTagService.deletePresetTag(OrganizationPresetTagDeleteParamsBO.builder()
                .tagId(tagId)
                .orgId(sessionContext.orgId())
                .build());
    }
}
