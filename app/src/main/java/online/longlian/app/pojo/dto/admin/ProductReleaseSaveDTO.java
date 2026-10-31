package online.longlian.app.pojo.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "保存产品版本")
public class ProductReleaseSaveDTO {

    private static final String VERSION = "^[vV]?(0|[1-9]\\d{0,6})\\.(0|[1-9]\\d{0,6})\\.(0|[1-9]\\d{0,6})$";

    @NotBlank(message = "产品版本不能为空")
    @Pattern(regexp = VERSION, message = "版本格式不正确")
    private String version;

    @NotBlank(message = "前端版本不能为空")
    @Pattern(regexp = VERSION, message = "版本格式不正确")
    private String frontendVersion;

    @NotBlank(message = "后端版本不能为空")
    @Pattern(regexp = VERSION, message = "版本格式不正确")
    private String backendVersion;

    @Size(max = 40, message = "标题最多 40 字")
    private String title;

    @Size(max = 120, message = "摘要最多 120 字")
    private String summary;

    @Size(max = 4000, message = "更新说明最多 4000 字")
    @Schema(description = "更新说明，Markdown")
    private String content;

    @Schema(description = "为 true 时创建并发布；更新接口忽略")
    private Boolean publish;
}
