package online.longlian.app.service.orgadmin.impl;

import online.longlian.app.pojo.bo.OrgMemberBaseTaskSubmitCountItemBO;
import online.longlian.app.pojo.bo.OrgnMemberBaseTaskSubmitCountResultBO;
import online.longlian.app.pojo.entity.BaseTask;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.pojo.entity.TaskSubmission;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 将提交记录按原子任务维度聚合计数并组装为 BO 结果。
 */
@Component
public class SubmissionCountAssembler {

    /**
     * 按原子任务聚合提交数并构建结果。
     */
    public OrgnMemberBaseTaskSubmitCountResultBO assembleResult(
            OrganizationMember member,
            List<BaseTask> baseTasks,
            Map<Long, Long> nodeIdToBaseTaskId,
            List<TaskSubmission> submissions) {

        Map<Long, Long> submitCountByBaseTask = submissions.stream()
                .collect(Collectors.groupingBy(
                        s -> nodeIdToBaseTaskId.get(s.getItemTaskNodeId()),
                        Collectors.counting()));

        List<OrgMemberBaseTaskSubmitCountItemBO> items = baseTasks.stream()
                .map(bt -> OrgMemberBaseTaskSubmitCountItemBO.builder()
                        .baseTaskId(bt.getId())
                        .baseTaskName(bt.getName())
                        .submitCount(submitCountByBaseTask.getOrDefault(bt.getId(), 0L).intValue())
                        .build())
                .sorted(Comparator.comparing(OrgMemberBaseTaskSubmitCountItemBO::getBaseTaskName))
                .toList();

        return toResult(member, items);
    }

    /**
     * 无提交数据时的空结果。若 baseTasks 为空则 items 为空列表。
     */
    public OrgnMemberBaseTaskSubmitCountResultBO buildEmptyResult(
            OrganizationMember member, List<BaseTask> baseTasks) {

        List<OrgMemberBaseTaskSubmitCountItemBO> items = baseTasks.stream()
                .map(bt -> OrgMemberBaseTaskSubmitCountItemBO.builder()
                        .baseTaskId(bt.getId())
                        .baseTaskName(bt.getName())
                        .submitCount(0)
                        .build())
                .sorted(Comparator.comparing(OrgMemberBaseTaskSubmitCountItemBO::getBaseTaskName))
                .toList();

        return toResult(member, items);
    }

    private OrgnMemberBaseTaskSubmitCountResultBO toResult(
            OrganizationMember member,
            List<OrgMemberBaseTaskSubmitCountItemBO> items) {
        return OrgnMemberBaseTaskSubmitCountResultBO.builder()
                .memberId(member.getId())
                .userId(member.getUserId())
                .totalSubmitCount(member.getSubmitCount())
                .items(items)
                .build();
    }
}
