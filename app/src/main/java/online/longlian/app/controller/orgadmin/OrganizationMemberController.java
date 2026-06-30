package online.longlian.app.controller.orgadmin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.annotation.ResponseMessage;
import online.longlian.app.common.annotation.UserSession;
import online.longlian.app.common.resolver.SessionContext;
import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberChangeStatusParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberInfoResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgAdminApplicationInfoResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgAdminApplicationListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgAdminGenerateJoinOrgInviteCodeParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgAdminGenerateJoinOrgInviteCodeResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgAdminReviewApplicationParamsBO;
import online.longlian.app.pojo.bo.common.PageParamsBO;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberBaseTaskSubmitCountParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberBaseTaskSubmitCountResultBO;
import online.longlian.app.pojo.dto.common.ChangeStatusDTO;
import online.longlian.app.pojo.dto.orgadmin.ApplicationListDTO;
import online.longlian.app.pojo.dto.orgadmin.ApplicationReviewDTO;
import online.longlian.app.pojo.dto.orgadmin.OrgMemberListDTO;
import online.longlian.app.pojo.vo.common.PageResultVO;
import online.longlian.app.pojo.vo.orgadmin.ApplicationInfoVO;
import online.longlian.app.pojo.vo.orgadmin.InviteCodeVO;
import online.longlian.app.pojo.vo.orgadmin.OrgMemberBaseTaskSubmitCountVO;
import online.longlian.app.pojo.vo.orgadmin.OrgMemberBaseTaskSubmitCountItemVO;
import online.longlian.app.pojo.vo.orgadmin.OrgMemberInfoVO;
import online.longlian.app.service.orgadmin.OrganizationMemberService;
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

    @Operation(
        summary = "分页查询待审核入组申请列表",
        description = "仅返回 status=PENDING(待审核) 的申请，默认按申请时间倒序"
    )
    @PostMapping("/applications")
    @ResponseMessage("查询成功")
    public PageResultVO<ApplicationInfoVO> listApplications(
            @UserSession(required = true) SessionContext sessionContext,
            @RequestBody @Valid ApplicationListDTO applicationListDTO) {
        PageResultBO<OrgAdminApplicationInfoResultBO> resultBO = organizationMemberService.listApplications(
                OrgAdminApplicationListParamsBO.builder()
                        .orgId(sessionContext.orgId())
                        .keyword(applicationListDTO.getKeyword())
                        .startApplyTime(applicationListDTO.getStartApplyTime())
                        .endApplyTime(applicationListDTO.getEndApplyTime())
                        .orderDir(applicationListDTO.getOrderDir())
                        .page(new PageParamsBO(applicationListDTO.getPageNum(), applicationListDTO.getPageSize()))
                        .build()
        );

        List<ApplicationInfoVO> applicationInfoVOS = resultBO.getList().stream()
                .map(bo -> {
                    ApplicationInfoVO applicationInfoVO = new ApplicationInfoVO();
                    BeanUtils.copyProperties(bo, applicationInfoVO);
                    return applicationInfoVO;
                })
                .toList();
        return new PageResultVO<>(applicationInfoVOS, resultBO.getTotal());
    }

    @Operation(
        summary = "审核入组申请",
        description = "管理员对单条申请执行通过(APPROVED)或拒绝(REJECTED)操作"
    )
    @Parameter(name = "applicationId", description = "入组申请ID")
    @PutMapping("/applications/{applicationId}/review")
    @ResponseMessage("审核完成")
    public void reviewApplication(@UserSession(required = true) SessionContext sessionContext,
                                   @PathVariable Long applicationId,
                                   @RequestBody @Valid ApplicationReviewDTO applicationReviewDTO) {
        organizationMemberService.reviewApplication(
                OrgAdminReviewApplicationParamsBO.builder()
                        .applicationId(applicationId)
                        .orgId(sessionContext.orgId())
                        .reviewerId(sessionContext.userId())
                        .applicationStatus(applicationReviewDTO.getApplicationStatus())
                        .reviewRemark(applicationReviewDTO.getReviewRemark())
                        .build()
        );
    }

    @Operation(
        summary = "分页查询组员列表",
        description = "仅返回已通过审核的成员，默认按入组时间倒序"
    )
    @PostMapping("")
    @ResponseMessage("查询成功")
    public PageResultVO<OrgMemberInfoVO> listMembers(
            @UserSession(required = true) SessionContext sessionContext,
            @RequestBody @Valid OrgMemberListDTO orgMemberListDTO) {
        PageResultBO<OrgMemberInfoResultBO> resultBO = organizationMemberService.listMembers(
                OrgMemberListParamsBO.builder()
                        .orgId(sessionContext.orgId())
                        .keyword(orgMemberListDTO.getKeyword())
                        .startJoinedTime(orgMemberListDTO.getStartJoinedTime())
                        .endJoinedTime(orgMemberListDTO.getEndJoinedTime())
                        .orderDir(orgMemberListDTO.getOrderDir())
                        .page(new PageParamsBO(orgMemberListDTO.getPageNum(), orgMemberListDTO.getPageSize()))
                        .build()
        );

        List<OrgMemberInfoVO> memberInfoVOS = resultBO.getList().stream()
                .map(bo -> {
                    OrgMemberInfoVO orgMemberInfoVO = new OrgMemberInfoVO();
                    BeanUtils.copyProperties(bo, orgMemberInfoVO);
                    return orgMemberInfoVO;
                })
                .toList();
        return new PageResultVO<>(memberInfoVOS, resultBO.getTotal());
    }

    @Operation(
        summary = "查询组员各原子任务提交数"
    )
    @Parameter(name = "memberId", description = "成员ID")
    @GetMapping("/{memberId}/base-tasks/submit-counts")
    @ResponseMessage("查询成功")
    public OrgMemberBaseTaskSubmitCountVO getMemberBaseTaskSubmitCounts(
            @UserSession(required = true) SessionContext sessionContext,
            @PathVariable Long memberId) {
        OrgMemberBaseTaskSubmitCountResultBO resultBO = organizationMemberService.getMemberBaseTaskSubmitCounts(
                OrgMemberBaseTaskSubmitCountParamsBO.builder()
                        .memberId(memberId)
                        .orgId(sessionContext.orgId())
                        .build()
        );

        return OrgMemberBaseTaskSubmitCountVO.builder()
                .memberId(resultBO.getMemberId())
                .userId(resultBO.getUserId())
                .totalSubmitCount(resultBO.getTotalSubmitCount())
                .list(resultBO.getItems().stream()
                        .map(item -> OrgMemberBaseTaskSubmitCountItemVO.builder()
                                .baseTaskId(item.getBaseTaskId())
                                .baseTaskName(item.getBaseTaskName())
                                .submitCount(item.getSubmitCount())
                                .build())
                        .toList())
                .build();
    }

    @Operation(
            summary = "启用/禁用组员",
            description = "禁用后用户无法登录；超管身份不可被禁用。status: ENABLED-启用，DISABLED-禁用"
    )
    @PatchMapping("/{memberId}/status")
    public Result<Void> changeMemberStatus(@UserSession(required = true) SessionContext sessionContext,
                                            @PathVariable Long memberId,
                                            @RequestBody @Valid ChangeStatusDTO changeStatusDTO) {
        organizationMemberService.changeMemberStatus(
                OrgMemberChangeStatusParamsBO.builder()
                        .orgId(sessionContext.orgId())
                        .memberId(memberId)
                        .status(changeStatusDTO.getStatus())
                        .build()
        );
        return Result.success(null);
    }

    @Operation(
        summary = "生成加入组织邀请码（管理员）",
        description = "生成一次性邀请码（6位字母数字），有效期30分钟；供组织管理员邀请用户加入当前组织使用"
    )
    @PostMapping("/invite-codes/join-org")
    @ResponseMessage("生成成功")
    public InviteCodeVO generateJoinOrgInviteCode(
            @UserSession(required = true) SessionContext sessionContext) {
        OrgAdminGenerateJoinOrgInviteCodeResultBO resultBO = organizationMemberService.generateJoinOrgInviteCode(
                new OrgAdminGenerateJoinOrgInviteCodeParamsBO(sessionContext.userId(), sessionContext.orgId())
        );
        return InviteCodeVO.builder()
                .inviteCode(resultBO.getInviteCode())
                .expireAt(resultBO.getExpireAt())
                .build();
    }
}
