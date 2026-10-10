package online.longlian.app.service.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.BaseTaskMapper;
import online.longlian.app.pojo.entity.BaseTask;
import online.longlian.common.enumeration.Status;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BaseTaskReferenceService {

    private final BaseTaskMapper baseTaskMapper;

    // 锁必须持有到引用写入完成；统一加锁顺序避免多任务之间的锁顺序反转。
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockBaseTasks(List<Long> baseTaskIds) {
        List<Long> orderedIds = baseTaskIds.stream().filter(id -> id != null).distinct().sorted().toList();
        for (Long baseTaskId : orderedIds) {
            BaseTask baseTask = baseTaskMapper.selectOne(new LambdaQueryWrapper<BaseTask>()
                    .eq(BaseTask::getId, baseTaskId)
                    .last("FOR UPDATE"));
            if (baseTask == null || baseTask.getStatus() != Status.ENABLED) {
                throw new AppException(ResultCode.PARAM_ERROR, "原子任务不存在或已禁用");
            }
        }
    }
}
