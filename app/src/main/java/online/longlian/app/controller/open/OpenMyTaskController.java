package online.longlian.app.controller.open;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.security.OpenApiPrincipal;
import online.longlian.app.common.util.OpenQuerySupport;
import online.longlian.app.pojo.bo.open.OpenMyTaskBO;
import online.longlian.app.pojo.bo.open.OpenMyTaskListParamsBO;
import online.longlian.app.pojo.bo.open.OpenPageResultBO;
import online.longlian.app.pojo.dto.open.OpenMyTaskListDTO;
import online.longlian.app.pojo.vo.open.OpenMyTaskVO;
import online.longlian.app.pojo.vo.open.OpenPageVO;
import online.longlian.app.service.open.OpenMyTaskService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 开放面我的待办查询。组织上下文全部来自密钥解出的 principal，不接受请求指定。
 */
@RequiredArgsConstructor
@Tag(name = "开放接口-我的待办", description = "密钥用户的已接取任务")
@SecurityRequirement(name = "XApiKey")
@RequestMapping("/open/v1/my-tasks")
@RestController
public class OpenMyTaskController {

    private final OpenMyTaskService openMyTaskService;

    @Operation(summary = "我的待办",
            description = "密钥用户在密钥组织内已接取（CLAIMED）的任务，按接取时间升序；"
                    + "bucket=SUBMITTABLE 可提交，WAITING 等前置，不传返回全部")
    @GetMapping
    public OpenPageVO<OpenMyTaskVO> listMyTasks(
            @AuthenticationPrincipal OpenApiPrincipal principal,
            @Valid OpenMyTaskListDTO dto) {
        OpenPageResultBO<OpenMyTaskBO> page = openMyTaskService.listMyTasks(new OpenMyTaskListParamsBO(
                principal.userId(), principal.orgId(), dto.getBucket(),
                OpenQuerySupport.parseCursor(dto.getCursor()),
                OpenQuerySupport.resolveLimit(dto.getLimit())));
        List<OpenMyTaskVO> items = page.list().stream().map(bo -> {
            OpenMyTaskVO vo = new OpenMyTaskVO();
            vo.setTaskInstanceId(bo.taskInstanceId());
            vo.setNodeName(bo.nodeName());
            vo.setItemId(bo.itemId());
            vo.setItemTitle(bo.itemTitle());
            vo.setProjectId(bo.projectId());
            vo.setProjectName(bo.projectName());
            vo.setTaskStatus(bo.taskStatus());
            vo.setRevision(bo.revision());
            vo.setClaimedAt(bo.claimedAt());
            vo.setBucket(bo.bucket());
            return vo;
        }).toList();
        return new OpenPageVO<>(items, page.nextCursor(), page.hasMore());
    }
}
