package online.longlian.app.service.orgadmin.impl;

import lombok.RequiredArgsConstructor;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.OrgAdminApplicationInfoResultBO;
import online.longlian.app.pojo.bo.OrgMemberInfoResultBO;
import online.longlian.app.pojo.entity.GroupApplication;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.generator.enumeration.ApplicationType;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 将实体列表转换为 BO 列表，负责批量回填关联数据（用户信息、头像 URL）。
 */
@Component
@RequiredArgsConstructor
public class MemberAssembler {

    private final UserMapper userMapper;
    private final ResourceService resourceService;

    public List<OrgMemberInfoResultBO> assembleMembers(List<OrganizationMember> members) {
        if (members.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查询关联用户，回填昵称、用户名等展示字段
        List<Long> userIds = members.stream()
                .map(OrganizationMember::getUserId)
                .distinct()
                .toList();
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds)
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        // 头像通过 ResourceService 批量获取 URL
        Map<Long, String> avatarUrlMap = resourceService.getResourceReadUrls(userMap.values().stream()
                        .map(User::getAvatarFileId)
                        .filter(fileId -> fileId != null && fileId > 0)
                        .distinct()
                        .toList())
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getUrl()));

        return members.stream()
                .map(member -> toMemberInfoResult(member, userMap, avatarUrlMap))
                .toList();
    }

    public List<OrgAdminApplicationInfoResultBO> assembleApplications(List<GroupApplication> applications) {
        if (applications.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> existingUserIds = applications.stream()
                .filter(app -> app.getApplicationType() == ApplicationType.EXISTING_USER)
                .map(GroupApplication::getUserId)
                .filter(userId -> userId != null && userId > 0)
                .distinct()
                .toList();

        Map<Long, User> existingUserMap = existingUserIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.selectBatchIds(existingUserIds)
                        .stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));

        Map<Long, String> avatarUrlMap = resourceService.getResourceReadUrls(existingUserMap.values().stream()
                        .map(User::getAvatarFileId)
                        .filter(fileId -> fileId != null && fileId > 0)
                        .distinct()
                        .toList())
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getUrl()));

        return applications.stream()
                .map(application -> toApplicationInfoResult(application, existingUserMap, avatarUrlMap))
                .toList();
    }

    private OrgMemberInfoResultBO toMemberInfoResult(OrganizationMember member, Map<Long, User> userMap, Map<Long, String> avatarUrlMap) {
        User user = userMap.get(member.getUserId());
        return OrgMemberInfoResultBO.builder()
                .id(member.getId())
                .userId(member.getUserId())
                .nickname(user != null ? user.getNickname() : null)
                .username(user != null ? user.getUsername() : null)
                .avatarUrl(user != null && user.getAvatarFileId() != null ? avatarUrlMap.get(user.getAvatarFileId()) : null)
                .joinedAt(member.getJoinedAt())
                .lastSubmittedAt(member.getLastSubmittedAt())
                .submitCount(member.getSubmitCount())
                .orgRole(member.getOrgRole())
                .status(member.getStatus())
                .build();
    }

    private OrgAdminApplicationInfoResultBO toApplicationInfoResult(GroupApplication application, Map<Long, User> existingUserMap, Map<Long, String> avatarUrlMap) {
        OrgAdminApplicationInfoResultBO.OrgAdminApplicationInfoResultBOBuilder builder = OrgAdminApplicationInfoResultBO.builder()
                .id(application.getId())
                .userId(application.getUserId())
                .appliedAt(application.getCreatedAt());
        User user = existingUserMap.get(application.getUserId());
        if (user != null) {
            builder.nickname(user.getNickname())
                    .username(user.getUsername());
            if (user.getAvatarFileId() != null) {
                builder.avatarUrl(avatarUrlMap.get(user.getAvatarFileId()));
            }
        } else {
            builder.nickname(application.getNickname())
                    .username(application.getUsername());
        }
        return builder.build();
    }
}
