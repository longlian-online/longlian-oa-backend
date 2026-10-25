package online.longlian.app.service.app.impl;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.UserOperationLogMapper;
import online.longlian.app.pojo.entity.UserOperationLog;
import online.longlian.common.enumeration.UserOperationType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserOperationLogService {

    private final UserOperationLogMapper userOperationLogMapper;
    private final Clock clock;

    @Async("operationLogExecutor")
    public void log(Long userId, Long projectId, Long itemId,
                    UserOperationType operationType, Object requestBody) {
        try {
            userOperationLogMapper.insert(createLog(userId, projectId, itemId, operationType, requestBody));
        } catch (Exception e) {
            log.warn("记录用户操作日志失败 userId={} type={}", userId, operationType, e);
        }
    }

    /** 负责人风险操作的审计与状态、提交历史和计数必须同事务成功或回滚。 */
    @Transactional(propagation = Propagation.MANDATORY)
    public void logSynchronously(Long userId, Long projectId, Long itemId,
                                 UserOperationType operationType, Object requestBody) {
        if (userOperationLogMapper.insert(createLog(userId, projectId, itemId, operationType, requestBody)) != 1) {
            throw new AppException(ResultCode.OPERATION_FAIL, "操作审计写入失败");
        }
    }

    private UserOperationLog createLog(Long userId, Long projectId, Long itemId,
                                       UserOperationType operationType, Object requestBody) {
        LocalDateTime now = LocalDateTime.now(clock);
        return UserOperationLog.builder()
                .userId(userId)
                .projectId(projectId)
                .itemId(itemId)
                .operationType(operationType)
                .requestBody(requestBody != null ? JSON.toJSONString(requestBody) : null)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
