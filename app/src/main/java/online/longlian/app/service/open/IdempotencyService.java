package online.longlian.app.service.open;

import online.longlian.app.pojo.bo.open.IdempotencyCheckParamsBO;
import online.longlian.app.pojo.bo.open.IdempotencyCheckResultBO;
import online.longlian.app.pojo.bo.open.IdempotencyRecordParamsBO;

/**
 * 写请求幂等：查重 + 同事务记录首次结果。
 * 快照超限（64KB）只存标记不存正文，重复请求返回命中标记而非回放原响应。
 */
public interface IdempotencyService {

    /**
     * 写接口业务执行前的查重，只读不改库。
     */
    IdempotencyCheckResultBO check(IdempotencyCheckParamsBO params);

    /**
     * 首次执行成功后写幂等记录与响应快照。必须在业务事务内调用（MANDATORY）；
     * 同键并发撞唯一索引抛幂等冲突（10006），整个事务随调用方回滚。
     */
    void record(IdempotencyRecordParamsBO params);
}
