package online.longlian.app.controller.orgadmin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.annotation.ResponseMessage;
import online.longlian.app.common.annotation.UserSession;
import online.longlian.app.common.enumeration.OrganizationDeclaration;
import online.longlian.app.common.resolver.SessionContext;
import online.longlian.app.pojo.bo.common.PageParamsBO;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeChangeStatusParamsBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeCreateParamsBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeCustomFieldBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeDeleteParamsBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeListResultBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeUpdateParamsBO;
import online.longlian.app.pojo.dto.common.ChangeStatusDTO;
import online.longlian.app.pojo.dto.orgadmin.ProjectTypeCreateDTO;
import online.longlian.app.pojo.dto.orgadmin.ProjectTypeCustomFieldDTO;
import online.longlian.app.pojo.dto.orgadmin.ProjectTypeListDTO;
import online.longlian.app.pojo.dto.orgadmin.ProjectTypeUpdateDTO;
import online.longlian.app.pojo.vo.common.PageResultVO;
import online.longlian.app.pojo.vo.orgadmin.ProjectTypeAdminVO;
import online.longlian.app.pojo.vo.orgadmin.ProjectTypeCustomFieldVO;
import online.longlian.app.service.orgadmin.ProjectTypeService;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "企划类型管理", description = "企划类型的增删改查与状态管理，仅管理员可操作")
@RequestMapping("/orgadmin/project-types")
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ORG_ADMIN')")
public class ProjectTypeController {

    private final ProjectTypeService projectTypeService;

    @Operation(summary = "分页查询企划类型列表", description = "支持名称模糊搜索，默认按创建时间倒序")
    @GetMapping("")
    @ResponseMessage("查询成功")
    public PageResultVO<ProjectTypeAdminVO> listProjectTypes(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
            @ModelAttribute @Valid ProjectTypeListDTO projectTypeListDTO) {
        PageResultBO<ProjectTypeListResultBO> resultBO = projectTypeService.listProjectTypes(
                ProjectTypeListParamsBO.builder()
                        .orgId(sessionContext.orgId())
                        .keyword(projectTypeListDTO.getKeyword())
                        .orderDir(projectTypeListDTO.getOrderDir())
                        .page(new PageParamsBO(projectTypeListDTO.getPageNum(), projectTypeListDTO.getPageSize()))
                        .build()
        );

        List<ProjectTypeAdminVO> projectTypeAdminVOList = resultBO.getList().stream().map(bo -> {
            ProjectTypeAdminVO projectTypeAdminVO = new ProjectTypeAdminVO();
            BeanUtils.copyProperties(bo, projectTypeAdminVO, "customFields");
            projectTypeAdminVO.setCustomFields(bo.getCustomFields().stream().map(field -> {
                ProjectTypeCustomFieldVO item = new ProjectTypeCustomFieldVO();
                BeanUtils.copyProperties(field, item);
                return item;
            }).toList());
            return projectTypeAdminVO;
        }).toList();

        return new PageResultVO<>(projectTypeAdminVOList, resultBO.getTotal());
    }

    @Operation(summary = "创建企划类型")
    @PostMapping("")
    @ResponseMessage("创建成功")
    public void createProjectType(@UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
                                   @RequestBody @Valid ProjectTypeCreateDTO projectTypeCreateDTO) {
        ProjectTypeCreateParamsBO.ProjectTypeCreateParamsBOBuilder builder = ProjectTypeCreateParamsBO.builder()
                .orgId(sessionContext.orgId())
                .creatorId(sessionContext.userId())
                .name(projectTypeCreateDTO.getName().trim())
                .priority(projectTypeCreateDTO.getPriority() == null ? 0 : projectTypeCreateDTO.getPriority());
        if (projectTypeCreateDTO.getCustomFields() != null) {
            builder.customFields(toCustomFields(projectTypeCreateDTO.getCustomFields()));
        }
        projectTypeService.createProjectType(builder.build());
    }

    @Operation(summary = "修改企划类型", description = "可修改名称、优先级和默认自定义表单。自定义表单省略时保持原设置")
    @PutMapping("/{typeId}")
    @ResponseMessage("修改成功")
    public void updateProjectType(@UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
                                  @PathVariable Long typeId,
                                  @RequestBody @Valid ProjectTypeUpdateDTO projectTypeUpdateDTO) {
        ProjectTypeUpdateParamsBO.ProjectTypeUpdateParamsBOBuilder builder = ProjectTypeUpdateParamsBO.builder()
                .typeId(typeId).orgId(sessionContext.orgId()).name(projectTypeUpdateDTO.getName().trim())
                .priority(projectTypeUpdateDTO.getPriority());
        if (projectTypeUpdateDTO.getCustomFields() != null) {
            builder.customFields(toCustomFields(projectTypeUpdateDTO.getCustomFields()));
        }
        projectTypeService.updateProjectType(builder.build());
    }

    private List<ProjectTypeCustomFieldBO> toCustomFields(List<ProjectTypeCustomFieldDTO> fields) {
        return fields.stream()
                .map(field -> ProjectTypeCustomFieldBO.builder()
                        .presetFieldId(field.getPresetFieldId())
                        .required(field.getRequired())
                        .build())
                .toList();
    }

    @Operation(summary = "删除企划类型", description = "仅允许删除未被企划引用的类型")
    @DeleteMapping("/{typeId}")
    @ResponseMessage("删除成功")
    public void deleteProjectType(@UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
                                  @PathVariable Long typeId) {
        projectTypeService.deleteProjectType(ProjectTypeDeleteParamsBO.builder()
                .typeId(typeId)
                .orgId(sessionContext.orgId())
                .build());
    }

    @Operation(summary = "启用/禁用企划类型", description = "禁用后用户端不展示该类型，但已有数据保留。status: ENABLED-启用，DISABLED-禁用")
    @PatchMapping("/{typeId}/status")
    @ResponseMessage("状态修改成功")
    public void changeProjectTypeStatus(@UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
                                                 @PathVariable Long typeId,
                                                 @RequestBody @Valid ChangeStatusDTO changeStatusDTO) {
        projectTypeService.changeProjectTypeStatus(
                ProjectTypeChangeStatusParamsBO.builder()
                        .typeId(typeId)
                        .orgId(sessionContext.orgId())
                        .status(changeStatusDTO.getStatus())
                        .build()
        );
    }
}
