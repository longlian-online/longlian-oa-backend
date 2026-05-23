package online.longlian.app.pojo.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import online.longlian.app.pojo.dto.common.PageRequestDTO;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "工坊任务流模板查询请求参数")
public class WorkshopTaskTemplateDTO extends PageRequestDTO {

    @Size(max = 50, message = "搜索关键词长度不能超过50")
    @Schema(description = "搜索关键词（模板名称）")
    private String keyword;

    @Schema(description = "仅查看我创建的模板")
    private Boolean isMyCreated;
}
