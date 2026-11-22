package online.longlian.app.controller.open;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.security.OpenApiPrincipal;
import online.longlian.app.common.util.OpenIdempotencySupport;
import online.longlian.app.pojo.bo.open.IdempotencyCheckParamsBO;
import online.longlian.app.pojo.bo.open.OpenTaskAbandonParamsBO;
import online.longlian.app.pojo.bo.open.OpenTaskClaimParamsBO;
import online.longlian.app.pojo.bo.open.OpenTaskClaimResultBO;
import online.longlian.app.pojo.bo.open.OpenTaskSubmitParamsBO;
import online.longlian.app.pojo.dto.open.OpenTaskAbandonDTO;
import online.longlian.app.pojo.dto.open.OpenTaskSubmitDTO;
import online.longlian.app.pojo.vo.open.OpenTaskClaimVO;
import online.longlian.app.pojo.vo.open.OpenTaskSubmitVO;
import online.longlian.app.service.open.OpenTaskCommandService;
import online.longlian.app.service.open.OpenWriteCommandService;
import online.longlian.common.enumeration.IdempotencySource;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 开放面任务流转（接取/放弃/提交）。用户与组织上下文全部来自密钥解出的 principal，不接受请求指定。
 * 返回类型为 Object：幂等回放时 data 是快照反序列化结果，不能再按 VO 强转。
 */
@RequiredArgsConstructor
@Tag(name = "开放接口-任务流转", description = "密钥认证的任务接取、放弃与提交")
@SecurityRequirement(name = "XApiKey")
@RequestMapping("/open/v1/tasks/{instanceId}")
@RestController
public class OpenTaskCommandController {

    private final OpenWriteCommandService openWriteCommandService;
    private final OpenTaskCommandService openTaskCommandService;

    @Operation(summary = "接取任务",
            description = "状态机、revision 乐观锁与自动入组规则同站内；响应只含自动入组标记，"
                    + "同幂等键重发回放首次结果，任务归属不变")
    @PostMapping("/claim")
    public Object claimTask(HttpServletRequest request,
                            @AuthenticationPrincipal OpenApiPrincipal principal,
                            @PathVariable Long instanceId) {
        IdempotencyCheckParamsBO checkParams =
                OpenIdempotencySupport.buildParams(request, IdempotencySource.OPEN_API, principal.keyId());
        return openWriteCommandService.execute(checkParams, () -> {
            OpenTaskClaimResultBO bo = openTaskCommandService.claimTask(new OpenTaskClaimParamsBO(
                    instanceId, principal.userId(), principal.orgId()));
            OpenTaskClaimVO vo = new OpenTaskClaimVO();
            vo.setAutoJoinedProject(bo.autoJoinedProject());
            return vo;
        });
    }

    @Operation(summary = "放弃任务",
            description = "须匹配当前 revision（expectedRevision）；接取人放弃规则同站内，"
                    + "成功无业务数据，同幂等键重发不重复执行")
    @PostMapping("/abandon")
    public Object abandonTask(HttpServletRequest request,
                              @AuthenticationPrincipal OpenApiPrincipal principal,
                              @PathVariable Long instanceId,
                              @Valid @RequestBody OpenTaskAbandonDTO dto) {
        IdempotencyCheckParamsBO checkParams =
                OpenIdempotencySupport.buildParams(request, IdempotencySource.OPEN_API, principal.keyId());
        return openWriteCommandService.execute(checkParams, () -> {
            openTaskCommandService.abandonTask(new OpenTaskAbandonParamsBO(
                    instanceId, principal.userId(), principal.orgId(), dto.getExpectedRevision()));
            return null;
        });
    }

    @Operation(summary = "提交任务",
            description = "values 按节点 metaSchema 由服务端校验，文件字段传 {\"fileId\":...}；"
                    + "提交规则同站内，响应只含本次提交版本号，同幂等键重发回放首次结果、提交版本数不涨")
    @PostMapping("/submit")
    public Object submitTask(HttpServletRequest request,
                             @AuthenticationPrincipal OpenApiPrincipal principal,
                             @PathVariable Long instanceId,
                             @Valid @RequestBody OpenTaskSubmitDTO dto) {
        IdempotencyCheckParamsBO checkParams =
                OpenIdempotencySupport.buildParams(request, IdempotencySource.OPEN_API, principal.keyId());
        return openWriteCommandService.execute(checkParams, () -> {
            OpenTaskSubmitParamsBO params = new OpenTaskSubmitParamsBO(
                    instanceId, principal.userId(), principal.orgId(), dto.getValues());
            OpenTaskSubmitVO vo = new OpenTaskSubmitVO();
            vo.setVersionNo(openTaskCommandService.submitTask(params).versionNo());
            return vo;
        });
    }
}
