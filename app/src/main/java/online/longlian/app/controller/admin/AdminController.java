package online.longlian.app.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.bo.AdminCreateParamsBO;
import online.longlian.app.pojo.bo.AdminListParamsBO;
import online.longlian.app.pojo.bo.AdminListResultBO;
import online.longlian.app.pojo.bo.PageResultBO;
import online.longlian.app.pojo.bo.PageParamsBO;
import online.longlian.app.pojo.dto.admin.AdminCreateDTO;
import online.longlian.app.pojo.dto.admin.AdminListDTO;
import online.longlian.app.pojo.vo.admin.AdminVO;
import online.longlian.app.pojo.vo.common.PageResultVO;
import online.longlian.app.service.admin.AdminManagementService;
import online.longlian.app.service.admin.AdminSessionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "管理员接口", description = "管理员相关接口")
@RequestMapping("/admin/admins")
@RestController
@RequiredArgsConstructor
public class AdminController {

    private final AdminManagementService adminManagementService;
    private final AdminSessionService adminSessionService;

    @Operation(summary = "创建管理员")
    @PostMapping("/")
    public Result<Long> create(@RequestBody @Valid AdminCreateDTO dto) {
        Long adminId = adminManagementService.create(
                AdminCreateParamsBO.builder()
                        .username(dto.getUsername())
                        .password(dto.getPassword())
                        .build(),
                adminSessionService.getCurrentAdminId()
        );
        return Result.success("创建成功", adminId);
    }

    @Operation(summary = "删除管理员")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminManagementService.delete(id, adminSessionService.getCurrentAdminId());
        return Result.success("删除成功");
    }

    @Operation(summary = "分页查询管理员列表")
    @GetMapping("/")
    public Result<PageResultVO<AdminVO>> list(@RequestBody @Valid AdminListDTO dto) {
        AdminListParamsBO bo = AdminListParamsBO.builder()
                .page(new PageParamsBO(dto.getPageNum(), dto.getPageSize()))
                .build();

        PageResultBO<AdminListResultBO> adminPage = adminManagementService.list(bo);

        List<AdminVO> list = adminPage
                .getList()
                .stream()
                .map(admin -> new AdminVO(
                        admin.getId(),
                        admin.getUsername(),
                        admin.getRole(),
                        admin.getLastLoginAt(),
                        admin.getCreatedAt())
                )
                .toList();

        PageResultVO<AdminVO> pageResultVO = new PageResultVO<>(list, adminPage.getTotal());

        return Result.success("ok", pageResultVO);
    }
}
