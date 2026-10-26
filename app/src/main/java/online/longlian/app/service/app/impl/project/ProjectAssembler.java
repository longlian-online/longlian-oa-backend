package online.longlian.app.service.app.impl.project;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.mapper.ProjectTypeMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.app.ProjectListResultBO;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.pojo.entity.Project;
import online.longlian.app.pojo.entity.ProjectType;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.service.resource.ResourceService;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProjectAssembler {

    /** 项目列表封面按 EdgeOne 图片即时处理参数缩放到 400x600。 */
    private static final String COVER_RESIZE_PARAM = "eo-img.resize=w/400/h/600";

    private final ProjectTypeMapper projectTypeMapper;
    private final UserMapper userMapper;
    private final OrganizationMemberMapper organizationMemberMapper;
    private final ResourceService resourceService;

    public List<ProjectListResultBO> assembleList(List<Project> projects, Long orgId) {
        if (projects.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> typeIds = projects.stream().map(Project::getTypeId).distinct().toList();
        Map<Long, String> typeNameMap = projectTypeMapper.selectBatchIds(typeIds).stream()
                .collect(Collectors.toMap(ProjectType::getId, ProjectType::getName));

        List<Long> creatorIds = projects.stream().map(Project::getCreatorId).distinct().toList();
        Map<Long, User> userMap = userMapper.selectBatchIds(creatorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        Map<Long, OrganizationMember> memberMap = organizationMemberMapper.selectList(
                new LambdaQueryWrapper<OrganizationMember>()
                        .select(OrganizationMember::getUserId, OrganizationMember::getLastSubmittedAt)
                        .eq(OrganizationMember::getOrgId, orgId)
                        .in(OrganizationMember::getUserId, creatorIds)
        ).stream().collect(Collectors.toMap(OrganizationMember::getUserId, Function.identity()));

        List<Long> coverFileIds = projects.stream()
                .map(Project::getCoverFileId).filter(id -> id != null && id > 0).distinct().toList();
        Map<Long, String> coverUrlMap = coverFileIds.isEmpty()
                ? Collections.emptyMap()
                : resourceService.getResourceReadUrls(coverFileIds).entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getUrl()));

        List<Long> avatarFileIds = userMap.values().stream()
                .map(User::getAvatarFileId).filter(id -> id != null && id > 0).distinct().toList();
        Map<Long, String> avatarUrlMap = avatarFileIds.isEmpty()
                ? Collections.emptyMap()
                : resourceService.getResourceReadUrls(avatarFileIds).entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getUrl()));

        return projects.stream()
                .map(project -> {
                    User creator = userMap.get(project.getCreatorId());
                    OrganizationMember member = memberMap.get(project.getCreatorId());
                    String avatarUrl = null;
                    if (creator != null && creator.getAvatarFileId() != null) {
                        avatarUrl = avatarUrlMap.get(creator.getAvatarFileId());
                    }
                    return ProjectListResultBO.builder()
                            .id(project.getId())
                            .title(project.getTitle())
                            .alias(project.getAlias())
                            .description(project.getDescription())
                            .coverUrl(resizeCoverUrl(coverUrlMap.get(project.getCoverFileId())))
                            .projectType(typeNameMap.get(project.getTypeId()))
                            .projectStatus(project.getStatus())
                            .metadata(project.getMetadata())
                            .creatorAvatarUrl(avatarUrl)
                            .creatorNickname(creator != null ? creator.getNickname() : null)
                            .creatorLastSubmittedAt(member != null ? member.getLastSubmittedAt() : null)
                            .build();
                })
                .toList();
    }

    private String resizeCoverUrl(String url) {
        if (url == null) {
            return null;
        }
        return url + (url.contains("?") ? "&" : "?") + COVER_RESIZE_PARAM;
    }
}
