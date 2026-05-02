package online.longlian.app.pojo.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import online.longlian.app.pojo.dto.common.PageRequestDTO;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "管理员列表查询请求参数")
public class AdminListDTO extends PageRequestDTO {
}
