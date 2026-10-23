package online.longlian.app.pojo.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "一种存储状态的数量和字节")
public class StorageStateStatsVO {
    @Schema(description = "文件数")
    private long count;

    @Schema(description = "字节数")
    private long bytes;
}
