package online.longlian.app.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.annotation.ResponseMessage;
import online.longlian.app.common.annotation.UserSession;
import online.longlian.app.common.enumeration.OrganizationDeclaration;
import online.longlian.app.common.resolver.SessionContext;
import online.longlian.app.pojo.bo.common.PageParamsBO;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.bo.productrelease.ProductReleaseCurrentResultBO;
import online.longlian.app.pojo.bo.productrelease.ProductReleaseNoticeResultBO;
import online.longlian.app.pojo.dto.common.PageRequestDTO;
import online.longlian.app.pojo.vo.app.ProductReleaseCurrentVO;
import online.longlian.app.pojo.vo.app.ProductReleaseNoticeVO;
import online.longlian.app.pojo.vo.common.PageResultVO;
import online.longlian.app.service.productrelease.ProductReleaseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "更新说明", description = "用户查看产品版本更新说明")
@RequestMapping("/app/product-releases")
@RestController("appProductReleaseController")
@RequiredArgsConstructor
public class ProductReleaseController {

    private final ProductReleaseService productReleaseService;

    @Operation(summary = "当前未读的最新更新说明")
    @GetMapping("/current")
    @ResponseMessage("ok")
    public ProductReleaseCurrentVO current(@UserSession(OrganizationDeclaration.NONE) SessionContext sessionContext) {
        ProductReleaseCurrentResultBO current = productReleaseService.current(sessionContext.userId());
        return new ProductReleaseCurrentVO(toNotice(current.getRelease()), current.getOlderCount());
    }

    @Operation(summary = "已发布的更新说明")
    @GetMapping("/")
    @UserSession(OrganizationDeclaration.NONE)
    @ResponseMessage("ok")
    public PageResultVO<ProductReleaseNoticeVO> list(@Valid PageRequestDTO dto) {
        PageResultBO<ProductReleaseNoticeResultBO> page = productReleaseService.listPublished(
                new PageParamsBO(dto.getPageNum(), dto.getPageSize()));
        List<ProductReleaseNoticeVO> list = page.getList().stream().map(ProductReleaseController::toNotice).toList();
        return new PageResultVO<>(list, page.getTotal());
    }

    @Operation(summary = "把已读位置移到这条已发布更新")
    @PostMapping("/{id}/seen")
    @ResponseMessage("ok")
    public void seen(@UserSession(OrganizationDeclaration.NONE) SessionContext sessionContext, @PathVariable Long id) {
        productReleaseService.markSeen(sessionContext.userId(), id);
    }

    private static ProductReleaseNoticeVO toNotice(ProductReleaseNoticeResultBO result) {
        if (result == null) {
            return null;
        }
        return ProductReleaseNoticeVO.builder()
                .id(result.getId())
                .version(result.getVersion())
                .title(result.getTitle())
                .summary(result.getSummary())
                .publishedAt(result.getPublishedAt())
                .added(result.getAdded())
                .changed(result.getChanged())
                .fixed(result.getFixed())
                .build();
    }
}
