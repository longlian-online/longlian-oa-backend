package online.longlian.app.service.orgadmin.impl.orgmember;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.enumeration.SortDirection;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.pojo.bo.app.ClaimedTaskLoadParamsBO;
import online.longlian.app.pojo.bo.app.ClaimedTaskLoadResultBO;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberInfoResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberNoteParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberWorkloadListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberWorkloadResultBO;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.service.app.ClaimedTaskLoadService;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 组员负载按待提交数量排序。数量来自任务域，一个汉化组的成员规模适合一次取出后在内存分页，
 * 避免把任务统计 SQL 写进成员查询。
 */
@Component
@RequiredArgsConstructor
public class MemberWorkloadHandler {

    private final OrganizationMemberMapper organizationMemberMapper;
    private final MemberQueryBuilder memberQueryBuilder;
    private final MemberAssembler memberAssembler;
    private final ClaimedTaskLoadService claimedTaskLoadService;

    public PageResultBO<OrgMemberWorkloadResultBO> list(OrgMemberWorkloadListParamsBO params) {
        List<OrganizationMember> members = organizationMemberMapper.selectList(
                memberQueryBuilder.buildWorkloadMemberQuery(params));
        if (members.isEmpty()) {
            return new PageResultBO<>(List.of(), 0L);
        }
        List<Long> userIds = members.stream().map(OrganizationMember::getUserId).distinct().toList();
        Map<Long, List<ClaimedTaskLoadResultBO>> tasksByUser = claimedTaskLoadService.listClaimedTasks(
                        ClaimedTaskLoadParamsBO.builder().orgId(params.getOrgId()).assigneeIds(userIds).build())
                .stream()
                .collect(Collectors.groupingBy(ClaimedTaskLoadResultBO::getAssigneeId));

        Comparator<OrganizationMember> byLoad = Comparator.comparingInt(
                (OrganizationMember member) -> tasksByUser.getOrDefault(member.getUserId(), List.of()).size());
        if (params.getOrderDir() != SortDirection.ASC) {
            byLoad = byLoad.reversed();
        }
        List<OrganizationMember> sorted = members.stream()
                .sorted(byLoad.thenComparing(OrganizationMember::getId))
                .toList();

        int pageNum = params.getPage().getPageNum();
        int pageSize = params.getPage().getPageSize();
        int from = Math.min((pageNum - 1) * pageSize, sorted.size());
        int to = Math.min(from + pageSize, sorted.size());
        List<OrganizationMember> page = sorted.subList(from, to);
        Map<Long, OrgMemberInfoResultBO> profiles = memberAssembler.assembleMembers(page).stream()
                .collect(Collectors.toMap(OrgMemberInfoResultBO::getId, Function.identity()));

        List<OrgMemberWorkloadResultBO> result = page.stream()
                .map(member -> toResult(member, profiles.get(member.getId()),
                        tasksByUser.getOrDefault(member.getUserId(), List.of())))
                .toList();
        return new PageResultBO<>(result, (long) sorted.size());
    }

    public void updateNote(OrgMemberNoteParamsBO params) {
        OrganizationMember member = organizationMemberMapper.selectById(params.getMemberId());
        if (member == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "成员不存在");
        }
        if (!params.getOrgId().equals(member.getOrgId())) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "无权操作该成员");
        }
        organizationMemberMapper.update(null, new LambdaUpdateWrapper<OrganizationMember>()
                .eq(OrganizationMember::getId, member.getId())
                .set(OrganizationMember::getNote, params.getNote().trim()));
    }

    private OrgMemberWorkloadResultBO toResult(OrganizationMember member,
                                                OrgMemberInfoResultBO profile,
                                                List<ClaimedTaskLoadResultBO> tasks) {
        int waitingCount = (int) tasks.stream().filter(task -> task.getWaitingStageName() != null).count();
        return OrgMemberWorkloadResultBO.builder()
                .id(member.getId())
                .userId(member.getUserId())
                .nickname(profile.getNickname())
                .username(profile.getUsername())
                .avatarUrl(profile.getAvatarUrl())
                .status(member.getStatus())
                .note(member.getNote())
                .claimedCount(tasks.size())
                .readyCount(tasks.size() - waitingCount)
                .waitingCount(waitingCount)
                .tasks(tasks)
                .build();
    }
}
