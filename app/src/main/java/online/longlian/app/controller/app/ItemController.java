package online.longlian.app.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.annotation.ResponseMessage;
import online.longlian.app.common.annotation.UserSession;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.resolver.SessionContext;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.ProjectMapper;
import online.longlian.app.pojo.bo.common.PageParamsBO;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.bo.app.ItemCreateParamsBO;
import online.longlian.app.pojo.bo.app.ItemListParamsBO;
import online.longlian.app.pojo.bo.app.ItemOperationParamsBO;
import online.longlian.app.pojo.dto.app.ProjectItemCreateDTO;
import online.longlian.app.pojo.dto.app.ProjectItemListDTO;
import online.longlian.app.pojo.entity.Project;
import online.longlian.app.pojo.vo.app.ProjectItemListVO;
import online.longlian.app.pojo.vo.common.PageResultVO;
import online.longlian.app.service.app.ItemService;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "项目相关接口", description = "项目列表、创建编辑、发布")
@RequestMapping("/app/projects/{projectId}/items")
@RestController
@RequiredArgsConstructor
public class ItemController {

    private final ProjectMapper projectMapper;
    private final ItemService itemService;

    @Operation(
        summary = "分页查询项目列表",
        description = "支持标题模糊搜索、状态筛选（进行中/已完成/已公布）与时间排序；默认展示进行中并按更新时间倒序"
    )
    @Parameter(name = "projectId", description = "企划ID")
    @GetMapping("")
    @ResponseMessage("查询成功")
    public PageResultVO<ProjectItemListVO> listProjectItems(
            @UserSession SessionContext sessionContext,
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
    public void createProjectItem(@UserSession SessionContext sessionContext,
                                   @PathVariable Long projectId,
                                   @RequestBody @Valid ProjectItemCreateDTO projectItemCreateDTO) {
        checkProjectCreator(projectId, sessionContext.userId(), sessionContext.orgId());

        itemService.createProjectItem(
                ItemCreateParamsBO.builder()
                        .projectId(projectId)
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
    public void deleteProjectItem(@UserSession SessionContext sessionContext,
                                   @PathVariable Long projectId,
                                   @PathVariable Long itemId) {
        checkProjectCreator(projectId, sessionContext.userId(), sessionContext.orgId());
        itemService.deleteProjectItem(
                ItemOperationParamsBO.builder().projectId(projectId).itemId(itemId).build());
    }

    @Operation(summary = "公布项目")
    @Parameter(name = "projectId", description = "企划ID")
    @Parameter(name = "itemId", description = "项目ID")
    @PatchMapping("/{itemId}/publish")
    @ResponseMessage("公布成功")
    public void publishProjectItem(@UserSession SessionContext sessionContext,
                                    @PathVariable Long projectId,
                                    @PathVariable Long itemId) {
        checkProjectCreator(projectId, sessionContext.userId(), sessionContext.orgId());
        itemService.publishProjectItem(
                ItemOperationParamsBO.builder().projectId(projectId).itemId(itemId).build());
    }

    private void checkProjectCreator(Long projectId, Long userId, Long orgId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null || !project.getOrgId().equals(orgId)) {
            throw new AppException(ResultCode.DATA_NOT_EXIT);
        }
        if (!project.getCreatorId().equals(userId)) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION);
        }
    }
}
