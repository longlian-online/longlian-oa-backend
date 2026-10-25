package online.longlian.app.pojo.dto.app;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "保存企划内全部项目顺序")
public class ProjectItemReorderDTO {
    @NotNull(message = "原项目顺序不能为空")
    @ArraySchema(schema = @Schema(type = "string", description = "项目ID"))
    private List<@NotNull @Positive Long> expectedItemIds;

    @NotNull(message = "项目顺序不能为空")
    @ArraySchema(schema = @Schema(type = "string", description = "项目ID"))
    private List<@NotNull @Positive Long> itemIds;
}
