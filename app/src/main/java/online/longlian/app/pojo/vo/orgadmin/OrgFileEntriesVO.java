package online.longlian.app.pojo.vo.orgadmin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import online.longlian.app.common.annotation.JsonLongIdString;

import java.util.List;

@Data
@Schema(description = "组织文件页当前文件夹")
public class OrgFileEntriesVO {
    @JsonLongIdString
    @Schema(type = "string", description = "当前企划类型 ID；非类型分层或类型根目录为空")
    private Long typeId;

    @Schema(description = "当前企划类型名称")
    private String typeName;

    @JsonLongIdString
    @Schema(type = "string", description = "当前企划 ID；根目录为空")
    private Long projectId;

    @Schema(description = "当前企划名称")
    private String projectTitle;

    @JsonLongIdString
    @Schema(type = "string", description = "当前项目 ID；企划目录为空")
    private Long itemId;

    @Schema(description = "当前项目名称")
    private String itemTitle;

    @Schema(description = "当前文件夹的条目总数")
    private Long total;

    @Schema(description = "当前页的条目")
    private List<OrgFileEntryVO> entries;
}
