package online.longlian.app.service.open;

import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.OpenAppException;
import online.longlian.app.common.result.OpenResultCode;
import online.longlian.app.pojo.bo.open.IdempotencyCheckParamsBO;
import online.longlian.app.pojo.bo.open.IdempotencyCheckResultBO;
import online.longlian.app.pojo.bo.open.IdempotencyRecordParamsBO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

/**
 * 开放面写接口统一幂等编排：查重 → 业务 → 本事务内写记录与快照。
 * 命中回放首次结果；同键不同请求体拒绝。
 * 并发同键的防重不靠这里：files 类业务加入本事务、随唯一索引整笔回滚；
 * 任务命令在站内 mutateTask 的 REQUIRES_NEW + FOR UPDATE 中串行，重复执行被
 * 站内 revision 状态机拒绝，本事务随异常回滚、不留记录，稍后重试即得首次响应。
 * 业务方法返回的 VO 就是快照内容：对外契约要求写接口的响应可原样回放，因此业务结果必须小且稳定。
 */
@Service
@RequiredArgsConstructor
public class OpenWriteCommandService {

    private final IdempotencyService idempotencyService;

    @Transactional(rollbackFor = Exception.class)
    public Object execute(IdempotencyCheckParamsBO params, Supplier<Object> business) {
        IdempotencyCheckResultBO existing = idempotencyService.check(params);
        switch (existing.outcome()) {
            case HIT:
                // 快照超限时 data 为 null：返回幂等命中（code 0）而非回放原响应
                return existing.data();
            case CONFLICT:
                throw new OpenAppException(OpenResultCode.IDEMPOTENCY_CONFLICT,
                        "幂等键冲突或首笔仍在处理中，请稍后重试");
            case MISS:
            default:
                Object result = business.get();
                idempotencyService.record(new IdempotencyRecordParamsBO(
                        params.source(), params.callerId(), params.idempotencyKey(),
                        params.method(), params.path(), params.bodyHash(), result));
                return result;
        }
    }
}
