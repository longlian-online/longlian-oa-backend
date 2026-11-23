package online.longlian.app.pojo.vo.open;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 开放面企划类型。字段冻结：只增不删。
 */
@Data
@AllArgsConstructor
@Schema(description = "开放接口企划类型")
public class OpenProjectTypeVO {

    @Schema(description = "类型ID")
    private Long id;

    @Schema(description = "类型名称")
    private String name;
}
