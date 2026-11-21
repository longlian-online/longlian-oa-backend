package online.longlian.app.service.open;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.util.OpenCursorCodec;
import online.longlian.app.mapper.ProjectMapper;
import online.longlian.app.pojo.bo.common.ResourceReadUrlGetResultBO;
import online.longlian.app.pojo.bo.open.OpenCursorBO;
import online.longlian.app.pojo.bo.open.OpenPageResultBO;
import online.longlian.app.pojo.bo.open.OpenProjectBO;
import online.longlian.app.pojo.bo.open.OpenProjectListParamsBO;
import online.longlian.app.pojo.entity.Project;
import online.longlian.app.service.resource.ResourceService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 开放面企划查询。
 * 跨模块只读（standard #9）：开放面是跨领域的查询域，复用各领域实体做键集分页；
 * 业务规则（写入、状态流转）一律走各领域 Service，本域不写业务表。
 * 键集分页无法用 Page 插件表达，用 LIMIT 取 limit+1 判定 hasMore。
 */
@Service
@RequiredArgsConstructor
public class OpenProjectServiceImpl implements OpenProjectService {

    /** 业务时钟与库内时间一致，游标毫秒值按此时区换算。 */
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final ProjectMapper projectMapper;
    private final ResourceService resourceService;

    @Override
    public OpenPageResultBO<OpenProjectBO> listProjects(OpenProjectListParamsBO params) {
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<Project>()
                .eq(Project::getOrgId, params.orgId())
                .orderByAsc(Project::getUpdatedAt)
                .orderByAsc(Project::getId)
                .last("LIMIT " + (params.limit() + 1));
        if (params.updatedSince() != null) {
            wrapper.ge(Project::getUpdatedAt, params.updatedSince());
        }
        if (params.cursor() != null) {
            LocalDateTime cursorTime = toDateTime(params.cursor().afterUpdatedAtMillis());
            wrapper.and(w -> w.gt(Project::getUpdatedAt, cursorTime)
                    .or(inner -> inner.eq(Project::getUpdatedAt, cursorTime)
                            .gt(Project::getId, params.cursor().afterId())));
        }
        List<Project> rows = projectMapper.selectList(wrapper);
        boolean hasMore = rows.size() > params.limit();
        if (hasMore) {
            rows = rows.subList(0, params.limit());
        }
        if (rows.isEmpty()) {
            return OpenPageResultBO.of(List.of(), null, false);
        }
        Map<Long, String> readUrls = batchReadUrls(rows);
        List<OpenProjectBO> items = rows.stream()
                .map(project -> toBO(project, readUrls, params.userId()))
                .toList();
        if (!hasMore) {
            return OpenPageResultBO.of(items, null, false);
        }
        Project last = rows.get(rows.size() - 1);
        String nextCursor = OpenCursorCodec.encode(
                new OpenCursorBO(last.getId(), toMillis(last.getUpdatedAt())));
        return OpenPageResultBO.of(items, nextCursor, true);
    }

    private LocalDateTime toDateTime(Long epochMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), BUSINESS_ZONE);
    }

    private long toMillis(LocalDateTime time) {
        return time.atZone(BUSINESS_ZONE).toInstant().toEpochMilli();
    }

    private Map<Long, String> batchReadUrls(List<Project> rows) {
        List<Long> fileIds = rows.stream()
                .flatMap(project -> java.util.Arrays.asList(project.getCoverFileId(), project.getThumbnailFileId()).stream())
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .toList();
        if (fileIds.isEmpty()) {
            // 必须用 HashMap：封面/缩略图为 null 时 get(null) 不能抛 NPE
            return new HashMap<>();
        }
        Map<Long, ResourceReadUrlGetResultBO> urls = resourceService.getResourceReadUrls(fileIds);
        Map<Long, String> result = new HashMap<>();
        urls.forEach((id, bo) -> {
            if (bo != null) {
                result.put(id, bo.getUrl());
            }
        });
        return result;
    }

    private OpenProjectBO toBO(Project project, Map<Long, String> readUrls, Long userId) {
        return new OpenProjectBO(
                project.getId(),
                project.getTitle(),
                project.getAlias(),
                readUrls.get(project.getCoverFileId()),
                readUrls.get(project.getThumbnailFileId()),
                project.getStatus(),
                Objects.equals(project.getCreatorId(), userId),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
