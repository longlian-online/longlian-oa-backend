package online.longlian.app.pojo.bo.productrelease;

import lombok.AllArgsConstructor;
import lombok.Data;
import online.longlian.app.pojo.bo.common.PageParamsBO;
import online.longlian.common.enumeration.ProductReleaseStatus;

@Data
@AllArgsConstructor
public class ProductReleaseListParamsBO {
    private ProductReleaseStatus status;
    private PageParamsBO page;
}
