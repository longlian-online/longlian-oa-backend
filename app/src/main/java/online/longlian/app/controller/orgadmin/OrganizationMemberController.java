package online.longlian.app.controller.orgadmin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.bo.OrgAdminApplicationInfoResultBO;
import online.longlian.app.pojo.bo.OrgAdminApplicationListParamsBO;
import online.longlian.app.pojo.bo.OrgAdminGenerateJoinOrgInviteCodeParamsBO;
import online.longlian.app.pojo.bo.OrgAdminGenerateJoinOrgInviteCodeResultBO;
import online.longlian.app.pojo.bo.OrgAdminReviewApplicationParamsBO;
import online.longlian.app.pojo.bo.PageParamsBO;
import online.longlian.app.pojo.bo.PageResultBO;
import online.longlian.app.pojo.dto.common.ChangeStatusDTO;
import online.longlian.app.pojo.dto.orgadmin.ApplicationListDTO;
import online.longlian.app.pojo.dto.orgadmin.ApplicationReviewDTO;
import online.longlian.app.pojo.dto.orgadmin.OrgMemberListDTO;
import online.longlian.app.pojo.vo.common.PageResultVO;
import online.longlian.app.pojo.vo.orgadmin.ApplicationInfoVO;
import online.longlian.app.pojo.vo.orgadmin.InviteCodeVO;
import online.longlian.app.pojo.vo.orgadmin.OrgMemberBaseTaskSubmitCountVO;
import online.longlian.app.pojo.vo.orgadmin.OrgMemberInfoVO;
import online.longlian.app.service.common.CurrentOrganizationService;
import online.longlian.app.service.user.OrganizationMemberService;
import online.longlian.app.service.user.SessionService;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "组织成员管理", description = "组织成员管理：入组申请审核、组员列表、邀请")
@RequestMapping("/orgadmin/members")
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ORG_ADMIN')")
public class OrganizationMemberController {

    private final OrganizationMemberService organizationMemberService;
    private final SessionService sessionService;
    private final CurrentOrganizationService currentOrganizationService;

    // -------------------------
    // 入组申请
    // -------------------------

    @Operation(
        summary = "分页查询待审核入组申请列表",
        description = "仅返回 status=PENDING(待审核) 的申请，默认按申请时间倒序"
    )
    @PostMapping("/applications")
    public Result<PageResultVO<ApplicationInfoVO>> listApplications(
            @RequestBody @Valid ApplicationListDTO applicationListDTO) {
        Long currentUserId = sessionService.getCurrentUserId();
        Long currentOrgId = currentOrganizationService.requireCurrentOrgId(currentUserId);

        PageResultBO<OrgAdminApplicationInfoResultBO> resultBO = organizationMemberService.listApplications(
                OrgAdminApplicationListParamsBO.builder()
                        .orgId(currentOrgId)
                        .keyword(applicationListDTO.getKeyword())
                        .startApplyTime(applicationListDTO.getStartApplyTime())
                        .endApplyTime(applicationListDTO.getEndApplyTime())
                        .orderDir(applicationListDTO.getOrderDir())
                        .page(new PageParamsBO(applicationListDTO.getPageNum(), applicationListDTO.getPageSize()))
                        .build()
        );

        List<ApplicationInfoVO> applicationInfoVOS = resultBO.getList().stream()
                .map(bo -> {
                    ApplicationInfoVO vo = new ApplicationInfoVO();
                    BeanUtils.copyProperties(bo, vo);
                    return vo;
                })
                .toList();
        return Result.success("查询成功", new PageResultVO<>(applicationInfoVOS, resultBO.getTotal()));
    }

    @Operation(
        summary = "审核入组申请",
        description = "管理员对单条申请执行通过(APPROVED)或拒绝(REJECTED)操作"
    )
    @Parameter(name = "applicationId", description = "入组申请ID")
    @PutMapping("/applications/{applicationId}/review")
    public Result<Void> reviewApplication(
            @PathVariable Long applicationId,
            @RequestBody @Valid ApplicationReviewDTO applicationReviewDTO) {
        Long currentUserId = sessionService.getCurrentUserId();
        Long currentOrgId = currentOrganizationService.requireCurrentOrgId(currentUserId);

        organizationMemberService.reviewApplication(
                OrgAdminReviewApplicationParamsBO.builder()
                        .applicationId(applicationId)
                        .orgId(currentOrgId)
                        .reviewerId(currentUserId)
                        .applicationStatus(applicationReviewDTO.getApplicationStatus())
                        .reviewRemark(applicationReviewDTO.getReviewRemark())
                        .build()
        );
        return Result.success("审核完成");
    }

    // -------------------------
    // 组员列表
    // -------------------------

    @Operation(
        summary = "分页查询组员列表",
        description = "仅返回已通过审核的成员，默认按入组时间倒序"
    )
    @PostMapping("")
    public Result<PageResultVO<OrgMemberInfoVO>> listMembers(
            @RequestBody @Valid OrgMemberListDTO orgMemberListDTO) {
        // TODO
        // return organizationMemberService.listMembers(orgMemberListDTO);
        return Result.success("查询成功", null);
    }

    @Operation(
        summary = "查询组员各原子任务提交数"
    )
    @Parameter(name = "memberId", description = "成员ID")
    @GetMapping("/{memberId}/base-tasks/submit-counts")
    public Result<OrgMemberBaseTaskSubmitCountVO> getMemberBaseTaskSubmitCounts(@PathVariable Long memberId) {
        // TODO
        // return organizationMemberService.getMemberBaseTaskSubmitCounts(memberId);
        return Result.success("查询成功", null);
    }

    @Operation(
            summary = "启用/禁用组员",
            description = "禁用后用户无法登录；超管身份不可被禁用。status: ENABLED-启用，DISABLED-禁用"
    )
    @PatchMapping("/{memberId}/status")
    public Result<Void> changeMemberStatus(@PathVariable Long memberId, @RequestBody @Valid ChangeStatusDTO changeStatusDTO) {
        // TODO
        // organizationMemberService.changeMemberStatus(changeStatusDTO.getStatus());
        return Result.success(null);
    }

    // -------------------------
    // 邀请（管理员生成）
    // -------------------------
    @Operation(
        summary = "生成加入组织邀请码（管理员）",
        description = "生成一次性邀请码（6位字母数字），有效期30分钟；供组织管理员邀请用户加入当前组织使用"
    )
    @PostMapping("/invite-codes/join-org")
    public Result<InviteCodeVO> generateJoinOrgInviteCode() {
        Long currentUserId = sessionService.getCurrentUserId();
        Long currentOrgId = currentOrganizationService.requireCurrentOrgId(currentUserId);

        OrgAdminGenerateJoinOrgInviteCodeResultBO resultBO = organizationMemberService.generateJoinOrgInviteCode(
                new OrgAdminGenerateJoinOrgInviteCodeParamsBO(currentUserId, currentOrgId)
        );
        InviteCodeVO inviteCodeVO = InviteCodeVO.builder()
                .inviteCode(resultBO.getInviteCode())
                .expireAt(resultBO.getExpireAt())
                .build();
        return Result.success("生成成功", inviteCodeVO);
    }
}
