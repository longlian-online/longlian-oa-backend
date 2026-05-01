package online.longlian.app.pojo.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "删除管理员请求参数")
public class AdminDeleteDTO {

    @NotNull(message = "管理员ID不能为空")
    @Schema(description = "管理员ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;
}
