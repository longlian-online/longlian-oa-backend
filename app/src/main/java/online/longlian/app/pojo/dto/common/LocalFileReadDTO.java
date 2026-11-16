package online.longlian.app.pojo.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "本地文件读取参数")
public class LocalFileReadDTO {

    @NotBlank
    @Schema(description = "文件存储标识（storageKey）")
    private String key;

    @NotNull
    @Schema(description = "链接过期时间（Unix 秒）")
    private Long expires;

    @NotBlank
    @Pattern(regexp = "[0-9a-f]{64}")
    @Schema(description = "链接签名")
    private String signature;
}
