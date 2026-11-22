package online.longlian.app.pojo.vo.open;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 开放接口统一游标分页结果。不提供跳页，调用方顺序翻页。
 */
@Data
@lombok.AllArgsConstructor
@Schema(description = "开放接口游标分页结果")
public class OpenPageVO<T> {

    @Schema(description = "当前页数据")
    private List<T> list;

    @Schema(description = "下一页游标，hasMore 为 false 时为 null")
    private String nextCursor;

    @Schema(description = "是否还有下一页")
    private boolean hasMore;
}
