package online.longlian.app.service.productrelease;

import online.longlian.app.pojo.bo.common.PageParamsBO;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.bo.productrelease.ProductReleaseBaselineResultBO;
import online.longlian.app.pojo.bo.productrelease.ProductReleaseCurrentResultBO;
import online.longlian.app.pojo.bo.productrelease.ProductReleaseDetailResultBO;
import online.longlian.app.pojo.bo.productrelease.ProductReleaseListItemResultBO;
import online.longlian.app.pojo.bo.productrelease.ProductReleaseListParamsBO;
import online.longlian.app.pojo.bo.productrelease.ProductReleaseNoticeResultBO;
import online.longlian.app.pojo.bo.productrelease.ProductReleaseSaveParamsBO;
import online.longlian.app.pojo.bo.productrelease.ProductReleaseUpdateParamsBO;

public interface ProductReleaseService {

    Long create(ProductReleaseSaveParamsBO params);

    void update(ProductReleaseUpdateParamsBO params);

    void publish(Long id);

    void withdraw(Long id);

    ProductReleaseDetailResultBO getAdmin(Long id);

    PageResultBO<ProductReleaseListItemResultBO> listAdmin(ProductReleaseListParamsBO params);

    ProductReleaseBaselineResultBO baseline();

    ProductReleaseCurrentResultBO current(Long userId);

    PageResultBO<ProductReleaseNoticeResultBO> listPublished(PageParamsBO page);

    void markSeen(Long userId, Long releaseId);
}
