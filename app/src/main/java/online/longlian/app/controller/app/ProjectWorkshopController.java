package online.longlian.app.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.dto.app.WorkshopListDTO;
import online.longlian.app.pojo.dto.app.WorkshopTaskTemplateCreateDTO;
import online.longlian.app.pojo.dto.app.WorkshopTaskTemplateDTO;
import online.longlian.app.pojo.vo.app.WorkshopProjectInfoVO;
import online.longlian.app.pojo.vo.app.WorkshopTaskTemplateVO;
import online.longlian.app.pojo.vo.common.PageResultVO;
import online.longlian.app.service.user.ProjectWorkshopService;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "工坊接口", description = "用户个人工坊：企划列表、任务流模板列表与创建")
@RequestMapping("/app/workshop")
@RestController
@RequiredArgsConstructor
public class ProjectWorkshopController {

    private final ProjectWorkshopService projectWorkshopService;

    @Operation(
        summary = "分页查询工坊企划列表",
        description = "查询当前用户工坊中的企划列表，支持标题模糊搜索、类型筛选、仅看我创建，默认按添加时间倒序。"
    )
    @PostMapping("/list")
    public Result<PageResultVO<WorkshopProjectInfoVO>> getMyWorkshopList(
            @RequestBody @Valid WorkshopListDTO workshopListDTO) {
        // TODO
        // return projectWorkshopService.getMyWorkshopList(workshopListDTO);
        return Result.success("查询成功", null);
    }

    @Operation(
            summary = "分页查询工坊任务流模板列表",
            description = "查询当前用户工坊中的任务流模板（组织通用模板+我的个人模板），支持模板名称模糊搜索、仅看我创建，默认按创建时间倒序。"
    )
    @PostMapping("/task-template/list")
    public Result<PageResultVO<WorkshopTaskTemplateVO>> getWorkshopTaskTemplateList(
            @RequestBody @Valid WorkshopTaskTemplateDTO workshopTaskTemplateDTO) {
        // TODO
        // return projectWorkshopService.getWorkshopTaskTemplateList(workshopTaskTemplateDTO);
        return Result.success("查询成功", null);
    }

    @Operation(
            summary = "创建工坊个人任务流模板",
            description = "用户在可视化编辑器中搭建节点结构后保存为个人任务流模板，scope 固定为 PERSONAL。"
    )
    @PostMapping("/task-template")
    public Result<Void> createWorkshopTaskTemplate(
            @RequestBody @Valid WorkshopTaskTemplateCreateDTO workshopTaskTemplateCreateDTO) {
        // TODO
        // return projectWorkshopService.createWorkshopTaskTemplate(workshopTaskTemplateCreateDTO);
        return Result.success("创建成功", null);
    }

    @Operation(
            summary = "更新工坊个人任务流模板",
            description = "仅模板创建者可更新自己的个人模板（isMine=true 时前端展示更新按钮）。组织通用模板不可在此更新。"
    )
    @Parameter(name = "templateId", description = "任务流模板ID")
    @PutMapping("/task-template/{templateId}")
    public Result<Void> updateWorkshopTaskTemplate(
            @PathVariable Long templateId,
            @RequestBody @Valid WorkshopTaskTemplateCreateDTO workshopTaskTemplateCreateDTO) {
        // TODO
        // return projectWorkshopService.updateWorkshopTaskTemplate(templateId, workshopTaskTemplateCreateDTO);
        return Result.success("更新成功", null);
    }
}
