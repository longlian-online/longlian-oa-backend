package online.longlian.app.controller.orgadmin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.annotation.ResponseMessage;
import online.longlian.app.common.annotation.UserSession;
import online.longlian.app.common.resolver.SessionContext;
import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.bo.common.PageParamsBO;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectAdminListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectAdminListResultBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectChangeStatusParamsBO;
import online.longlian.app.pojo.dto.common.ChangeStatusDTO;
import online.longlian.app.pojo.dto.orgadmin.ProjectAdminListDTO;
import online.longlian.app.pojo.vo.common.PageResultVO;
import online.longlian.app.pojo.vo.orgadmin.ProjectAdminInfoVO;
import online.longlian.app.service.orgadmin.ProjectService;
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

    @Operation(summary = "管理端分页查询企划列表", description = "支持标题模糊搜索、类型精确筛选、创建时间区间，默认创建时间倒序")
    @PostMapping("")
    @ResponseMessage("查询成功")
    public PageResultVO<ProjectAdminInfoVO> getAdminProjectList(
            @UserSession(required = true) SessionContext sessionContext,
            @RequestBody @Valid ProjectAdminListDTO projectAdminListDTO) {
        PageResultBO<ProjectAdminListResultBO> resultBO = projectService.getAdminProjectList(
                ProjectAdminListParamsBO.builder()
                        .orgId(sessionContext.orgId())
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

        return new PageResultVO<>(projectAdminInfoVOList, resultBO.getTotal());
    }

    @Operation(summary = "启用/禁用企划", description = "禁用后用户端不展示该企划。status: ENABLED-启用，DISABLED-禁用")
    @PatchMapping("/{projectId}/status")
    public Result<Void> changeProjectStatus(@UserSession(required = true) SessionContext sessionContext,
                                             @PathVariable Long projectId,
                                             @RequestBody @Valid ChangeStatusDTO changeStatusDTO) {
        projectService.changeProjectStatus(
                ProjectChangeStatusParamsBO.builder()
                        .projectId(projectId)
                        .orgId(sessionContext.orgId())
                        .status(changeStatusDTO.getStatus())
                        .build()
        );
        return Result.success(null);
    }
}
