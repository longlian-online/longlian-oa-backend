package online.longlian.app.pojo.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import online.longlian.app.pojo.dto.common.PageRequestDTO;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "存储统计查询参数")
public class StorageStatsQueryDTO extends PageRequestDTO {

    @Size(max = 32, message = "组织名长度不能超过 32 个字符")
    @Schema(description = "组织名")
    private String orgName;
}
