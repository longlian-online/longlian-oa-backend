package online.longlian.app.service.organization;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.mapper.ProjectMapper;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.pojo.entity.Project;
import online.longlian.common.enumeration.Status;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 组织成员的有效提交统计，由任务提交、打回和重置流程调用。
 * 仅通过企划读取组织归属，不修改企划；保留按企划定位成员的调用语义。
 */
@Component
@RequiredArgsConstructor
public class MemberSubmissionStatisticsService {

    private final OrganizationMemberMapper organizationMemberMapper;
    private final ProjectMapper projectMapper;
    private final Clock clock;

    public void incrementSubmitCount(Long userId, Long projectId) {
        incrementSubmitCount(userId, projectId, LocalDateTime.now(clock));
    }

    /**
     * @param submittedAt 这次提交记入的时间。补记更早的成果时不能把最近提交时间改回去。
     */
    public void incrementSubmitCount(Long userId, Long projectId, LocalDateTime submittedAt) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            return;
        }
        OrganizationMember member = findMember(userId, project.getOrgId(), true);
        if (member == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime recorded = member.getLastSubmittedAt();
        LocalDateTime lastSubmittedAt = recorded == null || !submittedAt.isBefore(recorded)
                ? submittedAt : recorded;
        organizationMemberMapper.update(null,
                new LambdaUpdateWrapper<OrganizationMember>()
                        .eq(OrganizationMember::getId, member.getId())
                        .setSql("submit_count = submit_count + 1")
                        .set(OrganizationMember::getLastSubmittedAt, lastSubmittedAt)
                        .set(OrganizationMember::getUpdatedAt, now));
    }

    public void revertSubmitCount(Long userId, Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            return;
        }
        // 已禁用的提交人仍保留历史提交计数，打回时同样需要撤回这一次有效提交。
        OrganizationMember member = findMember(userId, project.getOrgId(), false);
        if (member == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        organizationMemberMapper.update(null,
                new LambdaUpdateWrapper<OrganizationMember>()
                        .eq(OrganizationMember::getId, member.getId())
                        .gt(OrganizationMember::getSubmitCount, 0)
                        .setSql("submit_count = submit_count - 1")
                        .set(OrganizationMember::getUpdatedAt, now));
    }

    private OrganizationMember findMember(Long userId, Long orgId, boolean enabledOnly) {
        return organizationMemberMapper.selectOne(
                new LambdaQueryWrapper<OrganizationMember>()
                        .eq(OrganizationMember::getUserId, userId)
                        .eq(OrganizationMember::getOrgId, orgId)
                        .eq(enabledOnly, OrganizationMember::getStatus, Status.ENABLED)
                        .last("LIMIT 1"));
    }
}
