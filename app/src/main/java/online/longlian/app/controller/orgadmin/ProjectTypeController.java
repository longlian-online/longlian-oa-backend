package online.longlian.app.controller.orgadmin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.enumeration.Status;
import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.dto.ChangeStatusDTO;
import online.longlian.app.pojo.dto.ProjectTypeCreateDTO;
import online.longlian.app.pojo.dto.ProjectTypeListDTO;
import online.longlian.app.pojo.vo.PageResultVO;
import online.longlian.app.pojo.vo.ProjectTypeAdminVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "企划类型管理", description = "企划类型的增删改查与状态管理，仅管理员可操作")
@RequestMapping("/orgadmin/types")
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ORG_ADMIN')")
public class ProjectTypeController {

    // private final ProjectTypeService projectTypeService;

    @Operation(summary = "分页查询企划类型列表", description = "支持名称模糊搜索，默认按创建时间倒序")
    @GetMapping("")
    public Result<PageResultVO<ProjectTypeAdminVO>> listProjectTypes(
            @ModelAttribute @Valid ProjectTypeListDTO projectTypeListDTO) {
        // TODO
        // return projectTypeService.listProjectTypes(projectTypeListDTO);
        return Result.success("查询成功", null);
    }

    @Operation(summary = "创建企划类型")
    @PostMapping("")
    public Result<Void> createProjectType(
            @RequestBody @Valid ProjectTypeCreateDTO projectTypeCreateDTO) {
        // TODO
        // return projectTypeService.createProjectType(projectTypeCreateDTO);
        return Result.success("创建成功");
    }

    @Operation(summary = "启用/禁用企划类型", description = "禁用后用户端不展示该类型，但已有数据保留。status: ENABLED-启用，DISABLED-禁用")
    @PatchMapping("/{typeId}/status")
    public Result<Void> changeProjectTypeStatus(@PathVariable Long typeId, @RequestBody @Valid ChangeStatusDTO changeStatusDTO) {
        // TODO
        // projectTypeService.changeProjectTypeStatus(changeStatusDTO.getStatus());
        return Result.success(null);
    }
}
