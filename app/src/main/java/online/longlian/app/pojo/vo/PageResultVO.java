package online.longlian.app.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@Data
@AllArgsConstructor
@Schema(description = "通用分页返回结果")
public class PageResultVO<T> {

    @Schema(description = "数据列表")
    private List<T> list;

    @Schema(description = "总条数")
    private Long total;

    @Schema(description = "当前页码")
    private Integer pageNum;

    @Schema(description = "每页条数")
    private Integer pageSize;

    @Schema(description = "是否有下一页")
    private Boolean hasNext;
}