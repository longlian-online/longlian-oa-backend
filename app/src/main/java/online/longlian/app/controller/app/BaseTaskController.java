package online.longlian.app.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.annotation.ResponseMessage;
import online.longlian.app.common.annotation.UserSession;
import online.longlian.app.common.enumeration.OrganizationDeclaration;
import online.longlian.app.common.resolver.SessionContext;
import online.longlian.app.pojo.vo.app.BaseTaskOptionVO;
import online.longlian.app.service.orgadmin.BaseTaskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "原子任务选项", description = "用户端选择可添加到工作流的原子任务")
@RequestMapping("/app/task/base")
@RestController("appBaseTaskController")
@RequiredArgsConstructor
public class BaseTaskController {

    private final BaseTaskService baseTaskService;

    @Operation(
            summary = "查询可添加的原子任务",
            description = "当前组织未删除且启用的原子任务，供编辑项目工作流和个人模板使用。不含提交字段，不要求组织管理员。"
    )
    @GetMapping("/options")
    @ResponseMessage("查询成功")
    public List<BaseTaskOptionVO> listOptions(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext) {
        return baseTaskService.listEnabledOptions(sessionContext.orgId()).stream()
                .map(task -> new BaseTaskOptionVO(
                        task.id(), task.name(), task.icon(), task.description(), task.status()))
                .toList();
    }
}
