package online.longlian.app.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.Getter;

@Data
@Getter
@Schema(description = "查询请求参数")
public class PageRequestDTO {
    @Min(value = 1, message = "页码不能小于1")
    @Schema(description = "当前页码")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = 100, message = "每页条数最大为100")
    @Schema(description = "每页条数")
    private Integer pageSize = 10;
}
