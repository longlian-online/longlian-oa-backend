package online.longlian.app.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.constants.InviteConstants;
import online.longlian.app.common.enumeration.SortDirection;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.util.RandomCodeUtil;
import online.longlian.app.mapper.GroupApplicationMapper;
import online.longlian.app.mapper.OrganizationJoinOtpMapper;
import online.longlian.app.mapper.OrganizationMapper;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.OneTimePasswordCreateParamsBO;
import online.longlian.app.pojo.bo.OrgAdminApplicationInfoResultBO;
import online.longlian.app.pojo.bo.OrgAdminApplicationListParamsBO;
import online.longlian.app.pojo.bo.OrgAdminGenerateJoinOrgInviteCodeParamsBO;
import online.longlian.app.pojo.bo.OrgAdminGenerateJoinOrgInviteCodeResultBO;
import online.longlian.app.pojo.bo.OrgAdminReviewApplicationParamsBO;
import online.longlian.app.pojo.bo.PageResultBO;
import online.longlian.app.pojo.entity.GroupApplication;
import online.longlian.app.pojo.entity.OneTimePassword;
import online.longlian.app.pojo.entity.Organization;
import online.longlian.app.pojo.entity.OrganizationJoinOtp;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.service.common.OneTimePasswordService;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.app.service.user.OrganizationMemberService;
import online.longlian.generator.enumeration.ApplicationStatus;
import online.longlian.generator.enumeration.ApplicationType;
import online.longlian.generator.enumeration.OTPType;
import online.longlian.generator.enumeration.Status;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrganizationMemberServiceImpl implements OrganizationMemberService {

    private static final DateTimeFormatter DEFAULT_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(InviteConstants.DEFAULT_DATE_TIME_PATTERN);

    private final OrganizationMapper organizationMapper;
    private final UserMapper userMapper;
    private final GroupApplicationMapper groupApplicationMapper;
    private final OrganizationMemberMapper organizationMemberMapper;
    private final OneTimePasswordService oneTimePasswordService;
    private final OrganizationJoinOtpMapper organizationJoinOtpMapper;
    private final ResourceService resourceService;

    @Override
    public PageResultBO<OrgAdminApplicationInfoResultBO> listApplications(@NonNull OrgAdminApplicationListParamsBO params) {
        LambdaQueryWrapper<GroupApplication> queryWrapper = new LambdaQueryWrapper<GroupApplication>()
                .eq(GroupApplication::getOrgId, params.getOrgId())
                .eq(GroupApplication::getStatus, ApplicationStatus.PENDING)
                .ge(params.getStartApplyTime() != null, GroupApplication::getCreatedAt, params.getStartApplyTime())
                .le(params.getEndApplyTime() != null, GroupApplication::getCreatedAt, params.getEndApplyTime());

        if (StringUtils.hasText(params.getKeyword())) {
            String keyword = params.getKeyword().trim();
            List<Long> matchedUserIds = userMapper.selectList(new LambdaQueryWrapper<User>()
                            .select(User::getId)
                            .like(User::getNickname, keyword))
                    .stream()
                    .map(User::getId)
                    .toList();
            queryWrapper.and(wrapper -> {
                wrapper.eq(GroupApplication::getApplicationType, ApplicationType.REGISTER)
                        .like(GroupApplication::getNickname, keyword);
                if (!matchedUserIds.isEmpty()) {
                    wrapper.or(nested -> nested
                            .eq(GroupApplication::getApplicationType, ApplicationType.EXISTING_USER)
                            .in(GroupApplication::getUserId, matchedUserIds));
                }
            });
        }

        if (params.getOrderDir() == SortDirection.ASC) {
            queryWrapper.orderByAsc(GroupApplication::getCreatedAt);
        } else {
            queryWrapper.orderByDesc(GroupApplication::getCreatedAt);
        }

        Page<GroupApplication> page = new Page<>(params.getPage().getPageNum(), params.getPage().getPageSize());
        Page<GroupApplication> applicationPage = groupApplicationMapper.selectPage(page, queryWrapper);
        List<GroupApplication> applications = applicationPage.getRecords();
        if (applications.isEmpty()) {
            return new PageResultBO<>(Collections.emptyList(), applicationPage.getTotal());
        }

        Map<Long, User> existingUserMap = userMapper.selectBatchIds(applications.stream()
                        .filter(application -> application.getApplicationType() == ApplicationType.EXISTING_USER)
                        .map(GroupApplication::getUserId)
                        .filter(userId -> userId != null && userId > 0)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, String> avatarUrlMap = resourceService.getResourceReadUrls(existingUserMap.values().stream()
                .map(User::getAvatarFileId)
                .filter(fileId -> fileId != null && fileId > 0)
                .distinct()
                .toList())
                // 返回的对象中包含其他元信息，这里只取 url
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getUrl()));

        List<OrgAdminApplicationInfoResultBO> list = applications.stream()
                .map(application -> toApplicationInfoResult(application, existingUserMap, avatarUrlMap))
                .toList();
        return new PageResultBO<>(list, applicationPage.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewApplication(@NonNull OrgAdminReviewApplicationParamsBO params) {
        GroupApplication application = groupApplicationMapper.selectById(params.getApplicationId());
        if (application == null || !params.getOrgId().equals(application.getOrgId())) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "入组申请不存在");
        }
        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new AppException(ResultCode.OPERATION_FAIL, "该申请已审核");
        }

        LocalDateTime now = LocalDateTime.now();
        Long approvedUserId = null;
        if (params.getApplicationStatus() == ApplicationStatus.APPROVED) {
            approvedUserId = approveApplication(application, now);
        } else if (params.getApplicationStatus() == ApplicationStatus.REJECTED) {
            rejectApplication(application, now);
        }

        LambdaUpdateWrapper<GroupApplication> updateWrapper = new LambdaUpdateWrapper<GroupApplication>()
                .eq(GroupApplication::getId, application.getId())
                .eq(GroupApplication::getStatus, ApplicationStatus.PENDING)
                .set(GroupApplication::getStatus, params.getApplicationStatus())
                .set(GroupApplication::getReviewerId, params.getReviewerId())
                .set(GroupApplication::getReviewedAt, now)
                .set(GroupApplication::getReviewRemark, params.getReviewRemark() == null ? "" : params.getReviewRemark())
                .set(GroupApplication::getUpdatedAt, now);
        if (approvedUserId != null) {
            updateWrapper.set(GroupApplication::getUserId, approvedUserId);
        }
        groupApplicationMapper.update(null, updateWrapper);
    }

    @Override
    public OrgAdminGenerateJoinOrgInviteCodeResultBO generateJoinOrgInviteCode(@NonNull OrgAdminGenerateJoinOrgInviteCodeParamsBO params) {
        Organization organization = getEnabledOrganization(params.getOrgId());
        LocalDateTime expiredAt = LocalDateTime.now().plusMinutes(InviteConstants.INVITE_EXPIRE_MINUTES);
        String inviteCode = RandomCodeUtil.generateCode(InviteConstants.INVITE_CODE_LENGTH);

        OneTimePassword oneTimePassword = oneTimePasswordService.generateOTP(
                OneTimePasswordCreateParamsBO.builder()
                        .code(inviteCode)
                        .expiredAt(expiredAt)
                        .bizType(OTPType.OrganizationUserInvite)
                        .creatorId(params.getCreatorId())
                        .build()
        );

        OrganizationJoinOtp organizationJoinOtp = OrganizationJoinOtp.builder()
                .otpId(oneTimePassword.getId())
                .orgId(organization.getId())
                .build();
        organizationJoinOtpMapper.insert(organizationJoinOtp);

        return OrgAdminGenerateJoinOrgInviteCodeResultBO.builder()
                .inviteCode(inviteCode)
                .expireAt(expiredAt.format(DEFAULT_DATE_TIME_FORMATTER))
                .build();
    }

    private Long approveApplication(GroupApplication application, LocalDateTime now) {
        User user = switch (application.getApplicationType()) {
            case REGISTER -> createUserByApplication(application, now);
            case EXISTING_USER -> getExistingApplicationUser(application);
        };

        OrganizationMember organizationMember = OrganizationMember.builder()
                .orgId(application.getOrgId())
                .userId(user.getId())
                .orgRole(InviteConstants.ROLE_ORG_USER)
                .joinedAt(now)
                .submitCount(0)
                .status(Status.ENABLED)
                .createdAt(now)
                .updatedAt(now)
                .build();
        organizationMemberMapper.insert(organizationMember);

        return user.getId();
    }

    private User createUserByApplication(GroupApplication application, LocalDateTime now) {
        User user = User.builder()
                .username(application.getUsername())
                .password(application.getPassword())
                .nickname(application.getNickname())
                .email(application.getEmail())
                .status(Status.ENABLED)
                .defaultOrgId(application.getOrgId())
                .createdAt(now)
                .updatedAt(now)
                .build();
        userMapper.insert(user);
        return user;
    }

    private User getExistingApplicationUser(GroupApplication application) {
        User user = userMapper.selectById(application.getUserId());
        if (user == null) {
            throw new AppException(ResultCode.USER_NOT_EXIT);
        }
        if (user.getStatus() == Status.DISABLED) {
            throw new AppException(ResultCode.OPERATION_FAIL, "申请人已被禁用");
        }
        OrganizationMember existedMember = organizationMemberMapper.selectOne(new LambdaQueryWrapper<OrganizationMember>()
                .eq(OrganizationMember::getOrgId, application.getOrgId())
                .eq(OrganizationMember::getUserId, user.getId())
                .last("LIMIT 1"));
        if (existedMember != null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "申请人已加入该组织");
        }
        return user;
    }

    private void assertCanJoinOrganization(User user, Long orgId) {

    }

    private void rejectApplication(GroupApplication application, LocalDateTime now) {

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

    private Organization getEnabledOrganization(Long orgId) {
        Organization organization = organizationMapper.selectById(orgId);
        if (organization == null || organization.getStatus() == Status.DISABLED) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "组织不存在或已禁用");
        }
        return organization;
    }
}
