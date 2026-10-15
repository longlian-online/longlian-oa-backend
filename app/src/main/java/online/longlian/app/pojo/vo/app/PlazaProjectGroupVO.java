package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.app.common.annotation.JsonLongIdString;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "缺人任务企划分组")
public class PlazaProjectGroupVO {

    @JsonLongIdString
    @Schema(type = "string", description = "企划ID")
    private Long projectId;

    @Schema(description = "企划标题")
    private String title;

    @Schema(description = "企划类型名称")
    private String typeName;

    @Schema(description = "封面图URL")
    private String coverUrl;

    @Schema(description = "缩略图URL")
    private String thumbnailUrl;

    @Schema(description = "缺人任务总数")
    private Integer pendingCount;

    @Schema(description = "涉及条目数")
    private Integer itemCount;

    @Schema(description = "缺人任务列表")
    private List<PlazaTaskVO> tasks;
}
