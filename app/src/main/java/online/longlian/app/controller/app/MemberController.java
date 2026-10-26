package online.longlian.app.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.annotation.ResponseMessage;
import online.longlian.app.common.annotation.UserSession;
import online.longlian.app.common.enumeration.OrganizationDeclaration;
import online.longlian.app.common.resolver.SessionContext;
import online.longlian.app.pojo.vo.app.MemberOptionVO;
import online.longlian.app.service.common.OrganizationMembershipService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "组织组员选项", description = "当前组织可供选择的启用组员，不绑定具体业务")
@RequestMapping("/app/members")
@RestController("appMemberController")
@RequiredArgsConstructor
public class MemberController {
    private final OrganizationMembershipService organizationMembershipService;

    @Operation(summary = "查询当前组织组员", description = "返回当前组织全部启用组员，按昵称、用户 ID 排序")
    @GetMapping("")
    @ResponseMessage("查询成功")
    public List<MemberOptionVO> listMembers(
            @UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext) {
        return organizationMembershipService.listEnabledMembers(sessionContext.orgId()).stream()
                .map(member -> MemberOptionVO.builder()
                        .userId(member.userId())
                        .nickname(member.nickname())
                        .build())
                .toList();
    }
}
