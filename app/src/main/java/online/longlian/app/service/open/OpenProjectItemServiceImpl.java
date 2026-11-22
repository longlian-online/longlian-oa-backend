package online.longlian.app.service.open;

import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.util.OpenCursorCodec;
import online.longlian.app.pojo.bo.app.item.ItemKeysetListParamsBO;
import online.longlian.app.pojo.bo.app.item.ItemKeysetListResultBO;
import online.longlian.app.pojo.bo.app.item.ProjectItemListResultBO;
import online.longlian.app.pojo.bo.open.OpenCursorBO;
import online.longlian.app.pojo.bo.open.OpenPageResultBO;
import online.longlian.app.pojo.bo.open.OpenProjectItemBO;
import online.longlian.app.pojo.bo.open.OpenProjectItemListParamsBO;
import online.longlian.app.service.app.item.ItemService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 开放面企划下项目查询。
 * 跨模块只读（standard #9）：开放面是跨领域的查询域，键集游标与增量过滤在本域组装，
 * 项目口径（可见性、进度、当前节点、提交记录标记）全部走项目域 {@link ItemService}，本域不写业务表。
 * 键集分页无法用 Page 插件表达，取 limit+1 判定 hasMore。
 */
@Service
@RequiredArgsConstructor
public class OpenProjectItemServiceImpl implements OpenProjectItemService {

    /** 业务时钟与库内时间一致，游标毫秒值按此时区换算。 */
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final ItemService itemService;

    @Override
    public OpenPageResultBO<OpenProjectItemBO> listProjectItems(OpenProjectItemListParamsBO params) {
        OpenCursorBO cursor = params.cursor();
        if (cursor != null && cursor.afterUpdatedAtMillis() == null) {
            // 交叉端点的游标缺少时间分量，按参数错误拒绝
            throw new AppException(ResultCode.PARAM_ERROR, "游标格式错误");
        }
        LocalDateTime afterUpdatedAt = cursor == null ? null : toDateTime(cursor.afterUpdatedAtMillis());
        Long afterId = cursor == null ? null : cursor.afterId();
        ItemKeysetListResultBO page = itemService.listProjectItemsKeyset(new ItemKeysetListParamsBO(
                params.projectId(), params.orgId(), afterUpdatedAt, afterId, params.updatedSince(), params.limit()));
        List<OpenProjectItemBO> items = page.list().stream()
                .map(this::toBO)
                .toList();
        if (!page.hasMore() || page.lastId() == null) {
            return new OpenPageResultBO<>(items, null, false);
        }
        String nextCursor = OpenCursorCodec.encode(
                new OpenCursorBO(page.lastId(), toMillis(page.lastUpdatedAt())));
        return new OpenPageResultBO<>(items, nextCursor, true);
    }

    private OpenProjectItemBO toBO(ProjectItemListResultBO row) {
        return new OpenProjectItemBO(row.getId(), row.getTitle(), row.getStatus(), row.getCurrentNodeName(),
                row.getProgressPercent(), row.getHasSubmissionRecord(), row.getCreatedAt(), row.getUpdatedAt());
    }

    private LocalDateTime toDateTime(Long epochMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), BUSINESS_ZONE);
    }

    private long toMillis(LocalDateTime time) {
        return time.atZone(BUSINESS_ZONE).toInstant().toEpochMilli();
    }
}
