package online.longlian.app.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.bo.app.TaskTemplateOptionsParamsBO;
import online.longlian.app.pojo.vo.app.TaskTemplateOptionVO;
import online.longlian.app.service.app.SessionService;
import online.longlian.app.service.app.TaskTemplateService;
import online.longlian.app.service.common.CurrentOrganizationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "流程模板接口", description = "用户端流程模板查询，提供创建项目时的模板下拉选项")
@RequestMapping("/app/task-template")
@RestController
@RequiredArgsConstructor
public class TaskTemplateController {

    private final TaskTemplateService taskTemplateService;
    private final SessionService sessionService;
    private final CurrentOrganizationService currentOrganizationService;

    @Operation(
            summary = "获取用户可选流程模板",
            description = "创建项目弹窗使用的流程模板下拉选项，查询组织通用模板+我的个人模板"
    )
    @GetMapping("/options")
    public Result<List<TaskTemplateOptionVO>> listTaskTemplateOptions() {
        Long userId = sessionService.getCurrentUserId();
        Long orgId = currentOrganizationService.resolveCurrentOrgId(userId);

        List<TaskTemplateOptionVO> options = taskTemplateService.listOptions(
                TaskTemplateOptionsParamsBO.builder()
                        .orgId(orgId)
                        .userId(userId)
                        .build());
        return Result.success("查询成功", options);
    }
}
