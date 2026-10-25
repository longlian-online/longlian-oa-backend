package online.longlian.app.service.app.impl.taskinstance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.enumeration.PlazaSortBy;
import online.longlian.common.enumeration.Status;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.ProjectTypeMapper;
import online.longlian.app.mapper.TaskInstanceMapper;
import online.longlian.app.pojo.bo.app.ItemStageNodeStatusBO;
import online.longlian.app.pojo.bo.app.PlazaItemStageBO;
import online.longlian.app.pojo.bo.app.PlazaProjectSummaryBO;
import online.longlian.app.pojo.bo.app.PlazaTaskRowBO;
import online.longlian.app.pojo.bo.app.TaskPlazaParamsBO;
import online.longlian.app.pojo.bo.app.TaskPlazaSearchParamsBO;
import online.longlian.app.pojo.bo.common.ResourceReadUrlGetResultBO;
import online.longlian.app.pojo.entity.ProjectType;
import online.longlian.app.pojo.vo.app.PlazaProjectGroupVO;
import online.longlian.app.pojo.vo.common.PageResultVO;
import online.longlian.app.service.app.TaskPlazaService;
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

    /** 任务大厅缩略图按 EdgeOne 图片即时处理参数缩放到 144x144。 */
    private static final String THUMBNAIL_RESIZE_PARAM = "eo-img.resize=w/144/h/144";

    private final TaskInstanceMapper taskInstanceMapper;
    private final ProjectTypeMapper projectTypeMapper;
    private final ResourceService resourceService;
    private final TaskPlazaAssembler taskPlazaAssembler;

    @Override
    public PageResultVO<PlazaProjectGroupVO> getTaskPlaza(TaskPlazaParamsBO params) {
        TaskPlazaSearchParamsBO searchParams = TaskPlazaSearchParamsBO.builder()
                .orgId(params.getOrgId())
                .typeId(resolveTypeId(params.getOrgId(), params.getProjectType()))
                .matchPattern(buildMatchPattern(params.getKeyword()))
                .sortBy(params.getSortBy())
                .stageScope(params.getStageScope())
                .build();

        long total = taskInstanceMapper.selectPlazaProjectCount(searchParams);
        if (total == 0) {
            return new PageResultVO<>(Collections.emptyList(), 0L);
        }

        int offset = (params.getPage().getPageNum() - 1) * params.getPage().getPageSize();
        List<PlazaProjectSummaryBO> summaries = taskInstanceMapper.selectPlazaProjectSummaries(
                searchParams, offset, params.getPage().getPageSize());

        List<Long> projectIds = summaries.stream().map(PlazaProjectSummaryBO::getProjectId).toList();
        List<PlazaTaskRowBO> rows = taskInstanceMapper.selectPlazaTaskRows(searchParams, projectIds);

        List<Long> itemIds = rows.stream().map(PlazaTaskRowBO::getItemId).distinct().toList();
        Map<Long, List<Integer>> itemSorts = itemIds.isEmpty()
                ? Collections.emptyMap()
                : taskInstanceMapper.selectPlazaItemStages(itemIds).stream()
                        .collect(Collectors.groupingBy(PlazaItemStageBO::getItemId,
                                Collectors.collectingAndThen(
                                        Collectors.mapping(PlazaItemStageBO::getSort,
                                                Collectors.toCollection(TreeSet::new)),
                                        List::copyOf)));
        Map<Long, List<ItemStageNodeStatusBO>> nodesByItem = itemIds.isEmpty()
                ? Map.of()
                : taskInstanceMapper.selectItemStageNodeStatuses(itemIds).stream()
                        .collect(Collectors.groupingBy(ItemStageNodeStatusBO::getItemId));

        List<PlazaProjectGroupVO> groups = taskPlazaAssembler.assembleGroups(
                summaries, rows, itemSorts,
                loadImageUrls(summaries, PlazaProjectSummaryBO::getCoverFileId),
                resizeThumbnailUrls(loadImageUrls(summaries, PlazaProjectSummaryBO::getThumbnailFileId)),
                nodesByItem);
        return new PageResultVO<>(groups, total);
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
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> resizeThumbnailUrl(entry.getValue())));
    }

    private static String resizeThumbnailUrl(String url) {
        return url + (url.contains("?") ? "&" : "?") + THUMBNAIL_RESIZE_PARAM;
    }
}
