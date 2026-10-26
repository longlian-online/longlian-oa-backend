package online.longlian.app.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.annotation.ResponseMessage;
import online.longlian.app.pojo.bo.admin.AdminOrganizationListParamsBO;
import online.longlian.app.pojo.bo.common.PageParamsBO;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.dto.admin.StorageStatsQueryDTO;
import online.longlian.app.pojo.vo.admin.StorageStatsVO;
import online.longlian.app.pojo.vo.common.PageResultVO;
import online.longlian.app.service.admin.StorageStatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "存储统计", description = "按组织查看存储占用")
@RequestMapping("/admin/storage-stats")
@RestController
@RequiredArgsConstructor
public class StorageStatsController {
    private final StorageStatsService storageStatsService;

    @Operation(summary = "分页查询组织存储统计")
    @GetMapping("/")
    @ResponseMessage("ok")
    public PageResultVO<StorageStatsVO> list(@Valid StorageStatsQueryDTO dto) {
        PageResultBO<StorageStatsVO> page = storageStatsService.list(new AdminOrganizationListParamsBO(
                dto.getOrgName(), null, null, new PageParamsBO(dto.getPageNum(), dto.getPageSize())));
        return new PageResultVO<>(page.getList(), page.getTotal());
    }
}
