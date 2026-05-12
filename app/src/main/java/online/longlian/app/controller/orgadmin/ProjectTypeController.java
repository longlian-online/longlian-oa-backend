package online.longlian.app.controller.orgadmin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.bo.PageParamsBO;
import online.longlian.app.pojo.bo.PageResultBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeChangeStatusParamsBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeCreateParamsBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeListResultBO;
import online.longlian.app.pojo.dto.common.ChangeStatusDTO;
import online.longlian.app.pojo.dto.orgadmin.ProjectTypeCreateDTO;
import online.longlian.app.pojo.dto.orgadmin.ProjectTypeListDTO;
import online.longlian.app.pojo.vo.common.PageResultVO;
import online.longlian.app.pojo.vo.orgadmin.ProjectTypeAdminVO;
import online.longlian.app.service.common.CurrentOrganizationService;
import online.longlian.app.service.orgadmin.ProjectTypeService;
import online.longlian.app.service.user.SessionService;
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
    private final SessionService sessionService;
    private final CurrentOrganizationService currentOrganizationService;

    @Operation(summary = "分页查询企划类型列表", description = "支持名称模糊搜索，默认按创建时间倒序")
    @GetMapping("")
    public Result<PageResultVO<ProjectTypeAdminVO>> listProjectTypes(
            @ModelAttribute @Valid ProjectTypeListDTO projectTypeListDTO) {
        Long userId = sessionService.getCurrentUserId();
        Long orgId = currentOrganizationService.requireCurrentOrgId(userId);

        PageResultBO<ProjectTypeListResultBO> resultBO = projectTypeService.listProjectTypes(
                ProjectTypeListParamsBO.builder()
                        .orgId(orgId)
                        .keyword(projectTypeListDTO.getKeyword())
                        .orderDir(projectTypeListDTO.getOrderDir())
                        .page(new PageParamsBO(projectTypeListDTO.getPageNum(), projectTypeListDTO.getPageSize()))
                        .build()
        );

        List<ProjectTypeAdminVO> projectTypeAdminVOList = resultBO.getList().stream().map(bo -> {
            ProjectTypeAdminVO projectTypeAdminVO = new ProjectTypeAdminVO();
            BeanUtils.copyProperties(bo, projectTypeAdminVO);
            return projectTypeAdminVO;
        }).toList();

        return Result.success("查询成功", new PageResultVO<>(projectTypeAdminVOList, resultBO.getTotal()));
    }

    @Operation(summary = "创建企划类型")
    @PostMapping("")
    public Result<Void> createProjectType(
            @RequestBody @Valid ProjectTypeCreateDTO projectTypeCreateDTO) {
        Long userId = sessionService.getCurrentUserId();
        Long orgId = currentOrganizationService.requireCurrentOrgId(userId);

        projectTypeService.createProjectType(
                ProjectTypeCreateParamsBO.builder()
                        .orgId(orgId)
                        .creatorId(userId)
                        .name(projectTypeCreateDTO.getName())
                        .build()
        );
        return Result.success("创建成功");
    }

    @Operation(summary = "启用/禁用企划类型", description = "禁用后用户端不展示该类型，但已有数据保留。status: ENABLED-启用，DISABLED-禁用")
    @PatchMapping("/{typeId}/status")
    public Result<Void> changeProjectTypeStatus(@PathVariable Long typeId, @RequestBody @Valid ChangeStatusDTO changeStatusDTO) {
        Long userId = sessionService.getCurrentUserId();
        Long orgId = currentOrganizationService.requireCurrentOrgId(userId);

        projectTypeService.changeProjectTypeStatus(
                ProjectTypeChangeStatusParamsBO.builder()
                        .typeId(typeId)
                        .orgId(orgId)
                        .status(changeStatusDTO.getStatus())
                        .build()
        );
        return Result.success(null);
    }
}
