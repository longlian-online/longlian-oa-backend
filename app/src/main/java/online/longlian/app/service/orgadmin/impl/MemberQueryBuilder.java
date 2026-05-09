package online.longlian.app.service.orgadmin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.enumeration.SortDirection;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.OrgAdminApplicationListParamsBO;
import online.longlian.app.pojo.bo.OrgMemberListParamsBO;
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

        if (params.getOrderDir() == SortDirection.ASC) {
            queryWrapper.orderByAsc(OrganizationMember::getJoinedAt);
        } else {
            queryWrapper.orderByDesc(OrganizationMember::getJoinedAt);
        }

        return queryWrapper;
    }
}
