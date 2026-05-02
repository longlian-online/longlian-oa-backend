package online.longlian.app.pojo.vo.orgadmin;

import online.longlian.app.common.annotation.JsonLongIdString;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import online.longlian.generator.enumeration.Status;

import java.time.LocalDateTime;

@Data
@Schema(description = "原子任务信息")
public class BaseTaskVO {

    @JsonLongIdString
    @Schema(type = "string", description = "原子任务ID")
    private Long id;

    @Schema(description = "任务名称")
    private String name;

    @Schema(description = "任务说明")
    private String description;

    @Schema(description = "图标URL")
    private String iconUrl;

    @Schema(description = "元数据字段定义(JSON数组)")
    private String metaSchema;

    @Schema(description = "引用次数（该任务被引用到任务流模板节点中的次数）")
    private Integer refCount;

    @Schema(description = "状态：ENABLED-启用，DISABLED-禁用")
    private Status status;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
