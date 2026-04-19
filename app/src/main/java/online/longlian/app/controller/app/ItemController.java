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
import online.longlian.app.pojo.dto.app.ProjectItemCreateDTO;
import online.longlian.app.pojo.dto.app.ProjectItemListDTO;
import online.longlian.app.pojo.entity.Project;
import online.longlian.app.pojo.vo.app.ProjectItemListVO;
import online.longlian.app.pojo.vo.app.TaskTemplateOptionVO;
import online.longlian.app.pojo.vo.common.PageResultVO;
import online.longlian.app.service.user.ProjectService;
import online.longlian.app.service.user.SessionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "项目相关接口", description = "项目列表、创建编辑、发布")
@RequestMapping("/app/projects/{projectId}/items")
@RestController
@RequiredArgsConstructor
public class ItemController {

    private final ProjectService projectService;
    private final SessionService sessionService;
    // private final ItemService itemService;

    @Operation(
        summary = "分页查询项目列表",
        description = "支持标题模糊搜索、状态筛选（进行中/已完成/已公布）与时间排序；默认展示进行中并按更新时间倒序"
    )
    @Parameter(name = "projectId", description = "企划ID")
    @GetMapping("")
    public Result<PageResultVO<ProjectItemListVO>> listProjectItems(
            @PathVariable Long projectId,
            @ModelAttribute @Valid ProjectItemListDTO projectItemListDTO) {
        // TODO
        // return itemService.listProjectItems(projectId, projectItemListDTO);
        return Result.success("查询成功", null);
    }

    @Operation(summary = "创建项目", description = "创建项目并关联流程模板")
    @Parameter(name = "projectId", description = "企划ID")
    @PostMapping("")
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
    @DeleteMapping("/{itemId}")
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
    @PatchMapping("/{itemId}/publish")
    public Result<Void> publishProjectItem(
            @PathVariable Long projectId,
            @PathVariable Long itemId) {
        checkProjectCreator(projectId);
        // TODO
        // return itemService.publishProjectItem(projectId, itemId);
        return Result.success("公布成功");
    }

    // TODO 应放在 “流程模板”相关控制类中
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
        Long currentUserId = sessionService.getCurrentUserId();
        if (!project.getCreatorId().equals(currentUserId)) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION);
        }
    }
}
