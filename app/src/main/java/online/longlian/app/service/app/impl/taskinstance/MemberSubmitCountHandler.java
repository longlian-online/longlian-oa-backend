package online.longlian.app.service.app.impl.taskinstance;

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

@Component
@RequiredArgsConstructor
public class MemberSubmitCountHandler {

    private final OrganizationMemberMapper organizationMemberMapper;
    private final ProjectMapper projectMapper;
    private final Clock clock;

    public void incrementSubmitCount(Long userId, Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            return;
        }
        OrganizationMember member = findMember(userId, project.getOrgId());
        if (member == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        organizationMemberMapper.update(null,
                new LambdaUpdateWrapper<OrganizationMember>()
                        .eq(OrganizationMember::getId, member.getId())
                        .setSql("submit_count = submit_count + 1")
                        .set(OrganizationMember::getLastSubmittedAt, now)
                        .set(OrganizationMember::getUpdatedAt, now));
    }

    public void revertSubmitCount(Long userId, Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            return;
        }
        OrganizationMember member = findMember(userId, project.getOrgId());
        if (member == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        organizationMemberMapper.update(null,
                new LambdaUpdateWrapper<OrganizationMember>()
                        .eq(OrganizationMember::getId, member.getId())
                        .gt(OrganizationMember::getSubmitCount, 0)
                        .setSql("submit_count = submit_count - 1")
                        .set(OrganizationMember::getLastSubmittedAt, now)
                        .set(OrganizationMember::getUpdatedAt, now));
    }

    private OrganizationMember findMember(Long userId, Long orgId) {
        return organizationMemberMapper.selectOne(
                new LambdaQueryWrapper<OrganizationMember>()
                        .eq(OrganizationMember::getUserId, userId)
                        .eq(OrganizationMember::getOrgId, orgId)
                        .eq(OrganizationMember::getStatus, Status.ENABLED)
                        .last("LIMIT 1"));
    }
}
