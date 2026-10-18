package online.longlian.app.pojo.dto.orgadmin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import online.longlian.app.pojo.dto.common.PageRequestDTO;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "删除申请列表查询")
public class DeletionRequestListDTO extends PageRequestDTO {
}
