package online.longlian.app.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.annotation.ResponseMessage;
import online.longlian.app.pojo.bo.admin.AdminOrganizationListParamsBO;
import online.longlian.app.pojo.bo.admin.StorageCategoryStatsResultBO;
import online.longlian.app.pojo.bo.admin.StorageStateStatsResultBO;
import online.longlian.app.pojo.bo.admin.StorageStatsResultBO;
import online.longlian.app.pojo.bo.common.PageParamsBO;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.dto.admin.StorageStatsQueryDTO;
import online.longlian.app.pojo.vo.admin.StorageCategoryStatsVO;
import online.longlian.app.pojo.vo.admin.StorageStateStatsVO;
import online.longlian.app.pojo.vo.admin.StorageStatsVO;
import online.longlian.app.pojo.vo.common.PageResultVO;
import online.longlian.app.service.admin.storage.StorageStatsService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
        PageResultBO<StorageStatsResultBO> page = storageStatsService.list(new AdminOrganizationListParamsBO(
                dto.getOrgName(), null, null, new PageParamsBO(dto.getPageNum(), dto.getPageSize())));
        List<StorageStatsVO> statsVOList = page.getList().stream().map(bo -> {
            StorageStatsVO statsVO = new StorageStatsVO();
            BeanUtils.copyProperties(bo, statsVO, "cover", "thumbnail", "taskSubmit");
            statsVO.setCover(toCategoryStatsVO(bo.getCover()));
            statsVO.setThumbnail(toCategoryStatsVO(bo.getThumbnail()));
            statsVO.setTaskSubmit(toCategoryStatsVO(bo.getTaskSubmit()));
            return statsVO;
        }).toList();
        return new PageResultVO<>(statsVOList, page.getTotal());
    }

    private StorageCategoryStatsVO toCategoryStatsVO(StorageCategoryStatsResultBO categoryBO) {
        StorageCategoryStatsVO categoryVO = new StorageCategoryStatsVO();
        BeanUtils.copyProperties(categoryBO, categoryVO, "unbound", "active", "retention", "recycled");
        categoryVO.setUnbound(toStateStatsVO(categoryBO.getUnbound()));
        categoryVO.setActive(toStateStatsVO(categoryBO.getActive()));
        categoryVO.setRetention(toStateStatsVO(categoryBO.getRetention()));
        categoryVO.setRecycled(toStateStatsVO(categoryBO.getRecycled()));
        return categoryVO;
    }

    private StorageStateStatsVO toStateStatsVO(StorageStateStatsResultBO stateBO) {
        StorageStateStatsVO stateVO = new StorageStateStatsVO();
        BeanUtils.copyProperties(stateBO, stateVO);
        return stateVO;
    }
}
