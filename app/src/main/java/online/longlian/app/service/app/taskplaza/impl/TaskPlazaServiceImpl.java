package online.longlian.app.service.app.taskplaza.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.enumeration.PlazaSortBy;
import online.longlian.common.enumeration.Status;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.ProjectTypeMapper;
import online.longlian.app.mapper.TaskInstanceMapper;
import online.longlian.app.pojo.bo.app.item.ItemStageSortResultBO;
import online.longlian.app.pojo.bo.app.taskinstance.ItemStageNodeStatusBO;
import online.longlian.app.pojo.bo.app.taskplaza.PlazaProjectSummaryBO;
import online.longlian.app.pojo.bo.app.taskplaza.PlazaTaskRowBO;
import online.longlian.app.pojo.bo.app.taskplaza.TaskPlazaKeysetParamsBO;
import online.longlian.app.pojo.bo.app.taskplaza.TaskPlazaKeysetResultBO;
import online.longlian.app.pojo.bo.app.taskplaza.TaskPlazaParamsBO;
import online.longlian.app.pojo.bo.app.taskplaza.TaskPlazaSearchParamsBO;
import online.longlian.app.pojo.bo.common.ResourceReadUrlGetResultBO;
import online.longlian.app.pojo.entity.ProjectType;
import online.longlian.app.pojo.bo.app.taskplaza.PlazaProjectGroupResultBO;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.service.app.item.ItemService;
import online.longlian.app.service.app.taskplaza.TaskPlazaService;
import online.longlian.app.service.resource.ImageResizePreset;
import online.longlian.app.service.resource.ResourceService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.function.Function;

/**
 * 任务广场服务实现：一次请求内完成 企划聚合分页 → 组内实例行 → 阶段推导 → 装配，避免逐条目 N+1。
 */
@Service
@RequiredArgsConstructor
public class TaskPlazaServiceImpl implements TaskPlazaService {

    private final TaskInstanceMapper taskInstanceMapper;
    private final ProjectTypeMapper projectTypeMapper;
    private final ResourceService resourceService;
    private final TaskPlazaAssembler taskPlazaAssembler;
    private final ItemService itemService;

    @Override
    public PageResultBO<PlazaProjectGroupResultBO> getTaskPlaza(TaskPlazaParamsBO params) {
        TaskPlazaSearchParamsBO searchParams = TaskPlazaSearchParamsBO.builder()
                .orgId(params.getOrgId())
                .baseTaskId(params.getBaseTaskId())
                .typeId(resolveTypeId(params.getOrgId(), params.getProjectType()))
                .matchPattern(buildMatchPattern(params.getKeyword()))
                .sortBy(params.getSortBy())
                .stageScope(params.getStageScope())
                .build();

        long total = taskInstanceMapper.selectPlazaProjectCount(searchParams);
        if (total == 0) {
            return new PageResultBO<>(Collections.emptyList(), 0L);
        }

        int offset = (params.getPage().getPageNum() - 1) * params.getPage().getPageSize();
        List<PlazaProjectSummaryBO> summaries = taskInstanceMapper.selectPlazaProjectSummaries(
                searchParams, offset, params.getPage().getPageSize());
        if (summaries.isEmpty()) {
            return new PageResultBO<>(Collections.emptyList(), total);
        }

        List<Long> projectIds = summaries.stream().map(PlazaProjectSummaryBO::getProjectId).toList();
        List<PlazaTaskRowBO> rows = taskInstanceMapper.selectPlazaTaskRows(searchParams, projectIds);

        List<Long> itemIds = rows.stream().map(PlazaTaskRowBO::getItemId).distinct().toList();
        Map<Long, List<Integer>> itemSorts = itemIds.isEmpty()
                ? Collections.emptyMap()
                : itemService.listItemStageSorts(itemIds).stream()
                        .collect(Collectors.groupingBy(ItemStageSortResultBO::getItemId,
                                Collectors.collectingAndThen(
                                        Collectors.mapping(ItemStageSortResultBO::getSort,
                                                Collectors.toCollection(TreeSet::new)),
                                        List::copyOf)));
        Map<Long, List<ItemStageNodeStatusBO>> nodesByItem = itemIds.isEmpty()
                ? Map.of()
                : taskInstanceMapper.selectItemStageNodeStatuses(itemIds).stream()
                        .collect(Collectors.groupingBy(ItemStageNodeStatusBO::getItemId));

        List<PlazaProjectGroupResultBO> groups = taskPlazaAssembler.assembleGroups(
                summaries, rows, itemSorts,
                loadImageUrls(summaries, PlazaProjectSummaryBO::getCoverFileId),
                resizeThumbnailUrls(loadImageUrls(summaries, PlazaProjectSummaryBO::getThumbnailFileId)),
                nodesByItem, params.getBaseTaskId());
        return new PageResultBO<>(groups, total);
    }

