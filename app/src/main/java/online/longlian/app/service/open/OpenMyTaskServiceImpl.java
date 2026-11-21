package online.longlian.app.service.open;

import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.OpenAppException;
import online.longlian.app.common.result.OpenResultCode;
import online.longlian.app.common.util.OpenCursorCodec;
import online.longlian.app.pojo.bo.app.mytask.MyTaskKeysetItemBO;
import online.longlian.app.pojo.bo.app.mytask.MyTaskKeysetParamsBO;
import online.longlian.app.pojo.bo.app.mytask.MyTaskKeysetResultBO;
import online.longlian.app.pojo.bo.open.OpenCursorBO;
import online.longlian.app.pojo.bo.open.OpenMyTaskBO;
import online.longlian.app.pojo.bo.open.OpenMyTaskListParamsBO;
import online.longlian.app.pojo.bo.open.OpenPageResultBO;
import online.longlian.app.service.app.mytask.MyTaskService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 开放面我的待办查询。
 * 跨模块只读（standard #9）：开放面是跨领域的查询域，键集游标在本域编码，
 * 待办口径（CLAIMED、可提交/等前置分组、按接取时间排序）全部走我的待办域 {@link MyTaskService}。
 */
@Service
@RequiredArgsConstructor
public class OpenMyTaskServiceImpl implements OpenMyTaskService {

    /** 业务时钟与库内时间一致，游标毫秒值按此时区换算。 */
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final MyTaskService myTaskService;

    @Override
    public OpenPageResultBO<OpenMyTaskBO> listMyTasks(OpenMyTaskListParamsBO params) {
        OpenCursorBO cursor = params.cursor();
        if (cursor != null && cursor.afterUpdatedAtMillis() == null) {
            // 交叉端点的游标缺少时间分量，按参数错误拒绝
            throw new OpenAppException(OpenResultCode.PARAM_ERROR, "游标格式错误");
        }
        LocalDateTime afterClaimedAt = cursor == null ? null : toDateTime(cursor.afterUpdatedAtMillis());
        Long afterInstanceId = cursor == null ? null : cursor.afterId();
        MyTaskKeysetResultBO page = myTaskService.listMyTasksKeyset(new MyTaskKeysetParamsBO(
                params.userId(), params.orgId(), params.bucket(), afterClaimedAt, afterInstanceId, params.limit()));
        List<OpenMyTaskBO> items = page.list().stream().map(this::toBO).toList();
        if (!page.hasMore() || page.lastInstanceId() == null) {
            return new OpenPageResultBO<>(items, null, false);
        }
        String nextCursor = OpenCursorCodec.encode(new OpenCursorBO(
                page.lastInstanceId(), toMillis(page.lastClaimedAt())));
        return new OpenPageResultBO<>(items, nextCursor, true);
    }

    private OpenMyTaskBO toBO(MyTaskKeysetItemBO item) {
        return new OpenMyTaskBO(item.instanceId(), item.nodeName(), item.itemId(), item.itemTitle(),
                item.projectId(), item.projectName(), item.taskStatus(), item.revision(),
                item.claimedAt(), item.bucket());
    }

    private LocalDateTime toDateTime(Long epochMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), BUSINESS_ZONE);
    }

    private long toMillis(LocalDateTime time) {
        return time.atZone(BUSINESS_ZONE).toInstant().toEpochMilli();
    }
}
