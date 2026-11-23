package online.longlian.app.pojo.vo.orgadmin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "展开后的提交文件")
public class OrgFileExpansionVO {

    @Schema(description = "当前有效且可用的提交文件。不可用的不在其中")
    private List<OrgFileBatchFileVO> files;
}
