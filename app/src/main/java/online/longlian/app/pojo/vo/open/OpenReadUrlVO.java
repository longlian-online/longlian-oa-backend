package online.longlian.app.pojo.vo.open;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 开放面按次签发的读取链接。字段冻结：只增不删。
 */
@Data
@AllArgsConstructor
@Schema(description = "开放接口读取链接")
public class OpenReadUrlVO {

    @Schema(description = "读取链接，短期有效，过期后重新签发")
    private String readUrl;

    @Schema(description = "到期时间（Unix 秒）")
    private Long expireAt;
}
