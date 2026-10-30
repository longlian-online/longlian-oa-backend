package online.longlian.app.pojo.dto.orgadmin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "组员备注")
public class OrgMemberNoteDTO {

    @NotNull(message = "备注不能为空")
    @Size(max = 500, message = "备注不能超过 500 个字符")
    @Schema(description = "管理员备注。空白表示清空，最多 500 个字符")
    private String note;
}
