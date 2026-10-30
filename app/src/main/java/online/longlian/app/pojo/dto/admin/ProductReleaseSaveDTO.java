package online.longlian.app.pojo.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

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

    @Size(max = 20, message = "新增最多 20 条")
    private List<@NotBlank(message = "新增不能有空白条目") @Size(max = 80, message = "新增每条最多 80 字") String> added;

    @Size(max = 20, message = "调整最多 20 条")
    private List<@NotBlank(message = "调整不能有空白条目") @Size(max = 80, message = "调整每条最多 80 字") String> changed;

    @Size(max = 20, message = "修复最多 20 条")
    private List<@NotBlank(message = "修复不能有空白条目") @Size(max = 80, message = "修复每条最多 80 字") String> fixed;

    @Schema(description = "为 true 时创建并发布；更新接口忽略")
    private Boolean publish;
}