    /**
     * 开放面任务广场的键集分页：筛选与装配完全复用 getTaskPlaza 的私有件
     * （类型解析、关键词转义、装配器与阶段推导），本方法只把企划窗口从 offset 分页
     * 换成 (updated_at, id) 键集，供开放面顺序翻页。
     */
    @Override
    public TaskPlazaKeysetResultBO getTaskPlazaKeyset(TaskPlazaKeysetParamsBO params) {
        TaskPlazaSearchParamsBO searchParams = TaskPlazaSearchParamsBO.builder()
                .orgId(params.orgId())
                .baseTaskId(params.baseTaskId())
                .matchPattern(buildMatchPattern(params.keyword()))
                .stageScope(params.stageScope())
                .build();
        List<PlazaProjectSummaryBO> summaries = taskInstanceMapper.selectPlazaProjectSummariesKeyset(
                searchParams, params.afterProjectUpdatedAt(), params.afterProjectId(),
                params.limit() + 1);
        boolean hasMore = summaries.size() > params.limit();
        List<PlazaProjectSummaryBO> window = hasMore ? summaries.subList(0, params.limit()) : summaries;
        if (window.isEmpty()) {
            return new TaskPlazaKeysetResultBO(Collections.emptyList(), null, null, false);
        }

        List<Long> projectIds = window.stream().map(PlazaProjectSummaryBO::getProjectId).toList();
        List<PlazaTaskRowBO> rows = taskInstanceMapper.selectPlazaTaskRows(searchParams, projectIds);

        List<Long> itemIds = rows.stream().map(PlazaTaskRowBO::getItemId).distinct().toList();
        Map<Long, List<Integer>> itemSorts = itemIds.isEmpty()
                ? Collections.emptyMap()
                : itemService.listItemStageSorts(itemIds).stream()
                        .collect(Collectors.groupingBy(ItemStageSortResultBO::getItemId,
                                Collectors.collectingAndThen(
                                        Collectors.mapping(ItemStageSortResultBO::getSort,
                                                Collectors.toCollection(TreeSet::new)),
                                        List::copyOf)));
        Map<Long, List<ItemStageNodeStatusBO>> nodesByItem = itemIds.isEmpty()
                ? Map.of()
                : taskInstanceMapper.selectItemStageNodeStatuses(itemIds).stream()
                        .collect(Collectors.groupingBy(ItemStageNodeStatusBO::getItemId));

        List<PlazaProjectGroupResultBO> groups = taskPlazaAssembler.assembleGroups(
                window, rows, itemSorts,
                loadImageUrls(window, PlazaProjectSummaryBO::getCoverFileId),
                resizeThumbnailUrls(loadImageUrls(window, PlazaProjectSummaryBO::getThumbnailFileId)),
                nodesByItem, params.baseTaskId());
        PlazaProjectSummaryBO last = window.get(window.size() - 1);
        return new TaskPlazaKeysetResultBO(groups, last.getProjectId(), last.getUpdatedAt(), hasMore);
    }

    /** 与企划列表一致：按名称精确匹配当前组织下启用中的类型，未命中视为参数错误 */
    private Long resolveTypeId(Long orgId, String projectType) {
        if (!StringUtils.hasText(projectType)) {
            return null;
        }
        ProjectType type = projectTypeMapper.selectOne(
                new LambdaQueryWrapper<ProjectType>()
                        .eq(ProjectType::getOrgId, orgId)
                        .eq(ProjectType::getName, projectType.trim())
                        .eq(ProjectType::getStatus, Status.ENABLED)
                        .last("LIMIT 1"));
        if (type == null) {
            throw new AppException(ResultCode.PARAM_ERROR, "企划类型不存在或已禁用");
        }
        return type.getId();
    }

    private String buildMatchPattern(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return "%" + keyword.trim()
                .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }

    private Map<Long, String> loadImageUrls(List<PlazaProjectSummaryBO> summaries,
                                            Function<PlazaProjectSummaryBO, Long> fileIdOf) {
        List<Long> fileIds = summaries.stream()
                .map(fileIdOf)
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (fileIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, ResourceReadUrlGetResultBO> urlMap = resourceService.getResourceReadUrls(fileIds);
        return summaries.stream()
                .filter(summary -> {
                    Long fileId = fileIdOf.apply(summary);
                    return fileId != null && fileId > 0 && urlMap.containsKey(fileId);
                })
                .collect(Collectors.toMap(PlazaProjectSummaryBO::getProjectId,
                        summary -> urlMap.get(fileIdOf.apply(summary)).getUrl()));
    }

    private Map<Long, String> resizeThumbnailUrls(Map<Long, String> urls) {
        if (urls.isEmpty()) {
            return urls;
        }
        return urls.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> ImageResizePreset.THUMBNAIL.apply(entry.getValue())));
    }
}
