package online.longlian.app.pojo.vo.orgadmin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "组织文件页批量下载的读取链接")
public class OrgFileBatchDownloadVO {

    @Schema(description = "可下载的提交文件。不可用的文件不在其中")
    private List<OrgFileBatchFileVO> files;
}
