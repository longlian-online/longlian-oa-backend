package online.longlian.app.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.dto.ApplicationListDTO;
import online.longlian.app.pojo.dto.ApplicationReviewDTO;
import online.longlian.app.pojo.dto.OrgMemberListDTO;
import online.longlian.app.pojo.vo.ApplicationInfoVO;
import online.longlian.app.pojo.vo.InviteLinkVO;
import online.longlian.app.pojo.vo.OrgMemberInfoVO;
import online.longlian.app.pojo.vo.PageResultVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "组织成员管理接口", description = "组织成员管理：入组申请审核、组员列表、邀请")
@RequestMapping("/app/org/member")
@RestController
@RequiredArgsConstructor
public class OrganizationMemberController {

    // private final OrganizationMemberService organizationMemberService;

    // -------------------------
    // 入组申请
    // -------------------------

    @Operation(
        summary = "分页查询待审核入组申请列表",
        description = "仅返回 status=PENDING(待审核) 的申请，默认按申请时间倒序"
    )
    @PostMapping("/applications")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public Result<PageResultVO<ApplicationInfoVO>> listApplications(
            @RequestBody @Valid ApplicationListDTO applicationListDTO) {
        // TODO
        // return organizationMemberService.listApplications(applicationListDTO);
        return Result.success("查询成功", null);
    }

    @Operation(
        summary = "审核入组申请",
        description = "管理员对单条申请执行通过(APPROVED)或拒绝(REJECTED)操作"
    )
    @Parameter(name = "applicationId", description = "入组申请ID")
    @PutMapping("/applications/{applicationId}/review")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public Result<Void> reviewApplication(
            @PathVariable Long applicationId,
            @RequestBody @Valid ApplicationReviewDTO applicationReviewDTO) {
        // TODO
        // return organizationMemberService.reviewApplication(applicationId, applicationReviewDTO);
        return Result.success("审核完成");
    }

    // -------------------------
    // 组员列表
    // -------------------------

    @Operation(
        summary = "分页查询组员列表",
        description = "仅返回已通过审核的成员，默认按入组时间倒序"
    )
    @PostMapping("/list")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public Result<PageResultVO<OrgMemberInfoVO>> listMembers(
            @RequestBody @Valid OrgMemberListDTO orgMemberListDTO) {
        // TODO
        // return organizationMemberService.listMembers(orgMemberListDTO);
        return Result.success("查询成功", null);
    }

    @Operation(
        summary = "启用/禁用组员",
        description = "禁用后用户无法登录；超管身份不可被禁用。status: 1-启用，0-禁用"
    )
    @Parameter(name = "memberId", description = "成员记录ID（organization_member.id）")
    @Parameter(name = "status", description = "目标状态：1-启用，0-禁用")
    @PatchMapping("/{memberId}/status")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public Result<Void> changeMemberStatus(
            @PathVariable Long memberId,
            @RequestParam Integer status) {
        // TODO
        // return organizationMemberService.changeMemberStatus(memberId, status);
        return Result.success(null);
    }

    // -------------------------
    // 邀请
    // -------------------------

    @Operation(
        summary = "生成邀请链接",
        description = "生成一次性邀请链接和邀请码，有效期 30 分钟。新用户通过链接注册后仍需在入组申请页审核"
    )
    @PostMapping("/invite")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public Result<InviteLinkVO> generateInviteLink() {
        // TODO
        // return organizationMemberService.generateInviteLink();
        return Result.success("生成成功", null);
    }
}
