package online.longlian.app.service.open;

import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.OpenAppException;
import online.longlian.app.common.result.OpenResultCode;
import online.longlian.app.common.util.OpenCursorCodec;
import online.longlian.app.pojo.bo.app.taskplaza.TaskPlazaKeysetParamsBO;
import online.longlian.app.pojo.bo.app.taskplaza.TaskPlazaKeysetResultBO;
import online.longlian.app.pojo.bo.open.OpenCursorBO;
import online.longlian.app.pojo.bo.open.OpenPageResultBO;
import online.longlian.app.pojo.bo.open.OpenTaskPlazaListParamsBO;
import online.longlian.app.pojo.bo.app.taskplaza.PlazaProjectGroupResultBO;
import online.longlian.app.service.app.taskplaza.TaskPlazaService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 开放面任务广场查询。
 * 跨模块只读（standard #9）：开放面是跨领域的查询域，键集游标在本域编码；
 * 缺人口径与装配（阶段推导、槽位统计）全部走任务广场域 {@link TaskPlazaService}。
 */
@Service
@RequiredArgsConstructor
public class OpenTaskPlazaServiceImpl implements OpenTaskPlazaService {

    /** 业务时钟与库内时间一致，游标毫秒值按此时区换算。 */
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final TaskPlazaService taskPlazaService;

    @Override
    public OpenPageResultBO<PlazaProjectGroupResultBO> getTaskPlaza(OpenTaskPlazaListParamsBO params) {
        OpenCursorBO cursor = params.cursor();
        if (cursor != null && cursor.afterUpdatedAtMillis() == null) {
            // 交叉端点的游标缺少时间分量，按参数错误拒绝
            throw new OpenAppException(OpenResultCode.PARAM_ERROR, "游标格式错误");
        }
        LocalDateTime afterUpdatedAt = cursor == null ? null : toDateTime(cursor.afterUpdatedAtMillis());
        Long afterProjectId = cursor == null ? null : cursor.afterId();
        TaskPlazaKeysetResultBO page = taskPlazaService.getTaskPlazaKeyset(new TaskPlazaKeysetParamsBO(
                params.orgId(), params.baseTaskId(), params.keyword(), params.stageScope(),
                afterUpdatedAt, afterProjectId, params.limit()));
        if (!page.hasMore() || page.lastProjectId() == null) {
            return new OpenPageResultBO<>(page.groups(), null, false);
        }
        String nextCursor = OpenCursorCodec.encode(new OpenCursorBO(
                page.lastProjectId(), toMillis(page.lastProjectUpdatedAt())));
        return new OpenPageResultBO<>(page.groups(), nextCursor, true);
    }

    private LocalDateTime toDateTime(Long epochMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), BUSINESS_ZONE);
    }

    private long toMillis(LocalDateTime time) {
        return time.atZone(BUSINESS_ZONE).toInstant().toEpochMilli();
    }
}
