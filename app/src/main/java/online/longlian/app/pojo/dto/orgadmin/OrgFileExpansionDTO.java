package online.longlian.app.pojo.dto.orgadmin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "展开选中的组织文件")
public class OrgFileExpansionDTO {

    @Valid
    @NotEmpty(message = "请选择要展开的文件")
    @Size(max = 100, message = "一次最多选择 100 项")
    @Schema(description = "选中的文件或文件夹", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<OrgFileBatchTargetDTO> targets;
}
