package online.longlian.app.service.orgadmin.impl.basetask;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.BaseTaskMapper;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.mapper.ItemTaskNodeMapper;
import online.longlian.app.mapper.TaskTemplateNodeMapper;
import online.longlian.app.pojo.bo.orgadmin.BaseTaskChangeStatusParamsBO;
import online.longlian.app.pojo.bo.orgadmin.BaseTaskCreateParamsBO;
import online.longlian.app.pojo.bo.orgadmin.BaseTaskDeleteParamsBO;
import online.longlian.app.pojo.bo.orgadmin.BaseTaskListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.BaseTaskListResultBO;
import online.longlian.app.pojo.entity.BaseTask;
import online.longlian.app.pojo.entity.ItemTaskNode;
import online.longlian.app.pojo.entity.TaskTemplateNode;
import online.longlian.app.service.orgadmin.BaseTaskService;
import online.longlian.app.service.common.TaskFormService;
import online.longlian.common.enumeration.Status;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BaseTaskServiceImpl implements BaseTaskService {

    private final BaseTaskMapper baseTaskMapper;
    private final TaskTemplateNodeMapper taskTemplateNodeMapper;
    private final ItemTaskNodeMapper itemTaskNodeMapper;
    private final Clock clock;
    private final BaseTaskQueryBuilder baseTaskQueryBuilder;
    private final BaseTaskAssembler baseTaskAssembler;
    private final TaskFormService taskFormService;

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
                .icon(params.getIcon())
                .metaSchema(taskFormService.serializeFields(params.getSubmitFields()))
                .status(Status.ENABLED)
                .creatorId(params.getCreatorId())
                .createdAt(now)
                .updatedAt(now)
                .build();
        baseTaskMapper.insert(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBaseTask(BaseTaskDeleteParamsBO params) {
        BaseTask task = baseTaskMapper.selectOne(new LambdaQueryWrapper<BaseTask>()
                .eq(BaseTask::getId, params.getTaskId())
                .last("FOR UPDATE"));
        if (task == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "原子任务不存在");
        }
        if (!task.getOrgId().equals(params.getOrgId())) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "无权操作该原子任务");
        }
        boolean referenced = taskTemplateNodeMapper.selectCount(new LambdaQueryWrapper<TaskTemplateNode>()
                .eq(TaskTemplateNode::getBaseTaskId, params.getTaskId())) > 0
                || itemTaskNodeMapper.selectCount(new LambdaQueryWrapper<ItemTaskNode>()
                .eq(ItemTaskNode::getBaseTaskId, params.getTaskId())) > 0;
        if (referenced) {
            throw new AppException(ResultCode.PARAM_ERROR, "该原子任务已被任务模板或项目任务节点引用，请改为禁用");
        }
        baseTaskMapper.deleteById(params.getTaskId());
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockBaseTasks(List<Long> baseTaskIds) {
        List<Long> orderedIds = baseTaskIds.stream().filter(id -> id != null).distinct().sorted().toList();
        if (orderedIds.isEmpty()) {
            return;
        }
        List<BaseTask> tasks = baseTaskMapper.selectList(new LambdaQueryWrapper<BaseTask>()
                .in(BaseTask::getId, orderedIds)
                .orderByAsc(BaseTask::getId)
                .last("FOR UPDATE"));
        if (tasks.size() != orderedIds.size()
                || tasks.stream().anyMatch(task -> task.getStatus() != Status.ENABLED)) {
            throw new AppException(ResultCode.PARAM_ERROR, "原子任务不存在或已禁用");
        }
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
                        .ne(BaseTask::getStatus, params.getStatus())
                        .set(BaseTask::getStatus, params.getStatus())
                        .set(BaseTask::getUpdatedAt, LocalDateTime.now(clock))
        );
    }
}
