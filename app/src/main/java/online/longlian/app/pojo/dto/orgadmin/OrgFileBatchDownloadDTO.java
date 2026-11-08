package online.longlian.app.pojo.dto.orgadmin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "组织文件页批量下载")
public class OrgFileBatchDownloadDTO {

    @Valid
    @NotEmpty(message = "请选择要下载的文件")
    @Size(max = 100, message = "一次最多选择 100 项")
    @Schema(description = "选中的文件或文件夹", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<OrgFileBatchTargetDTO> targets;
}
