package online.longlian.app.service.orgadmin.impl.orgmember;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.enumeration.SortDirection;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.orgadmin.OrgAdminApplicationListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberWorkloadListParamsBO;
import online.longlian.app.pojo.entity.GroupApplication;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.pojo.entity.User;
import online.longlian.common.enumeration.ApplicationStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 将成员/申请查询条件的组装逻辑从 ServiceImpl 中剥离
 */
@Component
@RequiredArgsConstructor
public class MemberQueryBuilder {

    private final UserMapper userMapper;

    /**
     * 入组申请查询构建
     */
    public LambdaQueryWrapper<GroupApplication> buildApplicationListQuery(OrgAdminApplicationListParamsBO params) {
        LambdaQueryWrapper<GroupApplication> queryWrapper = new LambdaQueryWrapper<GroupApplication>()
                .eq(GroupApplication::getOrgId, params.getOrgId())
                .eq(GroupApplication::getStatus, ApplicationStatus.PENDING)
                .ge(params.getStartApplyTime() != null, GroupApplication::getCreatedAt, params.getStartApplyTime())
                .le(params.getEndApplyTime() != null, GroupApplication::getCreatedAt, params.getEndApplyTime());

        if (StringUtils.hasText(params.getKeyword())) {
            queryWrapper.like(GroupApplication::getNickname, params.getKeyword().trim());
        }

        // 提交默认倒序
        if (params.getOrderDir() == SortDirection.ASC) {
            queryWrapper.orderByAsc(GroupApplication::getCreatedAt);
        } else {
            queryWrapper.orderByDesc(GroupApplication::getCreatedAt);
        }

        return queryWrapper;
    }

    /**
     * 组织成员查询构建
     */
    public LambdaQueryWrapper<OrganizationMember> buildMemberListQuery(OrgMemberListParamsBO params) {
        LambdaQueryWrapper<OrganizationMember> queryWrapper = new LambdaQueryWrapper<OrganizationMember>()
                .eq(OrganizationMember::getOrgId, params.getOrgId())
                .ge(params.getStartJoinedTime() != null, OrganizationMember::getJoinedAt, params.getStartJoinedTime())
                .le(params.getEndJoinedTime() != null, OrganizationMember::getJoinedAt, params.getEndJoinedTime());

        if (StringUtils.hasText(params.getKeyword())) {
            String keyword = params.getKeyword().trim();
            List<Long> matchedUserIds = userMapper.selectList(new LambdaQueryWrapper<User>()
                            .select(User::getId)
                            .like(User::getNickname, keyword))
                    .stream()
                    .map(User::getId)
                    .toList();
            if (!matchedUserIds.isEmpty()) {
                queryWrapper.in(OrganizationMember::getUserId, matchedUserIds);
            } else {
                // 无匹配用户时返回空结果
                queryWrapper.eq(OrganizationMember::getId, -1L);
            }
        }

        // 同秒入组的成员用唯一 ID 打破平局，避免 LIMIT 翻页时重复或遗漏。
        if (params.getOrderDir() == SortDirection.ASC) {
            queryWrapper.orderByAsc(OrganizationMember::getJoinedAt, OrganizationMember::getId);
        } else {
            queryWrapper.orderByDesc(OrganizationMember::getJoinedAt, OrganizationMember::getId);
        }

        return queryWrapper;
    }

    /**
     * 负载页先取出当前组织匹配昵称的成员，再按待提交数量在内存中排序分页。
     * 排序键属于任务域，不在这条成员查询里排序。
     */
    public LambdaQueryWrapper<OrganizationMember> buildWorkloadMemberQuery(OrgMemberWorkloadListParamsBO params) {
        LambdaQueryWrapper<OrganizationMember> queryWrapper = new LambdaQueryWrapper<OrganizationMember>()
                .eq(OrganizationMember::getOrgId, params.getOrgId());
        if (StringUtils.hasText(params.getKeyword())) {
            String keyword = params.getKeyword().trim();
            List<Long> matchedUserIds = userMapper.selectList(new LambdaQueryWrapper<User>()
                            .select(User::getId)
                            .like(User::getNickname, keyword))
                    .stream()
                    .map(User::getId)
                    .toList();
            if (!matchedUserIds.isEmpty()) {
                queryWrapper.in(OrganizationMember::getUserId, matchedUserIds);
            } else {
                queryWrapper.eq(OrganizationMember::getId, -1L);
            }
        }
        return queryWrapper;
    }
}
