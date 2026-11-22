package online.longlian.app.controller.open;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.security.OpenApiPrincipal;
import online.longlian.app.common.util.OpenQuerySupport;
import online.longlian.app.pojo.bo.app.taskplaza.PlazaProjectGroupResultBO;
import online.longlian.app.pojo.bo.app.taskplaza.PlazaTaskResultBO;
import online.longlian.app.pojo.bo.open.OpenPageResultBO;
import online.longlian.app.pojo.bo.open.OpenTaskPlazaListParamsBO;
import online.longlian.app.pojo.dto.open.OpenTaskPlazaListDTO;
import online.longlian.app.pojo.vo.open.OpenPageVO;
import online.longlian.app.pojo.vo.open.OpenPlazaProjectGroupVO;
import online.longlian.app.pojo.vo.open.OpenPlazaTaskVO;
import online.longlian.app.pojo.vo.open.OpenTaskTypeOptionVO;
import online.longlian.app.service.orgadmin.basetask.BaseTaskService;
import online.longlian.app.service.open.OpenTaskPlazaService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 开放面任务广场查询。组织上下文全部来自密钥解出的 principal，不接受请求指定。
 */
@RequiredArgsConstructor
@Tag(name = "开放接口-任务广场", description = "密钥认证的缺人任务发现")
@SecurityRequirement(name = "XApiKey")
@RequestMapping("/open/v1/task-plaza")
@RestController
public class OpenTaskPlazaController {

    private final OpenTaskPlazaService openTaskPlazaService;
    private final BaseTaskService baseTaskService;

    @Operation(summary = "任务广场（缺人任务）",
            description = "当前组织可接取的缺人任务按企划分组，企划按 (updated_at, id) 键集升序；"
                    + "stageScope=ALL 返回所有阶段，CURRENT（默认）仅返回首个未完成阶段")
    @GetMapping
    public OpenPageVO<OpenPlazaProjectGroupVO> getTaskPlaza(
            @AuthenticationPrincipal OpenApiPrincipal principal,
            @Valid OpenTaskPlazaListDTO dto) {
        OpenPageResultBO<PlazaProjectGroupResultBO> page = openTaskPlazaService.getTaskPlaza(
                new OpenTaskPlazaListParamsBO(principal.orgId(), dto.getBaseTaskId(), dto.getKeyword(),
                        dto.getStageScope(), OpenQuerySupport.parseCursor(dto.getCursor()),
                        OpenQuerySupport.resolveLimit(dto.getLimit())));
        List<OpenPlazaProjectGroupVO> groups = page.list().stream().map(this::toGroupVO).toList();
        return new OpenPageVO<>(groups, page.nextCursor(), page.hasMore());
    }

    @Operation(summary = "任务广场任务类型选项",
            description = "当前组织启用的原子任务及已有流程仍引用的停用原子任务")
    @GetMapping("/task-types")
    public List<OpenTaskTypeOptionVO> listTaskTypes(
            @AuthenticationPrincipal OpenApiPrincipal principal) {
        return baseTaskService.listPlazaTaskTypes(principal.orgId()).stream()
                .map(option -> new OpenTaskTypeOptionVO(option.id(), option.name()))
                .toList();
    }

    private OpenPlazaProjectGroupVO toGroupVO(PlazaProjectGroupResultBO bo) {
        OpenPlazaProjectGroupVO vo = new OpenPlazaProjectGroupVO();
        vo.setProjectId(bo.getProjectId());
        vo.setTitle(bo.getTitle());
        vo.setTypeName(bo.getTypeName());
        vo.setCoverUrl(bo.getCoverUrl());
        vo.setThumbnailUrl(bo.getThumbnailUrl());
        vo.setPendingCount(bo.getPendingCount());
        vo.setItemCount(bo.getItemCount());
        vo.setTasks(bo.getTasks() == null ? List.of()
                : bo.getTasks().stream().map(this::toTaskVO).toList());
        return vo;
    }

    private OpenPlazaTaskVO toTaskVO(PlazaTaskResultBO bo) {
        OpenPlazaTaskVO vo = new OpenPlazaTaskVO();
        vo.setInstanceId(bo.getInstanceId());
        vo.setItemId(bo.getItemId());
        vo.setItemTitle(bo.getItemTitle());
        vo.setNodeName(bo.getNodeName());
        vo.setStageIndex(bo.getStageIndex());
        vo.setStageTotal(bo.getStageTotal());
        vo.setParallelTotal(bo.getParallelTotal());
        vo.setFilledSlots(bo.getFilledSlots());
        vo.setPendingSlots(bo.getPendingSlots());
        vo.setSlotIndex(bo.getSlotIndex());
        vo.setSlotTotal(bo.getSlotTotal());
        vo.setSerialStep(bo.getSerialStep());
        vo.setCreatedAt(bo.getCreatedAt());
        vo.setIsCurrentStage(bo.getIsCurrentStage());
        return vo;
    }
}
