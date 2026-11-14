package online.longlian.app.service.app.impl.taskinstance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.mapper.TaskSubmissionMapper;
import online.longlian.app.pojo.bo.app.ProjectMemberSubmissionStatBO;
import online.longlian.app.pojo.entity.TaskSubmission;
import online.longlian.common.enumeration.TaskSubmissionStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 企划成员的有效提交。TaskInstanceService 已依赖 ProjectService，不能从成员列表反向调用。
 * 只统计仍为已提交的记录：打回和重置不再算完成，也不再作为上一次提交时间。
 */
@Component
@RequiredArgsConstructor
public class ProjectMemberSubmissionStats {

    private final TaskSubmissionMapper taskSubmissionMapper;

    public Map<Long, ProjectMemberSubmissionStatBO> forMembers(Long projectId, List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        List<TaskSubmission> rows = taskSubmissionMapper.selectList(new LambdaQueryWrapper<TaskSubmission>()
                .select(TaskSubmission::getSubmitterId, TaskSubmission::getTaskInstanceId, TaskSubmission::getCreatedAt)
                .eq(TaskSubmission::getProjectId, projectId)
                .eq(TaskSubmission::getStatus, TaskSubmissionStatus.SUBMITTED)
                .in(TaskSubmission::getSubmitterId, userIds));
        Map<Long, LocalDateTime> latest = new HashMap<>();
        Map<Long, Set<Long>> tasks = new HashMap<>();
        for (TaskSubmission row : rows) {
            latest.merge(row.getSubmitterId(), row.getCreatedAt(),
                    (left, right) -> left.isAfter(right) ? left : right);
            tasks.computeIfAbsent(row.getSubmitterId(), ignored -> new HashSet<>()).add(row.getTaskInstanceId());
        }
        Map<Long, ProjectMemberSubmissionStatBO> stats = new HashMap<>();
        latest.forEach((userId, submittedAt) ->
                stats.put(userId, new ProjectMemberSubmissionStatBO(submittedAt, tasks.get(userId).size())));
        return stats;
    }
}
