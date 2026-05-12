package online.longlian.app.controller.orgadmin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.bo.PageParamsBO;
import online.longlian.app.pojo.bo.PageResultBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectAdminListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectAdminListResultBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectChangeStatusParamsBO;
import online.longlian.app.pojo.dto.common.ChangeStatusDTO;
import online.longlian.app.pojo.dto.orgadmin.ProjectAdminListDTO;
import online.longlian.app.pojo.vo.common.PageResultVO;
import online.longlian.app.pojo.vo.orgadmin.ProjectAdminInfoVO;
import online.longlian.app.service.common.CurrentOrganizationService;
import online.longlian.app.service.orgadmin.ProjectService;
import online.longlian.app.service.user.SessionService;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "企划接口", description = "企划相关接口")
@RequestMapping("/orgadmin/projects")
@RestController("orgAdminProjectController")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ORG_ADMIN')")
public class ProjectController {

    private final ProjectService projectService;
    private final SessionService sessionService;
    private final CurrentOrganizationService currentOrganizationService;

    @Operation(summary = "管理端分页查询企划列表", description = "支持标题模糊搜索、类型精确筛选、创建时间区间，默认创建时间倒序")
    @PostMapping("")
    public Result<PageResultVO<ProjectAdminInfoVO>> getAdminProjectList(
            @RequestBody @Valid ProjectAdminListDTO projectAdminListDTO) {
        Long userId = sessionService.getCurrentUserId();
        Long orgId = currentOrganizationService.requireCurrentOrgId(userId);

        PageResultBO<ProjectAdminListResultBO> resultBO = projectService.getAdminProjectList(
                ProjectAdminListParamsBO.builder()
                        .orgId(orgId)
                        .keyword(projectAdminListDTO.getKeyword())
                        .typeId(projectAdminListDTO.getTypeId())
                        .startCreatedTime(projectAdminListDTO.getStartCreatedTime())
                        .endCreatedTime(projectAdminListDTO.getEndCreatedTime())
                        .orderDir(projectAdminListDTO.getOrderDir())
                        .page(new PageParamsBO(projectAdminListDTO.getPageNum(), projectAdminListDTO.getPageSize()))
                        .build()
        );

        List<ProjectAdminInfoVO> projectAdminInfoVOList = resultBO.getList().stream().map(bo -> {
            ProjectAdminInfoVO projectAdminInfoVO = new ProjectAdminInfoVO();
            BeanUtils.copyProperties(bo, projectAdminInfoVO);
            return projectAdminInfoVO;
        }).toList();

        return Result.success("查询成功", new PageResultVO<>(projectAdminInfoVOList, resultBO.getTotal()));
    }

    @Operation(summary = "启用/禁用企划", description = "禁用后用户端不展示该企划。status: ENABLED-启用，DISABLED-禁用")
    @PatchMapping("/{projectId}/status")
    public Result<Void> changeProjectStatus(@PathVariable Long projectId, @RequestBody @Valid ChangeStatusDTO changeStatusDTO) {
        Long userId = sessionService.getCurrentUserId();
        Long orgId = currentOrganizationService.requireCurrentOrgId(userId);

        projectService.changeProjectStatus(
                ProjectChangeStatusParamsBO.builder()
                        .projectId(projectId)
                        .orgId(orgId)
                        .status(changeStatusDTO.getStatus())
                        .build()
        );
        return Result.success(null);
    }
}
