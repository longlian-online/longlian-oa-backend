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
    private StorageStateStatsVO unbound;
    private StorageStateStatsVO active;
    private StorageStateStatsVO retention;
    private StorageStateStatsVO recycled;
}
