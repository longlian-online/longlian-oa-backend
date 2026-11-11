package online.longlian.app.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.annotation.ResponseMessage;
import online.longlian.app.common.annotation.UserSession;
import online.longlian.app.common.enumeration.OrganizationDeclaration;
import online.longlian.app.common.resolver.SessionContext;
import online.longlian.app.pojo.bo.common.PageParamsBO;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.bo.app.ItemCompleteParamsBO;
import online.longlian.app.pojo.bo.app.ItemCreateParamsBO;
import online.longlian.app.pojo.bo.app.ItemDeleteParamsBO;
import online.longlian.app.pojo.bo.app.ItemListParamsBO;
import online.longlian.app.pojo.bo.app.ItemOrderQueryParamsBO;
import online.longlian.app.pojo.bo.app.ItemReorderParamsBO;
import online.longlian.app.pojo.dto.app.ProjectItemReorderDTO;
import online.longlian.app.pojo.vo.app.ProjectItemOrderEntryVO;
import online.longlian.app.pojo.bo.app.ItemNodeCreateParamsBO;
import online.longlian.app.pojo.dto.app.ProjectItemCreateDTO;
import online.longlian.app.pojo.dto.app.ProjectItemListDTO;
import online.longlian.app.pojo.dto.app.DeletionReasonDTO;
import online.longlian.app.service.app.DeletionRequestService;
import online.longlian.app.pojo.vo.app.ProjectItemListVO;
import online.longlian.app.pojo.vo.common.PageResultVO;
import online.longlian.app.service.app.ItemService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "项目相关接口", description = "项目列表、创建、排序和删除申请")
@RequestMapping("/app/projects/{projectId}/items")
@RestController
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;
    private final DeletionRequestService deletionRequestService;

    @Operation(
        summary = "分页查询项目列表",
        description = "支持标题模糊搜索、完成情况筛选与自定义或时间排序。默认展示进行中并按负责人维护的自定义顺序展示。进行中和已完成都按项目保存的状态筛选，不按任务是否完成计算。"
    )
    @Parameter(name = "projectId", description = "企划ID")
    @GetMapping("")
    @ResponseMessage("查询成功")
    public PageResultVO<ProjectItemListVO> listProjectItems(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
            @PathVariable Long projectId,
            @ModelAttribute @Valid ProjectItemListDTO projectItemListDTO) {
        PageResultBO<ProjectItemListVO> resultBO = itemService.listProjectItems(
                ItemListParamsBO.builder()
                        .projectId(projectId)
                        .orgId(sessionContext.orgId())
                        .keyword(projectItemListDTO.getKeyword())
                        .status(projectItemListDTO.getStatus())
                        .sortBy(projectItemListDTO.getSortBy())
                        .page(new PageParamsBO(projectItemListDTO.getPageNum(), projectItemListDTO.getPageSize()))
                        .build());

        return new PageResultVO<>(resultBO.getList(), resultBO.getTotal());
    }

    @Operation(summary = "创建项目", description = "创建项目并按请求中的步骤和执行人生成流程，不按模板补齐")
    @Parameter(name = "projectId", description = "企划ID")
    @PostMapping("")
    @ResponseMessage("创建成功")
    public void createProjectItem(@UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
                                   @PathVariable Long projectId,
                                   @RequestBody @Valid ProjectItemCreateDTO projectItemCreateDTO) {
        itemService.createProjectItem(
                ItemCreateParamsBO.builder()
                        .projectId(projectId)
                        .orgId(sessionContext.orgId())
                        .title(projectItemCreateDTO.getTitle())
                        .taskTemplateId(projectItemCreateDTO.getTaskTemplateId())
                        .creatorId(sessionContext.userId())
                        .nodes(projectItemCreateDTO.getNodes() == null ? null : projectItemCreateDTO.getNodes().stream()
                                .map(node -> ItemNodeCreateParamsBO.builder()
                                        .baseTaskId(node.getBaseTaskId())
                                        .sort(node.getSort())
                                        .parallelSort(node.getParallelSort())
                                        .name(node.getName())
                                        .assigneeId(node.getAssigneeId())
                                        .build())
                                .toList())
                        .build());
    }

    @Operation(summary = "获取全部项目排序标题", description = "仅企划负责人可读，不分页且不受完成情况筛选影响")
    @GetMapping("/order")
    @ResponseMessage("查询成功")
    public List<ProjectItemOrderEntryVO> getProjectItemOrder(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
            @PathVariable Long projectId) {
        return itemService.getProjectItemOrder(ItemOrderQueryParamsBO.builder()
                        .projectId(projectId)
                        .orgId(sessionContext.orgId())
                        .operatorId(sessionContext.userId())
                        .build()).stream()
                .map(item -> new ProjectItemOrderEntryVO(item.getId(), item.getTitle()))
                .toList();
    }

    @Operation(summary = "保存全部项目顺序", description = "仅企划负责人可操作；原顺序过期时拒绝覆盖")
    @PutMapping("/order")
    @ResponseMessage("顺序已保存")
    public void reorderProjectItems(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
            @PathVariable Long projectId,
            @RequestBody @Valid ProjectItemReorderDTO dto) {
        itemService.reorderProjectItems(ItemReorderParamsBO.builder()
                .projectId(projectId)
                .orgId(sessionContext.orgId())
                .operatorId(sessionContext.userId())
                .expectedItemIds(dto.getExpectedItemIds())
                .itemIds(dto.getItemIds())
                .build());
    }

    @Operation(
        summary = "完成项目",
        description = "企划负责人、组织所有者或组织管理员将项目标为已完成，并记录完成时间。必须已有任务且全部任务已完成。已完成时再次调用保持已完成和原完成时间。"
    )
    @Parameter(name = "projectId", description = "企划ID")
    @Parameter(name = "itemId", description = "项目ID")
    @PostMapping("/{itemId}/complete")
    @ResponseMessage("项目已完成")
    public void completeProjectItem(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
            @PathVariable Long projectId,
            @PathVariable Long itemId) {
        itemService.completeProjectItem(ItemCompleteParamsBO.builder()
                .projectId(projectId)
                .itemId(itemId)
                .orgId(sessionContext.orgId())
                .userId(sessionContext.userId())
                .build());
    }

    @Operation(summary = "删除项目", description = "仅企划负责人可删除从未提交过的项目。存在过提交记录（含已打回、已重置）时必须提交删除申请")
    @Parameter(name = "projectId", description = "企划ID")
    @Parameter(name = "itemId", description = "项目ID")
    @DeleteMapping("/{itemId}")
    @ResponseMessage("删除成功")
    public void deleteProjectItem(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
            @PathVariable Long projectId,
            @PathVariable Long itemId) {
        itemService.deleteProjectItem(ItemDeleteParamsBO.builder()
                .projectId(projectId)
                .itemId(itemId)
                .orgId(sessionContext.orgId())
                .userId(sessionContext.userId())
                .build());
    }

    @Operation(summary = "提交项目删除申请", description = "项目存在过提交记录（含已打回、已重置）时，由企划负责人填写原因后提交，组织管理员审核通过后软删除")
    @Parameter(name = "projectId", description = "企划ID")
    @Parameter(name = "itemId", description = "项目ID")
    @PostMapping("/{itemId}/deletion-requests")
    @ResponseMessage("已提交删除申请")
    public void createItemDeletionRequest(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
            @PathVariable Long projectId,
            @PathVariable Long itemId,
            @RequestBody @Valid DeletionReasonDTO reasonDTO) {
        deletionRequestService.createItemRequest(
                projectId, itemId, sessionContext.userId(), sessionContext.orgId(), reasonDTO.getReason());
    }

}
