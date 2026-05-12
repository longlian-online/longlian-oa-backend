package online.longlian.app.service.orgadmin.impl.orgmember;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.mapper.BaseTaskMapper;
import online.longlian.app.mapper.ItemTaskNodeMapper;
import online.longlian.app.mapper.TaskSubmissionMapper;
import online.longlian.app.pojo.bo.OrgMemberBaseTaskSubmitCountResultBO;
import online.longlian.app.pojo.entity.BaseTask;
import online.longlian.app.pojo.entity.ItemTaskNode;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.pojo.entity.TaskSubmission;
import online.longlian.common.enumeration.Status;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 处理成员提交统计查询。
 */
@Component
@RequiredArgsConstructor
public class MemberSubmissionHandler {

    private final BaseTaskMapper baseTaskMapper;
    private final ItemTaskNodeMapper itemTaskNodeMapper;
    private final TaskSubmissionMapper taskSubmissionMapper;
    private final SubmissionCountAssembler submissionCountAssembler;

    /**
     * 从原子任务→任务节点→提交记录逐层关联，每层结果为空时提前返回
     * 委托SubmissionCountAssembler组装
     */
    public OrgMemberBaseTaskSubmitCountResultBO getSubmitCounts(OrganizationMember member) {
        List<BaseTask> baseTasks = baseTaskMapper.selectList(
                new LambdaQueryWrapper<BaseTask>()
                        .eq(BaseTask::getOrgId, member.getOrgId())
                        .eq(BaseTask::getStatus, Status.ENABLED));
        if (baseTasks.isEmpty()) {
            return submissionCountAssembler.buildEmptyResult(member, Collections.emptyList());
        }

        List<Long> baseTaskIds = baseTasks.stream().map(BaseTask::getId).toList();
        List<ItemTaskNode> taskNodes = itemTaskNodeMapper.selectList(
                new LambdaQueryWrapper<ItemTaskNode>()
                        .in(ItemTaskNode::getBaseTaskId, baseTaskIds));
        if (taskNodes.isEmpty()) {
            return submissionCountAssembler.buildEmptyResult(member, baseTasks);
        }

        Map<Long, Long> nodeIdToBaseTaskId = taskNodes.stream()
                .collect(Collectors.toMap(ItemTaskNode::getId, ItemTaskNode::getBaseTaskId));
        List<Long> nodeIds = taskNodes.stream().map(ItemTaskNode::getId).toList();

        List<TaskSubmission> submissions = taskSubmissionMapper.selectList(
                new LambdaQueryWrapper<TaskSubmission>()
                        .eq(TaskSubmission::getSubmitterId, member.getUserId())
                        .in(TaskSubmission::getItemTaskNodeId, nodeIds));

        return submissionCountAssembler.assembleResult(member, baseTasks, nodeIdToBaseTaskId, submissions);
    }
}
