package online.longlian.app.service.app.impl.deletion;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.DeletionRequestMapper;
import online.longlian.app.pojo.bo.app.DeletionRequestInfoBO;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.entity.DeletionRequest;
import online.longlian.app.service.app.DeletionRequestService;
import online.longlian.app.service.app.ItemService;
import online.longlian.app.service.app.ProjectService;
import online.longlian.app.service.app.UserService;
import online.longlian.common.enumeration.DeletionRequestStatus;
import online.longlian.common.enumeration.DeletionTargetType;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DeletionRequestServiceImpl implements DeletionRequestService {

    private final DeletionRequestMapper deletionRequestMapper;
    private final ProjectService projectService;
    private final ItemService itemService;
    private final UserService userService;
    private final Clock clock;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createProjectRequest(Long projectId, Long userId, Long orgId, String reason) {
        String title = projectService.requireCreatorProjectTitle(projectId, userId, orgId);
        if (!itemService.hasProjectItems(projectId)) {
            throw new AppException(ResultCode.PARAM_ERROR, "企划没有项目，可直接删除");
        }
        insert(DeletionTargetType.PROJECT, projectId, projectId, title, userId, orgId, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createItemRequest(Long projectId, Long itemId, Long userId, Long orgId, String reason) {
        projectService.requireCreatorProjectTitle(projectId, userId, orgId);
        String title = itemService.requireItemTitle(projectId, itemId);
        if (!itemService.hasSubmittedTask(itemId)) {
            throw new AppException(ResultCode.PARAM_ERROR, "项目没有已提交任务，可直接删除");
        }
        insert(DeletionTargetType.ITEM, itemId, projectId, title, userId, orgId, reason);
    }

    @Override
    public PageResultBO<DeletionRequestInfoBO> listPending(Long orgId, int pageNum, int pageSize) {
        Page<DeletionRequest> page = deletionRequestMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<DeletionRequest>()
                        .eq(DeletionRequest::getOrgId, orgId)
                        .eq(DeletionRequest::getStatus, DeletionRequestStatus.PENDING)
                        .orderByDesc(DeletionRequest::getCreatedAt)
                        .orderByDesc(DeletionRequest::getId));
        List<DeletionRequest> requests = page.getRecords();
        if (requests.isEmpty()) {
            return new PageResultBO<>(Collections.emptyList(), page.getTotal());
        }
        Map<Long, String> requesterNames = userService.displayNames(
                requests.stream().map(DeletionRequest::getRequesterId).distinct().toList());
        List<DeletionRequestInfoBO> list = requests.stream()
                .map(request -> DeletionRequestInfoBO.builder()
                        .id(request.getId())
                        .targetType(request.getTargetType())
                        .targetId(request.getTargetId())
                        .projectId(request.getProjectId())
                        .title(request.getTargetTitle())
                        .reason(request.getReason())
                        .requesterName(requesterNames.get(request.getRequesterId()))
                        .createdAt(request.getCreatedAt())
                        .build())
                .toList();
        return new PageResultBO<>(list, page.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void review(Long requestId, Long orgId, Long reviewerId, DeletionRequestStatus status) {
        if (status != DeletionRequestStatus.APPROVED && status != DeletionRequestStatus.REJECTED) {
            throw new AppException(ResultCode.PARAM_ERROR, "审核结果只能为通过或拒绝");
        }
        DeletionRequest request = deletionRequestMapper.selectById(requestId);
        if (request == null || !orgId.equals(request.getOrgId())) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "删除申请不存在");
        }
        if (request.getStatus() != DeletionRequestStatus.PENDING) {
            throw new AppException(ResultCode.OPERATION_FAIL, "该申请已审核");
        }
        if (status == DeletionRequestStatus.APPROVED) {
            apply(request, orgId);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        int updated = deletionRequestMapper.update(null, new LambdaUpdateWrapper<DeletionRequest>()
                .eq(DeletionRequest::getId, requestId)
                .eq(DeletionRequest::getStatus, DeletionRequestStatus.PENDING)
                .set(DeletionRequest::getStatus, status)
                .set(DeletionRequest::getReviewerId, reviewerId)
                .set(DeletionRequest::getReviewedAt, now)
                .set(DeletionRequest::getUpdatedAt, now));
        if (updated != 1) {
            throw new AppException(ResultCode.OPERATION_FAIL, "该申请已审核");
        }
    }

    private void apply(DeletionRequest request, Long orgId) {
        if (request.getTargetType() == DeletionTargetType.PROJECT) {
            itemService.deleteProjectItemsForReview(request.getTargetId(), orgId);
            projectService.deleteProjectForReview(request.getTargetId(), orgId);
            return;
        }
        itemService.deleteProjectItemForReview(request.getProjectId(), request.getTargetId(), orgId);
    }

    private void insert(DeletionTargetType targetType, Long targetId, Long projectId, String title,
                        Long userId, Long orgId, String reason) {
        LocalDateTime now = LocalDateTime.now(clock);
        DeletionRequest request = DeletionRequest.builder()
                .orgId(orgId)
                .targetType(targetType)
                .targetId(targetId)
                .projectId(projectId)
                .targetTitle(title == null ? "" : title)
                .reason(reason.trim())
                .requesterId(userId)
                .status(DeletionRequestStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();
        try {
            deletionRequestMapper.insert(request);
        } catch (DuplicateKeyException exception) {
            throw new AppException(ResultCode.OPERATION_FAIL, "已提交删除申请，请等待审核");
        }
    }
}
