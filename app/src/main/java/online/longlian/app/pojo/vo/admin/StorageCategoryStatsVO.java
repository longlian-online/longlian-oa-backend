package online.longlian.app.pojo.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "一类资源的四种存储状态")
public class StorageCategoryStatsVO {

    @Schema(description = "未绑定业务对象的资源")
    private StorageStateStatsVO unbound;

    @Schema(description = "已被业务对象使用的资源")
    private StorageStateStatsVO active;

    @Schema(description = "保留期内待回收的资源")
    private StorageStateStatsVO retention;

    @Schema(description = "已物理回收的资源")
    private StorageStateStatsVO recycled;
}
