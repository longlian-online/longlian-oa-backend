package online.longlian.app.service.orgadmin.impl.tasktemplate;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.TaskTemplateMapper;
import online.longlian.app.mapper.TaskTemplateNodeMapper;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateNodeCreateParamsBO;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateUpdateParamsBO;
import online.longlian.app.pojo.entity.TaskTemplate;
import online.longlian.app.pojo.entity.TaskTemplateNode;
import online.longlian.common.enumeration.Status;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class TaskTemplateHandler {

    private final TaskTemplateMapper taskTemplateMapper;
    private final TaskTemplateNodeMapper taskTemplateNodeMapper;
    private final Clock clock;

    @Transactional(rollbackFor = Exception.class)
    public void updateTaskTemplate(TaskTemplateUpdateParamsBO params) {
        TaskTemplate template = taskTemplateMapper.selectById(params.getTemplateId());
        if (template == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "任务模板不存在");
        }
        if (!template.getOrgId().equals(params.getOrgId())) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "无权操作该任务模板");
        }
        if (template.getStatus() == Status.DISABLED) {
            throw new AppException(ResultCode.OPERATION_FAIL, "任务模板已被禁用");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        taskTemplateMapper.update(null,
                new LambdaUpdateWrapper<TaskTemplate>()
                        .eq(TaskTemplate::getId, params.getTemplateId())
                        .set(TaskTemplate::getName, params.getName())
                        .set(TaskTemplate::getDescription, params.getDescription())
                        .set(TaskTemplate::getUpdatedAt, now));

        taskTemplateNodeMapper.update(null,
                new LambdaUpdateWrapper<TaskTemplateNode>()
                        .eq(TaskTemplateNode::getTaskTemplateId, params.getTemplateId())
                        .isNull(TaskTemplateNode::getDeletedAt)
                        .set(TaskTemplateNode::getDeletedAt, now));

        for (TaskTemplateNodeCreateParamsBO node : params.getNodes()) {
            taskTemplateNodeMapper.insert(
                    TaskTemplateNode.builder()
                            .taskTemplateId(params.getTemplateId())
                            .baseTaskId(node.getBaseTaskId())
                            .sort(node.getSort())
                            .parallelSort(node.getParallelSort())
                            .createdAt(now)
                            .updatedAt(now)
                            .build());
        }
    }
}
