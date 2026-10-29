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
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetFieldCreateParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetFieldDeleteParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetFieldListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetFieldListResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetFieldUpdateParamsBO;
import online.longlian.app.pojo.dto.orgadmin.OrganizationPresetFieldCreateDTO;
import online.longlian.app.pojo.dto.orgadmin.OrganizationPresetFieldListDTO;
import online.longlian.app.pojo.dto.orgadmin.OrganizationPresetFieldUpdateDTO;
import online.longlian.app.pojo.vo.common.PageResultVO;
import online.longlian.app.pojo.vo.orgadmin.OrganizationPresetFieldAdminVO;
import online.longlian.app.service.orgadmin.OrganizationPresetFieldService;
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

@Tag(name = "组织预置表单字段", description = "组织预置表单字段的增删改查，仅管理员可操作")
@RequestMapping("/orgadmin/preset-fields")
@RestController("orgAdminOrganizationPresetFieldController")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ORG_ADMIN')")
public class OrganizationPresetFieldController {

    private final OrganizationPresetFieldService organizationPresetFieldService;

    @Operation(summary = "分页查询组织预置表单字段", description = "支持字段名模糊搜索，默认按创建时间倒序")
    @GetMapping("")
    @ResponseMessage("查询成功")
    public PageResultVO<OrganizationPresetFieldAdminVO> listPresetFields(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
            @ModelAttribute @Valid OrganizationPresetFieldListDTO listDTO) {
        PageResultBO<OrganizationPresetFieldListResultBO> resultBO = organizationPresetFieldService.listPresetFields(
                OrganizationPresetFieldListParamsBO.builder()
                        .orgId(sessionContext.orgId())
                        .keyword(listDTO.getKeyword())
                        .orderDir(listDTO.getOrderDir())
                        .page(new PageParamsBO(listDTO.getPageNum(), listDTO.getPageSize()))
                        .build()
        );
        List<OrganizationPresetFieldAdminVO> list = resultBO.getList().stream().map(bo -> {
            OrganizationPresetFieldAdminVO vo = new OrganizationPresetFieldAdminVO();
            BeanUtils.copyProperties(bo, vo);
            return vo;
        }).toList();
        return new PageResultVO<>(list, resultBO.getTotal());
    }

    @Operation(summary = "创建组织预置表单字段")
    @PostMapping("")
    @ResponseMessage("创建成功")
    public void createPresetField(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
            @RequestBody @Valid OrganizationPresetFieldCreateDTO createDTO) {
        organizationPresetFieldService.createPresetField(OrganizationPresetFieldCreateParamsBO.builder()
                .orgId(sessionContext.orgId())
                .creatorId(sessionContext.userId())
                .name(createDTO.getName().trim())
                .build());
    }

    @Operation(summary = "修改组织预置表单字段")
    @PutMapping("/{fieldId}")
    @ResponseMessage("修改成功")
    public void updatePresetField(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
            @PathVariable Long fieldId,
            @RequestBody @Valid OrganizationPresetFieldUpdateDTO updateDTO) {
        organizationPresetFieldService.updatePresetField(OrganizationPresetFieldUpdateParamsBO.builder()
                .fieldId(fieldId)
                .orgId(sessionContext.orgId())
                .name(updateDTO.getName().trim())
                .build());
    }

    @Operation(summary = "删除组织预置表单字段", description = "删除后不能再选择。已经写入企划的字段不受影响，类型默认表单中的引用会去掉")
    @DeleteMapping("/{fieldId}")
    @ResponseMessage("删除成功")
    public void deletePresetField(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
            @PathVariable Long fieldId) {
        organizationPresetFieldService.deletePresetField(OrganizationPresetFieldDeleteParamsBO.builder()
                .fieldId(fieldId)
                .orgId(sessionContext.orgId())
                .build());
    }
}
