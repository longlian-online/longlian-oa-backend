package online.longlian.app.pojo.dto.open;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import online.longlian.app.common.enumeration.PlazaStageScope;

@Data
@Schema(description = "任务广场键集列表请求")
public class OpenTaskPlazaListDTO {

    @Positive(message = "任务类型ID必须为正整数")
    @Schema(type = "string", description = "原子任务ID，空表示全部任务类型")
    private Long baseTaskId;

    @Size(max = 50, message = "搜索关键词长度不能超过50")
    @Schema(description = "搜索关键词（匹配企划名、条目名、任务名）")
    private String keyword;

    @Schema(description = "阶段范围，默认 CURRENT；ALL 包含后续阶段")
    private PlazaStageScope stageScope;

    @Schema(description = "每页企划数，默认 50，上限 200")
    @Min(1)
    @Max(200)
    private Integer limit;

    @Schema(description = "上一页返回的游标，首页不传")
    private String cursor;
}
