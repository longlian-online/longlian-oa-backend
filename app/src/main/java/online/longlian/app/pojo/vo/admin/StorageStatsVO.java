package online.longlian.app.pojo.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.app.common.annotation.JsonLongIdString;
import online.longlian.common.enumeration.Status;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "组织存储统计")
public class StorageStatsVO {
    @JsonLongIdString
    @Schema(type = "string", description = "组织ID")
    private Long id;

    @Schema(description = "组织名称")
    private String name;

    @Schema(description = "组织状态")
    private Status status;

    @Schema(description = "仍占用的字节，不含已回收和头像")
    private long occupiedBytes;

    @Schema(description = "已物理回收的字节，不含头像")
    private long reclaimedBytes;

    private StorageCategoryStatsVO cover;
    private StorageCategoryStatsVO thumbnail;
    private StorageCategoryStatsVO taskSubmit;
}
