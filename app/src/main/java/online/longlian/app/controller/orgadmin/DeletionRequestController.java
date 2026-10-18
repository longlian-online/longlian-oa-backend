package online.longlian.app.controller.orgadmin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.annotation.ResponseMessage;
import online.longlian.app.common.annotation.UserSession;
import online.longlian.app.common.enumeration.OrganizationDeclaration;
import online.longlian.app.common.resolver.SessionContext;
import online.longlian.app.pojo.bo.app.DeletionRequestInfoBO;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.dto.orgadmin.DeletionRequestListDTO;
import online.longlian.app.pojo.dto.orgadmin.DeletionRequestReviewDTO;
import online.longlian.app.pojo.vo.common.PageResultVO;
import online.longlian.app.pojo.vo.orgadmin.DeletionRequestInfoVO;
import online.longlian.app.service.app.DeletionRequestService;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "删除申请", description = "组织管理员审核企划和项目删除申请")
@RequestMapping("/orgadmin/deletion-requests")
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ORG_ADMIN')")
public class DeletionRequestController {

    private final DeletionRequestService deletionRequestService;

    @Operation(summary = "分页查询待审核删除申请", description = "仅返回待审核申请，按申请时间倒序")
    @GetMapping("")
    @ResponseMessage("查询成功")
    public PageResultVO<DeletionRequestInfoVO> listDeletionRequests(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
            @ModelAttribute @Valid DeletionRequestListDTO listDTO) {
        PageResultBO<DeletionRequestInfoBO> result = deletionRequestService.listPending(
                sessionContext.orgId(), listDTO.getPageNum(), listDTO.getPageSize());
        List<DeletionRequestInfoVO> list = result.getList().stream().map(bo -> {
            DeletionRequestInfoVO vo = new DeletionRequestInfoVO();
            BeanUtils.copyProperties(bo, vo);
            return vo;
        }).toList();
        return new PageResultVO<>(list, result.getTotal());
    }

    @Operation(summary = "审核删除申请", description = "通过后软删除对应企划或项目；拒绝则保留原数据")
    @Parameter(name = "requestId", description = "删除申请ID")
    @PatchMapping("/{requestId}")
    @ResponseMessage("审核完成")
    public void reviewDeletionRequest(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext,
            @PathVariable Long requestId,
            @RequestBody @Valid DeletionRequestReviewDTO reviewDTO) {
        deletionRequestService.review(requestId, sessionContext.orgId(), sessionContext.userId(), reviewDTO.getStatus());
    }
}
