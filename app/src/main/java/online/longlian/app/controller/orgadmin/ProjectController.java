package online.longlian.app.controller.orgadmin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.dto.common.ChangeStatusDTO;
import online.longlian.app.pojo.dto.orgadmin.ProjectAdminListDTO;
import online.longlian.app.pojo.vo.common.PageResultVO;
import online.longlian.app.pojo.vo.orgadmin.ProjectAdminInfoVO;
import online.longlian.app.service.user.ProjectService;
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

    @Operation(summary = "管理端分页查询企划列表", description = "支持标题模糊搜索、类型精确筛选、创建时间区间，默认创建时间倒序")
    @PostMapping("")
    public Result<PageResultVO<ProjectAdminInfoVO>> getAdminProjectList(
            @RequestBody @Valid ProjectAdminListDTO projectAdminListDTO) {
        // TODO
        // return projectService.getAdminProjectList(projectAdminListDTO);
        return Result.success("查询成功", null);
    }
    @Operation(summary = "启用/禁用企划", description = "禁用后用户端不展示该企划。status: ENABLED-启用，DISABLED-禁用")
    @PatchMapping("/{projectId}/status")
    public Result<Void> changeProjectStatus(@PathVariable Long projectId, @RequestBody @Valid ChangeStatusDTO changeStatusDTO) {
        // TODO
        // projectService.changeProjectStatus(changeStatusDTO.getStatus());
        return Result.success(null);
    }
}
