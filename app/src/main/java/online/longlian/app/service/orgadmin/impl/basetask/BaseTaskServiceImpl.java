package online.longlian.app.service.orgadmin.impl.basetask;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.BaseTaskMapper;
import online.longlian.app.pojo.bo.PageResultBO;
import online.longlian.app.pojo.bo.orgadmin.BaseTaskChangeStatusParamsBO;
import online.longlian.app.pojo.bo.orgadmin.BaseTaskCreateParamsBO;
import online.longlian.app.pojo.bo.orgadmin.BaseTaskListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.BaseTaskListResultBO;
import online.longlian.app.pojo.entity.BaseTask;
import online.longlian.app.service.orgadmin.BaseTaskService;
import online.longlian.generator.enumeration.Status;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BaseTaskServiceImpl implements BaseTaskService {

    private final BaseTaskMapper baseTaskMapper;
    private final Clock clock;
    private final BaseTaskQueryBuilder baseTaskQueryBuilder;
    private final BaseTaskAssembler baseTaskAssembler;

    @Override
    public PageResultBO<BaseTaskListResultBO> listBaseTasks(BaseTaskListParamsBO params) {
        Page<BaseTask> page = new Page<>(params.getPage().getPageNum(), params.getPage().getPageSize());
        LambdaQueryWrapper<BaseTask> queryWrapper = baseTaskQueryBuilder.buildListQuery(params);
        Page<BaseTask> taskPage = baseTaskMapper.selectPage(page, queryWrapper);
        List<BaseTask> tasks = taskPage.getRecords();
        if (tasks.isEmpty()) {
            return new PageResultBO<>(Collections.emptyList(), taskPage.getTotal());
        }
        return new PageResultBO<>(baseTaskAssembler.assembleBaseTaskList(tasks), taskPage.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createBaseTask(BaseTaskCreateParamsBO params) {
        LocalDateTime now = LocalDateTime.now(clock);
        BaseTask task = BaseTask.builder()
                .orgId(params.getOrgId())
                .name(params.getName())
                .description(params.getDescription())
                .iconFileId(params.getIconFileId())
                .metaSchema(params.getMetaSchema())
                .status(Status.ENABLED)
                .creatorId(params.getCreatorId())
                .createdAt(now)
                .updatedAt(now)
                .build();
        baseTaskMapper.insert(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeBaseTaskStatus(BaseTaskChangeStatusParamsBO params) {
        BaseTask task = baseTaskMapper.selectById(params.getTaskId());
        if (task == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "原子任务不存在");
        }
        if (!task.getOrgId().equals(params.getOrgId())) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "无权操作该原子任务");
        }
        baseTaskMapper.update(null,
                new LambdaUpdateWrapper<BaseTask>()
                        .eq(BaseTask::getId, params.getTaskId())
                        .set(BaseTask::getStatus, params.getStatus())
                        .set(BaseTask::getUpdatedAt, LocalDateTime.now(clock))
        );
    }
}
