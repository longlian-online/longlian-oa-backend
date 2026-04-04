package online.longlian.app.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.Result;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.util.ThreadLocalUtil;
import online.longlian.app.pojo.dto.ProjectItemCreateDTO;
import online.longlian.app.pojo.dto.ProjectItemListDTO;
import online.longlian.app.pojo.entity.Project;
import online.longlian.app.pojo.vo.PageResultVO;
import online.longlian.app.pojo.vo.ProjectItemListVO;
import online.longlian.app.pojo.vo.TaskTemplateOptionVO;
import online.longlian.app.service.user.ProjectService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "项目相关接口", description = "项目列表、创建编辑、发布")
@RequestMapping("/app/project/item")
@RestController
@RequiredArgsConstructor
public class ItemController {

    private final ProjectService projectService;
    // private final ItemService itemService;

    @Operation(
        summary = "分页查询项目列表",
        description = "支持标题模糊搜索、状态筛选（进行中/已完成/已公布）与时间排序；默认展示进行中并按更新时间倒序"
    )
    @Parameter(name = "projectId", description = "企划ID")
    @PostMapping("/{projectId}/list")
    public Result<PageResultVO<ProjectItemListVO>> listProjectItems(
            @PathVariable Long projectId,
            @RequestBody @Valid ProjectItemListDTO projectItemListDTO) {
        // TODO
        // return itemService.listProjectItems(projectId, projectItemListDTO);
        return Result.success("查询成功", null);
    }

    @Operation(summary = "创建项目", description = "创建项目并关联流程模板")
    @Parameter(name = "projectId", description = "企划ID")
    @PostMapping("/{projectId}")
    public Result<Void> createProjectItem(
            @PathVariable Long projectId,
            @RequestBody @Valid ProjectItemCreateDTO projectItemCreateDTO) {
        checkProjectCreator(projectId);
        // TODO
        // return itemService.createProjectItem(projectId, projectItemCreateDTO);
        return Result.success("创建成功");
    }

    @Operation(summary = "删除项目")
    @Parameter(name = "projectId", description = "企划ID")
    @Parameter(name = "itemId", description = "项目ID")
    @DeleteMapping("/{projectId}/{itemId}")
    public Result<Void> deleteProjectItem(
            @PathVariable Long projectId,
            @PathVariable Long itemId) {
        checkProjectCreator(projectId);
        // TODO
        // return itemService.deleteProjectItem(projectId, itemId);
        return Result.success("删除成功");
    }

    @Operation(summary = "公布项目")
    @Parameter(name = "projectId", description = "企划ID")
    @Parameter(name = "itemId", description = "项目ID")
    @PatchMapping("/{projectId}/{itemId}/publish")
    public Result<Void> publishProjectItem(
            @PathVariable Long projectId,
            @PathVariable Long itemId) {
        checkProjectCreator(projectId);
        // TODO
        // return itemService.publishProjectItem(projectId, itemId);
        return Result.success("公布成功");
    }

    @Operation(summary = "获取用户可选流程模板", description = "创建项目弹窗使用的流程模板下拉选项")
    @GetMapping("/template-options")
    public Result<List<TaskTemplateOptionVO>> listTaskTemplateOptions() {
        // TODO
        // return itemService.listTaskTemplateOptions();
        return Result.success("查询成功", null);
    }

    private void checkProjectCreator(Long projectId) {
        Project project = projectService.getById(projectId);
        if (project == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT);
        }
        Long currentUserId = ThreadLocalUtil.getUserBO().getId();
        if (!project.getCreatorId().equals(currentUserId)) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION);
        }
    }
}
