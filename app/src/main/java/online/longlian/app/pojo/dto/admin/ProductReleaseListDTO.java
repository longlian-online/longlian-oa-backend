package online.longlian.app.pojo.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import online.longlian.app.pojo.dto.common.PageRequestDTO;
import online.longlian.common.enumeration.ProductReleaseStatus;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "产品版本列表查询")
public class ProductReleaseListDTO extends PageRequestDTO {

    @Schema(description = "状态，不传则全部")
    private ProductReleaseStatus status;
}
