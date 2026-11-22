package online.longlian.app.service.open;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.IdempotencyRecordMapper;
import online.longlian.app.pojo.bo.open.IdempotencyCheckParamsBO;
import online.longlian.app.pojo.bo.open.IdempotencyCheckResultBO;
import online.longlian.app.pojo.bo.open.IdempotencyCheckOutcome;
import online.longlian.app.pojo.bo.open.IdempotencyRecordParamsBO;
import online.longlian.app.pojo.entity.IdempotencyRecord;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 写请求幂等。落库而非 Redis：开放面有写操作后，缓存故障的降级语义（放行重复或拒绝服务）两难，
 * 唯一索引是最无聊的强保证，且记录与业务写入同事务、提交与否精确对齐。
 */
@Service
@RequiredArgsConstructor
public class IdempotencyServiceImpl implements IdempotencyService {

    /** 快照上限 64KB：超限不存正文，重复请求只返回命中标记，不回放原响应。 */
    private static final int SNAPSHOT_MAX_BYTES = 64 * 1024;

    private final IdempotencyRecordMapper idempotencyRecordMapper;
    private final Clock clock;

    @Override
    public IdempotencyCheckResultBO check(IdempotencyCheckParamsBO params) {
        IdempotencyRecord record = idempotencyRecordMapper.selectOne(new LambdaQueryWrapper<IdempotencyRecord>()
                .eq(IdempotencyRecord::getSource, params.source())
                .eq(IdempotencyRecord::getCallerId, params.callerId())
                .eq(IdempotencyRecord::getIdempotencyKey, params.idempotencyKey())
                .last("LIMIT 1"));
        if (record == null) {
            return new IdempotencyCheckResultBO(IdempotencyCheckOutcome.MISS, null);
        }
        boolean sameRequest = Objects.equals(record.getMethod(), params.method())
                && Objects.equals(record.getPath(), params.path())
                && Objects.equals(record.getBodyHash(), params.bodyHash());
        if (!sameRequest) {
            return new IdempotencyCheckResultBO(IdempotencyCheckOutcome.CONFLICT, null);
        }
        return new IdempotencyCheckResultBO(IdempotencyCheckOutcome.HIT,
                record.getSnapshot() == null ? null : JSON.parse(record.getSnapshot()));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void record(IdempotencyRecordParamsBO params) {
        String snapshotJson = params.resultData() == null ? null : JSON.toJSONString(params.resultData());
        boolean truncated = snapshotJson != null
                && snapshotJson.getBytes(StandardCharsets.UTF_8).length > SNAPSHOT_MAX_BYTES;
        IdempotencyRecord record = IdempotencyRecord.builder()
                .source(params.source())
                .callerId(params.callerId())
                .idempotencyKey(params.idempotencyKey())
                .method(params.method())
                .path(params.path())
                .bodyHash(params.bodyHash())
                .snapshot(truncated ? null : snapshotJson)
                .snapshotTruncated((byte) (truncated ? 1 : 0))
                .createdAt(LocalDateTime.now(clock))
                .updatedAt(LocalDateTime.now(clock))
                .build();
        try {
            idempotencyRecordMapper.insert(record);
        } catch (DuplicateKeyException e) {
            // 并发同键：首笔尚未提交，撞唯一索引，整笔事务随调用方回滚；稍后原样重试即得首次响应
            throw new AppException(ResultCode.OPEN_IDEMPOTENCY_CONFLICT);
        }
    }
}
