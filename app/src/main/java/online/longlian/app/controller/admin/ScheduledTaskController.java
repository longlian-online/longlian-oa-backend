package online.longlian.app.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.bo.ScheduledTaskDefinition;
import online.longlian.app.pojo.dto.admin.ScheduleTriggerDTO;
import online.longlian.app.pojo.vo.admin.ScheduledTaskVO;
import online.longlian.app.scheduled.ScheduledTask;
import online.longlian.app.scheduled.ScheduledTaskEngine;
import online.longlian.app.service.scheduled.ScheduledTaskLogService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "定时任务管理", description = "查看、手动触发定时任务")
@RequestMapping("/admin/scheduled-tasks")
@RestController
@RequiredArgsConstructor
public class ScheduledTaskController {

    private final ScheduledTaskEngine engine;
    private final ScheduledTaskLogService taskLogService;

    @Operation(summary = "列出所有已注册的定时任务")
    @GetMapping("/")
    public Result<List<ScheduledTaskVO>> listTasks() {
        Map<String, ScheduledTask> tasks = engine.getRegisteredTasks();
        List<ScheduledTaskVO> vos = new ArrayList<>();
        for (Map.Entry<String, ScheduledTask> entry : tasks.entrySet()) {
            String name = entry.getKey();
            ScheduledTaskDefinition def = entry.getValue().getDefinition();

            vos.add(ScheduledTaskVO.builder()
                    .taskName(name)
                    .description(def.getDescription())
                    .cronExpression(def.getCronExpression())
                    .enabled(def.isEnabled())
                    .build());
        }
        return Result.success("ok", vos);
    }

    @Operation(summary = "手动触发定时任务",
            description = "传入 executeTime 可模拟任意时间点触发，不传则为当前时间。")
    @PostMapping("/{taskName}/trigger")
    public Result<Void> trigger(@PathVariable String taskName, @RequestBody ScheduleTriggerDTO dto) {
        engine.trigger(taskName, dto.getExecuteTime());
        return Result.success("触发成功");
    }
}