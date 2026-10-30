package online.longlian.app.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.annotation.ResponseMessage;
import online.longlian.app.pojo.bo.common.PageParamsBO;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.bo.productrelease.ProductReleaseBaselineResultBO;
import online.longlian.app.pojo.bo.productrelease.ProductReleaseDetailResultBO;
import online.longlian.app.pojo.bo.productrelease.ProductReleaseListItemResultBO;
import online.longlian.app.pojo.bo.productrelease.ProductReleaseListParamsBO;
import online.longlian.app.pojo.bo.productrelease.ProductReleaseSaveParamsBO;
import online.longlian.app.pojo.bo.productrelease.ProductReleaseUpdateParamsBO;
import online.longlian.app.pojo.dto.admin.ProductReleaseListDTO;
import online.longlian.app.pojo.dto.admin.ProductReleaseSaveDTO;
import online.longlian.app.pojo.vo.admin.ProductReleaseBaselineVO;
import online.longlian.app.pojo.vo.admin.ProductReleaseDetailVO;
import online.longlian.app.pojo.vo.admin.ProductReleaseListItemVO;
import online.longlian.app.pojo.vo.common.PageResultVO;
import online.longlian.app.service.productrelease.ProductReleaseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "产品版本", description = "系统管理员维护面向用户的更新说明")
@RequestMapping("/admin/product-releases")
@RestController("adminProductReleaseController")
@RequiredArgsConstructor
public class ProductReleaseController {

    private final ProductReleaseService productReleaseService;

    @Operation(summary = "分页查询产品版本")
    @GetMapping("/")
    @ResponseMessage("ok")
    public PageResultVO<ProductReleaseListItemVO> list(@Valid ProductReleaseListDTO dto) {
        PageResultBO<ProductReleaseListItemResultBO> page = productReleaseService.listAdmin(
                new ProductReleaseListParamsBO(dto.getStatus(), new PageParamsBO(dto.getPageNum(), dto.getPageSize())));
        List<ProductReleaseListItemVO> list = page.getList().stream().map(ProductReleaseController::toListItem).toList();
        return new PageResultVO<>(list, page.getTotal());
    }

    @Operation(summary = "当前已发布产品版本绑定的两端版本")
    @GetMapping("/baseline")
    @ResponseMessage("ok")
    public ProductReleaseBaselineVO baseline() {
        ProductReleaseBaselineResultBO baseline = productReleaseService.baseline();
        if (baseline == null) {
            return null;
        }
        return new ProductReleaseBaselineVO(baseline.getFrontendVersion(), baseline.getBackendVersion());
    }

    @Operation(summary = "产品版本详情")
    @GetMapping("/{id}")
    @ResponseMessage("ok")
    public ProductReleaseDetailVO detail(@PathVariable Long id) {
        return toDetail(productReleaseService.getAdmin(id));
    }

    @Operation(summary = "新建产品版本")
    @PostMapping("/")
    @ResponseMessage("已保存")
    public Long create(@RequestBody @Valid ProductReleaseSaveDTO dto) {
        return productReleaseService.create(toSaveParams(dto));
    }

    @Operation(summary = "修改产品版本")
    @PutMapping("/{id}")
    @ResponseMessage("已保存")
    public void update(@PathVariable Long id, @RequestBody @Valid ProductReleaseSaveDTO dto) {
        productReleaseService.update(toUpdateParams(id, dto));
    }

    @Operation(summary = "发布草稿")
    @PostMapping("/{id}/publish")
    @ResponseMessage("已发布")
    public void publish(@PathVariable Long id) {
        productReleaseService.publish(id);
    }

    @Operation(summary = "撤回已发布的产品版本")
    @PostMapping("/{id}/withdraw")
    @ResponseMessage("已撤回")
    public void withdraw(@PathVariable Long id) {
        productReleaseService.withdraw(id);
    }

    private static ProductReleaseSaveParamsBO toSaveParams(ProductReleaseSaveDTO dto) {
        return ProductReleaseSaveParamsBO.builder()
                .version(dto.getVersion())
                .frontendVersion(dto.getFrontendVersion())
                .backendVersion(dto.getBackendVersion())
                .title(dto.getTitle())
                .summary(dto.getSummary())
                .added(dto.getAdded())
                .changed(dto.getChanged())
                .fixed(dto.getFixed())
                .publish(Boolean.TRUE.equals(dto.getPublish()))
                .build();
    }

    private static ProductReleaseUpdateParamsBO toUpdateParams(Long id, ProductReleaseSaveDTO dto) {
        return ProductReleaseUpdateParamsBO.builder()
                .id(id)
                .version(dto.getVersion())
                .frontendVersion(dto.getFrontendVersion())
                .backendVersion(dto.getBackendVersion())
                .title(dto.getTitle())
                .summary(dto.getSummary())
                .added(dto.getAdded())
                .changed(dto.getChanged())
                .fixed(dto.getFixed())
                .build();
    }

    private static ProductReleaseDetailVO toDetail(ProductReleaseDetailResultBO result) {
        return ProductReleaseDetailVO.builder()
                .id(result.getId())
                .version(result.getVersion())
                .frontendVersion(result.getFrontendVersion())
                .backendVersion(result.getBackendVersion())
                .title(result.getTitle())
                .summary(result.getSummary())
                .status(result.getStatus())
                .publishedAt(result.getPublishedAt())
                .updatedAt(result.getUpdatedAt())
                .added(result.getAdded())
                .changed(result.getChanged())
                .fixed(result.getFixed())
                .build();
    }

    private static ProductReleaseListItemVO toListItem(ProductReleaseListItemResultBO result) {
        return ProductReleaseListItemVO.builder()
                .id(result.getId())
                .version(result.getVersion())
                .frontendVersion(result.getFrontendVersion())
                .backendVersion(result.getBackendVersion())
                .title(result.getTitle())
                .status(result.getStatus())
                .publishedAt(result.getPublishedAt())
                .updatedAt(result.getUpdatedAt())
                .build();
    }
}
