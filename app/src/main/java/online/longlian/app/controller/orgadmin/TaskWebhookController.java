package online.longlian.app.controller.orgadmin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.annotation.ResponseMessage;
import online.longlian.app.common.annotation.UserSession;
import online.longlian.app.common.enumeration.OrganizationDeclaration;
import online.longlian.app.common.resolver.SessionContext;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.bo.orgadmin.TaskWebhookFailureResultBO;
import online.longlian.app.pojo.dto.orgadmin.TaskWebhookFailureListDTO;
import online.longlian.app.pojo.vo.common.PageResultVO;
import online.longlian.app.pojo.vo.orgadmin.TaskWebhookFailureVO;
import online.longlian.app.service.orgadmin.TaskWebhookFailureService;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "任务 webhook", description = "查看和重放失败的任务 webhook")
@RequestMapping("/orgadmin/task/webhook")
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ORG_ADMIN')")
public class TaskWebhookController {
    private final TaskWebhookFailureService taskWebhookFailureService;

    @Operation(summary = "分页查询失败的任务 webhook", description = "只返回当前组织的失败记录，不含地址和密钥")
    @PostMapping("/failures")
    @ResponseMessage("查询成功")
    public PageResultVO<TaskWebhookFailureVO> listFailures(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
            @RequestBody @Valid TaskWebhookFailureListDTO dto) {
        PageResultBO<TaskWebhookFailureResultBO> result = taskWebhookFailureService.listFailures(
                sessionContext.orgId(), dto.getPageNum(), dto.getPageSize());
        List<TaskWebhookFailureVO> list = result.getList().stream().map(bo -> {
            TaskWebhookFailureVO vo = new TaskWebhookFailureVO();
            BeanUtils.copyProperties(bo, vo);
            return vo;
        }).toList();
        return new PageResultVO<>(list, result.getTotal());
    }

    @Operation(summary = "重放失败的任务 webhook", description = "把失败记录放回待投递。成功或属于其他组织的记录不会被重放")
    @PostMapping("/{messageId}/replay")
    @ResponseMessage("已重新投递")
    public void replay(@UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
                       @PathVariable Long messageId) {
        taskWebhookFailureService.replay(sessionContext.orgId(), messageId);
    }
}
