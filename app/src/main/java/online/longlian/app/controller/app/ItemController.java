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
import online.longlian.app.pojo.bo.app.ItemCreateParamsBO;
import online.longlian.app.pojo.bo.app.ItemListParamsBO;
import online.longlian.app.pojo.bo.app.ItemOperationParamsBO;
import online.longlian.app.pojo.dto.app.ProjectItemCreateDTO;
import online.longlian.app.pojo.dto.app.ProjectItemListDTO;
import online.longlian.app.pojo.dto.app.DeletionReasonDTO;
import online.longlian.app.service.app.DeletionRequestService;
import online.longlian.app.pojo.vo.app.ProjectItemListVO;
import online.longlian.app.pojo.vo.common.PageResultVO;
import online.longlian.app.service.app.ItemService;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "项目相关接口", description = "项目列表、创建和删除")
@RequestMapping("/app/projects/{projectId}/items")
@RestController
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;
    private final DeletionRequestService deletionRequestService;

    @Operation(
        summary = "分页查询项目列表",
        description = "支持标题模糊搜索、完成情况筛选与时间排序。进行中包含没有任务或仍有未完成任务的项目；已完成指至少有一个任务且全部任务已完成。默认展示进行中并按更新时间倒序"
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
                        .sortByTime(projectItemListDTO.getSortByTime())
                        .orderDir(projectItemListDTO.getOrderDir())
                        .page(new PageParamsBO(projectItemListDTO.getPageNum(), projectItemListDTO.getPageSize()))
                        .build());

        return new PageResultVO<>(resultBO.getList(), resultBO.getTotal());
    }

    @Operation(summary = "创建项目", description = "创建项目并关联流程模板，自动生成任务流和所有任务实例")
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
                        .build());
    }

    @Operation(summary = "删除项目")
    @Parameter(name = "projectId", description = "企划ID")
    @Parameter(name = "itemId", description = "项目ID")
    @DeleteMapping("/{itemId}")
    @ResponseMessage("删除成功")
    public void deleteProjectItem(@UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
                                   @PathVariable Long projectId,
                                   @PathVariable Long itemId) {
        itemService.deleteProjectItem(
                ItemOperationParamsBO.builder()
                        .projectId(projectId)
                        .itemId(itemId)
                        .orgId(sessionContext.orgId())
                        .operatorId(sessionContext.userId())
                        .build());
    }

    @Operation(summary = "提交项目删除申请", description = "项目存在已提交任务时，由企划负责人填写原因后提交，组织管理员审核通过后软删除")
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
