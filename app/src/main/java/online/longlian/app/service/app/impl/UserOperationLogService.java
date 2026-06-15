package online.longlian.app.service.app.impl;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.mapper.UserOperationLogMapper;
import online.longlian.app.pojo.entity.UserOperationLog;
import online.longlian.common.enumeration.UserOperationType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserOperationLogService {

    private final UserOperationLogMapper userOperationLogMapper;

    @Async("verifyCodeExecutor")
    public void log(Long userId, Long projectId, Long itemId,
                    UserOperationType operationType, Object requestBody) {
        try {
            UserOperationLog operationLog = UserOperationLog.builder()
                    .userId(userId)
                    .projectId(projectId)
                    .itemId(itemId)
                    .operationType(operationType)
                    .requestBody(requestBody != null ? JSON.toJSONString(requestBody) : null)
                    .createdAt(LocalDateTime.now())
                    .build();
            userOperationLogMapper.insert(operationLog);
        } catch (Exception e) {
            log.warn("记录用户操作日志失败 userId={} type={}", userId, operationType, e);
        }
    }
}
