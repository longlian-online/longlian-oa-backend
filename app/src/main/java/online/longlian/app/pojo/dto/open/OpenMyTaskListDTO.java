package online.longlian.app.pojo.dto.open;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import online.longlian.app.common.enumeration.MyTaskBucket;

@Data
@Schema(description = "我的待办键集列表请求")
public class OpenMyTaskListDTO {

    @Schema(description = "分组：SUBMITTABLE-可提交，WAITING-等前置；不传返回全部")
    private MyTaskBucket bucket;

    @Schema(description = "每页数量，默认 50，上限 200")
    @Min(1)
    @Max(200)
    private Integer limit;

    @Schema(description = "上一页返回的游标，首页不传")
    private String cursor;
}
